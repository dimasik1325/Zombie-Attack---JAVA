package game.assets;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Процедурная генерация текстур (чистая AWT, без внешних файлов).
 * Все изображения рисуются кодом: бетон, стены, ящики, кожа зомби,
 * кровь, вспышка, виньетки, фон меню. Игра выглядит цельной даже
 * без единого внешнего ассета.
 */
public final class ProcTex {

    private ProcTex() {
    }

    /** Изображение по имени текстуры. */
    public static BufferedImage make(String name) {
        switch (name) {
            case "floor": return floor();
            case "wall": return wall();
            case "crate": return crate();
            case "metal": return metal();
            case "skin": return skin();
            case "blood": return blood();
            case "flash": return flash();
            case "vignette": return vignette();
            case "menuBg": return menuBackground();
            case "health": return healthIcon();
            default: throw new IllegalArgumentException("Неизвестная текстура: " + name);
        }
    }

    /** Нужно ли повторение (тайлинг) текстуры. */
    public static boolean repeating(String name) {
        switch (name) {
            case "floor":
            case "wall":
            case "crate":
            case "metal":
            case "skin":
                return true;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------
    // Пол: бетонная плитка с грязью и трещинами
    // ------------------------------------------------------------
    private static BufferedImage floor() {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random rng = new Random(71);

        // базовый тёмный бетон
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int n = rng.nextInt(14) - 7;
                int v = 38 + n + (int) (Math.sin(x * 0.05) * 3);
                img.setRGB(x, y, rgb(v + 4, v + 6, v + 10));
            }
        }

        // швы плитки 128x128
        g.setColor(color(18, 20, 26));
        for (int i = 0; i <= size; i += 128) {
            g.fillRect(0, i - 2, size, 4);
            g.fillRect(i - 2, 0, 4, size);
        }

        // трещины (случайные ломаные)
        g.setColor(color(22, 24, 30));
        for (int c = 0; c < 14; c++) {
            int x = rng.nextInt(size), y = rng.nextInt(size);
            for (int s = 0; s < 26; s++) {
                g.fillRect(x, y, 2, 2);
                x += rng.nextInt(7) - 3;
                y += rng.nextInt(7) - 2;
            }
        }

        // тёмные пятна (грязь/кровь)
        for (int s = 0; s < 26; s++) {
            int x = rng.nextInt(size), y = rng.nextInt(size);
            int r = 10 + rng.nextInt(46);
            int dark = 120 + rng.nextInt(80);
            g.setColor(new java.awt.Color(0, 0, 0, dark / 3));
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Стена: бетон с вертикальными панелями и потёками
    // ------------------------------------------------------------
    private static BufferedImage wall() {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random rng = new Random(133);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int n = rng.nextInt(12) - 6;
                int v = 56 + n;
                // вертикальная затирка панелей каждые 128 px
                if (x % 128 < 3) v -= 22;
                // потёки сверху вниз
                int streak = (int) (Math.sin(x * 0.9) * rng.nextGaussian() * 6);
                v += streak;
                img.setRGB(x, y, rgb(v, v - 2, v - 6));
            }
        }

        // сколы и выбоины
        for (int s = 0; s < 40; s++) {
            int x = rng.nextInt(size), y = rng.nextInt(size);
            int r = 3 + rng.nextInt(9);
            g.setColor(color(30, 30, 32));
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
        // грязь снизу
        g.setPaint(new java.awt.GradientPaint(0, size * 0.6f, new java.awt.Color(0, 0, 0, 0),
                0, size, new java.awt.Color(0, 0, 0, 150)));
        g.fillRect(0, 0, size, size);
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Ящик: деревянные доски
    // ------------------------------------------------------------
    private static BufferedImage crate() {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Random rng = new Random(9);
        int planks = 4;

        for (int p = 0; p < planks; p++) {
            int y0 = p * size / planks;
            int base = 96 + rng.nextInt(30);
            for (int y = y0; y < y0 + size / planks && y < size; y++) {
                for (int x = 0; x < size; x++) {
                    // волокна дерева
                    int grain = (int) (Math.sin(x * 0.3 + p * 2.1) * 6 + rng.nextInt(8) - 4);
                    img.setRGB(x, y, rgb(base + grain - 24, base + grain - 46, base + grain - 66));
                }
            }
            // зазор между досками
            g.setColor(color(40, 28, 18));
            g.fillRect(0, y0, size, 3);
        }

        // окантовка ящика
        g.setColor(color(64, 46, 30));
        g.fillRect(0, 0, size, 10);
        g.fillRect(0, size - 10, size, 10);
        g.fillRect(0, 0, 10, size);
        g.fillRect(size - 10, 0, 10, size);

        // гвозди
        g.setColor(color(120, 118, 112));
        for (int i = 0; i < 8; i++) {
            int x = 14 + (i % 2) * (size - 32);
            int y = 18 + i * (size - 36) / 7;
            g.fillOval(x, y, 5, 5);
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Металл: для контейнеров и бочек
    // ------------------------------------------------------------
    private static BufferedImage metal() {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(4242);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int brushed = (int) (Math.sin(y * 0.7) * 4 + rng.nextInt(10) - 5);
                int v = 66 + brushed;
                img.setRGB(x, y, rgb(v - 4, v, v + 8));
            }
        }
        Graphics2D g = img.createGraphics();
        // заклёпки по краю
        g.setColor(color(38, 40, 46));
        for (int i = 12; i < size; i += 32) {
            g.fillOval(i, 6, 6, 6);
            g.fillOval(i, size - 12, 6, 6);
            g.fillOval(6, i, 6, 6);
            g.fillOval(size - 12, i, 6, 6);
        }
        // ржавые пятна
        for (int s = 0; s < 14; s++) {
            int x = rng.nextInt(size), y = rng.nextInt(size), r = 4 + rng.nextInt(18);
            g.setColor(new java.awt.Color(90, 42, 16, 70));
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Кожа зомби: пятнистая зелёная плоть
    // ------------------------------------------------------------
    private static BufferedImage skin() {
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(666);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int blotch = (int) (Math.sin(x * 0.2) * Math.cos(y * 0.23) * 12);
                int n = rng.nextInt(16) - 8;
                int r = 66 + n + blotch / 2;
                int gg = 112 + n + blotch;
                int b = 60 + n;
                img.setRGB(x, y, rgb(r, gg, b));
            }
        }
        Graphics2D g = img.createGraphics();
        // красные ссадины
        for (int s = 0; s < 6; s++) {
            int x = rng.nextInt(size), y = rng.nextInt(size);
            g.setColor(new java.awt.Color(120, 20, 14, 120));
            g.drawLine(x, y, x + rng.nextInt(20) - 10, y + rng.nextInt(20) - 10);
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Кровь: красный радиальный сплэш с альфой (для частиц)
    // ------------------------------------------------------------
    private static BufferedImage blood() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Random rng = new Random(31);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - size / 2f) / (size / 2f);
                float dy = (y - size / 2f) / (size / 2f);
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                a = a * a * (0.7f + rng.nextFloat() * 0.3f);
                img.setRGB(x, y, rgba(150, 12, 10, (int) (a * 255)));
            }
        }
        return img;
    }

