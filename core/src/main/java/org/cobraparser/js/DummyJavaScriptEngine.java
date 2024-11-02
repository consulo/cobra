package org.cobraparser.js;

import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.HTMLAbstractUIElement;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.domimpl.HTMLScriptElementImpl;
import org.cobraparser.html.domimpl.NodeImpl;
import org.cobraparser.html.js.*;
import org.cobraparser.ua.UserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.views.DocumentView;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
class DummyJavaScriptEngine implements JavaScriptEngine {
    private static class DummyEventTargetManager implements EventTargetManager {
        private static final DummyEventTargetManager ourInstance = new DummyEventTargetManager();

        @Override
        public void addEventListener(NodeImpl node, String type, EventListener listener, boolean useCapture) {

        }

        @Override
        public void addEventListener(NodeImpl node, String type, Object listener) {

        }

        @Override
        public void addEventListener(NodeImpl node, String type, Object listener, boolean useCapture) {

        }

        @Override
        public boolean dispatchEvent(NodeImpl node, Event evt) throws EventException {
            return false;
        }

        @Override
        public void removeEventListener(NodeImpl node, String type, EventListener listener, boolean useCapture) {

        }

        @Override
        public void removeEventListener(NodeImpl node, String type, Object listener, boolean useCapture) {

        }
    }

    private static class DummyWindow implements Window {
        private static final DummyWindow ourInstance = new DummyWindow();

        @Override
        public Location getLocation() {
            return null;
        }

        @Override
        public void addJSTask(JSTask task) {

        }

        @Override
        public void addJSTaskUnchecked(JSTask task) {

        }

        @Override
        public int addJSUniqueTask(int oldId, JSTask task) {
            return 0;
        }

        @Override
        public EventTargetManager getEventTargetManager() {
            return DummyEventTargetManager.ourInstance;
        }

        @Override
        public void setDocument(Document document) {

        }

        @Override
        public void domContentLoaded(Event domContentLoadedEvent) {

        }

        @Override
        public void jobsFinished() {

        }

        @Override
        public boolean hasPendingTasks() {
            return false;
        }

        @Override
        public void evalInScope(String javascript) {

        }

        @Override
        public Window open(String relativeUrl, String windowName, String windowFeatures, boolean replace) {
            return ourInstance;
        }

        @Override
        public void addEventListener(String type, EventListener listener, boolean useCapture) {

        }

        @Override
        public void removeEventListener(String type, EventListener listener, boolean useCapture) {

        }

        @Override
        public boolean dispatchEvent(org.w3c.dom.events.Event evt) throws EventException {
            return false;
        }

        @Override
        public DocumentView getDocument() {
            return null;
        }
    }

    private static class DummyEvent implements Event {
        private static final DummyEvent ourInstance = new DummyEvent();

        @Override
        public boolean isPropagationStopped() {
            return false;
        }

        @Override
        public void setPhase(short newPhase) {

        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public EventTarget getTarget() {
            return null;
        }

        @Override
        public EventTarget getCurrentTarget() {
            return null;
        }

        @Override
        public short getEventPhase() {
            return 0;
        }

        @Override
        public boolean getBubbles() {
            return false;
        }

        @Override
        public boolean getCancelable() {
            return false;
        }

        @Override
        public long getTimeStamp() {
            return 0;
        }

        @Override
        public void stopPropagation() {

        }

        @Override
        public void preventDefault() {

        }

        @Override
        public void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg) {

        }
    }

    @Override
    public Window createWindow(HtmlRendererContext rcontext, UserAgentContext uaContext) {
        return DummyWindow.ourInstance;
    }

    @Override
    public Window getWindow(HtmlRendererContext rcontext) {
        return DummyWindow.ourInstance;
    }

    @Override
    public Event createEvent(String type, Node srcElement) {
        return DummyEvent.ourInstance;
    }

    @Override
    public Event createEvent(String type, Node srcElement, KeyEvent keyEvent) {
        return DummyEvent.ourInstance;
    }

    @Override
    public Event createEvent(String type, Node srcElement, InputEvent mouseEvent, int leafX, int leafY) {
        return DummyEvent.ourInstance;
    }

    @Override
    public Object getJavaObject(Object javascriptObject, Class<?> type) {
        return null;
    }

    @Override
    public boolean executeFunction(NodeImpl element, Object f, Object event, Window window) {
        return false;
    }

    @Override
    public Map<String, Object> getEventFunctions(HTMLAbstractUIElement element, Map<String, Object> map, Object varValue, String attributeName, Object[] result) {
        return Map.of();
    }

    @Override
    public void evaluateScript(HTMLDocumentImpl doc, HTMLScriptElementImpl scriptElement, String text, String scriptURI, int baseLineNumber, UserAgentContext bcontext) {

    }
}
