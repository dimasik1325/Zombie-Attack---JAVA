package game.assets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Процедурная иконка окна игры (тот же мотив, что и у .exe:
 * голова зомби с красными глазами). Возвращаются несколько размеров
 * для AppSettings.setIcons().
 */
public final class IconFactory {

    private IconFactory() {
    }

    /** Набор иконок окна в стандартных размерах. */
    public static BufferedImage[] createIcons() {
        int[] sizes = {256, 128, 64, 48, 32, 16};
        List<BufferedImage> icons = new ArrayList<>(sizes.length);
        for (int s : sizes) {
            icons.add(drawFace(s));
        }
        return icons.toArray(new BufferedImage[0]);
    }

    /** Рисует голову зомби заданного размера. */
    public static BufferedImage drawFace(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float f = s;

        // фон: тёмный градиент
        g.setPaint(new GradientPaint(0f, 0f, new Color(16, 18, 24), 0f, f, new Color(6, 7, 10)));
        g.fillRect(0, 0, s, s);

        // зеленоватое свечение
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(f * 0.5f, f * 0.52f), f * 0.55f,
                new float[]{0f, 1f},
                new Color[]{new Color(44, 84, 40), new Color(10, 14, 10, 0)}));
        g.fillRect(0, 0, s, s);

        // голова
        g.setColor(new Color(58, 110, 52));
        g.fillRoundRect((int) (s * 0.18), (int) (s * 0.16), (int) (s * 0.64), (int) (s * 0.68),
                (int) (s * 0.22), (int) (s * 0.22));

        // тёмная «кожа»-тени
        g.setColor(new Color(38, 78, 36));
        g.fillOval((int) (s * 0.24), (int) (s * 0.62), (int) (s * 0.52), (int) (s * 0.20));

        // глаза (красное свечение)
        g.setColor(new Color(190, 28, 20));
        g.fillOval((int) (s * 0.28), (int) (s * 0.34), (int) (s * 0.16), (int) (s * 0.13));
        g.fillOval((int) (s * 0.56), (int) (s * 0.34), (int) (s * 0.16), (int) (s * 0.13));
        g.setColor(new Color(255, 96, 60));
        g.fillOval((int) (s * 0.31), (int) (s * 0.365), (int) (s * 0.07), (int) (s * 0.05));
        g.fillOval((int) (s * 0.59), (int) (s * 0.365), (int) (s * 0.07), (int) (s * 0.05));

        // шрам
        g.setStroke(new BasicStroke(Math.max(1f, s / 64f)));
        g.setColor(new Color(30, 50, 30));
        g.drawLine((int) (s * 0.5), (int) (s * 0.18), (int) (s * 0.56), (int) (s * 0.30));

        // зубастый рот
        g.setColor(new Color(20, 26, 20));
        int mx = (int) (s * 0.30), my = (int) (s * 0.62), mw = (int) (s * 0.40), mh = (int) (s * 0.14);
        g.fillRect(mx, my, mw, mh);
        g.setColor(new Color(210, 210, 190));
        int teeth = 5;
        for (int i = 0; i < teeth; i++) {
            int tx = mx + (int) (i * mw / (float) teeth) + 1;
            int tw = Math.max(1, (int) (mw / (float) teeth) - 2);
            g.fillPolygon(new int[]{tx, tx + tw, tx + tw / 2},
                    new int[]{my, my, my + mh / 2}, 3);
        }
        g.dispose();
        return img;
    }
}
