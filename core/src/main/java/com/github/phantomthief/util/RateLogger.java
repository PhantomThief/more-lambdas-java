package com.github.phantomthief.util;

import static com.github.phantomthief.tuple.Tuple.tuple;
import static org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;
import static org.slf4j.spi.LocationAwareLogger.INFO_INT;
import static org.slf4j.spi.LocationAwareLogger.TRACE_INT;
import static org.slf4j.spi.LocationAwareLogger.WARN_INT;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import com.github.phantomthief.tuple.ThreeTuple;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 使用 {@link SimpleRateLimiter} 来控制打 log 输出的频率，避免出现 log flood 占用过高的 CPU
 * 对于需要用一个 logger 打不同类型的日志，又不希望不同日志输出频率互相干扰（比如高频日志把低频日志淹没）
 * 可以使用 {@link #perFormatStringRateLogger(Logger)} 方式构建 logger，这时候，不同日志的区分方式是以 log.info(msg, args...);
 * 中第一个参数 msg 来区分，相同的 msg 会被认为是相同类型的日志，从而共享相同的频次限制；
 *
 * 使用方法:
 * {@code <pre>
 *
 * class MyObject {
 *   private static final Logger logger = LoggerFactory.getLogger(MyObject.class); // normal one
 *   private static final Logger rateLogger = RateLogger.rateLogger(logger); // wrap as a rate one
 *
 *   void foo() {
 *     rateLogger.info("my message"); // use as normal logger.
 *   }
 * }
 *
 * </pre>}
 *
 * @author w.vela
 * Created on 2017-02-24.
 */
public class RateLogger implements Logger {

    /**
     * fully qualified class name, for short
     */
    private static final String FQCN = RateLogger.class.getName();

    private static final double DEFAULT_PERMITS_PER_SECOND = 1;
    private static final int MAX_PER_FORMAT_CACHE_SIZE = 100;
    // 直接使用Map来cache RateLogger。Logger的数量是有限的，LogBack也是使用了Map来Cache，所以没必要用一个支持evict的Cache。
    private static final ConcurrentMap<ThreeTuple<String, Double, Boolean>, RateLogger> CACHE = new ConcurrentHashMap<>();

    private final Logger logger;
    private final LocationAwareLogger locationAwareLogger;

    private final SimpleRateLimiter rateLimiter;
    private final LoadingCache<String, SimpleRateLimiter> perFormatStringRateLimiter;

    private RateLogger(Logger logger, double permitsPerSecond, boolean perFormatString) {
        this.logger = logger;
        if (logger instanceof LocationAwareLogger) {
            this.locationAwareLogger = (LocationAwareLogger) logger;
        } else {
            this.locationAwareLogger = null;
        }
        if (perFormatString) {
            this.perFormatStringRateLimiter = CacheBuilder.newBuilder()
                    .maximumSize(MAX_PER_FORMAT_CACHE_SIZE)
                    .build(new CacheLoader<String, SimpleRateLimiter>() {
                        @Override
                        public SimpleRateLimiter load(String s) {
                            return SimpleRateLimiter.create(permitsPerSecond);
                        }
                    });
        } else {
            this.perFormatStringRateLimiter = null;
        }
        this.rateLimiter = SimpleRateLimiter.create(permitsPerSecond);
    }

    /**
     * 工厂方法
     *
     * @param logger 要封装的logger实例
     */
    public static RateLogger rateLogger(Logger logger) {
        return rateLogger(logger, DEFAULT_PERMITS_PER_SECOND);
    }

    /**
     * 工厂方法，和 {@link #rateLogger} 的区别是，会按照不同的 msg 分别采样计算
     *
     * @param logger 要封装的logger实例
     */
    public static RateLogger perFormatStringRateLogger(Logger logger) {
        return perFormatStringRateLogger(logger,  DEFAULT_PERMITS_PER_SECOND);
    }

    /**
     * 工厂方法
     *
     * @param logger 要封装的logger实例
     * @param permitsPer 打log的每秒允许个数，例如传入0.2，就意味着五秒打一条log
     */
    public static RateLogger rateLogger(Logger logger, double permitsPer) {
        return rateLogger(logger, permitsPer, false);
    }

    /**
     * 工厂方法，和 {@link #rateLogger} 的区别是，会按照不同的 msg 分别采样计算
     *
     * @param logger 要封装的logger实例
     * @param permitsPer 打log的每秒允许个数，例如传入0.2，就意味着五秒打一条log
     */
    public static RateLogger perFormatStringRateLogger(Logger logger, double permitsPer) {
        return rateLogger(logger, permitsPer, true);
    }

    /**
     * 工厂方法
     *
     * @param logger 要封装的logger实例
     * @param permitsPer 打log的每秒允许个数，例如传入0.2，就意味着五秒打一条log
     * @param perFormatString 如果为 {@code true}，则按照每个 formatString 为单位而不是整个 logger 为单位执行采样
     */
    private static RateLogger rateLogger(Logger logger, double permitsPer, boolean perFormatString) {
        String name = logger.getName();
        ThreeTuple<String, Double, Boolean> key = tuple(name, permitsPer, perFormatString);
        RateLogger rateLogger = CACHE.get(key);
        if (rateLogger != null) {
            return rateLogger;
        }
        return CACHE.computeIfAbsent(key, it -> new RateLogger(logger, it.getSecond(), it.getThird()));
    }

    private SimpleRateLimiter canDo(@Nullable String msg) {
        if (msg == null) {
            return rateLimiter;
        } else {
            if (perFormatStringRateLimiter == null) {
                return rateLimiter;
            } else {
                return perFormatStringRateLimiter.getUnchecked(msg);
            }
        }
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    private String msg(String msg) {
        return "[IGNORED={}]" + msg;
    }

    private Object[] args(SimpleRateLimiter limiter, Object[] args) {
        Object[] result;
        long skip = limiter.getSkipCountAndClear();
        if (args == null) {
            result = new Object[] {skip};
        } else {
            result = ArrayUtils.addAll(new Object[] {skip}, args);
        }
        return result;
    }

    private Object[] args(SimpleRateLimiter limiter, Object arg) {
        return args(limiter, new Object[] {arg});
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, TRACE_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.trace(msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, TRACE_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.trace(msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, TRACE_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.trace(msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, TRACE_INT, msg(format), args(limiter, arguments), null);
                } else {
                    logger.trace(msg(format), args(limiter, arguments));
                }
            }
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, TRACE_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.trace(msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (isTraceEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.trace(marker, msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (isTraceEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.trace(marker, msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (isTraceEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.trace(marker, msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        if (isTraceEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(format), args(limiter, argArray), null);
                } else {
                    logger.trace(marker, msg(format), args(limiter, argArray));
                }
            }
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (isTraceEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.trace(marker, msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.debug(msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.debug(msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.debug(msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(format), args(limiter, arguments), null);
                } else {
                    logger.debug(msg(format), args(limiter, arguments));
                }
            }
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.debug(msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (isDebugEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.debug(marker, msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (isDebugEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.debug(marker, msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (isDebugEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.debug(marker, msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        if (isDebugEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(format), args(limiter, argArray), null);
                } else {
                    logger.debug(marker, msg(format), args(limiter, argArray));
                }
            }
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (isDebugEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.debug(marker, msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, INFO_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.info(msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, INFO_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.info(msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, INFO_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.info(msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, INFO_INT, msg(format), args(limiter, arguments), null);
                } else {
                    logger.info(msg(format), args(limiter, arguments));
                }
            }
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, INFO_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.info(msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (isInfoEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, INFO_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.info(marker, msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (isInfoEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, INFO_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.info(marker, msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (isInfoEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, INFO_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.info(marker, msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        if (isInfoEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, INFO_INT, msg(format), args(limiter, argArray), null);
                } else {
                    logger.info(marker, msg(format), args(limiter, argArray));
                }
            }
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (isInfoEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, INFO_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.info(marker, msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, WARN_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.warn(msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, WARN_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.warn(msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, WARN_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.warn(msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, WARN_INT, msg(format), args(limiter, arguments), null);
                } else {
                    logger.warn(msg(format), args(limiter, arguments));
                }
            }
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, WARN_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.warn(msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (isWarnEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, WARN_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.warn(marker, msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (isWarnEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, WARN_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.warn(marker, msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (isWarnEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, WARN_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.warn(marker, msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        if (isWarnEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, WARN_INT, msg(format), args(limiter, argArray), null);
                } else {
                    logger.warn(marker, msg(format), args(limiter, argArray));
                }
            }
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (isWarnEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, WARN_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.warn(marker, msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, ERROR_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.error(msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, ERROR_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.error(msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, ERROR_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.error(msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, ERROR_INT, msg(format), args(limiter, arguments), null);
                } else {
                    logger.error(msg(format), args(limiter, arguments));
                }
            }
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(null, FQCN, ERROR_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.error(msg(msg), args(limiter, t));
                }
            }
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (isErrorEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(msg), args(limiter, null), null);
                } else {
                    logger.error(marker, msg(msg), args(limiter, null));
                }
            }
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (isErrorEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(format), args(limiter, arg), null);
                } else {
                    logger.error(marker, msg(format), args(limiter, arg));
                }
            }
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (isErrorEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(format), args(limiter, new Object[] {arg1, arg2}), null);
                } else {
                    logger.error(marker, msg(format), args(limiter, new Object[] {arg1, arg2}));
                }
            }
        }
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        if (isErrorEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(format);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(format), args(limiter, argArray), null);
                } else {
                    logger.error(marker, msg(format), args(limiter, argArray));
                }
            }
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (isErrorEnabled(marker)) {
            SimpleRateLimiter limiter = canDo(msg);
            if (limiter.tryAcquire()) {
                if (locationAwareLogger != null) {
                    locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(msg), args(limiter, null), t);
                } else {
                    logger.error(marker, msg(msg), args(limiter, t));
                }
            }
        }
    }
}
