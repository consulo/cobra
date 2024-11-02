package org.cobraparser.html.js;

public final class JSRunnableTask extends JSTask {
    private final Runnable runnable;

    public JSRunnableTask(final int priority, final Runnable runnable) {
        this(priority, "", runnable);
    }

    public JSRunnableTask(final int priority, final String description, final Runnable runnable) {
        super(priority, description);
        this.runnable = runnable;
    }

    @Override
    public String toString() {
        // return "JSRunnableTask [priority=" + priority + ", runnable=" + runnable + ", creationTime=" + creationTime + "]";
        return "JSRunnableTask [priority=" + priority + ", description=" + description + ", creationTime=" + creationTime + "]";
    }

    @Override
    public void run() {
        runnable.run();
    }

}
