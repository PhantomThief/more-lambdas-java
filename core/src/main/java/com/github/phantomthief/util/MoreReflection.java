package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.RateLimiter.create;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author w.vela
 * Created on 2018-06-13.
 */
public class MoreReflection {

    private static final Logger logger = LoggerFactory.getLogger(MoreReflection.class);

    private static final RateLimiter RATE_LIMITER = create(1);

    private static final StackTraceProvider STACK_TRACE_PROVIDER;

    static {
        StackTraceProvider temp = null;
        try {
            ServiceLoader<StackTraceProvider> loader = ServiceLoader.load(StackTraceProvider.class);
            Iterator<StackTraceProvider> iterator = loader.iterator();
            if (iterator.hasNext()) {
                temp = iterator.next();
            }
        } catch (Throwable e) {
            temp = new StackTraceProviderJdk8();
        }
        if (temp == null) {
            temp = new StackTraceProviderJdk8();
        }
        STACK_TRACE_PROVIDER = temp;
        logger.info("using [{}] as caller track implements", STACK_TRACE_PROVIDER.getClass().getName());
    }

    /**
     * @param message should contains {} for output placeholder.
     */
    public static void logDeprecated(@Nonnull String message) {
        if (RATE_LIMITER.tryAcquire()) {
            checkNotNull(message);
            StackTraceElement stack = getCallerPlace();
            if (stack != null) {
                logger.info(message, stack.getFileName() + ":" + stack.getLineNumber());
            }
        }
    }

    @Nullable
    public static StackTraceElement getCallerPlace() {
        return getCallerPlace(MoreReflection.class);
    }

    @Nullable
    public static StackTraceElement getCallerPlace(Class<?> locationAwareClass) {
        return STACK_TRACE_PROVIDER.getCallerPlace(locationAwareClass);
    }

    static StackTraceProvider getStackTraceProvider() {
        return STACK_TRACE_PROVIDER;
    }
}
