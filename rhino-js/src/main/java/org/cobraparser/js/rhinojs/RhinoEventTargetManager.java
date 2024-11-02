package org.cobraparser.js.rhinojs;

import org.cobraparser.html.domimpl.NodeImpl;
import org.cobraparser.html.js.Event;
import org.cobraparser.html.js.EventTargetManager;
import org.cobraparser.html.js.JSRunnableTask;
import org.cobraparser.html.js.Window;
import org.cobraparser.js.JavaScriptEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;

import java.util.*;

public final class RhinoEventTargetManager implements EventTargetManager {

    private final Map<NodeImpl, Map<String, List<EventListener>>> nodeOnEventListeners = new IdentityHashMap<>();
    private final Window window;

    public RhinoEventTargetManager(final Window window) {
        this.window = window;
    }

    @Override
    public void addEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture) {
        final List<EventListener> handlerList = getListenerList(type, node, true);
        handlerList.add(listener);
    }

    private List<EventListener> getListenerList(final String type, final NodeImpl node, final boolean createIfNotExist) {
        final Map<String, List<EventListener>> onEventListeners = getEventListeners(node, createIfNotExist);

        if (onEventListeners != null) {
            if (onEventListeners.containsKey(type)) {
                return onEventListeners.get(type);
            }
            else if (createIfNotExist) {
                final List<EventListener> handlerList = new ArrayList<>();
                onEventListeners.put(type, handlerList);
                return handlerList;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    private Map<String, List<EventListener>> getEventListeners(final NodeImpl node, final boolean createIfNotExist) {
        if (nodeOnEventListeners.containsKey(node)) {
            return nodeOnEventListeners.get(node);
        }
        else {
            if (createIfNotExist) {
                final Map<String, List<EventListener>> onEventListeners = new HashMap<>();
                nodeOnEventListeners.put(node, onEventListeners);
                return onEventListeners;
            }
            else {
                return null;
            }
        }
    }

    @Override
    public void removeEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture) {
        final Map<String, List<EventListener>> onEventListeners = getEventListeners(node, false);
        if (onEventListeners != null) {
            if (onEventListeners.containsKey(type)) {
                onEventListeners.get(type).remove(listener);
            }
        }

    }

    private List<Object> getFunctionList(final String type, final NodeImpl node, final boolean createIfNotExist) {
        final Map<String, List<Object>> onEventListeners = getEventFunctions(node, createIfNotExist);

        if (onEventListeners != null) {
            if (onEventListeners.containsKey(type)) {
                return onEventListeners.get(type);
            }
            else if (createIfNotExist) {
                final List<Object> handlerList = new ArrayList<>();
                onEventListeners.put(type, handlerList);
                return handlerList;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    private Map<String, List<Object>> getEventFunctions(final NodeImpl node, final boolean createIfNotExist) {
        if (nodeOnEventFunctions.containsKey(node)) {
            return nodeOnEventFunctions.get(node);
        }
        else {
            if (createIfNotExist) {
                final Map<String, List<Object>> onEventListeners = new HashMap<>();
                nodeOnEventFunctions.put(node, onEventListeners);
                return onEventListeners;
            }
            else {
                return null;
            }
        }
    }

    @Override
    public boolean dispatchEvent(final NodeImpl node, final Event evt) throws EventException {
        // dispatchEventToHandlers(node, evt, onEventListeners.get(evt.getType()));
        // dispatchEventToJSHandlers(node, evt, onEventHandlers.get(evt.getType()));

        // TODO: Event Bubbling
        // TODO: get Window into the propagation path
        final List<NodeImpl> propagationPath = getPropagationPath(node);

        // TODO: Capture phase, and distinction between target phase and bubbling phase
        evt.setPhase(org.w3c.dom.events.Event.AT_TARGET);
        // TODO: The JS Task should be added with the correct base URL
        window.addJSTask(new JSRunnableTask(0, "Event dispatch for " + evt, () -> {
            for (int i = 0; (i < propagationPath.size()) && !evt.isPropagationStopped(); i++) {
                final NodeImpl currNode = propagationPath.get(i);
                // System.out.println("Dipatching " + i + " to: " + currNode);
                // TODO: Make request manager checks here.
                dispatchEventToHandlers(currNode, evt);
                dispatchEventToJSHandlers(currNode, evt);
                evt.setPhase(org.w3c.dom.events.Event.BUBBLING_PHASE);
            }
        }
        ));

        // dispatchEventToHandlers(node, evt);
        // dispatchEventToJSHandlers(node, evt);
        return false;
    }

    private static List<NodeImpl> getPropagationPath(NodeImpl node) {
        final List<NodeImpl> nodes = new LinkedList<>();
        while (node != null) {
            if ((node instanceof Element) || (node instanceof Document)) { //  TODO || node instanceof Window) {
                nodes.add(node);
            }
            node = (NodeImpl) node.getParentNode();
        }

        // TODO
        // nodes.add(window);

        return nodes;
    }

    // private void dispatchEventToHandlers(final NodeImpl node, final Event event, final List<EventListener> handlers) {
    private void dispatchEventToHandlers(final NodeImpl node, final Event event) {
        final List<EventListener> handlers = getListenerList(event.getType(), node, false);
        if (handlers != null) {
            // We clone the collection and check if original collection still contains
            // the handler before dispatching
            // This is to avoid ConcurrentModificationException during dispatch
            final ArrayList<EventListener> handlersCopy = new ArrayList<>(handlers);
            for (final EventListener h : handlersCopy) {
                // TODO: Not sure if we should stop calling handlers after propagation is stopped
                // if (event.isPropagationStopped()) {
                // return;
                // }

                if (handlers.contains(h)) {
                    // window.addJSTask(new JSRunnableTask(0, "Event dispatch for: " + event, new Runnable(){
                    // public void run() {
                    h.handleEvent(event);
                    // }
                    // }));
                    // h.handleEvent(event);

                    // Executor.executeFunction(node, h, event);
                }
            }
        }
    }

    // protected void dispatchEventToJSHandlers(final NodeImpl node, final Event event, final List<Function> handlers) {
    protected void dispatchEventToJSHandlers(final NodeImpl node, final Event event) {
        final List<Object> handlers = getFunctionList(event.getType(), node, false);
        if (handlers != null) {
            // We clone the collection and check if original collection still contains
            // the handler before dispatching
            // This is to avoid ConcurrentModificationException during dispatch
            final ArrayList<Object> handlersCopy = new ArrayList<>(handlers);
            for (final Object h : handlersCopy) {
                // TODO: Not sure if we should stop calling handlers after propagation is stopped
                // if (event.isPropagationStopped()) {
                // return;
                // }

                if (handlers.contains(h)) {
                    // window.addJSTask(new JSRunnableTask(0, "Event dispatch for " + event, new Runnable(){
                    // public void run() {
                    JavaScriptEngine.get().executeFunction(node, h, event, window);
                    // }
                    // }));
                    // Executor.executeFunction(node, h, event);
                }
            }
        }
    }

    // private final Map<String, List<Function>> onEventHandlers = new HashMap<>();
    private final Map<NodeImpl, Map<String, List<Object>>> nodeOnEventFunctions = new IdentityHashMap<>();

    @Override
    public void addEventListener(final NodeImpl node, final String type, final Object listener) {
        addEventListener(node, type, listener, false);
    }

    @Override
    public void addEventListener(final NodeImpl node, final String type, final Object listener, final boolean useCapture) {
        // TODO
        // System.out.println("node by name: " + node.getNodeName() + " adding Event listener of type: " + type);

    /*
    List<Function> handlerList = null;
    if (onEventHandlers.containsKey(type)) {
      handlerList = onEventHandlers.get(type);
    } else {
      handlerList = new ArrayList<>();
      onEventHandlers.put(type, handlerList);
    }*/
        // final Map<String, List<Function>> handlerList = getEventFunctions(node, true);
        final List<Object> handlerList = getFunctionList(type, node, true);
        handlerList.add(listener);
    }

    @Override
    public void removeEventListener(final NodeImpl node, final String type, final Object listener, final boolean useCapture) {
        final Map<String, List<Object>> onEventListeners = getEventFunctions(node, false);
        if (onEventListeners != null) {
            if (onEventListeners.containsKey(type)) {
                onEventListeners.get(type).remove(listener);
            }
        }
    }

    public void reset() {
        nodeOnEventFunctions.clear();
        nodeOnEventListeners.clear();
    }

}
