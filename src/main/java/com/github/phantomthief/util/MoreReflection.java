package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.RateLimiter.create;
import static java.lang.Thread.currentThread;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

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

    /**
     * @param message should contains {} for output placeholder.
     */
    public static void logDeprecated(@Nonnull String message) {
        if (RATE_LIMITER.tryAcquire()) {
            checkNotNull(message);
            logDeprecated(it -> logger.info(message, it));
        }
    }

    static void logDeprecated(@Nonnull Consumer<String> consumer) {
        List<StackTraceElement> stackTrace = newArrayList(currentThread().getStackTrace());
        boolean afterSelf = false;
        boolean afterDeprecated = false;
        String deprecatedClass = null;
        String found = null;
        for (StackTraceElement stack : stackTrace) {
            if (stack.getClassName().equals(MoreReflection.class.getName())) {
                afterSelf = true;
                continue;
            }
            if (afterSelf) {
                if (deprecatedClass == null
                        && !stack.getClassName().equals(MoreReflection.class.getName())) {
                    deprecatedClass = stack.getClassName();
                }
            }
            if (stack.getClassName().equals(deprecatedClass)) {
                afterDeprecated = true;
                continue;
            }
            if (afterDeprecated) {
                found = stack.getFileName() + ":" + stack.getLineNumber();
                break;
            }
        }
        if (found != null) {
            consumer.accept(found);
        }
    }
}
