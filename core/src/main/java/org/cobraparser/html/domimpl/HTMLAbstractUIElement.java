package org.cobraparser.html.domimpl;

import org.cobraparser.js.JavaScriptEngine;

import java.util.Map;

/**
 * Implements common functionality of most elements.
 */
public class HTMLAbstractUIElement extends HTMLElementImpl {
    private Object onfocus, onblur, onclick, ondblclick, onmousedown, onmouseup, onmouseover, onmousemove, onmouseout, onkeypress,
        onkeydown, onkeyup, oncontextmenu;

    public HTMLAbstractUIElement(final String name) {
        super(name);
    }

    public Object getOnblur() {
        return this.getEventFunction(onblur, "onblur");
    }

    public void setOnblur(final Object onblur) {
        this.onblur = onblur;
    }

    public Object getOnclick() {
        return this.getEventFunction(onclick, "onclick");
    }

    public void setOnclick(final Object onclick) {
        this.onclick = onclick;
    }

    public Object getOndblclick() {
        return this.getEventFunction(ondblclick, "ondblclick");
    }

    public void setOndblclick(final Object ondblclick) {
        this.ondblclick = ondblclick;
    }

    public Object getOnfocus() {
        return this.getEventFunction(onfocus, "onfocus");
    }

    public void setOnfocus(final Object onfocus) {
        this.onfocus = onfocus;
    }

    public Object getOnkeydown() {
        return this.getEventFunction(onkeydown, "onkeydown");
    }

    public void setOnkeydown(final Object onkeydown) {
        this.onkeydown = onkeydown;
    }

    public Object getOnkeypress() {
        return this.getEventFunction(onkeypress, "onkeypress");
    }

    public void setOnkeypress(final Object onkeypress) {
        this.onkeypress = onkeypress;
    }

    public Object getOnkeyup() {
        return this.getEventFunction(onkeyup, "onkeyup");
    }

    public void setOnkeyup(final Object onkeyup) {
        this.onkeyup = onkeyup;
    }

    public Object getOnmousedown() {
        return this.getEventFunction(onmousedown, "onmousedown");
    }

    public void setOnmousedown(final Object onmousedown) {
        this.onmousedown = onmousedown;
    }

    public Object getOnmousemove() {
        return this.getEventFunction(onmousemove, "onmousemove");
    }

    public void setOnmousemove(final Object onmousemove) {
        this.onmousemove = onmousemove;
    }

    public Object getOnmouseout() {
        return this.getEventFunction(onmouseout, "onmouseout");
    }

    public void setOnmouseout(final Object onmouseout) {
        this.onmouseout = onmouseout;
    }

    public Object getOnmouseover() {
        return this.getEventFunction(onmouseover, "onmouseover");
    }

    public void setOnmouseover(final Object onmouseover) {
        this.onmouseover = onmouseover;
    }

    public Object getOnmouseup() {
        return this.getEventFunction(onmouseup, "onmouseup");
    }

    public void setOnmouseup(final Object onmouseup) {
        this.onmouseup = onmouseup;
    }

    public Object getOncontextmenu() {
        return this.getEventFunction(oncontextmenu, "oncontextmenu");
    }

    public void setOncontextmenu(final Object oncontextmenu) {
        this.oncontextmenu = oncontextmenu;
    }

    public void focus() {
        final UINode node = this.getUINode();
        if (node != null) {
            node.focus();
        }
    }

    public void blur() {
        final UINode node = this.getUINode();
        if (node != null) {
            node.blur();
        }
    }

    private Map<String, Object> functionByAttribute = null;

    protected Object getEventFunction(final Object varValue, final String attributeName) {
        Object[] result = new Object[1];
        functionByAttribute = JavaScriptEngine.get().getEventFunctions(this, functionByAttribute, varValue, attributeName, result);
        return result[0];
    }

    @Override
    protected void handleAttributeChanged(String name, String oldValue, String newValue) {
        super.handleAttributeChanged(name, oldValue, newValue);
        if (name.startsWith("on")) {
            synchronized (this) {
                final Map<String, Object> fba = this.functionByAttribute;
                if (fba != null) {
                    fba.remove(name);
                }
            }
        }
    }
}
