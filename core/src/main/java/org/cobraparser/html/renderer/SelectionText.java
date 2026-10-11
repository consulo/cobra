package org.cobraparser.html.renderer;

import org.cobraparser.html.domimpl.ModelNode;
import org.cobraparser.html.style.RenderState;
import org.cobraparser.util.Nodes;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;

import java.text.BreakIterator;
import java.util.Set;

/**
 * Text of a selection, and the ranges selected by double click, triple click and select all.
 * <p>
 * Text is produced from the DOM like a browser's {@code innerText}: whitespace collapses unless it is preformatted,
 * blocks start on new lines, paragraphs are separated by an empty line and table cells by tabs.
 */
public final class SelectionText {
    private static final Set<String> PARAGRAPH_LIKE = Set.of("P", "H1", "H2", "H3", "H4", "H5", "H6", "PRE", "BLOCKQUOTE", "TABLE", "UL", "OL", "DL");
    private static final Set<String> NOT_RENDERED = Set.of("HEAD", "TITLE", "SCRIPT", "STYLE", "NOSCRIPT", "TEMPLATE");

    private SelectionText() {
    }

    /**
     * Returns the text between two points, in any order.
     */
    public static String getText(SelectionPoint a, SelectionPoint b) {
        DomOrder order = new DomOrder();
        SelectionPoint start = order.min(a, b);
        SelectionPoint end = order.max(a, b);
        if (order.compare(start, end) == 0) {
            return "";
        }
        Node root = Nodes.getCommonAncestor(start.getNode(), end.getNode());
        if (root == null) {
            return "";
        }
        Extractor extractor = new Extractor(order, start, end);
        extractor.visit(root);
        return extractor.myText.toString();
    }

    /**
     * Returns the word containing the character at the given point, or the run of whitespace or punctuation there.
     */
    public static SelectionPoint[] getWordRange(SelectionPoint point) {
        Node node = point.getNode();
        String text = node.getNodeType() == Node.TEXT_NODE ? node.getNodeValue() : null;
        if (text == null || text.isEmpty()) {
            return new SelectionPoint[]{point, point};
        }
        int index = Math.max(0, Math.min(point.getOffset(), text.length() - 1));
        BreakIterator words = BreakIterator.getWordInstance();
        words.setText(text);
        int end = words.following(index);
        if (end == BreakIterator.DONE) {
            end = text.length();
        }
        int start = words.previous();
        if (start == BreakIterator.DONE) {
            start = 0;
        }
        return new SelectionPoint[]{new SelectionPoint(node, start), new SelectionPoint(node, end)};
    }

    /**
     * Returns the whole content of the nearest block containing the point, such as a paragraph or a list item.
     */
    public static SelectionPoint[] getBlockRange(SelectionPoint point) {
        Node block = point.getNode();
        while (block != null && !(block.getNodeType() == Node.ELEMENT_NODE && isBlock(block))) {
            block = block.getParentNode();
        }
        if (block == null) {
            return new SelectionPoint[]{point, point};
        }
        return new SelectionPoint[]{new SelectionPoint(block, 0), new SelectionPoint(block, DomOrder.childCount(block))};
    }

    /**
     * Returns the range covering the whole body of the document.
     */
    public static SelectionPoint[] getDocumentRange(Document document) {
        Node root = document instanceof HTMLDocument htmlDocument && htmlDocument.getBody() != null
            ? htmlDocument.getBody()
            : document.getDocumentElement();
        if (root == null) {
            root = document;
        }
        return new SelectionPoint[]{new SelectionPoint(root, 0), new SelectionPoint(root, DomOrder.childCount(root))};
    }

    private static RenderState getRenderState(Node node) {
        return node instanceof ModelNode modelNode ? modelNode.getRenderState() : null;
    }

    private static int getDisplay(Node node) {
        RenderState state = getRenderState(node);
        return state == null ? RenderState.DISPLAY_INLINE : state.getDisplay();
    }

    private static boolean isBlock(Node element) {
        return switch (getDisplay(element)) {
            case RenderState.DISPLAY_BLOCK, RenderState.DISPLAY_LIST_ITEM, RenderState.DISPLAY_TABLE, RenderState.DISPLAY_TABLE_ROW,
                 RenderState.DISPLAY_TABLE_CELL, RenderState.DISPLAY_TABLE_CAPTION, RenderState.DISPLAY_TABLE_ROW_GROUP,
                 RenderState.DISPLAY_TABLE_HEADER_GROUP, RenderState.DISPLAY_TABLE_FOOTER_GROUP -> true;
            default -> false;
        };
    }

