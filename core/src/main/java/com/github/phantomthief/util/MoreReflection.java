package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

/**
 * @author w.vela
 * Created on 2018-06-13.
 */
public class MoreReflection {

    private static final Logger logger = LoggerFactory.getLogger(MoreReflection.class);

    private static final SimpleRateLimiter RATE_LIMITER = SimpleRateLimiter.create(1);

    private static final StackTraceProvider STACK_TRACE_PROVIDER;

    static {
        StackTraceProvider temp = null;
        try {
            ServiceLoader<StackTraceProvider> loader = ServiceLoader.load(StackTraceProvider.class);
            Iterator<StackTraceProvider> iterator = loader.iterator();
            if (iterator.hasNext()) {
                temp = iterator.next();
            }
        } catch (UnsupportedClassVersionError e) {
            logger.info("failed to use jdk9's [JEP 259: Stack-Walking API] as caller tracker.");
        } catch (Throwable e) {
            logger.warn("failed to use jdk9's [JEP 259: Stack-Walking API] as caller tracker.", e);
        }
        if (temp == null) {
            temp = new StackTraceProviderJdk8();
        }
        STACK_TRACE_PROVIDER = temp;
        logger.info("using [{}] as caller tracker implementation.", STACK_TRACE_PROVIDER.getClass().getName());
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
        String name = locationAwareClass.getName();
        return STACK_TRACE_PROVIDER.getCallerPlace(name::equals, Predicates.alwaysFalse());
    }

    @Nullable
    public static StackTraceElement getCallerPlaceEx(@Nonnull Predicate<String> locationAwareClass,
            @Nonnull Predicate<String> ignore) {
        return STACK_TRACE_PROVIDER.getCallerPlace(checkNotNull(locationAwareClass), checkNotNull(ignore));
    }

    static StackTraceProvider getStackTraceProvider() {
        return STACK_TRACE_PROVIDER;
    }
}
