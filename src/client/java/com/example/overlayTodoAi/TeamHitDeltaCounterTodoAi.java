package com.example.overlayTodoAi;

import java.awt.Color;
import java.awt.Graphics2D;

/** Aggregate hit balance for teammates currently tracked by the client. */
public final class TeamHitDeltaCounterTodoAi {
    private static Object level;
    private static Object player;
    private static boolean enabled;
    private static int delta;
    private static int friendlyFire;
    private static long lastChange;
    private static final long IDLE_NANOS = 30_000_000_000L;

    private TeamHitDeltaCounterTodoAi() {}

    public static void updateSession(Object world, Object localPlayer, boolean requested) {
        boolean active = requested && world != null && localPlayer != null;
        if (level != world || player != localPlayer || enabled != active) {
            delta = 0;
            friendlyFire = 0;
        }
        level = world;
        player = localPlayer;
        enabled = active;
    }

    public static void recordDamage(boolean victimTracked, boolean attackerTracked,
                                    boolean friendlyHit, boolean selfHit) {
        recordDamage(victimTracked, attackerTracked, friendlyHit, selfHit, System.nanoTime());
    }

    static void recordDamage(boolean victimTracked, boolean attackerTracked,
                             boolean friendlyHit, boolean selfHit, long now) {
        expire(now);
        if (!enabled || selfHit || (!victimTracked && !attackerTracked)) return;
        if (friendlyHit) {
            friendlyFire++;
            delta--;
        } else if (attackerTracked) delta++;
        else delta--;
        lastChange = now;
    }

    private static void expire(long now) {
        if (now - lastChange >= IDLE_NANOS) {
            delta = 0;
            friendlyFire = 0;
        }
    }

    static String text(long now) {
        expire(now);
        return delta == 0 ? "" : (delta > 0 ? "+" : "") + delta + ", (" + friendlyFire + ")";
    }

    public static boolean draw(Graphics2D graphics, int width, int height) {
        String text = text(System.nanoTime());
        if (text.isEmpty()) return false;
        int x = Math.max(8, width - graphics.getFontMetrics().stringWidth(text) - 8);
        int y = height - graphics.getFontMetrics().getDescent() - 8;
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + 1, y + 1);
        graphics.setColor(delta > 0 ? Color.GREEN : Color.RED);
        graphics.drawString(text, x, y);
        return true;
    }
}