    private static final class Extractor {
        private final DomOrder myOrder;
        private final SelectionPoint myStart;
        private final SelectionPoint myEnd;

        private final StringBuilder myText = new StringBuilder();
        private int myPendingBreaks;
        private boolean myPendingTab;
        private boolean myPendingSpace;
        private boolean myDone;

        Extractor(DomOrder order, SelectionPoint start, SelectionPoint end) {
            myOrder = order;
            myStart = start;
            myEnd = end;
        }

        void visit(Node node) {
            if (myDone) {
                return;
            }
            switch (node.getNodeType()) {
                case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> visitText(node);
                case Node.ELEMENT_NODE -> visitElement(node);
                case Node.DOCUMENT_NODE, Node.DOCUMENT_FRAGMENT_NODE -> visitChildren(node);
                default -> {
                }
            }
        }

        private void visitChildren(Node node) {
            NodeList children = node.getChildNodes();
            int length = children == null ? 0 : children.getLength();
            for (int i = 0; i < length && !myDone; i++) {
                visit(children.item(i));
            }
        }

        private void visitElement(Node element) {
            if (NOT_RENDERED.contains(element.getNodeName().toUpperCase()) || getDisplay(element) == RenderState.DISPLAY_NONE) {
                return;
            }
            if (element.getParentNode() != null && myOrder.compare(myOrder.before(element), myEnd) >= 0) {
                myDone = true;
                return;
            }
            String name = element.getNodeName().toUpperCase();
            if ("BR".equals(name)) {
                if (myOrder.compare(myOrder.before(element), myStart) >= 0) {
                    lineBreak();
                }
                return;
            }
            int display = getDisplay(element);
            int breaks = isBlock(element) && display != RenderState.DISPLAY_TABLE_CELL ? (PARAGRAPH_LIKE.contains(name) ? 2 : 1) : 0;
            requestBreaks(breaks);
            visitChildren(element);
            requestBreaks(breaks);
            if (display == RenderState.DISPLAY_TABLE_CELL && myText.length() > 0) {
                myPendingTab = true;
            }
        }

        private void visitText(Node node) {
            String data = node.getNodeValue();
            if (data == null || data.isEmpty()) {
                return;
            }
            int length = data.length();
            if (myOrder.compare(node, 0, myEnd.getNode(), myEnd.getOffset()) >= 0) {
                myDone = true;
                return;
            }
            if (myOrder.compare(node, length, myStart.getNode(), myStart.getOffset()) <= 0) {
                return;
            }
            int from = myStart.getNode() == node ? Math.max(0, Math.min(length, myStart.getOffset())) : 0;
            int to = myEnd.getNode() == node ? Math.max(from, Math.min(length, myEnd.getOffset())) : length;
            RenderState state = getRenderState(node);
            if (state != null && state.getVisibility() != RenderState.VISIBILITY_VISIBLE) {
                return;
            }
            boolean preformatted = state != null && state.getWhiteSpace() == RenderState.WS_PRE;
            for (int i = from; i < to; i++) {
                char c = data.charAt(i);
                if (preformatted) {
                    if (c == '\r') {
                        continue;
                    }
                    flushSeparators();
                    myText.append(c);
                }
                else if (Character.isWhitespace(c)) {
                    myPendingSpace = myText.length() > 0;
                }
                else {
                    flushSeparators();
                    myText.append(c);
                }
            }
        }

        private void requestBreaks(int count) {
            if (myText.length() > 0) {
                myPendingBreaks = Math.max(myPendingBreaks, count);
            }
        }

        private void lineBreak() {
            flushSeparators();
            myText.append('\n');
        }

        private void flushSeparators() {
            if (myPendingBreaks > 0) {
                int existing = 0;
                while (existing < myText.length() && myText.charAt(myText.length() - 1 - existing) == '\n') {
                    existing++;
                }
                for (int i = existing; i < myPendingBreaks; i++) {
                    myText.append('\n');
                }
            }
            else if (myPendingTab) {
                myText.append('\t');
            }
            else if (myPendingSpace && myText.length() > 0 && myText.charAt(myText.length() - 1) != '\n') {
                myText.append(' ');
            }
            myPendingBreaks = 0;
            myPendingTab = false;
            myPendingSpace = false;
        }
    }
}
