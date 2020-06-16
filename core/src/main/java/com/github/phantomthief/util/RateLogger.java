package com.github.phantomthief.util;

import static com.github.phantomthief.tuple.Tuple.tuple;
import static org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;
import static org.slf4j.spi.LocationAwareLogger.INFO_INT;
import static org.slf4j.spi.LocationAwareLogger.TRACE_INT;
import static org.slf4j.spi.LocationAwareLogger.WARN_INT;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import com.github.phantomthief.tuple.TwoTuple;

/**
 * 使用 {@link SimpleRateLimiter} 来控制打 log 输出的频率，避免出现 log flood 占用过高的 CPU
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
    // 直接使用Map来cache RateLogger。Logger的数量是有限的，LogBack也是使用了Map来Cache，所以没必要用一个支持evict的Cache。
    private static final ConcurrentMap<TwoTuple<String, Double>, RateLogger> CACHE = new ConcurrentHashMap<>();

    private final Logger logger;
    private final LocationAwareLogger locationAwareLogger;

    private final SimpleRateLimiter rateLimiter;

    private RateLogger(Logger logger, double permitsPerSecond) {
        this.logger = logger;
        if (logger instanceof LocationAwareLogger) {
            this.locationAwareLogger = (LocationAwareLogger) logger;
        } else {
            this.locationAwareLogger = null;
        }
        this.rateLimiter = SimpleRateLimiter.create(permitsPerSecond);
    }

    public static RateLogger rateLogger(Logger logger) {
        return rateLogger(logger, DEFAULT_PERMITS_PER_SECOND);
    }

    /**
     * 工厂方法
     *
     * @param logger 要封装的logger实例
     * @param permitsPer 打log的每秒允许个数，例如传入0.2，就意味着五秒打一条log
     */
    public static RateLogger rateLogger(Logger logger, double permitsPer) {
        String name = logger.getName();
        TwoTuple<String, Double> key = tuple(name, permitsPer);
        RateLogger rateLogger = CACHE.get(key);
        if (rateLogger != null) {
            return rateLogger;
        }
        return CACHE.computeIfAbsent(key, it -> new RateLogger(logger, it.getSecond()));
    }

    private boolean canDo() {
        return rateLimiter.tryAcquire();
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    private String msg(String msg) {
        return "[IGNORED={}]" + msg;
    }

    private Object[] args(Object[] args) {
        Object[] result;
        long skip = rateLimiter.getSkipCountAndClear();
        if (args == null) {
            result = new Object[] {skip};
        } else {
            result = ArrayUtils.addAll(new Object[] {skip}, args);
        }
        return result;
    }

    private Object[] args(Object arg) {
        return args(new Object[]{arg});
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, TRACE_INT, msg(msg), args(null), null);
            } else {
                logger.trace(msg(msg), args(null));
            }
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, TRACE_INT, msg(format), args(arg), null);
            } else {
                logger.trace(msg(format), args(arg));
            }
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, TRACE_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.trace(msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, TRACE_INT, msg(format), args(arguments), null);
            } else {
                logger.trace(msg(format), args(arguments));
            }
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, TRACE_INT, msg(msg), args(null), t);
            } else {
                logger.trace(msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (isTraceEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(msg), args(null), null);
            } else {
                logger.trace(marker, msg(msg), args(null));
            }
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (isTraceEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(format), args(arg), null);
            } else {
                logger.trace(marker, msg(format), args(arg));
            }
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (isTraceEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.trace(marker, msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        if (isTraceEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(format), args(argArray), null);
            } else {
                logger.trace(marker, msg(format), args(argArray));
            }
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (isTraceEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, TRACE_INT, msg(msg), args(null), t);
            } else {
                logger.trace(marker, msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(msg), args(null), null);
            } else {
                logger.debug(msg(msg), args(null));
            }
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(format), args(arg), null);
            } else {
                logger.debug(msg(format), args(arg));
            }
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.debug(msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(format), args(arguments), null);
            } else {
                logger.debug(msg(format), args(arguments));
            }
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, DEBUG_INT, msg(msg), args(null), t);
            } else {
                logger.debug(msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (isDebugEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(msg), args(null), null);
            } else {
                logger.debug(marker, msg(msg), args(null));
            }
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (isDebugEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(format), args(arg), null);
            } else {
                logger.debug(marker, msg(format), args(arg));
            }
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (isDebugEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.debug(marker, msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        if (isDebugEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(format), args(argArray), null);
            } else {
                logger.debug(marker, msg(format), args(argArray));
            }
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (isDebugEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, DEBUG_INT, msg(msg), args(null), t);
            } else {
                logger.debug(marker, msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, INFO_INT, msg(msg), args(null), null);
            } else {
                logger.info(msg(msg), args(null));
            }
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, INFO_INT, msg(format), args(arg), null);
            } else {
                logger.info(msg(format), args(arg));
            }
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, INFO_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.info(msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, INFO_INT, msg(format), args(arguments), null);
            } else {
                logger.info(msg(format), args(arguments));
            }
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, INFO_INT, msg(msg), args(null), t);
            } else {
                logger.info(msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (isInfoEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, INFO_INT, msg(msg), args(null), null);
            } else {
                logger.info(marker, msg(msg), args(null));
            }
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (isInfoEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, INFO_INT, msg(format), args(arg), null);
            } else {
                logger.info(marker, msg(format), args(arg));
            }
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (isInfoEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, INFO_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.info(marker, msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        if (isInfoEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, INFO_INT, msg(format), args(argArray), null);
            } else {
                logger.info(marker, msg(format), args(argArray));
            }
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (isInfoEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, INFO_INT, msg(msg), args(null), t);
            } else {
                logger.info(marker, msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, WARN_INT, msg(msg), args(null), null);
            } else {
                logger.warn(msg(msg), args(null));
            }
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, WARN_INT, msg(format), args(arg), null);
            } else {
                logger.warn(msg(format), args(arg));
            }
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, WARN_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.warn(msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, WARN_INT, msg(format), args(arguments), null);
            } else {
                logger.warn(msg(format), args(arguments));
            }
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, WARN_INT, msg(msg), args(null), t);
            } else {
                logger.warn(msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (isWarnEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, WARN_INT, msg(msg), args(null), null);
            } else {
                logger.warn(marker, msg(msg), args(null));
            }
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (isWarnEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, WARN_INT, msg(format), args(arg), null);
            } else {
                logger.warn(marker, msg(format), args(arg));
            }
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (isWarnEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, WARN_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.warn(marker, msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        if (isWarnEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, WARN_INT, msg(format), args(argArray), null);
            } else {
                logger.warn(marker, msg(format), args(argArray));
            }
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (isWarnEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, WARN_INT, msg(msg), args(null), t);
            } else {
                logger.warn(marker, msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, ERROR_INT, msg(msg), args(null), null);
            } else {
                logger.error(msg(msg), args(null));
            }
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, ERROR_INT, msg(format), args(arg), null);
            } else {
                logger.error(msg(format), args(arg));
            }
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, ERROR_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.error(msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, ERROR_INT, msg(format), args(arguments), null);
            } else {
                logger.error(msg(format), args(arguments));
            }
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled() && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(null, FQCN, ERROR_INT, msg(msg), args(null), t);
            } else {
                logger.error(msg(msg), args(t));
            }
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (isErrorEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(msg), args(null), null);
            } else {
                logger.error(marker, msg(msg), args(null));
            }
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (isErrorEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(format), args(arg), null);
            } else {
                logger.error(marker, msg(format), args(arg));
            }
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (isErrorEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(format),
                        args(new Object[]{arg1, arg2}), null);
            } else {
                logger.error(marker, msg(format), args(new Object[]{arg1, arg2}));
            }
        }
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        if (isErrorEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(format), args(argArray), null);
            } else {
                logger.error(marker, msg(format), args(argArray));
            }
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (isErrorEnabled(marker) && canDo()) {
            if (locationAwareLogger != null) {
                locationAwareLogger.log(marker, FQCN, ERROR_INT, msg(msg), args(null), t);
            } else {
                logger.error(marker, msg(msg), args(t));
            }
        }
    }
}
