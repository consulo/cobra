package org.cobraparser.html.renderer;

import org.cobraparser.html.style.RenderState;
import org.w3c.dom.Node;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Maps between screen positions and {@link SelectionPoint}s, and paints the selection.
 * <p>
 * Everything walks the renderable tree with the same coordinate transforms as regular painting,
 * so whatever is hit or painted matches what is on screen after any relayout. Only the visible
 * part of the tree is walked, so the cost does not grow with the length of the document.
 */
public final class SelectionGeometry {
    private static final int REPLACED_ELEMENT_ALPHA = 96;

    private SelectionGeometry() {
    }

    /**
     * Finds the selection point for a position in the coordinates of the root block.
     * Positions outside of any text resolve to the nearest visible text, so dragging past
     * the end of a line, between blocks or outside of the panel still selects sensibly.
     *
     * @param caret true to get the character boundary nearest to the position (for drag selection),
     *              false to get the start of the character under the position (for word and block selection)
     * @return the point, or null if there is no visible text or replaced element
     */
    public static SelectionPoint hitTest(RBlock root, int x, int y, boolean caret) {
        HitTester hitTester = new HitTester(x, y);
        walkRoot(root, null, hitTester);
        return hitTester.getResult(x, caret);
    }

    /**
     * Paints the selection between two points, in any order, over the already painted root block.
     */
    public static void paint(Graphics g, RBlock root, SelectionPoint a, SelectionPoint b, Color background, Color foreground) {
        Range range = Range.of(new DomOrder(), a, b);
        if (range != null) {
            walkRoot(root, g.getClipBounds(), new Painter(g, range, background, foreground));
        }
    }

    /**
     * Returns the visible area whose selection highlight differs between two selections, so that only it
     * has to be repainted. Either selection may be given with null points when there is none.
     *
     * @return the area, or null if nothing visible changes
     */
    public static Rectangle getChangedArea(RBlock root, SelectionPoint oldA, SelectionPoint oldB, SelectionPoint newA,
                                           SelectionPoint newB) {
        DomOrder order = new DomOrder();
        Range oldRange = Range.of(order, oldA, oldB);
        Range newRange = Range.of(order, newA, newB);
        if (oldRange == null && newRange == null) {
            return null;
        }
        ChangeCollector collector = new ChangeCollector(oldRange, newRange);
        walkRoot(root, null, collector);
        return collector.myArea;
    }

    private interface LeafVisitor {
        /**
         * @param x    absolute x of the leaf
         * @param y    absolute y of the leaf
         * @param clip absolute area in which the leaf is visible
         * @param line absolute bounds of the line containing the leaf, or null if it is not on a line
         */
        void visit(BoundableRenderable leaf, int x, int y, Rectangle clip, Rectangle line);
    }

    /**
     * @param area the part of the root to visit, or null for all of its visible area
     */
    private static void walkRoot(RBlock root, Rectangle area, LeafVisitor visitor) {
        Rectangle clip = new Rectangle(0, 0, root.getWidth(), root.getHeight());
        if (area != null) {
            clip = clip.intersection(area);
            if (clip.isEmpty()) {
                return;
            }
        }
        walk(root, root.getX(), root.getY(), clip, null, visitor);
    }

