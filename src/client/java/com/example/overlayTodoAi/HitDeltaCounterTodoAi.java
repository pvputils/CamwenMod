package com.example.overlayTodoAi;

import java.awt.Color;
import java.awt.Graphics2D;

/** Session-local balance of server-confirmed player damage events. */
public final class HitDeltaCounterTodoAi {
    private static Object level;
    private static Object player;
    private static boolean enabled;
    private static int delta;
    private static final long IDLE_RESET_NANOS = 30_000_000_000L;
    private static long lastChangeNanos;

    private HitDeltaCounterTodoAi() {}

    public static void updateSession(Object currentLevel, Object currentPlayer, boolean requested) {
        boolean active = requested && currentLevel != null && currentPlayer != null;
        if (level != currentLevel || player != currentPlayer || enabled != active) delta = 0;
        level = currentLevel;
        player = currentPlayer;
        enabled = active;
    }

    /** Caller resolves both the victim and causing entity as players. */
    public static void recordDamage(int victimId, int attackerId, int localId) {
        recordDamage(victimId, attackerId, localId, System.nanoTime());
    }

    static void recordDamage(int victimId, int attackerId, int localId, long now) {
        expire(now);
        if (!enabled || victimId == attackerId) return;
        if (attackerId == localId) delta++;
        else if (victimId == localId) delta--;
        else return;
        lastChangeNanos = now;
    }

    public static int value() { return value(System.nanoTime()); }

    static int value(long now) {
        expire(now);
        return delta;
    }

    private static void expire(long now) {
        if (delta != 0 && now - lastChangeNanos >= IDLE_RESET_NANOS) delta = 0;
    }

    public static Color color() {
        value();
        return delta > 0 ? Color.GREEN : delta < 0 ? Color.RED : Color.WHITE;
    }

    public static void draw(Graphics2D graphics, int width, int height) {
        int current = value();
        if (current == 0) return;
        String text = (current > 0 ? "+" : "") + current;
        int x = Math.max(8, width - graphics.getFontMetrics().stringWidth(text) - 8);
        int y = height - graphics.getFontMetrics().getDescent() - 8;
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + 1, y + 1);
        graphics.setColor(color());
        graphics.drawString(text, x, y);
    }
}
