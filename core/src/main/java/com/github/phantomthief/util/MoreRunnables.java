package com.github.phantomthief.util;

import java.util.function.Supplier;

/**
 * MoreRunnables增强工具集合
 * <p>用于增强{@link Runnable}的工具集合</p>
 *
 * @author w.vela
 */
public final class MoreRunnables {

    /**
     * 将给定的{@link Runnable}包装为只执行一次的对象，通常可以用来做首次运行时初始化的工作
     *
     * @param runnable 传入的{@link Runnable}
     * @return 经过包装，只运行一次的{@link Runnable}
     */
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
