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
 * Created on Oct 8, 2005
 */
package org.cobraparser.html.domimpl;

import org.cobraparser.js.JavaScriptEngine;
import org.cobraparser.ua.NetworkRequest;
import org.cobraparser.ua.UserAgentContext;
import org.cobraparser.ua.UserAgentContext.Request;
import org.cobraparser.ua.UserAgentContext.RequestKind;
import org.cobraparser.util.SecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLScriptElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class HTMLScriptElementImpl extends HTMLElementImpl implements HTMLScriptElement {
    public HTMLScriptElementImpl() {
        super("SCRIPT", true);
    }

    public HTMLScriptElementImpl(final String name) {
        super(name, true);
    }

    private String text;

    @Override
    public String getText() {
        final String t = this.text;
        if (t == null) {
            return this.getRawInnerText(true);
        }
        else {
            return t;
        }
    }

    @Override
    public void setText(final String text) {
        this.text = text;
    }

    @Override
    public String getHtmlFor() {
        return this.getAttribute("htmlFor");
    }

    @Override
    public void setHtmlFor(final String htmlFor) {
        this.setAttribute("htmlFor", htmlFor);
    }

    @Override
    public String getEvent() {
        return this.getAttribute("event");
    }

    @Override
    public void setEvent(final String event) {
        this.setAttribute("event", event);
    }

    private boolean defer;

    @Override
    public boolean getDefer() {
        return this.defer;
    }

    @Override
    public void setDefer(final boolean defer) {
        this.defer = defer;
    }

    @Override
    public String getSrc() {
        return this.getAttribute("src");
    }

    @Override
    public void setSrc(final String src) {
        this.setAttribute("src", src);
    }

    @Override
    public String getType() {
        return this.getAttribute("type");
    }

    @Override
    public void setType(final String type) {
        this.setAttribute("type", type);
    }

    private static final String[] jsTypes = {
        "application/ecmascript",
        "application/javascript",
        "application/x-ecmascript",
        "application/x-javascript",
        "text/ecmascript",
        "text/javascript",
        "text/javascript1.0",
        "text/javascript1.1",
        "text/javascript1.2",
        "text/javascript1.3",
        "text/javascript1.4",
        "text/javascript1.5",
        "text/jscript",
        "text/livescript",
        "text/x-ecmascript",
        "text/x-javascript"
    };

    protected final void processScript() {
        final String scriptType = getType();
        if (scriptType != null) {
            if (Arrays.stream(jsTypes).noneMatch(e -> e.equals(scriptType))) {
                ((HTMLDocumentImpl) HTMLScriptElementImpl.this.document).markJobsFinished(1, false);
                return;
            }
        }
        final UserAgentContext bcontext = this.getUserAgentContext();
        if (bcontext == null) {
            throw new IllegalStateException("No user agent context.");
        }
        final Document docObj = this.document;
        if (!(docObj instanceof HTMLDocumentImpl)) {
            throw new IllegalStateException("no valid document");
        }
        final HTMLDocumentImpl doc = (HTMLDocumentImpl) docObj;
        if (bcontext.isScriptingEnabled()) {
            String text;
            final String scriptURI;
            int baseLineNumber;
            final String src = this.getSrc();
            if (src == null) {
                final Request request = new Request(doc.getDocumentURL(), RequestKind.JavaScript);
                if (bcontext.isRequestPermitted(request)) {
                    text = this.getText();
                    scriptURI = doc.getBaseURI();
                    baseLineNumber = 1; // TODO: Line number of inner text??
                }
                else {
                    text = null;
                    scriptURI = null;
                    baseLineNumber = -1;
                }
            }
            else {
                this.informExternalScriptLoading();
                try {
                    final URL scriptURL = doc.getFullURL(src);
                    scriptURI = scriptURL.toExternalForm();
                    // Perform a synchronous request
                    final NetworkRequest request = bcontext.createHttpRequest();
                    SecurityUtil.doPrivileged(() -> {
                        // Code might have restrictions on accessing
                        // items from elsewhere.
                        try {
                            request.open("GET", scriptURI, false);
                            request.send(null, new Request(scriptURL, RequestKind.JavaScript));
                        }
                        catch (final java.io.IOException thrown) {
                            logger.warn("processScript()", thrown);
                        }
                        return null;
                    });
                    final int status = request.getStatus();
                    if ((status != 200) && (status != 0)) {
                        this.warn("Script at [" + scriptURI + "] failed to load; HTTP status: " + status + ".");
                        return;
                    }
                    text = request.getResponseText();
                    baseLineNumber = 1;
                }
                catch (final MalformedURLException mfe) {
                    throw new IllegalArgumentException(mfe);
                }
            }

            JavaScriptEngine.get().evaluateScript(doc, this, text, scriptURI, baseLineNumber, bcontext);
        }
        else {
            doc.markJobsFinished(1, false);
        }
    }

    @Override
    protected void appendInnerTextImpl(final StringBuffer buffer) {
        // nop
    }

    @Override
    protected void handleDocumentAttachmentChanged() {
        if (isAttachedToDocument()) {
            ((HTMLDocumentImpl) document).addJob(() -> processScript(), false);
        }
        else {
            // TODO What does script element do when detached?
        }
        super.handleDocumentAttachmentChanged();
    }
}
