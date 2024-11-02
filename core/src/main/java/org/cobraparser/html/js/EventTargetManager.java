package org.cobraparser.html.js;

import org.cobraparser.html.domimpl.NodeImpl;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
public interface EventTargetManager {
    void addEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture);

    void addEventListener(final NodeImpl node, final String type, final Object listener);

    void addEventListener(final NodeImpl node, final String type, final Object listener, final boolean useCapture);

    boolean dispatchEvent(final NodeImpl node, final Event evt) throws EventException;

    void removeEventListener(final NodeImpl node, final String type, final EventListener listener, final boolean useCapture);

    void removeEventListener(final NodeImpl node, final String type, final Object listener, final boolean useCapture);
}
