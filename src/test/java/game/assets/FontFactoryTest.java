package game.assets;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты процедурного шрифта (headless; требует системных шрифтов AWT). */
class FontFactoryTest {

    @Test
    void atlasIsBuiltWithAllGlyphs() {
        Font font = new Font("SansSerif", Font.BOLD, FontFactory.FONT_SIZE);
        // в headless-контейнере может не быть шрифтов — тогда пропускаем
        Assumptions.assumeTrue(font.canDisplay('A'), "Системные шрифты недоступны");

        String chars = FontFactory.charSet();
        BufferedImage atlas = FontFactory.buildAtlas(font, chars);
        assertNotNull(atlas);
        assertEquals(1024, atlas.getWidth());
        assertEquals(1024, atlas.getHeight());

        // в атласе что-то нарисовано
        boolean anyInk = false;
        for (int x = 0; x < atlas.getWidth() && !anyInk; x += 7) {
            for (int y = 0; y < atlas.getHeight(); y += 7) {
                if (((atlas.getRGB(x, y) >> 24) & 0xFF) > 60) {
                    anyInk = true;
                    break;
                }
            }
        }
        assertTrue(anyInk, "атлас глифов не должен быть пустым");
    }

    @Test
    void fntContainsAllCharEntries() {
        Font font = new Font("SansSerif", Font.BOLD, FontFactory.FONT_SIZE);
        Assumptions.assumeTrue(font.canDisplay('A'), "Системные шрифты недоступны");

        String chars = FontFactory.charSet();
        String fnt = FontFactory.buildFnt(font, chars, 1024, 1024);
        assertTrue(fnt.startsWith("info face="), "заголовок info обязателен");
        assertTrue(fnt.contains("common lineHeight="), "блок common обязателен");
        assertTrue(fnt.contains("page id=0 file=\"hud_font.png\""), "страница текстуры обязательна");

        long charLines = fnt.lines().filter(l -> l.startsWith("char id=")).count();
        assertEquals(chars.length(), charLines, "запись для каждого символа");

        // кириллица присутствует (id 1040 = 'А')
        assertTrue(fnt.contains("char id=1040 "), "глиф кириллицы 'А' в .fnt");
    }

    @Test
    void charSetCoversCyrillicAndAscii() {
        String chars = FontFactory.charSet();
        assertTrue(chars.indexOf('Z') >= 0);
        assertTrue(chars.indexOf('А') >= 0);   // U+0410
        assertTrue(chars.indexOf('я') >= 0);   // U+044F
        assertTrue(chars.indexOf('ё') >= 0);
        assertTrue(chars.indexOf('—') >= 0);   // длинное тире для баннеров
    }
}
