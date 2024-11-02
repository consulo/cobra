package org.cobraparser.html.renderer;

import org.cobraparser.html.FormInput;
import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.*;
import org.cobraparser.html.js.Event;
import org.cobraparser.html.js.Window;
import org.cobraparser.html.style.RenderState;
import org.cobraparser.js.JavaScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Optional;

class HtmlController {
    private static final Logger logger = LoggerFactory.getLogger(HtmlController.class.getName());
    private static final HtmlController instance = new HtmlController();

    static HtmlController getInstance() {
        return instance;
    }

    /**
     * @return True to propagate further and false if the event was consumed.
     */
    public boolean onEnterPressed(final ModelNode node, final InputEvent event) {
        if (node instanceof HTMLInputElementImpl) {
            final HTMLInputElementImpl hie = (HTMLInputElementImpl) node;
            if (hie.isSubmittableWithEnterKey()) {
                hie.submitForm(null);
                return false;
            }
        }
        // No propagation
        return false;
    }

    // Quick hack
    private static Window getWindowFactory(final ElementImpl e) {
        final HTMLDocumentImpl doc = (HTMLDocumentImpl) e.getOwnerDocument();
        return doc.getWindow();
    }

    // Quick hack
  /*
  private static boolean runObject(final ElementImpl e, final Object f, final Event event) {
    final HTMLDocumentImpl doc = (HTMLDocumentImpl) e.getOwnerDocument();
    final Window window = doc.getWindow();
    window.addJSTask(new JSRunnableTask(0, "Object from HTMLController", () -> {
      Executor.executeFunction(e, f, event, window.getContextFactory());
    }));
    return false;
  }
  */

    public boolean onMouseClick(final ModelNode node, final MouseEvent event, final int x, final int y) {
        return onMouseClick(node, event, x, y, false);
    }

