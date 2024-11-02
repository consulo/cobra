package org.cobraparser.html.js;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class JSSupplierTask<T> extends JSTask {
    private final Supplier<T> supplier;
    private final Consumer<T> consumer;

    public JSSupplierTask(final int priority, final Supplier<T> supplier, final Consumer<T> consumer) {
        super(priority, "supplier description TODO");
        this.supplier = supplier;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        final T result = supplier.get();
        consumer.accept(result);
    }
}
