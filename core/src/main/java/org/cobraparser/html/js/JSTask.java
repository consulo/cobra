package org.cobraparser.html.js;

public abstract class JSTask implements Comparable<JSTask> {
    protected final int priority;
    protected final long creationTime;
    protected final String description;

    // TODO: Add a context parameter that will be combined with current context, to help with creation of timer tasks
    // public JSTask(final int priority, final Runnable runnable) {
    public JSTask(final int priority, final String description) {
        this.priority = priority;
        this.description = description;
        this.creationTime = System.nanoTime();
    }

    // TODO: Add a way to stop a task. It should return false if the task can't be stopped in which case a thread kill will be performed by the task scheduler.

    // TODO: Sorting by priority
    public int compareTo(final JSTask o) {
        final long diffCreation = (o.creationTime - creationTime);
        if (diffCreation < 0) {
            return 1;
        }
        else if (diffCreation == 0) {
            return 0;
        }
        else {
            return -1;
        }
    }

    public abstract void run();
}
