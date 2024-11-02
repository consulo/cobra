package org.cobraparser.js;

import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.HTMLAbstractUIElement;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.domimpl.HTMLScriptElementImpl;
import org.cobraparser.html.domimpl.NodeImpl;
import org.cobraparser.html.js.Event;
import org.cobraparser.html.js.Window;
import org.cobraparser.ua.UserAgentContext;
import org.w3c.dom.Node;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
public interface JavaScriptEngine {
    static JavaScriptEngine get() {
        return JavaScriptEngineHolder.ourInstance;
    }
    
    Window createWindow(HtmlRendererContext rcontext, final UserAgentContext uaContext);

    Window getWindow(final HtmlRendererContext rcontext);

    Event createEvent(final String type, final Node srcElement);

    Event createEvent(final String type, final Node srcElement, final KeyEvent keyEvent);

    Event createEvent(final String type, final Node srcElement, final InputEvent mouseEvent, final int leafX, final int leafY);

    Object getJavaObject(final Object javascriptObject, final Class<?> type);

    boolean executeFunction(final NodeImpl element, final Object f, final Object event, final Window window);

    Map<String, Object> getEventFunctions(HTMLAbstractUIElement element, Map<String, Object> map, Object varValue, final String attributeName, Object[] result);

    void evaluateScript(HTMLDocumentImpl doc,
                        HTMLScriptElementImpl scriptElement,
                        String text,
                        final String scriptURI,
                        int baseLineNumber,
                        UserAgentContext bcontext);
}