    private static void walk(RCollection collection, int x, int y, Rectangle parentClip, Rectangle line, LeafVisitor visitor) {
        Rectangle clip = parentClip;
        if (collection instanceof RBlock block) {
            Insets insets = block.getInsetsMarginBorder(block.hasHScrollBar, block.hasVScrollBar);
            Rectangle content = new Rectangle(x + insets.left, y + insets.top, block.getWidth() - insets.left - insets.right,
                block.getHeight() - insets.top - insets.bottom);
            clip = content.intersection(parentClip);
            if (clip.isEmpty()) {
                return;
            }
        }

        Iterator<? extends Renderable> renderables = collection.getRenderables(false);
        if (renderables == null) {
            return;
        }
        while (renderables.hasNext()) {
            Renderable renderable = renderables.next();
            BoundableRenderable child;
            Rectangle bounds;
            if (renderable instanceof PositionedRenderable positioned) {
                if (positioned.isDelegated()) {
                    continue;
                }
                child = positioned.renderable;
                bounds = positioned.getVisualBounds();
            }
            else if (renderable instanceof BoundableRenderable boundable) {
                child = boundable;
                bounds = boundable.getVisualBounds();
            }
            else {
                continue;
            }
            if (child instanceof TranslatedRenderable translated && translated.getChild() instanceof BoundableRenderable translatedChild) {
                child = translatedChild;
            }

            int childX = x + bounds.x;
            int childY = y + bounds.y;
            if (child instanceof RLine childLine) {
                Rectangle lineBounds = new Rectangle(childX, childY, childLine.getWidth(), childLine.getHeight());
                // A line holds all of its content, so a line outside of the clip has nothing visible.
                if (lineBounds.intersects(clip)) {
                    walk(childLine, childX, childY, clip, lineBounds, visitor);
                }
            }
            else if (child instanceof RBlock childBlock) {
                // A block clips its content to its bounds, see RBlock.paint().
                if (clip.intersects(childX, childY, childBlock.getWidth(), childBlock.getHeight())) {
                    walk(childBlock, childX, childY, clip, null, visitor);
                }
            }
            else if (child instanceof RCollection childCollection && !(child instanceof RUIControl)) {
                walk(childCollection, childX, childY, clip, line, visitor);
            }
            else {
                visitor.visit(child, childX, childY, clip, line);
            }
        }
    }

    private static boolean isVisible(BoundableRenderable leaf) {
        RenderState state = leaf.getModelNode().getRenderState();
        return state == null || state.getVisibility() == RenderState.VISIBILITY_VISIBLE;
    }

    /**
     * Width of the first {@code count} shown characters of a word.
     */
    private static int prefixWidth(RWord word, int count) {
        if (count <= 0) {
            return 0;
        }
        if (count >= word.shownWord.length()) {
            return word.getWidth();
        }
        return word.fontMetrics.charsWidth(word.shownWord.toCharArray(), 0, count);
    }

    /**
     * Converts an offset in the text node to an index into the shown word.
     */
    private static int shownIndex(RWord word, int textOffset) {
        int shownLength = word.shownWord.length();
        if (textOffset >= word.textEnd) {
            return shownLength;
        }
        return Math.max(0, Math.min(shownLength, textOffset - word.textStart));
    }

    /**
     * A non-empty selection in document order, deciding which part of each leaf it covers.
     */
    private static final class Range {
        private final DomOrder myOrder;
        private final SelectionPoint myStart;
        private final SelectionPoint myEnd;

        private Range(DomOrder order, SelectionPoint start, SelectionPoint end) {
            myOrder = order;
            myStart = start;
            myEnd = end;
        }

        /**
         * @return the range between the points in any order, or null if a point is missing or both are the same
         */
        static Range of(DomOrder order, SelectionPoint a, SelectionPoint b) {
            if (a == null || b == null) {
                return null;
            }
            int comparison = order.compare(a, b);
            if (comparison == 0) {
                return null;
            }
            return comparison < 0 ? new Range(order, a, b) : new Range(order, b, a);
        }

        /**
         * @return the selected text offsets {@code [from, to)} of a word, or null if none of it is selected
         */
        int[] getSelectedText(RWord word) {
            if (word.textStart == word.textEnd || !intersects((Node) word.getModelNode(), word.textStart, word.textEnd)) {
                return null;
            }
            Node text = (Node) word.getModelNode();
            int from = myStart.getNode() == text ? Math.max(word.textStart, myStart.getOffset()) : word.textStart;
            int to = myEnd.getNode() == text ? Math.min(word.textEnd, myEnd.getOffset()) : word.textEnd;
            return from < to ? new int[]{from, to} : null;
        }

        /**
         * @return whether a leaf other than a word is highlighted
         */
        boolean covers(BoundableRenderable leaf) {
            if (leaf instanceof RBlank blank) {
                return intersects((Node) blank.getModelNode(), blank.textStart, blank.textEnd);
            }
            Node element = (Node) leaf.getModelNode();
            if (leaf instanceof RSpacing spacing) {
                SelectionPoint position = new SelectionPoint(element, spacing.leading ? 0 : DomOrder.childCount(element));
                return myOrder.compare(myStart, position) < 0 && myOrder.compare(position, myEnd) < 0;
            }
            if (leaf instanceof RUIControl) {
                return myOrder.compare(myStart, myOrder.before(element)) <= 0 && myOrder.compare(myOrder.after(element), myEnd) <= 0;
            }
            return false;
        }

