package com.example.aimassist;

import java.util.function.Supplier;

/** Temporary pick context, independent of stored configuration and other threads. */
public final class AimAssistMarginScopeTodoAi {
    private static final ThreadLocal<Float> CURRENT = new ThreadLocal<>();
    private AimAssistMarginScopeTodoAi() {}

    public static Float current() { return CURRENT.get(); }

    public static <T> T withMargin(Float margin, Supplier<T> action) {
        Float previous = CURRENT.get();
        try {
            if (margin != null) CURRENT.set(margin);
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}