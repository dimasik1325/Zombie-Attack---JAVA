package game.assets;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты процедурных текстур (headless, чистая AWT). */
class ProcTexTest {

    private static final String[] NAMES = {
            "floor", "wall", "crate", "metal", "skin", "blood", "flash", "vignette", "menuBg", "health"
    };

    @Test
    void allTexturesGenerated() {
        for (String name : NAMES) {
            BufferedImage img = ProcTex.make(name);
            assertNotNull(img, name);
            assertTrue(img.getWidth() >= 64, name + ": достаточный размер");
            assertTrue(img.getHeight() >= 64, name + ": достаточный размер");
        }
    }

    @Test
    void texturesAreNotBlank() {
        for (String name : NAMES) {
            BufferedImage img = ProcTex.make(name);
            // считаем вариацию по выборке пикселей: в текстуре должно быть «разнообразие»
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int x = 0; x < img.getWidth(); x += 13) {
                for (int y = 0; y < img.getHeight(); y += 13) {
                    int p = img.getRGB(x, y);
                    min = Math.min(min, p);
                    max = Math.max(max, p);
                }
            }
            assertTrue(min != max, name + ": в текстуре должна быть вариация");
        }
    }

    @Test
    void alphaTexturesHaveTransparency() {
        BufferedImage blood = ProcTex.make("blood");
        int center = blood.getRGB(blood.getWidth() / 2, blood.getHeight() / 2);
        int alpha = (center >> 24) & 0xFF;
        assertTrue(alpha > 100, "кровь в центре плотная");
        int corner = blood.getRGB(0, 0);
        assertEquals(0, (corner >> 24) & 0xFF, "край крови прозрачен");

        BufferedImage vignette = ProcTex.make("vignette");
        int vCenter = vignette.getRGB(vignette.getWidth() / 2, vignette.getHeight() / 2);
        assertEquals(0, (vCenter >> 24) & 0xFF, "центр виньетки прозрачен");
    }

    @Test
    void repeatingFlagsAreSane() {
        assertTrue(ProcTex.repeating("floor"));
        assertTrue(ProcTex.repeating("wall"));
        assertFalse(ProcTex.repeating("blood"));
        assertFalse(ProcTex.repeating("menuBg"));
    }

    @Test
    void unknownNameFailsClearly() {
        assertThrows(IllegalArgumentException.class, () -> ProcTex.make("nope"));
    }
}
