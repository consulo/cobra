/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Apr 16, 2005
 */
package org.cobraparser.html.gui;

import org.cobraparser.css.CSSVariableResolver;
import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.*;
import org.cobraparser.html.renderer.*;
import org.cobraparser.html.style.RenderState;
import org.cobraparser.html.style.RenderThreadState;
import org.cobraparser.ua.UserAgentContext;
import org.cobraparser.util.Nodes;
import org.cobraparser.util.gui.ColorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A Swing component that renders a HTML block, given by a DOM root or an
 * internal element, typically a DIV. This component <i>cannot</i> render
 * FRAMESETs. <code>HtmlBlockPanel</code> is used by {@link HtmlPanel} whenever
 * the DOM is determined <i>not</i> to be a FRAMESET.
 *
 * @see HtmlPanel
 * @see FrameSetPanel
 * @author J. H. S.
 */
public class HtmlBlockPanel extends JComponent implements NodeRenderer, RenderableContainer, ClipboardOwner {
  private static final long serialVersionUID = 7851587340938903001L;
  private static final Logger logger = LoggerFactory.getLogger(HtmlBlockPanel.class.getName());
  protected final FrameContext frameContext;
  protected final UserAgentContext ucontext;
  protected final HtmlRendererContext rcontext;

  private static final int AUTOSCROLL_DELAY = 40;
  private static final int AUTOSCROLL_MAX_STEP = 120;

  private enum SelectionUnit {
    CHARACTER, WORD, BLOCK
  }

  /** Where the selection started. */
  protected SelectionPoint startSelection;
  /** Where the selection currently ends; may be before {@link #startSelection}. */
  protected SelectionPoint endSelection;
  private SelectionUnit selectionUnit = SelectionUnit.CHARACTER;
  /** The word or block selected by a double or triple click, kept while dragging extends the selection by whole units. */
  private SelectionPoint[] selectionUnitRange;
  private boolean selecting;
  private boolean dragged;
  private Point pressPoint;
  private Point lastDragPoint;
  private javax.swing.Timer autoscrollTimer;
  protected RBlock rblock;
  protected int preferredWidth = -1;
  protected int defaultOverflowX = RenderState.OVERFLOW_AUTO;
  protected int defaultOverflowY = RenderState.OVERFLOW_SCROLL;
  private volatile boolean scrollCompleted = false;

  public HtmlBlockPanel(final UserAgentContext pcontext, final HtmlRendererContext rcontext, final FrameContext frameContext) {
    this(ColorFactory.TRANSPARENT, false, pcontext, rcontext, frameContext);
  }

