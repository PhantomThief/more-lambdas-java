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
    public StackTraceElement getCallerPlace(Class<?> locationAwareClass) {
        return stackWalker.walk(new Function<Stream<StackFrame>, StackTraceElement>() {

            private boolean afterSelf = false;
            private boolean afterDeprecated = false;
            private Class<?> deprecatedClass = null;

            @Override
            public StackTraceElement apply(Stream<StackFrame> stream) {
                return stream
                        .filter(stack -> {
                            if (stack.getDeclaringClass() == locationAwareClass) {
                                afterSelf = true;
                                return false;
                            }
                            if (afterSelf) {
                                if (deprecatedClass == null
                                        && stack.getDeclaringClass() != locationAwareClass) {
                                    deprecatedClass = stack.getDeclaringClass();
                                }
                            }
                            if (stack.getDeclaringClass() == deprecatedClass) {
                                afterDeprecated = true;
                                return false;
                            }
                            return afterDeprecated;
                        })
                        .map(StackFrame::toStackTraceElement)
                        .findFirst()
                        .orElse(null);
            }
        });
    }
}
