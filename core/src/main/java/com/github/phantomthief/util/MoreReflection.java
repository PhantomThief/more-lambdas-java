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
 * MoreReflection增强工具集合
 * <p>用于通过反射进行的一些通用操作的工具集合</p>
 *
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
     * 打印废弃方法调用的日志，默认将方法的调用位置作为参数，所以message中至少包含一个占位符。此日志只打印一次
     *
     * @param message 消息模板，至少包含一个日志变量占位符，用于输出调用位置的文件和行号
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

    /**
     * 获取当前方法的调用者，通过获取当前的调用栈向上查找获得
     *
     * @return StackTraceElement
     */
    @Nullable
    public static StackTraceElement getCallerPlace() {
        return getCallerPlace(MoreReflection.class);
    }

    /**
     * 获取当前方法的调用者，从指定的类位置向上查找
     *
     * @param locationAwareClass 指定类的类型
     * @return StackTraceElement
     */
    @Nullable
    public static StackTraceElement getCallerPlace(Class<?> locationAwareClass) {
        String name = locationAwareClass.getName();
        return STACK_TRACE_PROVIDER.getCallerPlace(name::equals, Predicates.alwaysFalse());
    }

    /**
     * 获取当前方法的调用者，从指定的类位置向上查找，并以指定的条件进行忽略
     *
     * @param locationAwareClass 指定类的类型
     * @param ignore 忽略条件断言
     * @return StackTraceElement
     */
    @Nullable
    public static StackTraceElement getCallerPlaceEx(@Nonnull Predicate<String> locationAwareClass,
            @Nonnull Predicate<String> ignore) {
        return STACK_TRACE_PROVIDER.getCallerPlace(checkNotNull(locationAwareClass), checkNotNull(ignore));
    }

    static StackTraceProvider getStackTraceProvider() {
        return STACK_TRACE_PROVIDER;
    }
}