  public HtmlBlockPanel(final Color background, final boolean opaque, final UserAgentContext pcontext, final HtmlRendererContext rcontext,
      final FrameContext frameContext) {
    this.setLayout(null);
    // Blocks are scrolled by Cobra itself, see autoscroll(). Swing's autoscroller cannot scroll them, and it
    // swallows the first drag event after the mouse comes back into the panel.
    this.setAutoscrolls(false);
    this.frameContext = frameContext;
    this.ucontext = pcontext;
    this.rcontext = rcontext;
    this.setOpaque(opaque);
    this.setBackground(background);
    final ActionListener actionListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final String command = e.getActionCommand();
        if ("copy".equals(command)) {
          copy();
        } else if ("selectAll".equals(command)) {
          selectAll();
        }
      }
    };
    if (!GraphicsEnvironment.isHeadless()) {
      final int menuShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      this.registerKeyboardAction(actionListener, "copy", KeyStroke.getKeyStroke(KeyEvent.VK_COPY, 0), JComponent.WHEN_FOCUSED);
      this.registerKeyboardAction(actionListener, "copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutMask), JComponent.WHEN_FOCUSED);
      this.registerKeyboardAction(actionListener, "copy", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_DOWN_MASK),
          JComponent.WHEN_FOCUSED);
      this.registerKeyboardAction(actionListener, "selectAll", KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutMask),
          JComponent.WHEN_FOCUSED);
    }
    this.addMouseListener(new MouseListener() {
      public void mouseClicked(final MouseEvent e) {
        onMouseClick(e);
      }

      public void mouseEntered(final MouseEvent e) {
      }

      public void mouseExited(final MouseEvent e) {
        onMouseExited(e);
      }

      public void mousePressed(final MouseEvent e) {
        onMousePressed(e);
      }

      public void mouseReleased(final MouseEvent e) {
        onMouseReleased(e);
      }
    });
    this.addMouseMotionListener(new MouseMotionListener() {
      /*
       * (non-Javadoc)
       *
       * @see
       * java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent
       * )
       */
      public void mouseDragged(final MouseEvent e) {
        onMouseDragged(e);
      }

      /*
       * (non-Javadoc)
       *
       * @see
       * java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent
       * )
       */
      public void mouseMoved(final MouseEvent arg0) {
        onMouseMoved(arg0);
      }
    });
    this.addMouseWheelListener(new MouseWheelListener() {
      public void mouseWheelMoved(final MouseWheelEvent e) {
        onMouseWheelMoved(e);
      }
    });
  }

  /**
   * Scrolls the body area to the given location.
   * <p>
   * This method should be called from the GUI thread.
   *
   * @param bounds
   *          The bounds in the scrollable block area that should become
   *          visible.
   * @param xIfNeeded
   *          If this parameter is true, scrolling will only occur if the
   *          requested bounds are not currently visible horizontally.
   * @param yIfNeeded
   *          If this parameter is true, scrolling will only occur if the
   *          requested bounds are not currently visible vertically.
   */
  public void scrollTo(final Rectangle bounds, final boolean xIfNeeded, final boolean yIfNeeded) {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) getRootNode();
    if (doc != null) {
      RBlock bodyBlock = (RBlock)((HTMLElementImpl) doc.getBody()).getUINode();
      bodyBlock.scrollTo(bounds, xIfNeeded, yIfNeeded);
    }
  }

  public void scrollBy(final int xOffset, final int yOffset) {
    final RBlock block = this.rblock;
    if (block != null) {
      if (xOffset != 0) {
        block.scrollBy(Adjustable.HORIZONTAL, xOffset);
      }
      if (yOffset != 0) {
        block.scrollBy(Adjustable.VERTICAL, yOffset);
      }
    }
  }

  /**
   * Scrolls the body area to the node given, if it is part of the current
   * document.
   * <p>
   * This method should be called from the GUI thread.
   *
   * @param node
   *          A DOM node.
   */
  public void scrollTo(final Node node) {
    final Rectangle bounds = this.getNodeBounds(node, true);
    if (bounds == null) {
      return;
    }
    this.scrollTo(bounds, true, false);
  }

  /**
   * Gets the rectangular bounds of the given node.
   * <p>
   * This method should be called from the GUI thread.
   *
   * @param node
   *          A node in the current document.
   * @param relativeToScrollable
   *          Whether the bounds should be relative to the scrollable body area.
   *          Otherwise, they are relative to the root block (which is the
   *          essentially the same as being relative to this
   *          <code>HtmlBlockPanel</code> minus Swing borders).
   */
  public Rectangle getNodeBounds(final Node node, final boolean relativeToScrollable) {
    final RBlock block = this.rblock;
    if (block == null) {
      return null;
    }
    // Find UINode first
    Node currentNode = node;
    UINode uiNode = null;
    while (currentNode != null) {
      if (currentNode instanceof HTMLElementImpl) {
        final HTMLElementImpl element = (HTMLElementImpl) currentNode;
        uiNode = element.getUINode();
        if (uiNode != null) {
          break;
        }
      }
      currentNode = currentNode.getParentNode();
    }
    if (uiNode == null) {
      return null;
    }
    final RCollection relativeTo = relativeToScrollable ? (RCollection) block.getRBlockViewport() : (RCollection) block;
    if (node == currentNode) {
      final BoundableRenderable br = (BoundableRenderable) uiNode;
      final Point guiPoint = br.getOriginRelativeTo(relativeTo);
      final Dimension size = br.getSize();
      return new Rectangle(guiPoint, size);
    } else {
      return this.scanNodeBounds((RCollection) uiNode, node, relativeTo);
    }
  }

  /**
   * Gets an aggregate of the bounds of renderer leaf nodes.
   */
  private Rectangle scanNodeBounds(final RCollection root, final Node node, final RCollection relativeTo) {
    final Iterator<? extends Renderable> i = root.getRenderables(false);
    Rectangle resultBounds = null;
    BoundableRenderable prevBoundable = null;
    if (i != null) {
      while (i.hasNext()) {
        final Renderable rn = i.next();
        final Renderable r = rn instanceof PositionedRenderable ? (((PositionedRenderable)rn).renderable) : rn;
        Rectangle subBounds = null;
        if (r instanceof RCollection) {
          final RCollection rc = (RCollection) r;
          prevBoundable = rc;
          subBounds = this.scanNodeBounds(rc, node, relativeTo);
        } else if (r instanceof BoundableRenderable) {
          final BoundableRenderable br = (BoundableRenderable) r;
          prevBoundable = br;
          if (Nodes.isSameOrAncestorOf(node, (Node) r.getModelNode())) {
            final Point origin = br.getOriginRelativeTo(relativeTo);
            final Dimension size = br.getSize();
            subBounds = new Rectangle(origin, size);
          }
        } else {
          // This would have to be a RStyleChanger. We rely on these
          // when the target node has blank content.
          if (Nodes.isSameOrAncestorOf(node, (Node) r.getModelNode())) {
            final int xInRoot = prevBoundable == null ? 0 : prevBoundable.getVisualX() + prevBoundable.getVisualWidth();
            final Point rootOrigin = root.getOriginRelativeTo(relativeTo);
            subBounds = new Rectangle(rootOrigin.x + xInRoot, rootOrigin.y, 0, root.getVisualHeight());
          }
        }
        if (subBounds != null) {
          if (resultBounds == null) {
            resultBounds = subBounds;
          } else {
            resultBounds = subBounds.union(resultBounds);
          }
        }
      }
    }
    return resultBounds;
  }

  public BoundableRenderable getRootRenderable() {
    return this.rblock;
  }

  /**
   * Allows {@link #getPreferredSize()} to render the HTML block in order to
   * determine the preferred size of this component. Note that
   * <code>getPreferredSize()<code> is a potentially time-consuming
   * operation if the preferred width is set.
   *
   * @param width
   *          The preferred blocked width. Use <code>-1</code> to unset.
   */
  public void setPreferredWidth(final int width) {
    this.preferredWidth = width;
  }

  /**
   * If the preferred size has been set with
   * {@link #setPreferredSize(Dimension)}, then that size is returned. Otherwise
   * a preferred size is calculated by rendering the HTML DOM, provided one is
   * available and a preferred width other than <code>-1</code> has been set
   * with {@link #setPreferredWidth(int)}: the size of the content laid out
   * within that width, so the width shrinks to the content and the height
   * covers all of it. An arbitrary preferred size is returned in other
   * scenarios.
   */
  @Override
  public Dimension getPreferredSize() {
    // Expected to be invoked in the GUI thread.
    if (this.isPreferredSizeSet()) {
      return super.getPreferredSize();
    }
    final int pw = this.preferredWidth;
    final RBlock block = this.rblock;
    if ((pw == -1) || (block == null)) {
      return new Dimension(600, 400);
    }
    // Layout should always be done in the GUI thread.
    if (SwingUtilities.isEventDispatchThread()) {
      return this.measureContent(block, pw);
    }
    final Dimension[] size = new Dimension[1];
    try {
      SwingUtilities.invokeAndWait(() -> size[0] = this.measureContent(block, pw));
    } catch (final Exception err) {
      logger.debug("Unable to do preferred size layout.", err);
    }
    return size[0] == null ? new Dimension(pw, 0) : size[0];
  }

  private Dimension measureContent(final RBlock block, final int width) {
    final NodeImpl rootNode = getRootNode();
    if (rootNode instanceof HTMLDocumentImpl) {
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) rootNode;
      if (!doc.layoutBlocked.get()) {
        doc.primeNodeData();
      }
    }

    final RenderThreadState state = RenderThreadState.getState();
    final boolean measuring = state.measuringContent;
    state.measuringContent = true;
    try {
      block.layout(width, 0, false, false, RenderState.OVERFLOW_VISIBLE, RenderState.OVERFLOW_VISIBLE, true);
    } finally {
      state.measuringContent = measuring;
    }
    final Dimension size = new Dimension(block.width, block.height);

    if ((this.getWidth() > 0) && (this.getHeight() > 0)) {
      this.doLayout();
    }
    return size;
  }

  @Override
  public void finalize() throws Throwable {
    super.finalize();
  }

  /**
   * Copies the selected text to the system clipboard. Does nothing, leaving
   * the clipboard untouched, if nothing is selected.
   */
  public boolean copy() {
    final String selection = this.getSelectionText();
    if ((selection == null) || selection.isEmpty()) {
      return false;
    }
    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(new StringSelection(selection), this);
    return true;
  }

  /**
   * Selects the whole document.
   */
  public void selectAll() {
    final NodeImpl rootNode = this.getRootNode();
    final Document document = rootNode instanceof Document ? (Document) rootNode : rootNode == null ? null : rootNode.getOwnerDocument();
    if (document != null) {
      final SelectionPoint[] range = SelectionText.getDocumentRange(document);
      this.frameContext.setSelection(range[0], range[1]);
      this.exportSystemSelection();
    }
  }

  /**
   * Puts the selected text into the system selection, used for middle-click
   * paste on X11. Does nothing on platforms without one.
   */
  private void exportSystemSelection() {
    if (GraphicsEnvironment.isHeadless()) {
      return;
    }
    final Clipboard systemSelection = Toolkit.getDefaultToolkit().getSystemSelection();
    final String selection = this.getSelectionText();
    if ((systemSelection != null) && (selection != null) && !selection.isEmpty()) {
      systemSelection.setContents(new StringSelection(selection), this);
    }
  }

  public int getFirstLineHeight() {
    final RBlock block = this.rblock;
    return block == null ? 0 : block.getFirstLineHeight();
  }

  public void setSelectionEnd(final SelectionPoint point) {
    this.updateSelection(this.startSelection, point);
  }

  public void setSelectionStart(final SelectionPoint point) {
    this.updateSelection(point, this.endSelection);
  }

  /**
   * Changes the selection and repaints only the text whose highlight changes,
   * instead of the whole document.
   */
  private void updateSelection(final SelectionPoint start, final SelectionPoint end) {
    final boolean hadSelection = this.hasSelection();
    final SelectionPoint oldStart = this.startSelection;
    final SelectionPoint oldEnd = this.endSelection;
    this.startSelection = start;
    this.endSelection = end;
    final boolean hasSelection = this.hasSelection();
    final RBlock block = this.rblock;
    if ((block == null) || !(hadSelection || hasSelection)) {
      return;
    }
    final Rectangle changed = SelectionGeometry.getChangedArea(block, hadSelection ? oldStart : null, hadSelection ? oldEnd : null,
        hasSelection ? start : null, hasSelection ? end : null);
    if (changed != null) {
      this.repaint(changed);
    }
  }

  /**
   * Paints pending selection changes right away, so the highlight follows the
   * mouse without waiting behind other queued events.
   */
  private void paintSelectionNow() {
    RepaintManager.currentManager(this).paintDirtyRegions();
  }

  public boolean isSelectionAvailable() {
    return this.hasSelection();
  }

  public Node getSelectionNode() {
    if (this.hasSelection()) {
      return Nodes.getCommonAncestor(this.startSelection.getNode(), this.endSelection.getNode());
    } else {
      return null;
    }
  }

  /**
   * Sets the root node to render. This method should be invoked in the GUI
   * dispatch thread.
   */
  public void setRootNode(final NodeImpl node) {
    this.startSelection = null;
    this.endSelection = null;
    scrollCompleted = false;
    layoutCompleted = new CompletableFuture<>();
    if (node != null) {
      final RBlock block = new RBlock(node, 0, this.ucontext, this.rcontext, this.frameContext, this);
      block.setDefaultOverflowX(this.defaultOverflowX);
      block.setDefaultOverflowY(this.defaultOverflowY);
      node.setUINode(block);
      this.rblock = block;
    } else {
      this.rblock = null;
    }
    this.invalidate();
    this.validateAll();
    this.repaint();
  }

  protected void validateAll() {
    Component toValidate = this;
    for (;;) {
      final Container parent = toValidate.getParent();
      if ((parent == null) || parent.isValid()) {
        break;
      }
      toValidate = parent;
    }
    toValidate.validate();
  }

  protected void revalidatePanel() {
    // Called in the GUI thread.
    this.invalidate();
    this.validate();
    // TODO: Could be paintImmediately.
    this.repaint();
  }

  private NodeImpl getRootNode() {
    final RBlock block = this.rblock;
    return block == null ? null : (NodeImpl) block.getModelNode();
  }

  private void onMouseClick(final MouseEvent event) {
    // Rely on AWT mouse-click only for double-clicks
    final RBlock block = this.rblock;
    if (block != null) {
      final int clickCount = event.getClickCount();
      if (SwingUtilities.isLeftMouseButton(event) && (clickCount > 1)) {
        // TODO: Double-click must be revised. It generates
        // a single click via mouse release.
        final Point point = event.getPoint();
        block.onDoubleClick(event, point.x, point.y);
      } else if (SwingUtilities.isMiddleMouseButton(event) && (clickCount == 1)) {
        block.onMiddleClick(event, event.getX(), event.getY());
      } else if (SwingUtilities.isRightMouseButton(event) && (clickCount == 1)) {
        block.onRightClick(event, event.getX(), event.getY());
      }
    }
  }

  private BoundableRenderable mousePressTarget;
  private Map<?, ?> desktopHints = null;

  private void onMousePressed(final MouseEvent event) {
    this.requestFocus();
    final RBlock block = this.rblock;
    if (block != null) {
      final Point point = event.getPoint();
      this.mousePressTarget = block;
      final int rx = point.x;
      final int ry = point.y;
      block.onMousePressed(event, rx, ry);
      // Only the primary button selects, so a context menu can still use the selection.
      if (SwingUtilities.isLeftMouseButton(event)) {
        this.startSelection(block, event);
        this.paintSelectionNow();
      }
    }
  }

  private void startSelection(final RBlock block, final MouseEvent event) {
    final Point point = event.getPoint();
    this.selecting = true;
    this.dragged = false;
    this.pressPoint = point;
    this.lastDragPoint = null;
    final int clickCount = event.getClickCount();
    if (clickCount >= 2) {
      final SelectionPoint character = SelectionGeometry.hitTest(block, point.x, point.y, false);
      if (character != null) {
        this.selectionUnit = clickCount == 2 ? SelectionUnit.WORD : SelectionUnit.BLOCK;
        final SelectionPoint[] range = this.getUnitRange(character);
        this.selectionUnitRange = range;
        this.frameContext.setSelection(range[0], range[1]);
        return;
      }
    }
    this.selectionUnit = SelectionUnit.CHARACTER;
    this.selectionUnitRange = null;
    final SelectionPoint caret = SelectionGeometry.hitTest(block, point.x, point.y, true);
    if (event.isShiftDown() && (this.startSelection != null) && (caret != null)) {
      this.frameContext.expandSelection(caret);
    } else {
      this.frameContext.resetSelection(caret);
    }
  }

  private SelectionPoint[] getUnitRange(final SelectionPoint character) {
    return this.selectionUnit == SelectionUnit.WORD ? SelectionText.getWordRange(character) : SelectionText.getBlockRange(character);
  }

  /**
   * Moves the focus end of the selection to a point in panel coordinates.
   */
  private void extendSelection(final Point point) {
    final RBlock block = this.rblock;
    if ((block == null) || (this.startSelection == null)) {
      return;
    }
    if (this.selectionUnit == SelectionUnit.CHARACTER) {
      final SelectionPoint caret = SelectionGeometry.hitTest(block, point.x, point.y, true);
      if (caret != null) {
        this.frameContext.expandSelection(caret);
      }
      return;
    }
    final SelectionPoint character = SelectionGeometry.hitTest(block, point.x, point.y, false);
    final SelectionPoint[] anchorRange = this.selectionUnitRange;
    if ((character == null) || (anchorRange == null)) {
      return;
    }
    // Extend by whole words or blocks, always keeping the originally clicked unit selected.
    final SelectionPoint[] range = this.getUnitRange(character);
    if (SelectionPoint.compare(range[0], anchorRange[0]) < 0) {
      this.frameContext.setSelection(anchorRange[1], range[0]);
    } else {
      this.frameContext.setSelection(anchorRange[0], range[1]);
    }
  }

  private void onMouseReleased(final MouseEvent event) {
    // A drag that selected text is not a click, e.g. it must not follow a link it ends on.
    final boolean click = !(SwingUtilities.isLeftMouseButton(event) && this.dragged);
    if (SwingUtilities.isLeftMouseButton(event) && this.selecting) {
      this.selecting = false;
      this.lastDragPoint = null;
      this.stopAutoscroll();
      this.exportSystemSelection();
    }
    final RBlock block = this.rblock;
    if (block != null) {
      final Point point = event.getPoint();
      final int rx = point.x;
      final int ry = point.y;
      if (SwingUtilities.isLeftMouseButton(event)) {
        if (click) {
          // TODO: This will be raised twice on a double-click.
          if (event.isControlDown()) {
            block.onMiddleClick(event, rx, ry);
          } else {
            block.onMouseClick(event, rx, ry);
          }
        }
      } else if (SwingUtilities.isRightMouseButton(event)) {
        block.onRightClick(event, rx, ry);
      }
      block.onMouseReleased(event, rx, ry);
      final BoundableRenderable oldTarget = this.mousePressTarget;
      if (oldTarget != null) {
        this.mousePressTarget = null;
        if (oldTarget != block) {
          oldTarget.onMouseDisarmed(event);
        }
      }
    } else {
      this.mousePressTarget = null;
    }
  }

  private void onMouseExited(final MouseEvent event) {
    final BoundableRenderable oldTarget = this.mousePressTarget;
    if (oldTarget != null) {
      this.mousePressTarget = null;
      oldTarget.onMouseDisarmed(event);
    }
  }

  private Renderable getInnerMostRenderable(final int x, final int y) {
    final RBlock block = this.rblock;
    BoundableRenderable r = block.getRenderable(x - block.getVisualX(), y - block.getVisualY());

    int xi = x, yi = y;
    BoundableRenderable inner = null;
    BoundableRenderable prevR = null;
    do {
      if (r instanceof RCollection) {
        RCollection rc = (RCollection) r;

        if (prevR != null) {
          final Point oi = prevR.getOriginRelativeTo(rc);
          xi -= oi.x;
          yi -= oi.y;
        }

        // xi -= rc.getVisualX();
        // yi -= rc.getVisualY();

        inner = rc.getRenderable(xi, yi);
        if (inner != null) {
          prevR = r;
          r = inner;
        }
      } else {
        inner = null;
      }
    } while (inner != null);

    return r;
  }

  private RBlock getContainingBlock(final Renderable r) {
    if (r instanceof RBlock) {
      return (RBlock) r;
    } else if (r instanceof TranslatedRenderable) {
      return getContainingBlock(((TranslatedRenderable) r).getChild());
    } else if (r == null) {
      return null;
    } else if (r instanceof BoundableRenderable) {
      return getContainingBlock(((BoundableRenderable)r).getParent());
    } else {
      return null;
    }
  }

  private void onMouseWheelMoved(final MouseWheelEvent mwe) {
    final RBlock block = this.rblock;
    if (block != null) {
      switch (mwe.getScrollType()) {
      case MouseWheelEvent.WHEEL_UNIT_SCROLL:
        final int factor = mwe.isShiftDown() ? 2 : 1;
        final int units = mwe.getWheelRotation() * mwe.getScrollAmount() * factor;
        final Renderable innerMostRenderable = getInnerMostRenderable(mwe.getX(), mwe.getY());
        boolean consumed = false;
        RBlock innerBlock = getContainingBlock(innerMostRenderable);
        do {
          if (innerBlock != null) {
            consumed = innerBlock.scrollByUnits(Adjustable.VERTICAL, units);
            innerBlock = getContainingBlock(innerBlock.getParent());
          }
        } while ((!consumed) && (innerBlock != null));
        if (this.selecting && (this.lastDragPoint != null)) {
          this.extendSelection(this.lastDragPoint);
        }
        break;
      }
    }
  }

  private void onMouseDragged(final MouseEvent event) {
    if (!this.selecting || !SwingUtilities.isLeftMouseButton(event) || (this.rblock == null)) {
      return;
    }
    final Point point = event.getPoint();
    final Point pressPoint = this.pressPoint;
    if ((pressPoint != null) && !this.dragged) {
      final int threshold = DragSource.getDragThreshold();
      this.dragged = (Math.abs(point.x - pressPoint.x) > threshold) || (Math.abs(point.y - pressPoint.y) > threshold);
    }
    this.lastDragPoint = point;
    this.extendSelection(point);
    this.paintSelectionNow();
    if ((point.y < 0) || (point.y >= this.getHeight())) {
      this.startAutoscroll();
    } else {
      this.stopAutoscroll();
    }
  }

  private void startAutoscroll() {
    javax.swing.Timer timer = this.autoscrollTimer;
    if (timer == null) {
      timer = new javax.swing.Timer(AUTOSCROLL_DELAY, e -> this.autoscroll());
      this.autoscrollTimer = timer;
    }
    if (!timer.isRunning()) {
      timer.start();
    }
  }

  private void stopAutoscroll() {
    final javax.swing.Timer timer = this.autoscrollTimer;
    if (timer != null) {
      timer.stop();
    }
  }

  /**
   * Scrolls while the mouse is dragged above or below the panel, faster the
   * further away it is, and extends the selection to the newly shown text.
   */
  private void autoscroll() {
    final Point point = this.lastDragPoint;
    final RBlock block = this.rblock;
    final int height = this.getHeight();
    if (!this.selecting || (point == null) || (block == null) || ((point.y >= 0) && (point.y < height))) {
      this.stopAutoscroll();
      return;
    }
    final int distance = point.y < 0 ? point.y : point.y - height + 1;
    final int step = Integer.signum(distance) * Math.min(AUTOSCROLL_MAX_STEP, Math.abs(distance) + 8);
    final int x = Math.max(0, Math.min(this.getWidth() - 1, point.x));
    final int y = point.y < 0 ? 0 : height - 1;
    RBlock innerBlock = getContainingBlock(getInnerMostRenderable(x, y));
    boolean scrolled = false;
    while (!scrolled && (innerBlock != null)) {
      scrolled = innerBlock.scrollBy(Adjustable.VERTICAL, step);
      innerBlock = getContainingBlock(innerBlock.getParent());
    }
    if (scrolled) {
      this.extendSelection(point);
    }
  }

  private void onMouseMoved(final MouseEvent event) {
    final RBlock block = this.rblock;
    if (block != null) {
      final Point point = event.getPoint();
      block.onMouseMoved(event, point.x, point.y, false, null);
    }
  }

  private boolean applyRenderHints = true;

  void disableRenderHints() {
    this.applyRenderHints = false;
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
   */
  // protected void paintComponent(Graphics g) {
  @Override
  public void paint(final Graphics g) {
    // We go against Sun's advice and override
    // paint() instead of paintComponent(). Scrollbars
    // do not repaint correctly if we use
    // paintComponent.
    if (this.isOpaque()) {
      // Background not painted by default in JComponent.
      final Rectangle clipBounds = g.getClipBounds();
      g.setColor(this.getBackground());
      g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
    }
    if (applyRenderHints && g instanceof Graphics2D) {
      final Graphics2D g2 = (Graphics2D) g;
      if (desktopHints == null) {
        desktopHints = (Map<?, ?>) (Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
      }
      if (desktopHints != null) {
        g2.addRenderingHints(desktopHints);
      } else {
        try {
          g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        } catch (final NoSuchFieldError e) {
          g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
      }

      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    final RBlock block = this.rblock;
    if (block != null) {
      block.paint(g);

      if (this.hasSelection()) {
        SelectionGeometry.paint(g, block, this.startSelection, this.endSelection, this.getSelectionBackground(),
            this.getSelectionForeground());
      }
    }

    // For debugging
    // drawGrid(g);
  }

  private void drawGrid(final Graphics g) {
      final int GRID_SIZE = 50;
      final int OFFSET_X = 0;
      final int OFFSET_Y = 0;
      // Draw grid for debug
      final Rectangle clipBounds = g.getClipBounds();
      g.setColor(new Color(0, 0, 0, 30));
      for (int i = 0; i < clipBounds.width; i+= GRID_SIZE) {
        g.drawLine(i + OFFSET_X, 0, i + OFFSET_X, clipBounds.height);
      }
      for (int j = 0; j < clipBounds.height; j+= GRID_SIZE) {
        g.drawLine(0, j + OFFSET_Y, clipBounds.width, j + OFFSET_Y);
      }
  }

  @Override
  public void doLayout() {
    final NodeImpl rootNode = getRootNode();
    if (rootNode instanceof HTMLDocumentImpl) {
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) rootNode;
      final boolean layoutBlocked = doc.layoutBlocked.get();
      if (layoutBlocked) {
        return;
      }

      // Note: There were issues with this previously. See GH #147
      doc.primeNodeData();

    }

    try {
      final Dimension size = this.getSize();
      this.clearComponents();
      final RBlock block = this.rblock;
      if (block != null) {
        block.layout(size.width, size.height, true, true, null, false);
        // Only set origin
        block.setOrigin(0, 0);
        block.updateWidgetBounds(0, 0);
        this.updateGUIComponents();
        // dumpRndTree(block);
        if (!scrollCompleted) {
          scrollCompleted = true;
          if (rootNode instanceof HTMLDocumentImpl) {
            final HTMLDocumentImpl doc = (HTMLDocumentImpl) rootNode;
            final String ref = doc.getDocumentURL().getRef();
            if (ref != null && ref.length() > 0) {
              scrollTo(doc.getElementById(ref));
            }
          }
        }
        layoutCompleted.complete(true);
      } else {
        if (this.getComponentCount() > 0) {
          this.removeAll();
        }
      }
    } catch (final Exception thrown) {
      logger.debug("Unexpected error in layout engine. Document is " + this.getRootNode(), thrown);
    }
  }

  /**
   * Implementation of UINode.repaint().
   */
  public void repaint(final ModelNode modelNode) {
    // this.rblock.invalidateRenderStyle();
    this.repaint();
  }

  /**
   * Gets the selected text, or <code>null</code> if nothing is selected.
   */
  public String getSelectionText() {
    if (this.hasSelection()) {
      return SelectionText.getText(this.startSelection, this.endSelection);
    } else {
      return null;
    }
  }

  public boolean hasSelection() {
    final SelectionPoint start = this.startSelection;
    final SelectionPoint end = this.endSelection;
    final NodeImpl rootNode = this.getRootNode();
    if ((start == null) || (end == null) || (rootNode == null) || start.equals(end)) {
      return false;
    }
    // Points left over from a previous document, or in removed nodes, are not a selection.
    final Node document = rootNode instanceof Document ? rootNode : rootNode.getOwnerDocument();
    return start.isIn(document) && end.isIn(document);
  }

  /**
   * Color behind selected text. Defaults to the <code>--cobra-selection-bg</code>
   * theme variable, which follows the look and feel.
   */
  protected Color getSelectionBackground() {
    final Color color = resolveThemeColor("--cobra-selection-bg", "TextArea.selectionBackground");
    return color != null ? color : new Color(0x2675BF);
  }

  /**
   * Color of selected text. Defaults to the <code>--cobra-selection-fg</code>
   * theme variable, which follows the look and feel.
   */
  protected Color getSelectionForeground() {
    final Color color = resolveThemeColor("--cobra-selection-fg", "TextArea.selectionForeground");
    return color != null ? color : Color.WHITE;
  }

  private static Color resolveThemeColor(final String variable, final String uiManagerKey) {
    final String value = CSSVariableResolver.INSTANCE.resolveVar(variable, null);
    if ((value != null) && !value.isBlank()) {
      try {
        final Color color = ColorFactory.getInstance().getColor(value);
        if (color != null) {
          return color;
        }
      } catch (final RuntimeException e) {
        logger.debug("Invalid selection color " + variable + ": " + value, e);
      }
    }
    return UIManager.getColor(uiManagerKey);
  }

  @Override
  protected void paintChildren(final Graphics g) {
    // Overridding with NOP. For various reasons,
    // the regular mechanism for painting children
    // needs to be handled by Cobra.
  }

  public Color getPaintedBackgroundColor() {
    return this.isOpaque() ? this.getBackground() : null;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * java.awt.datatransfer.ClipboardOwner#lostOwnership(java.awt.datatransfer
   * .Clipboard, java.awt.datatransfer.Transferable)
   */
  public void lostOwnership(final Clipboard arg0, final Transferable arg1) {
  }

  public void relayout() {
    // Expected to be called in the GUI thread.
    // Renderable branch should be invalidated at this
    // point, but this GUI component not necessarily.
    this.revalidatePanel();
  }

  public void invalidateLayoutUpTree() {
    // Called when renderable branch is invalidated.
    // We shouldn't do anything here. Changes in renderer
    // tree do not have any bearing on validity of GUI
    // component.
  }

  public void updateAllWidgetBounds() {
    this.rblock.updateWidgetBounds(0, 0);
  }

  public Point getGUIPoint(final int clientX, final int clientY) {
    // This is the GUI!
    return new Point(clientX, clientY);
  }

  public void focus() {
    this.grabFocus();
  }

  private boolean processingDocumentNotification = false;

  void processDocumentNotifications(final DocumentNotification[] notifications) {
    // Called in the GUI thread.
    if (this.processingDocumentNotification) {
      // This should not be possible. Even if
      // Javascript modifies the DOM during
      // parsing, this should be executed in
      // the GUI thread, not the parser thread.
      throw new IllegalStateException("Recursive");
    }
    this.processingDocumentNotification = true;
    try {
      // Note: It may be assumed that usually only generic
      // notifications come in batches. Other types
      // of noitifications probably come one by one.
      boolean topLayout = false;
      ArrayList<RElement> repainters = null;
      final int length = notifications.length;
      for (int i = 0; i < length; i++) {
        final DocumentNotification dn = notifications[i];
        final int type = dn.type;
        switch (type) {
        case DocumentNotification.GENERIC:
        case DocumentNotification.SIZE: {
          final NodeImpl node = dn.node;
          if (node == null) {
            // This is all-invalidate (new style sheet)
            if (logger.isInfoEnabled()) {
              logger.info("processDocumentNotifications(): Calling invalidateLayoutDeep().");
            }
            this.rblock.invalidateLayoutDeep();
            // this.rblock.invalidateRenderStyle();
          } else {
            final UINode uiNode = node.findUINode();
            if (uiNode != null) {
              final RElement relement = (RElement) uiNode;
              relement.invalidateLayoutUpTree();
              relement.invalidateLayoutDeep();
              // if(type == DocumentNotification.GENERIC) {
              // relement.invalidateRenderStyle();
              // }
            } else {
              if (logger.isInfoEnabled()) {
                logger.info("processDocumentNotifications(): Unable to find UINode for " + node);
              }
            }
          }
          topLayout = true;
          break;
        }
        case DocumentNotification.POSITION: {
          // TODO: Could be more efficient.
          final NodeImpl node = dn.node;
          final NodeImpl parent = (NodeImpl) node.getParentNode();
          if (parent != null) {
            final UINode uiNode = parent.findUINode();
            if (uiNode != null) {
              final RElement relement = (RElement) uiNode;
              relement.invalidateLayoutUpTree();
            }
          }
          topLayout = true;
          break;
        }
        case DocumentNotification.LOOK: {
          final NodeImpl node = dn.node;
          final UINode uiNode = node.findUINode();
          if (uiNode != null) {
            if (repainters == null) {
              repainters = new ArrayList<>(1);
            }
            final RElement relement = (RElement) uiNode;
            relement.invalidateRenderStyle();
            repainters.add(relement);
          }
          break;
        }
        default:
          break;
        }
      }
      if (topLayout) {
        this.revalidatePanel();
      } else {
        if (repainters != null) {
          final Iterator<RElement> i = repainters.iterator();
          while (i.hasNext()) {
            final RElement element = i.next();
            element.repaint();
          }
        }
      }
    } finally {
      this.processingDocumentNotification = false;
    }
  }

  public RenderableContainer getParentContainer() {
    return null;
  }

  public void addDelayedPair(final DelayedPair pair) {
    throw new UnsupportedOperationException("Delayed pairs are not being handled at this level.");
  }

  public Collection<DelayedPair> getDelayedPairs() {
    throw new UnsupportedOperationException("Delayed pairs are not being handled at this level.");
  }

  public void clearDelayedPairs() {
    throw new UnsupportedOperationException("Delayed pairs are not being handled at this level.");
  }

  private Set<Component> components;

  private void clearComponents() {
    final Set<Component> c = this.components;
    if (c != null) {
      c.clear();
    }
  }

  public Component addComponent(final Component component) {
    Set<Component> c = this.components;
    if (c == null) {
      c = new HashSet<>();
      this.components = c;
    }
    if (c.add(component)) {
      return component;
    } else {
      return null;
    }
  }

  private void updateGUIComponents() {
    // We use this method, instead of removing all components and
    // adding them back, because removal of components can cause
    // them to lose focus.

    final Set<Component> c = this.components;
    if (c == null) {
      if (this.getComponentCount() != 0) {
        this.removeAll();
      }
    } else {
      // Remove children not in the set.
      final Set<Component> workingSet = new HashSet<>();
      workingSet.addAll(c);
      int count = this.getComponentCount();
      for (int i = 0; i < count;) {
        final Component component = this.getComponent(i);
        if (!c.contains(component)) {
          this.remove(i);
          count = this.getComponentCount();
        } else {
          i++;
          workingSet.remove(component);
        }
      }
      // Add components in set that were not previously children.
      final Iterator<Component> wsi = workingSet.iterator();
      while (wsi.hasNext()) {
        final Component component = wsi.next();
        this.add(component);
      }
    }
  }

  public int getDefaultOverflowX() {
    return defaultOverflowX;
  }

  public void setDefaultOverflowX(final int defaultOverflowX) {
    if (defaultOverflowX != this.defaultOverflowX) {
      this.defaultOverflowX = defaultOverflowX;
      final RBlock block = this.rblock;
      if (block != null) {
        block.setDefaultOverflowX(defaultOverflowX);
        block.relayoutIfValid();
      }
    }
  }

  public int getDefaultOverflowY() {
    return defaultOverflowY;
  }

  public void setDefaultOverflowY(final int defaultOverflowY) {
    if (this.defaultOverflowY != defaultOverflowY) {
      this.defaultOverflowY = defaultOverflowY;
      final RBlock block = this.rblock;
      if (block != null) {
        block.setDefaultOverflowY(defaultOverflowY);
        block.relayoutIfValid();
      }
    }
  }

  @Override
  public Insets getInsets(final boolean hscroll, final boolean vscroll) {
    throw new UnsupportedOperationException(
        "Method added while implementing absolute positioned elements inside relative elements. But not implemented yet.");
  }

  @Override
  public Insets getInsetsMarginBorder(final boolean hscroll, final boolean vscroll) {
    throw new UnsupportedOperationException("Method added while fixing #32. Not implemented yet.");
  }

  @Override
  public int getVisualHeight() {
    return rblock.getVisualHeight();
  }

  @Override
  public int getVisualWidth() {
    return rblock.getVisualWidth();
  }

  @Override
  public Rectangle getVisualBounds() {
    return new Rectangle(getX(), getY(), getVisualWidth(), getVisualHeight());
  }

  public Point translateDescendentPoint(BoundableRenderable descendent, int x, int y) {
    return rblock.translateDescendentPoint(descendent, x, y);
  }

  @Override
  public Point getOriginRelativeTo(RCollection bodyLayout) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Point getOriginRelativeToAbs(RCollection bodyLayout) {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("unused")
  private static void dumpRndTree(final Renderable root) {
    System.out.println("------------------------------");
    RBlock.dumpRndTree("", true, root, true);
    System.out.println("------------------------------");
  }

  private CompletableFuture<Boolean> layoutCompleted = new CompletableFuture<>();

  public Future<Boolean> layoutCompletion() {
    return layoutCompleted;
  }

  public boolean isReadyToPaint() {
    final RBlock block = this.rblock;
    if (block != null) {
      final HTMLDocumentImpl doc = (HTMLDocumentImpl) block.getModelNode();
      return (!doc.getWindow().hasPendingTasks()) && block.isReadyToPaint();
    }
    return false;
  }
}