    // ------------------------------------------------------------
    // Вспышка выстрела: белое ядро, жёлтый ореол (для частиц)
    // ------------------------------------------------------------
    private static BufferedImage flash() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - size / 2f) / (size / 2f);
                float dy = (y - size / 2f) / (size / 2f);
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                int r = 255;
                int g = (int) (200 + 55 * a);
                int b = (int) (90 + 150 * a * a);
                img.setRGB(x, y, rgba(r, g, b, (int) (a * a * 255)));
            }
        }
        return img;
    }

    // ------------------------------------------------------------
    // Виньетка: тёмно-красная рамка для урона (прозрачный центр)
    // ------------------------------------------------------------
    private static BufferedImage vignette() {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - size / 2f) / (size / 2f);
                float dy = (y - size / 2f) / (size / 2f);
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, (d - 0.55f) / 0.45f);
                a = a * a;
                img.setRGB(x, y, rgba(110, 4, 4, (int) (a * 255)));
            }
        }
        return img;
    }

    // ------------------------------------------------------------
    // Фон меню: ночное небо, луна, силуэт города, дымка
    // ------------------------------------------------------------
    private static BufferedImage menuBackground() {
        int w = 1024, h = 576;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random rng = new Random(2023);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float t = y / (float) h;
                int r = (int) (14 + t * 10 + rng.nextInt(4) - 2);
                int g = (int) (16 + t * 12 + rng.nextInt(4) - 2);
                int b = (int) (28 + t * 14 + rng.nextInt(4) - 2);
                img.setRGB(x, y, rgb(r, g, b));
            }
        }
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // кроваво-красная луна
        g.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(w * 0.78f, h * 0.28f), 150f,
                new float[]{0f, 0.45f, 1f},
                new java.awt.Color[]{new java.awt.Color(190, 30, 20, 200),
                        new java.awt.Color(140, 22, 14, 90),
                        new java.awt.Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, w, h);
        g.setColor(new java.awt.Color(210, 60, 40));
        g.fillOval((int) (w * 0.72), (int) (h * 0.18), 110, 110);

        // силуэты зданий
        g.setColor(new java.awt.Color(8, 9, 13));
        int x = 0;
        while (x < w) {
            int bw = 40 + rng.nextInt(90);
            int bh = 60 + rng.nextInt(190);
            g.fillRect(x, h - bh, bw, bh);
            // редкие тусклые окна
            g.setColor(new java.awt.Color(60, 40, 20));
            for (int i = 0; i < 6; i++) {
                if (rng.nextFloat() < 0.3f) {
                    g.fillRect(x + 6 + rng.nextInt(Math.max(1, bw - 14)), h - bh + 8 + rng.nextInt(Math.max(1, bh - 20)), 4, 5);
                }
            }
            g.setColor(new java.awt.Color(8, 9, 13));
            x += bw + rng.nextInt(14);
        }

        // зомби-силуэт справа
        g.setColor(new java.awt.Color(5, 6, 8));
        int zx = (int) (w * 0.88), zy = h - 150;
        g.fillRect(zx, zy, 26, 90);      // туловище
        g.fillOval(zx + 4, zy - 26, 20, 24); // голова
        g.fillRect(zx - 12, zy + 6, 14, 8);  // руки вперёд
        g.fillRect(zx + 26, zy + 6, 14, 8);
        g.fillRect(zx + 2, zy + 90, 9, 60);  // ноги
        g.fillRect(zx + 16, zy + 90, 9, 60);

        // виньетка
        g.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(w / 2f, h / 2f), w * 0.62f,
                new float[]{0f, 0.6f, 1f},
                new java.awt.Color[]{new java.awt.Color(0, 0, 0, 0),
                        new java.awt.Color(0, 0, 0, 60),
                        new java.awt.Color(0, 0, 0, 210)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Аптечка: белая коробка с красным крестом
    // ------------------------------------------------------------
    private static BufferedImage healthIcon() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color(225, 228, 232));
        g.fillRect(0, 0, size, size);
        g.setColor(color(180, 20, 16));
        g.fillRect(size / 2 - 7, 12, 14, 40);
        g.fillRect(12, size / 2 - 7, 40, 14);
        g.setColor(color(120, 122, 128));
        g.drawRect(0, 0, size - 1, size - 1);
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------
    // Утилиты
    // ------------------------------------------------------------
    private static int rgb(int r, int g, int b) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }

    private static int rgba(int r, int g, int b, int a) {
        return (Math.max(0, Math.min(255, a)) << 24)
                | (Math.max(0, Math.min(255, r)) << 16)
                | (Math.max(0, Math.min(255, g)) << 8)
                | Math.max(0, Math.min(255, b));
    }

    private static java.awt.Color color(int r, int g, int b) {
        return new java.awt.Color(r, g, b);
    }

    /** Отрисовка с прозрачностью поверх изображения (для пятен). */
    @SuppressWarnings("unused")
    private static void blend(Graphics2D g, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