        private boolean intersects(Node text, int from, int to) {
            return myOrder.compare(text, to, myStart.getNode(), myStart.getOffset()) > 0
                && myOrder.compare(text, from, myEnd.getNode(), myEnd.getOffset()) < 0;
        }
    }

    /**
     * Finds the leaf nearest to a position: first by distance to its line, then horizontally.
     */
    private static final class HitTester implements LeafVisitor {
        private final int myX;
        private final int myY;

        private BoundableRenderable myBest;
        private int myBestX;
        private long myBestDistanceY = Long.MAX_VALUE;
        private long myBestDistanceX = Long.MAX_VALUE;

        HitTester(int x, int y) {
            myX = x;
            myY = y;
        }

        @Override
        public void visit(BoundableRenderable leaf, int x, int y, Rectangle clip, Rectangle line) {
            if (!(leaf instanceof RWord || leaf instanceof RBlank || leaf instanceof RUIControl)) {
                return;
            }
            Rectangle bounds = new Rectangle(x, y, leaf.getWidth(), leaf.getHeight());
            if (!clip.intersects(bounds.x, bounds.y, Math.max(1, bounds.width), Math.max(1, bounds.height))) {
                return;
            }
            Rectangle vertical = line != null ? line : bounds;
            long distanceY = distance(myY, vertical.y, vertical.y + vertical.height - 1);
            long distanceX = distance(myX, bounds.x, bounds.x + bounds.width);
            if (distanceY < myBestDistanceY || (distanceY == myBestDistanceY && distanceX < myBestDistanceX)) {
                myBest = leaf;
                myBestX = x;
                myBestDistanceY = distanceY;
                myBestDistanceX = distanceX;
            }
        }

        private static long distance(int value, int from, int to) {
            if (value < from) {
                return (long) from - value;
            }
            if (value > to) {
                return (long) value - to;
            }
            return 0;
        }

        SelectionPoint getResult(int x, boolean caret) {
            BoundableRenderable leaf = myBest;
            if (leaf == null) {
                return null;
            }
            int localX = x - myBestX;
            if (leaf instanceof RTab tab) {
                return point(tab, caret && localX > tab.getWidth() / 2 ? tab.textEnd : tab.textStart);
            }
            if (leaf instanceof RWord word) {
                return point(word, word.textStart + Math.min(characterIndex(word, localX, caret), word.textEnd - word.textStart));
            }
            if (leaf instanceof RBlank blank) {
                return point(blank, caret && localX > blank.getWidth() / 2 ? blank.textEnd : blank.textStart);
            }
            DomOrder order = new DomOrder();
            Node element = (Node) leaf.getModelNode();
            return caret && localX > leaf.getWidth() / 2 ? order.after(element) : order.before(element);
        }

        private static SelectionPoint point(BoundableRenderable leaf, int offset) {
            return new SelectionPoint((Node) leaf.getModelNode(), offset);
        }

        /**
         * @return for a caret the boundary nearest to {@code localX}, otherwise the index of the character under it
         */
        private static int characterIndex(RWord word, int localX, boolean caret) {
            int length = word.shownWord.length();
            if (length == 0 || localX <= 0) {
                return 0;
            }
            if (localX >= word.getWidth()) {
                return caret ? length : length - 1;
            }
            char[] chars = word.shownWord.toCharArray();
            int previousWidth = 0;
            for (int i = 1; i <= length; i++) {
                int width = word.fontMetrics.charsWidth(chars, 0, i);
                if (width > localX) {
                    if (!caret) {
                        return i - 1;
                    }
                    return localX - previousWidth < width - localX ? i - 1 : i;
                }
                previousWidth = width;
            }
            return caret ? length : length - 1;
        }
    }

    /**
     * Collects the visible leaves whose highlight differs between two selections.
     */
    private static final class ChangeCollector implements LeafVisitor {
        private final Range myOld;
        private final Range myNew;
        private Rectangle myArea;

        ChangeCollector(Range oldRange, Range newRange) {
            myOld = oldRange;
            myNew = newRange;
        }

