package com.example.overlayTodoAi;

import java.awt.Font;
import java.awt.image.BufferedImage;

public class TeamHitDeltaCounterTestTodoAi {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    public static void main(String[] args) {
        Object world = new Object(), player = new Object();
        TeamHitDeltaCounterTodoAi.updateSession(world, player, false);
        TeamHitDeltaCounterTodoAi.recordDamage(false, true, false, false, 0);
        check(TeamHitDeltaCounterTodoAi.text(0).isEmpty());
        TeamHitDeltaCounterTodoAi.updateSession(world, player, true);
        TeamHitDeltaCounterTodoAi.recordDamage(false, true, false, false, 0);
        check(TeamHitDeltaCounterTodoAi.text(0).equals("+1, (0)"));
        TeamHitDeltaCounterTodoAi.recordDamage(true, false, false, false, 1);
        check(TeamHitDeltaCounterTodoAi.text(1).isEmpty());
        TeamHitDeltaCounterTodoAi.recordDamage(true, true, true, false, 2);
        check(TeamHitDeltaCounterTodoAi.text(2).equals("-1, (1)"));
        TeamHitDeltaCounterTodoAi.recordDamage(true, true, true, true, 3);
        TeamHitDeltaCounterTodoAi.recordDamage(false, false, false, false, 4);
        check(TeamHitDeltaCounterTodoAi.text(4).equals("-1, (1)"));
        check(TeamHitDeltaCounterTodoAi.text(30_000_000_001L).equals("-1, (1)"));
        check(TeamHitDeltaCounterTodoAi.text(30_000_000_002L).isEmpty());
        TeamHitDeltaCounterTodoAi.recordDamage(false, true, false, false, 40_000_000_000L);
        check(TeamHitDeltaCounterTodoAi.text(40_000_000_000L).equals("+1, (0)"));
        TeamHitDeltaCounterTodoAi.recordDamage(true, true, true, false, 50_000_000_000L);
        TeamHitDeltaCounterTodoAi.recordDamage(true, true, true, false, 60_000_000_000L);
        check(TeamHitDeltaCounterTodoAi.text(89_999_999_999L).equals("-1, (2)"));
        check(TeamHitDeltaCounterTodoAi.text(90_000_000_000L).isEmpty());
        TeamHitDeltaCounterTodoAi.updateSession(new Object(), player, true);
        check(TeamHitDeltaCounterTodoAi.text(0).isEmpty());
        TeamHitDeltaCounterTodoAi.recordDamage(false, true, false, false);
        TeamHitDeltaCounterTodoAi.updateSession(world, new Object(), true);
        check(TeamHitDeltaCounterTodoAi.text(System.nanoTime()).isEmpty());
        TeamHitDeltaCounterTodoAi.recordDamage(false, true, false, false);
        TeamHitDeltaCounterTodoAi.updateSession(null, null, true);
        check(TeamHitDeltaCounterTodoAi.text(System.nanoTime()).isEmpty());
        TeamHitDeltaCounterTodoAi.updateSession(world, player, true);
        TeamHitDeltaCounterTodoAi.recordDamage(false, true, false, false);
        HitDeltaCounterTodoAi.updateSession(world, player, true);
        HitDeltaCounterTodoAi.recordDamage(2, 1, 1);
        BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));
        int rowHeight = graphics.getFontMetrics().getHeight() + 2;
        HitDeltaCounterTodoAi.draw(graphics, 320, 180 - rowHeight);
        check(TeamHitDeltaCounterTodoAi.draw(graphics, 320, 180));
        graphics.dispose();
        boolean upper = false, lower = false;
        for (int y = 0; y < 180; y++) for (int x = 200; x < 320; x++) {
            if (image.getRGB(x, y) != 0) {
                if (y < 180 - rowHeight - 8) upper = true;
                else lower = true;
            }
        }
        check(upper && lower);
        TeamHitDeltaCounterTodoAi.updateSession(world, player, false);
        TeamHitDeltaCounterTodoAi.updateSession(world, player, true);
        check(TeamHitDeltaCounterTodoAi.text(System.nanoTime()).isEmpty());
        check(HitDeltaCounterTodoAi.value() == 1);
        System.out.println("Team hit delta tests passed");
    }
}
