package game.assets;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Центральный доступ к ресурсам: генерирует процедурные звуки и шрифт,
 * регистрирует локаторы, создаёт и кэширует текстуры и материалы.
 */
public class AssetLib {

    private static final Logger LOG = Logger.getLogger(AssetLib.class.getName());

    private final AssetManager assetManager;
    private final Map<String, Texture2D> textures = new HashMap<>();

    private BitmapFont hudFont;
    private boolean cyrillic;

    public AssetLib(AssetManager assetManager) {
        this.assetManager = assetManager;
        DataDirs.ensureAll();

        // 1. Процедурные звуки: WAV в ~/.zombieattack/sounds + локатор каталога.
        //    Любая ошибка (антивирус, права, диск) не должна ронять игру — она останется без звука.
        try {
            int made = SoundFactory.generateMissing(DataDirs.soundsDir());
            LOG.info("Процедурные звуки: создано " + made + " новых файлов");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Не удалось сгенерировать звуки — игра без звука", e);
        }
        try {
            assetManager.registerLocator(DataDirs.soundsDir().getAbsolutePath(), FileLocator.class);
        } catch (Exception e) {
            LOG.warning("Локатор звуков не зарегистрирован: " + e.getMessage());
        }

        // 2. Локатор каталога шрифта + страховка абсолютных путей
        try {
            assetManager.registerLocator(DataDirs.fontDir().getAbsolutePath(), FileLocator.class);
        } catch (Exception e) {
            LOG.warning("Локатор шрифтов не зарегистрирован: " + e.getMessage());
        }
        try {
            assetManager.registerLocator("/", RuntimeLocator.class);
        } catch (Exception e) {
            LOG.warning("Runtime-локатор не зарегистрирован: " + e.getMessage());
        }

        // 3. Процедурный шрифт с кириллицей; при неудаче — Default.fnt + английский текст
        try {
            hudFont = FontFactory.generate(assetManager);
            cyrillic = true;
            LOG.info("Используется процедурный шрифт с кириллицей");
        } catch (Exception e) {
            LOG.warning("Процедурный шрифт недоступен (" + e.getMessage() + ") — используется Default.fnt");
            hudFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
            cyrillic = false;
        }
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public BitmapFont getHudFont() {
        return hudFont;
    }

    /** true, если доступен шрифт с кириллицей (русский HUD). */
    public boolean hasCyrillic() {
        return cyrillic;
    }

    /** Текстура по имени (создаётся процедурно и кэшируется). */
    public Texture2D texture(String name) {
        Texture2D tex = textures.get(name);
        if (tex != null) {
            return tex;
        }
        BufferedImage img = ProcTex.make(name);
        tex = toTexture(img, ProcTex.repeating(name));
        textures.put(name, tex);
        return tex;
    }

    /** BufferedImage → Texture2D (RGBA8, строки перевёрнуты для OpenGL). */
    public Texture2D toTexture(BufferedImage img, boolean repeat) {
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = h - 1; y >= 0; y--) {      // переворот: в OpenGL строка 0 — нижняя
            for (int x = 0; x < w; x++) {
                int p = pixels[y * w + x];
                buffer.put((byte) ((p >> 16) & 0xFF)); // R
                buffer.put((byte) ((p >> 8) & 0xFF));  // G
                buffer.put((byte) (p & 0xFF));         // B
                buffer.put((byte) ((p >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        Image image = new Image(Image.Format.RGBA8, w, h, buffer);
        Texture2D tex = new Texture2D(image);
        tex.setMagFilter(Texture.MagFilter.Bilinear);
        tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        if (repeat) {
            tex.setWrap(Texture.WrapMode.Repeat);
        }
        return tex;
    }

    // ------------------------------------------------------------
    //  Фабрики материалов
    // ------------------------------------------------------------

    /** Незатенённый материал с текстурой (HUD, частицы, эффекты). */
    public Material unshaded(String textureName) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", texture(textureName));
        return mat;
    }

    /** Незатенённый материал с чистым цветом. */
    public Material unshadedColor(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        return mat;
    }

    /** Незатенённый материал с текстурой и цветовой подкраской. */
    public Material unshadedTint(String textureName, ColorRGBA tint) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", texture(textureName));
        mat.setColor("Color", tint);
        return mat;
    }

    /** Освещаемый материал с текстурой (уровень, зомби, оружие). */
    public Material lighting(String textureName, ColorRGBA diffuse) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap", texture(textureName));
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", new ColorRGBA(0.35f, 0.35f, 0.38f, 1));
        mat.setColor("Diffuse", diffuse);
        mat.setColor("Specular", new ColorRGBA(0.15f, 0.15f, 0.18f, 1));
        mat.setFloat("Shininess", 12f);
        return mat;
    }

    /** Освещаемый материал с чистым цветом (оружие без текстур). */
    public Material lightingColor(ColorRGBA diffuse, ColorRGBA specular, float shininess) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", diffuse.mult(0.4f));
        mat.setColor("Diffuse", diffuse);
        mat.setColor("Specular", specular);
        mat.setFloat("Shininess", shininess);
        return mat;
    }

    /** Материал частиц (Particle.j3md + текстура). */
    public Material particle(String textureName) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", texture(textureName));
        return mat;
    }
}
