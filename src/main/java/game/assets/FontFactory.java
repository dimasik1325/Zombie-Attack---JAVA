package game.assets;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Процедурная генерация bitmap-шрифта с кириллицей для HUD.
 * AWT рисует глифы системного шрифта в атлас-текстуру, из неё
 * формируется AngelCode .fnt (текстовый формат, который понимает
 * BitmapFontLoader jME). Файлы пишутся в ~/.zombieattack/fonts.
 * Если системных шрифтов нет — вызывающий код откатывается
 * на стандартный Default.fnt с английскими надписями.
 */
public final class FontFactory {

    public static final String FONT_FILE = "hud_font.fnt";
    public static final String TEXTURE_FILE = "hud_font.png";

    /** Размер глифов в атласе. */
    public static final int FONT_SIZE = 42;
    private static final int CELL_W = 46;
    private static final int CELL_H = 58;
    private static final int ATLAS_W = 1024;
    private static final int ATLAS_H = 1024;

    private FontFactory() {
    }

    /** Набор символов: латиница, кириллица, цифры, пунктуация. */
    public static String charSet() {
        StringBuilder sb = new StringBuilder();
        for (int i = 32; i <= 126; i++) {          // ASCII
            sb.append((char) i);
        }
        sb.append("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯЁ");
        sb.append("абвгдежзийклмнопрстуфхцчшщъыьэюяё");
        sb.append("«»—–…");
        return sb.toString();
    }

    /**
     * Генерирует шрифт и возвращает его. Бросает исключение,
     * если системные шрифты недоступны (тогда вызывающий код
     * переключается на английские надписи со стандартным шрифтом).
     */
    public static BitmapFont generate(AssetManager assetManager) throws IOException {
        File dir = DataDirs.fontDir();
        DataDirs.ensureAll();

        Font font = new Font("SansSerif", Font.BOLD, FONT_SIZE);
        String chars = charSet();

        // Проверяем, что шрифт умеет отрисовывать ключевые символы
        int missing = 0;
        for (char c : "АБВЖКПФЦЩЫЭЮЯ".toCharArray()) {
            if (!font.canDisplay(c)) {
                missing++;
            }
        }
        if (missing > 4) {
            throw new IOException("Системный шрифт не поддерживает кириллицу (пропущено " + missing + ")");
        }

        File fntFile = new File(dir, FONT_FILE);
        File pngFile = new File(dir, TEXTURE_FILE);
        if (!fntFile.exists() || !pngFile.exists()) {
            BufferedImage atlas = buildAtlas(font, chars);
            javax.imageio.ImageIO.write(atlas, "png", pngFile);
            String fnt = buildFnt(font, chars, atlas.getWidth(), atlas.getHeight());
            try (FileOutputStream fos = new FileOutputStream(fntFile)) {
                fos.write(fnt.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Регистрируем локатор каталога (идемпотентно через AssetLib)
        return assetManager.loadFont(FONT_FILE);
    }

    /** Рисует атлас глифов и возвращает его. */
    public static BufferedImage buildAtlas(Font font, String chars) {
        BufferedImage atlas = new BufferedImage(ATLAS_W, ATLAS_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(font);
        g.setColor(Color.WHITE);

        FontMetrics fm = g.getFontMetrics(font);
        int col = 0, row = 0;
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (c != ' ') {
                int x = col * CELL_W;
                int y = row * CELL_H;
                // базовая линия на y + BASE
                g.drawString(String.valueOf(c), x + 1, y + base(fm));
            }
            col++;
            if (col * CELL_W >= ATLAS_W) {
                col = 0;
                row++;
            }
        }
        g.dispose();
        return atlas;
    }

    /** Формирует содержимое .fnt (текстовый формат AngelCode BMFont). */
    public static String buildFnt(Font font, String chars, int atlasW, int atlasH) {
        BufferedImage probe = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = probe.createGraphics();
        FontMetrics fm = g.getFontMetrics(font);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        g.dispose();

        int lineHeight = ascent + descent + 8;
        int base = ascent + 4;

        StringBuilder sb = new StringBuilder(16 * 1024);
        sb.append("info face=\"HudSans\" size=").append(FONT_SIZE)
                .append(" bold=1 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1 outline=0\n");
        sb.append("common lineHeight=").append(lineHeight)
                .append(" base=").append(base)
                .append(" scaleW=").append(atlasW)
                .append(" scaleH=").append(atlasH)
                .append(" pages=1 packed=0\n");
        sb.append("page id=0 file=\"").append(TEXTURE_FILE).append("\"\n");

        List<String> charLines = new ArrayList<>();
        int col = 0, row = 0;
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            int w = Math.max(2, fm.charWidth(c) + 2);
            int h = ascent + descent + 6;
            int x = col * CELL_W;
            int y = row * CELL_H;
            // yoffset: смещение верха глифа от верха строки
            int yoffset = 4;
            charLines.add("char id=" + (int) c
                    + " x=" + x + " y=" + y
                    + " width=" + w + " height=" + h
                    + " xoffset=0 yoffset=" + yoffset
                    + " xadvance=" + Math.max(2, fm.charWidth(c))
                    + " page=0 chnl=15");
            col++;
            if (col * CELL_W >= ATLAS_W) {
                col = 0;
                row++;
            }
        }

        sb.append("chars count=").append(charLines.size()).append("\n");
        for (String line : charLines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static int base(FontMetrics fm) {
        return fm.getAscent() + 4;
    }
}
