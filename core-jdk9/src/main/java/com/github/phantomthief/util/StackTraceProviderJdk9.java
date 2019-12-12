package com.github.phantomthief.util;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

import java.lang.StackWalker.StackFrame;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * use for jdk9+, for performances.
 *
 * @author w.vela
 * Created on 2019-07-31.
 */
public class StackTraceProviderJdk9 implements StackTraceProvider {

    private final StackWalker stackWalker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);

    @Nullable
    public StackTraceElement getCallerPlace(Class<?>... locationAwareClasses) {
        return stackWalker.walk(new Function<Stream<StackFrame>, StackTraceElement>() {

            private boolean afterSelf = false;
            private boolean afterDeprecated = false;
            private Class<?> deprecatedClass = null;

            private boolean contains(Class<?> type) {
                for (Class<?> locationAwareClass : locationAwareClasses) {
                    if (type == locationAwareClass) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public StackTraceElement apply(Stream<StackFrame> stream) {
                return stream
                        .filter(stack -> {
                            Class<?> declaringClass = stack.getDeclaringClass();
                            if (contains(declaringClass)) {
                                afterSelf = true;
                                return false;
                            }
                            if (afterSelf) {
                                if (deprecatedClass == null) {
                                    deprecatedClass = declaringClass;
                                }
                            }
                            if (declaringClass == deprecatedClass) {
                                afterDeprecated = true;
                                return false;
                            }
                            return afterDeprecated;
                        })
                        .findAny()
                        .map(StackFrame::toStackTraceElement)
                        .orElse(null);
            }
        });
    }
}
