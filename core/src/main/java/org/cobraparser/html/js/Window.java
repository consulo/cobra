package org.cobraparser.html.js;

import org.cobraparser.js.HideFromJS;
import org.cobraparser.util.ID;
import org.w3c.dom.Document;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.views.AbstractView;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
public interface Window extends AbstractView, EventTarget {
    Location getLocation();

    @HideFromJS
    void addJSTask(final JSTask task);

    @HideFromJS
    void addJSTaskUnchecked(final JSTask task);

    int addJSUniqueTask(final int oldId, final JSTask task);

    EventTargetManager getEventTargetManager();

    void setDocument(final Document document);

    void domContentLoaded(final Event domContentLoadedEvent);

    void jobsFinished();

    boolean hasPendingTasks();

    void evalInScope(final String javascript);

    default Window open(final String url) {
        return this.open(url, "window:" + String.valueOf(ID.generateLong()));
    }

    default Window open(final String url, final String windowName) {
        return this.open(url, windowName, "", false);
    }

    default Window open(final String url, final String windowName, final String windowFeatures) {
        return open(url, windowName, windowFeatures, false);
    }

    Window open(final String relativeUrl, final String windowName, final String windowFeatures, final boolean replace);
}
