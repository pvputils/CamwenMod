package com.example.overlayTodoAi;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class HitDeltaCounterTestTodoAi {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    public static void main(String[] args) {
        Object world = new Object(), player = new Object();
        HitDeltaCounterTodoAi.updateSession(world, player, false);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        check(HitDeltaCounterTodoAi.value() == 0);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        check(HitDeltaCounterTodoAi.value() == 1 && HitDeltaCounterTodoAi.color().equals(Color.GREEN));
        HitDeltaCounterTodoAi.recordDamage(1, 2, 1);
        check(HitDeltaCounterTodoAi.value() == 0 && HitDeltaCounterTodoAi.color().equals(Color.WHITE));
        HitDeltaCounterTodoAi.recordDamage(1, 2, 1);
        check(HitDeltaCounterTodoAi.value() == -1 && HitDeltaCounterTodoAi.color().equals(Color.RED));
        HitDeltaCounterTodoAi.recordDamage(2, 3, 1);
        HitDeltaCounterTodoAi.recordDamage(1, 1, 1);
        check(HitDeltaCounterTodoAi.value() == -1);
        HitDeltaCounterTodoAi.updateSession(world, new Object(), true);
        check(HitDeltaCounterTodoAi.value() == 0);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        HitDeltaCounterTodoAi.updateSession(new Object(), player, true);
        check(HitDeltaCounterTodoAi.value() == 0);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        HitDeltaCounterTodoAi.updateSession(null, null, true);
        check(HitDeltaCounterTodoAi.value() == 0);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        HitDeltaCounterTodoAi.updateSession(world, player, false);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        check(HitDeltaCounterTodoAi.value() == 0);
        BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        HitDeltaCounterTodoAi.draw(graphics, 320, 180);
        for (int y = 0; y < 180; y++) for (int x = 0; x < 320; x++) {
            check(image.getRGB(x, y) == 0);
        }
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        HitDeltaCounterTodoAi.draw(graphics, 320, 180);
        graphics.dispose();
        boolean bottomRight = false;
        for (int y = 150; y < 180; y++) for (int x = 200; x < 320; x++) {
            bottomRight |= image.getRGB(x, y) != 0;
        }
        check(bottomRight);
        HitDeltaCounterTodoAi.updateSession(world, player, false);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, 0L);
        check(HitDeltaCounterTodoAi.value(29_999_999_999L) == 1);
        HitDeltaCounterTodoAi.recordDamage(2, 3, 1, 29_000_000_000L);
        check(HitDeltaCounterTodoAi.value(30_000_000_000L) == 0);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, 31_000_000_000L);
        HitDeltaCounterTodoAi.recordDamage(1, 2, 1, 50_000_000_000L);
        HitDeltaCounterTodoAi.recordDamage(1, 2, 1, 60_000_000_000L);
        check(HitDeltaCounterTodoAi.value(89_999_999_999L) == -1);
        check(HitDeltaCounterTodoAi.value(90_000_000_000L) == 0);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, 100_000_000_000L);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, 130_000_000_000L);
        check(HitDeltaCounterTodoAi.value(130_000_000_000L) == 1);
        HitDeltaCounterTodoAi.updateSession(world, player, false);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, true, 0L);
        check(HitDeltaCounterTodoAi.text(0L).equals("+1, (1)"));
        HitDeltaCounterTodoAi.recordDamage(3, 1, 1, false, 1L);
        check(HitDeltaCounterTodoAi.text(1L).equals("+2, (1)"));
        HitDeltaCounterTodoAi.recordDamage(1, 2, 1, true, 2L);
        check(HitDeltaCounterTodoAi.text(2L).equals("+1, (1)"));
        HitDeltaCounterTodoAi.recordDamage(1, 1, 1, true, 3L);
        HitDeltaCounterTodoAi.recordDamage(2, 3, 1, true, 4L);
        check(HitDeltaCounterTodoAi.text(4L).equals("+1, (1)"));
        check(HitDeltaCounterTodoAi.text(30_000_000_002L).isEmpty());
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, false, 40_000_000_000L);
        check(HitDeltaCounterTodoAi.text(40_000_000_000L).equals("+1, (0)"));
        HitDeltaCounterTodoAi.updateSession(world, new Object(), true);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1, false, 50_000_000_000L);
        check(HitDeltaCounterTodoAi.text(50_000_000_000L).equals("+1, (0)"));
        System.out.println("Hit delta counter tests passed");
    }
}
