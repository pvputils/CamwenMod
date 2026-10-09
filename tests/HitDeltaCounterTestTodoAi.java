import com.example.overlayTodoAi.HitDeltaCounterTodoAi;
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
        graphics.dispose();
        boolean bottomRight = false;
        for (int y = 150; y < 180; y++) for (int x = 200; x < 320; x++) {
            bottomRight |= image.getRGB(x, y) != 0;
        }
        check(bottomRight);
        System.out.println("Hit delta counter tests passed");
    }
}