    /**
     * @return True to propagate further and false if the event was consumed.
     */
    public boolean onMouseClick(final ModelNode node, final MouseEvent event, final int x, final int y, boolean eventDispatched) {
        if (logger.isDebugEnabled()) {
            logger.debug("onMouseClick(): node=" + node + ",class=" + node.getClass().getName());
        }
        // System.out.println("HtmlController.onMouseClick(): " + node + " already dispatched: " + eventDispatched);

        // Get the node which is a valid Event target
    /*{
      NodeImpl target = (NodeImpl)node;
      while(target.getParentNode() != null) {
        if (target instanceof Element || target instanceof Document) { //  TODO || node instanceof Window) {
          break;
        }
        target = (NodeImpl) target.getParentNode();
      }
      final Event jsEvent = new Event("click", target, event, x, y);
      target.dispatchEvent(jsEvent);
    }*/

        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;

            final Event jsEvent = JavaScriptEngine.get().createEvent("click", uiElement, event, x, y);
            // System.out.println("Ui element: " + uiElement.getId());
            // uiElement.dispatchEvent(jsEvent);

            final Object f = uiElement.getOnclick();
            /* TODO: This is the original code which would return immediately if f returned false. */
            if (f != null) {
                // Changing argument to uiElement instead of event
                if (!JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement))) {
                    // if (!Executor.executeFunction(uiElement, f, uiElement, getWindowFactory(uiElement))) {
                    return false;
                }
            }
      /*
      // Alternate JS Task version:
      if (f != null) {
        runObject(uiElement, f, jsEvent);
      }*/

            final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
            if (rcontext != null) {
                if (!rcontext.onMouseClick(uiElement, event)) {
                    return false;
                }
            }
        }
        if (node instanceof HTMLLinkElementImpl) {
            final boolean navigated = ((HTMLLinkElementImpl) node).navigate();
            if (navigated) {
                return false;
            }
        }
        else if (node instanceof HTMLButtonElementImpl) {
            final HTMLButtonElementImpl button = (HTMLButtonElementImpl) node;
            final String rawType = button.getAttribute("type");
            String type;
            if (rawType == null) {
                type = "submit";
            }
            else {
                type = rawType.trim().toLowerCase();
            }
            if ("submit".equals(type)) {
                FormInput[] formInputs;
                final String name = button.getName();
                if (name == null) {
                    formInputs = null;
                }
                else {
                    formInputs = new FormInput[]{new FormInput(name, button.getValue())};
                }
                button.submitForm(formInputs);
                return false;
            }
            else if ("reset".equals(type)) {
                button.resetForm();
                return false;
            }
            else if ("button".equals(type)) {
                System.out.println("Button TODO;");
            }
            else {
                // NOP for "button"!
            }
        }
        if (!eventDispatched) {
            // Get the node which is a valid Event target
            if ((node instanceof Element) || (node instanceof Document)) { //  TODO || node instanceof Window) {
                // System.out.println("Click accepted on " + node);
                final NodeImpl target = (NodeImpl) node;
                final Event jsEvent = JavaScriptEngine.get().createEvent("click", target, event, x, y);
                target.dispatchEvent(jsEvent);
                eventDispatched = true;
            }
        }
        // } else {
        // System.out.println("Bumping click to parent");
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onMouseClick(parent, event, x, y, eventDispatched);
        // }
        // return false;
    /*
    final ModelNode parent = node.getParentModelNode();
    if (parent == null) {
      return true;
    }
    return this.onMouseClick(parent, event, x, y);*/
    }

    public boolean onMiddleClick(ModelNode node, MouseEvent event, int x, int y) {
        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
            if (rcontext != null) {
                // Needs to be done after Javascript, so the script
                // is able to prevent it.
                if (!rcontext.onMiddleClick(uiElement, event)) {
                    return false;
                }
            }
        }
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onMiddleClick(parent, event, x, y);
    }

    public boolean onContextMenu(final ModelNode node, final MouseEvent event, final int x, final int y) {
        if (logger.isDebugEnabled()) {
            logger.debug("onContextMenu(): node=" + node + ",class=" + node.getClass().getName());
        }
        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            final Object f = uiElement.getOncontextmenu();
            if (f != null) {
                final Event jsEvent = JavaScriptEngine.get().createEvent("contextmenu", uiElement, event, x, y);
                if (!JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement))) {
                    return false;
                }
            }
            final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
            if (rcontext != null) {
                // Needs to be done after Javascript, so the script
                // is able to prevent it.
                if (!rcontext.onContextMenu(uiElement, event)) {
                    return false;
                }
            }
        }
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onContextMenu(parent, event, x, y);
    }

    public void onMouseOver(final BaseBoundableRenderable renderable, final ModelNode nodeStart, final MouseEvent event, final int x,
                            final int y, final ModelNode limit) {
        {
            ModelNode node = nodeStart;
            while (node != null) {
                if (node == limit) {
                    break;
                }
                if (node instanceof HTMLAbstractUIElement) {
                    final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
                    uiElement.setMouseOver(true);
                    final Object f = uiElement.getOnmouseover();
                    if (f != null) {
                        final Event jsEvent = JavaScriptEngine.get().createEvent("mouseover", uiElement, event, x, y);
                        JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement));
                    }
                    final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
                    if (rcontext != null) {
                        rcontext.onMouseOver(uiElement, event);
                    }
                }
                node = node.getParentModelNode();
            }
        }

        setMouseOnMouseOver(renderable, nodeStart, limit);
    }

    private static void setMouseOnMouseOver(final BaseBoundableRenderable renderable, final ModelNode nodeStart, final ModelNode limit) {
        ModelNode node = nodeStart;
        while (node != null) {
            if (node == limit) {
                break;
            }
            if (node instanceof NodeImpl) {
                final NodeImpl uiElement = (NodeImpl) node;
                final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
                final RenderState rs = uiElement.getRenderState();
                final Optional<Cursor> cursorOpt = rs.getCursor();
                if (rcontext != null) {
                    if (cursorOpt.isPresent()) {
                        rcontext.setCursor(cursorOpt);
                        break;
                    }
                    else {
                        if (node.getParentModelNode() == limit) {
                            if ((renderable instanceof RWord) || (renderable instanceof RBlank)) {
                                rcontext.setCursor(Optional.of(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)));
                            }
                        }
                    }
                }
            }
            node = node.getParentModelNode();
        }
    }

    public void onMouseOut(final ModelNode nodeStart, final MouseEvent event, final int x, final int y, final ModelNode limit) {
        {
            ModelNode node = nodeStart;
            while (node != null) {
                if (node == limit) {
                    break;
                }
                if (node instanceof HTMLAbstractUIElement) {
                    final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
                    uiElement.setMouseOver(false);
                    final Object f = uiElement.getOnmouseout();
                    if (f != null) {
                        final Event jsEvent = JavaScriptEngine.get().createEvent("mouseout", uiElement, event, x, y);
                        JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement));
                    }
                    final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
                    if (rcontext != null) {
                        rcontext.onMouseOut(uiElement, event);
                    }

                }
                node = node.getParentModelNode();
            }
        }

        resetCursorOnMouseOut(nodeStart, limit);
    }

    private static void resetCursorOnMouseOut(final ModelNode nodeStart, final ModelNode limit) {
        Optional<Cursor> foundCursorOpt = Optional.empty();
        ModelNode node = limit;
        while (node != null) {
            if (node instanceof NodeImpl) {
                final NodeImpl uiElement = (NodeImpl) node;

                final RenderState rs = uiElement.getRenderState();
                final Optional<Cursor> cursorOpt = rs.getCursor();
                foundCursorOpt = cursorOpt;
                if (cursorOpt.isPresent()) {
                    break;
                }

            }
            node = node.getParentModelNode();
        }

        if (nodeStart instanceof NodeImpl) {
            final NodeImpl uiElement = (NodeImpl) nodeStart;
            final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
            // rcontext.setCursor(Optional.empty());
            if (rcontext != null) {
                rcontext.setCursor(foundCursorOpt);
            }
        }
    }

    /**
     * @return True to propagate further, false if consumed.
     */
    public boolean onDoubleClick(final ModelNode node, final MouseEvent event, final int x, final int y) {
        if (logger.isDebugEnabled()) {
            logger.debug("onDoubleClick(): node=" + node + ",class=" + node.getClass().getName());
        }
        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            final Object f = uiElement.getOndblclick();
            if (f != null) {
                final Event jsEvent = JavaScriptEngine.get().createEvent("dblclick", uiElement, event, x, y);
                if (!JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement))) {
                    return false;
                }
            }
            final HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
            if (rcontext != null) {
                if (!rcontext.onDoubleClick(uiElement, event)) {
                    return false;
                }
            }
        }
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onDoubleClick(parent, event, x, y);
    }

    /**
     * @return True to propagate further, false if consumed.
     */
    public boolean onMouseDisarmed(final ModelNode node, final MouseEvent event) {
        if (node instanceof HTMLLinkElementImpl) {
            ((HTMLLinkElementImpl) node).getCurrentStyle().setOverlayColor(null);
            return false;
        }
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onMouseDisarmed(parent, event);
    }

    /**
     * @return True to propagate further, false if consumed.
     */
    public boolean onMouseDown(final ModelNode node, final MouseEvent event, final int x, final int y) {
        boolean pass = true;
        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            final Object f = uiElement.getOnmousedown();
            if (f != null) {
                final Event jsEvent = JavaScriptEngine.get().createEvent("mousedown", uiElement, event, x, y);
                pass = JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement));
            }
        }
        if (node instanceof HTMLLinkElementImpl) {
            ((HTMLLinkElementImpl) node).getCurrentStyle().setOverlayColor("#9090FF80");
            return false;
        }
        if (!pass) {
            return false;
        }
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onMouseDown(parent, event, x, y);
    }

    /**
     * @return True to propagate further, false if consumed.
     */
    public boolean onMouseUp(final ModelNode node, final MouseEvent event, final int x, final int y) {
        boolean pass = true;
        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            final Object f = uiElement.getOnmouseup();
            if (f != null) {
                final Event jsEvent = JavaScriptEngine.get().createEvent("mouseup", uiElement, event, x, y);
                pass = JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement));
            }
        }
        if (node instanceof HTMLLinkElementImpl) {
            ((HTMLLinkElementImpl) node).getCurrentStyle().setOverlayColor(null);
            return false;
        }
        if (!pass) {
            return false;
        }
        final ModelNode parent = node.getParentModelNode();
        if (parent == null) {
            return true;
        }
        return this.onMouseUp(parent, event, x, y);
    }

    /**
     * @param node The node generating the event.
     * @param x    For images only, x coordinate of mouse click.
     * @param y    For images only, y coordinate of mouse click.
     * @return True to propagate further, false if consumed.
     */
    public boolean onPressed(final ModelNode node, final InputEvent event, final int x, final int y) {
        if (node instanceof HTMLAbstractUIElement) {
            final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            final Object f = uiElement.getOnclick();
            if (f != null) {
                final Event jsEvent = JavaScriptEngine.get().createEvent("click", uiElement, event, x, y);
                if (!JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement))) {
                    return false;
                }
            }
        }
        if (node instanceof HTMLInputElementImpl) {
            final HTMLInputElementImpl hie = (HTMLInputElementImpl) node;
            if (hie.isSubmitInput()) {
                FormInput[] formInputs;
                final String name = hie.getName();
                if (name == null) {
                    formInputs = null;
                }
                else {
                    formInputs = new FormInput[]{new FormInput(name, hie.getValue())};
                }
                hie.submitForm(formInputs);
            }
            else if (hie.isImageInput()) {
                final String name = hie.getName();
                final String prefix = name == null ? "" : name + ".";
                final FormInput[] extraFormInputs = new FormInput[]{new FormInput(prefix + "x", String.valueOf(x)),
                    new FormInput(prefix + "y", String.valueOf(y))};
                hie.submitForm(extraFormInputs);
            }
            else if (hie.isResetInput()) {
                hie.resetForm();
            }
        }
        if (node instanceof HTMLElementImpl) {
            final HTMLElementImpl htmlElem = (HTMLElementImpl) node;
            final Event evt = JavaScriptEngine.get().createEvent("click", htmlElem, event, x, y);
            htmlElem.dispatchEvent(evt);
        }
        // No propagate
        return false;
    }

    public boolean onChange(final ModelNode node) {
        if (node instanceof HTMLSelectElementImpl) {
            final HTMLSelectElementImpl uiElement = (HTMLSelectElementImpl) node;
            final Object f = uiElement.getOnchange();
            if (f != null) {
                final Event jsEvent = JavaScriptEngine.get().createEvent("change", uiElement);
                if (!JavaScriptEngine.get().executeFunction(uiElement, f, jsEvent, getWindowFactory(uiElement))) {
                    return false;
                }
            }
        }
        // No propagate
        return false;
    }

    public boolean onKeyUp(final ModelNode node, final KeyEvent ke) {
        boolean pass = true;
        if (node instanceof NodeImpl) {
            final NodeImpl uiElement = (NodeImpl) node;
            final Event jsEvent = JavaScriptEngine.get().createEvent("keyup", uiElement, ke);
            pass = uiElement.dispatchEvent(jsEvent);
            System.out.println("Dispatch result: " + pass);
        }
    /*
    if (node instanceof HTMLAbstractUIElement) {
      final HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
      final Object f = uiElement.getOnmouseup();
      if (f != null) {
        final Event jsEvent = new Event("keyup", uiElement, ke);
        pass = Executor.executeFunction(uiElement, f, jsEvent);
      }
    }*/
        return pass;
    }
}
