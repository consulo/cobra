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
 * Created on Jan 14, 2006
 */
package org.cobraparser.html.domimpl;

import org.cobraparser.html.FormInput;
import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.js.JSSupplierTask;
import org.cobraparser.html.js.Window;
import org.cobraparser.js.JavaScriptEngine;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLFormElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class HTMLFormElementImpl extends HTMLAbstractUIElement implements HTMLFormElement {
    public HTMLFormElementImpl(final String name) {
        super(name);
    }

    public HTMLFormElementImpl() {
        super("FORM");
    }

    public Object namedItem(final String name) {
        try {
            // TODO: This could use document.namedItem.
            this.visit(new NodeVisitor() {
                @Override
                public void visit(final Node node) {
                    if (HTMLFormElementImpl.isInput(node)) {
                        if (name.equals(((Element) node).getAttribute("name"))) {
                            throw new StopVisitorException(node);
                        }
                    }
                }
            });
        }
        catch (final StopVisitorException sve) {
            return sve.getTag();
        }
        return null;
    }

    public Object item(final int index) {
        try {
            this.visit(new NodeVisitor() {
                private int current = 0;

                @Override
                public void visit(final Node node) {
                    if (HTMLFormElementImpl.isInput(node)) {
                        if (this.current == index) {
                            throw new StopVisitorException(node);
                        }
                        this.current++;
                    }
                }
            });
        }
        catch (final StopVisitorException sve) {
            return sve.getTag();
        }
        return null;
    }

    private HTMLCollection elements;

    @Override
    public HTMLCollection getElements() {
        HTMLCollection elements = this.elements;
        if (elements == null) {
            elements = new DescendentHTMLCollection(this, new InputFilter(), this.treeLock, false);
            this.elements = elements;
        }
        return elements;
    }

    @Override
    public int getLength() {
        return this.getElements().getLength();
    }

    @Override
    public String getName() {
        return this.getAttribute("name");
    }

    @Override
    public void setName(final String name) {
        this.setAttribute("name", name);
    }

    @Override
    public String getAcceptCharset() {
        return this.getAttribute("acceptCharset");
    }

    @Override
    public void setAcceptCharset(final String acceptCharset) {
        this.setAttribute("acceptCharset", acceptCharset);
    }

    @Override
    public String getAction() {
        return this.getAttribute("action");
    }

    @Override
    public void setAction(final String action) {
        this.setAttribute("action", action);
    }

    @Override
    public String getEnctype() {
        return this.getAttribute("enctype");
    }

    @Override
    public void setEnctype(final String enctype) {
        this.setAttribute("enctype", enctype);
    }

    @Override
    public String getMethod() {
        String method = this.getAttribute("method");
        if (method == null) {
            method = "GET";
        }
        return method;
    }

    @Override
    public void setMethod(final String method) {
        this.setAttribute("method", method);
    }

    @Override
    public String getTarget() {
        return this.getAttribute("target");
    }

    @Override
    public void setTarget(final String target) {
        this.setAttribute("target", target);
    }

    @Override
    public void submit() {
        this.submit(null);
    }

    private Object onsubmit;

    public void setOnsubmit(final Object value) {
        this.onsubmit = value;
    }

    public Object getOnsubmit() {
        return this.getEventFunction(this.onsubmit, "onsubmit");
    }

    /**
     * This method should be called when form submission is done by a submit
     * button.
     *
     * @param extraFormInputs Any additional form inputs that need to be submitted, e.g. the
     *                        submit button parameter.
     */
    public final void submit(final FormInput[] extraFormInputs) {
        final Object onsubmit = this.getOnsubmit();
        if (onsubmit != null) {
            // TODO: onsubmit event object?
            // dispatchEvent(new Event("submit", this));
            final Window window = ((HTMLDocumentImpl) document).getWindow();
            window.addJSTask(new JSSupplierTask<>(0, () -> {
                return JavaScriptEngine.get().executeFunction(this, onsubmit, null, window);
            }, (result) -> {
                if (result) {
                    submitFormImpl(extraFormInputs);
                }
            }));
        }
        else {
            submitFormImpl(extraFormInputs);
        }

    }

    private void submitFormImpl(final FormInput[] extraFormInputs) {
        final HtmlRendererContext context = this.getHtmlRendererContext();
        if (context != null) {
            final ArrayList<FormInput> formInputs = new ArrayList<>();
            if (extraFormInputs != null) {
                for (final FormInput extraFormInput : extraFormInputs) {
                    formInputs.add(extraFormInput);
                }
            }
            this.visit(new NodeVisitor() {
                @Override
                public void visit(final Node node) {
                    if (node instanceof HTMLElementImpl) {
                        final FormInput[] fis = ((HTMLElementImpl) node).getFormInputs();
                        if (fis != null) {
                            for (final FormInput fi : fis) {
                                if (fi.getName() == null) {
                                    throw new IllegalStateException("Form input does not have a name: " + node);
                                }
                                formInputs.add(fi);
                            }
                        }
                    }
                }
            });
            final FormInput[] fia = formInputs.toArray(FormInput.EMPTY_ARRAY);
            String href = this.getAction();
            if (href == null) {
                href = this.getBaseURI();
            }
            try {
                final URL url = this.getFullURL(href);
                context.submitForm(this.getMethod(), url, this.getTarget(), this.getEnctype(), fia);
            }
            catch (final MalformedURLException mfu) {
                this.warn("submit()", mfu);
            }
        }
    }

    @Override
    public void reset() {
        this.visit(new NodeVisitor() {
            @Override
            public void visit(final Node node) {
                if (node instanceof HTMLBaseInputElement) {
                    ((HTMLBaseInputElement) node).resetInput();
                }
            }
        });
    }

    static boolean isInput(final Node node) {
        final String name = node.getNodeName().toLowerCase();
        return name.equals("input") || name.equals("textarea") || name.equals("select");
    }

    private class InputFilter implements NodeFilter {
        /*
         * (non-Javadoc)
         *
         * @see org.xamjwg.html.domimpl.NodeFilter#accept(org.w3c.dom.Node)
         */
        @Override
        public boolean accept(final Node node) {
            return HTMLFormElementImpl.isInput(node);
        }
    }
}
