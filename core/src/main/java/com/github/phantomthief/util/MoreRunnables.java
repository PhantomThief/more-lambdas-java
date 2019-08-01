package com.github.phantomthief.util;

import java.util.function.Supplier;

/**
 * @author w.vela
 */
public final class MoreRunnables {

    public static Runnable runOnce(Runnable runnable) {
        return new Runnable() {

            private final Supplier<Void> supplier = MoreSuppliers.lazy(() -> {
                runnable.run();
                return null;
            });

            @Override
            public void run() {
                supplier.get();
            }
        };
    }
}