        @Override
        public void visit(BoundableRenderable leaf, int x, int y, Rectangle clip, Rectangle line) {
            boolean changed;
            if (leaf instanceof RWord word) {
                changed = !Arrays.equals(myOld == null ? null : myOld.getSelectedText(word), myNew == null ? null : myNew.getSelectedText(word));
            }
            else {
                changed = (myOld != null && myOld.covers(leaf)) != (myNew != null && myNew.covers(leaf));
            }
            if (changed) {
                Rectangle bounds = new Rectangle(x, y, leaf.getWidth(), leaf.getHeight()).intersection(clip);
                if (!bounds.isEmpty()) {
                    myArea = myArea == null ? bounds : myArea.union(bounds);
                }
            }
        }
    }

    private static final class Painter implements LeafVisitor {
        private final Graphics myGraphics;
        private final Range myRange;
        private final Color myBackground;
        private final Color myForeground;

        Painter(Graphics graphics, Range range, Color background, Color foreground) {
            myGraphics = graphics;
            myRange = range;
            myBackground = background;
            myForeground = foreground;
        }

        @Override
        public void visit(BoundableRenderable leaf, int x, int y, Rectangle clip, Rectangle line) {
            if (!clip.intersects(x, y, leaf.getWidth(), leaf.getHeight())) {
                return;
            }
            if (leaf instanceof RWord word) {
                paintWord(word, x, y, clip);
            }
            else if (myRange.covers(leaf)) {
                Color color = myBackground;
                if (leaf instanceof RUIControl) {
                    color = new Color(myBackground.getRed(), myBackground.getGreen(), myBackground.getBlue(), REPLACED_ELEMENT_ALPHA);
                }
                fill(clip, x, y, leaf.getWidth(), leaf.getHeight(), color);
            }
        }

        private void paintWord(RWord word, int x, int y, Rectangle clip) {
            if (!isVisible(word)) {
                return;
            }
            int[] selected = myRange.getSelectedText(word);
            if (selected == null) {
                return;
            }
            int left = prefixWidth(word, shownIndex(word, selected[0]));
            int right = prefixWidth(word, shownIndex(word, selected[1]));
            if (right <= left) {
                return;
            }

            Graphics g = myGraphics.create();
            try {
                g.clipRect(clip.x, clip.y, clip.width, clip.height);
                g.clipRect(x + left, y, right - left, word.getHeight());
                g.setColor(myBackground);
                g.fillRect(x + left, y, right - left, word.getHeight());
                if (word instanceof RTab) {
                    return;
                }
                // Draw the whole word clipped to the selected part, so glyphs stay exactly where they were painted.
                g.setColor(myForeground);
                g.setFont(word.fontMetrics.getFont());
                g.drawString(word.shownWord, x, y + word.ascentPlusLeading);
                paintDecorations(g, word, x, y);
            }
            finally {
                g.dispose();
            }
        }

        private static void paintDecorations(Graphics g, RWord word, int x, int y) {
            RenderState state = word.getModelNode().getRenderState();
            int decorations = state == null ? 0 : state.getTextDecorationMask();
            if (decorations == 0) {
                return;
            }
            FontMetrics fm = word.fontMetrics;
            int width = word.getWidth();
            if ((decorations & RenderState.MASK_TEXTDECORATION_UNDERLINE) != 0) {
                int lineY = y + word.ascentPlusLeading + 2;
                g.drawLine(x, lineY, x + width, lineY);
            }
            if ((decorations & RenderState.MASK_TEXTDECORATION_LINE_THROUGH) != 0) {
                int lineY = y + fm.getLeading() + ((fm.getAscent() + fm.getDescent()) / 2);
                g.drawLine(x, lineY, x + width, lineY);
            }
            if ((decorations & RenderState.MASK_TEXTDECORATION_OVERLINE) != 0) {
                int lineY = y + fm.getLeading();
                g.drawLine(x, lineY, x + width, lineY);
            }
        }

        private void fill(Rectangle clip, int x, int y, int width, int height, Color color) {
            Graphics g = myGraphics.create();
            try {
                g.clipRect(clip.x, clip.y, clip.width, clip.height);
                g.setColor(color);
                g.fillRect(x, y, width, height);
            }
            finally {
                g.dispose();
            }
        }
    }
}
