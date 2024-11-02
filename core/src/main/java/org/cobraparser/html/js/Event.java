package org.cobraparser.html.js;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
public interface Event extends org.w3c.dom.events.Event {
    boolean isPropagationStopped();

    void setPhase(final short newPhase);
}
