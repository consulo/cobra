package org.cobraparser.js.rhinojs;

import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.HTMLAbstractUIElement;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.domimpl.HTMLScriptElementImpl;
import org.cobraparser.html.domimpl.NodeImpl;
import org.cobraparser.html.js.Event;
import org.cobraparser.html.js.JSRunnableTask;
import org.cobraparser.html.js.Window;
import org.cobraparser.js.JavaScriptEngine;
import org.cobraparser.ua.UserAgentContext;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
public class RhinoJavaScriptEngine implements JavaScriptEngine {
    private static final Logger logger = LoggerFactory.getLogger(RhinoJavaScriptEngine.class);

    @Override
    public Window createWindow(HtmlRendererContext rcontext, UserAgentContext uaContext) {
        return new RhinoWindow(rcontext, uaContext);
    }

    @Override
    public Window getWindow(HtmlRendererContext rcontext) {
        return RhinoWindow.getWindow(rcontext);
    }

    @Override
    public Event createEvent(String type, Node srcElement) {
        return new RhinoEvent(type, srcElement);
    }

    @Override
    public Event createEvent(String type, Node srcElement, KeyEvent keyEvent) {
        return new RhinoEvent(type, srcElement, keyEvent);
    }

    @Override
    public Event createEvent(String type, Node srcElement, InputEvent mouseEvent, int leafX, int leafY) {
        return new RhinoEvent(type, srcElement, mouseEvent, leafX, leafY);
    }

    @Override
    public Object getJavaObject(Object javascriptObject, Class<?> type) {
        return RhinoJavaScript.getInstance().getJavaObject(javascriptObject, type);
    }

    @Override
    public boolean executeFunction(NodeImpl element, Object f, Object event, Window window) {
        return Executor.executeFunction(element, (org.mozilla.javascript.Function) f, event, ((RhinoWindow) window).getContextFactory());
    }

    @Override
    public void evaluateScript(HTMLDocumentImpl doc,
                               HTMLScriptElementImpl scriptElement,
                               String text,
                               final String scriptURI,
                               int baseLineNumber,
                               UserAgentContext bcontext) {
        final Window window = doc.getWindow();
        if (text != null) {
            final String textSub = text.substring(0, Math.min(50, text.length())).replaceAll("\n", "");
            window.addJSTaskUnchecked(new JSRunnableTask(0, "script: " + textSub, new Runnable() {
                @Override
                public void run() {
                    // final Context ctx = Executor.createContext(HTMLScriptElementImpl.this.getDocumentURL(), bcontext);
                    final Context ctx = Executor.createContext(scriptElement.getDocumentURL(), bcontext, ((RhinoWindow) window).getContextFactory());
                    try {
                        final Scriptable scope = ((RhinoWindow) window).getWindowScope();
                        if (scope == null) {
                            throw new IllegalStateException("Scriptable (scope) instance was null");
                        }
                        try {
                            ctx.evaluateString(scope, text, scriptURI, baseLineNumber, null);
                            // Why catch this?
                            // } catch (final EcmaError ecmaError) {
                            // logger.log(Level.WARNING,
                            // "Javascript error at " + ecmaError.sourceName() + ":" + ecmaError.lineNumber() + ": " + ecmaError.getMessage(),
                            // ecmaError);
                        }
                        catch (final Exception err) {
                            Executor.logJSException(err);
                        }
                    }
                    finally {
                        Context.exit();
                        doc.markJobsFinished(1, false);
                    }
                }
            }));
        }
        else {
            doc.markJobsFinished(1, false);
        }
    }

    @Override
    public Map<String, Object> getEventFunctions(HTMLAbstractUIElement element,
                                                 Map<String, Object> map,
                                                 Object varValue,
                                                 final String attributeName,
                                                 Object[] result) {
        if (varValue != null) {
            result[0] = varValue;
            return map;
        }

        final String normalAttributeName = normalizeAttributeName(attributeName);
        synchronized (this) {
            Map<String, Object> fba = map;
            Object f = fba == null ? null : fba.get(normalAttributeName);
            if (f != null) {
                result[0] = f;
                return map;
            }
            final UserAgentContext uac = element.getUserAgentContext();
            if (uac == null) {
                throw new IllegalStateException("No user agent context.");
            }
            if (uac.isScriptingEnabled()) {
                final String attributeValue = element.getAttribute(attributeName);
                if ((attributeValue != null) && (attributeValue.length() != 0)) {
                    final String functionCode = "function " + normalAttributeName + "_" + System.identityHashCode(this) + "() { " + attributeValue
                        + " }";
                    final Document doc = element.getOwnerDocument();
                    if (doc == null) {
                        throw new IllegalStateException("Element does not belong to a document.");
                    }
                    final Window window = ((HTMLDocumentImpl) doc).getWindow();
                    final Context ctx = Executor.createContext(element.getDocumentURL(), uac, ((RhinoWindow) window).getContextFactory());
                    try {
                        final Scriptable scope = ((RhinoWindow) window).getWindowScope();
                        if (scope == null) {
                            throw new IllegalStateException("Scriptable (scope) instance was null");
                        }
                        final Scriptable thisScope = (Scriptable) RhinoJavaScript.getInstance().getJavascriptObject(this, scope);
                        try {
                            // TODO: Get right line number for script. //TODO: Optimize this
                            // in case it's called multiple times? Is that done?
                            f = ctx.compileFunction(thisScope, functionCode, element.getTagName() + "[" + element.getId() + "]." + attributeName, 1, null);
                        }
                        catch (final EcmaError ecmaError) {
                            logger.warn("Javascript error at " + ecmaError.sourceName() + ":" + ecmaError.lineNumber() + ": "
                                + ecmaError.getMessage(), ecmaError);
                            f = null;
                        }
                        catch (final Exception err) {
                            logger.warn("Unable to evaluate Javascript code", err);
                            f = null;
                        }
                    }
                    finally {
                        Context.exit();
                    }
                }
                if (fba == null) {
                    fba = new HashMap<>(1);
                }
                fba.put(normalAttributeName, f);
            }

            result[0] = f;
            return fba;
        }
    }

    protected final static String normalizeAttributeName(final String name) {
        return name.toLowerCase();
    }
}
