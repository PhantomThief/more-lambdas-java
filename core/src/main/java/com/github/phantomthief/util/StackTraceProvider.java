package com.github.phantomthief.util;

import javax.annotation.Nullable;

/**
 * @author w.vela
 * Created on 2019-08-01.
 */
interface StackTraceProvider {

    /**
     * 返回 {@param locationAwareClasses} 最接近调用方方向第二个元素（跳过一个）
     */
    @Nullable
    StackTraceElement getCallerPlace(Class<?>... locationAwareClasses);
}
