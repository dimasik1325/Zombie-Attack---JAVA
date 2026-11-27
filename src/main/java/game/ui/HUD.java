package game.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.ui.Picture;
import game.ai.WaveManager;
import game.entities.Player;
import game.weapons.Weapon;
import game.weapons.WeaponSystem;

/**
 * HUD: прицел, полоска здоровья, патроны, счёт, волна, баннеры,
 * виньетка урона, хит-маркер, пауза-оверлей.
 *
 * Слои (GuiComparator рисует по возрастанию z — больше z, «выше» слой):
 *  z=0     — фон/элементы HUD, прицел
 *  z=0.5   — виньетка урона
 *  z=0.8   — затемнение паузы
 *  z>=1    — все тексты
 */
public class HUD {

    private final Node gui;
    /** Корневой узел HUD — чтобы при detach убирать только свои элементы. */
    private final Node hudRoot = new Node("hud");
    private final BitmapFont font;
    private final Texts texts;
    private final AssetManager assetManager;
    private final int w, h;

    // прицел
    private final Node crosshair = new Node("crosshair");
    private final Node hitMarker = new Node("hitmarker");
    private float hitMarkerTimer;

    // тексты
    private final BitmapText waveText;
    private final BitmapText zombiesText;
    private final BitmapText scoreText;
    private final BitmapText healthText;
    private final BitmapText ammoText;
    private final BitmapText weaponText;
    private final BitmapText bannerText;
    private final BitmapText messageText;
    private final BitmapText hintText;
    private final BitmapText pauseTitle;
    private final BitmapText pauseHint1;
    private final BitmapText pauseHint2;

    // полоска здоровья
    private final Geometry healthBarBg;
    private final Geometry healthBarFill;
    private final Material healthFillMat;

    // виньетка урона (картинка с альфой)
    private final Picture vignette;
    private float vignetteAlpha;

    // затемнение паузы
    private final Geometry pauseDim;

    // таймеры сообщений
    private float bannerTimer;
    private float messageTimer;
    private float hintTimer;

    @SuppressWarnings("this-escape")
    public HUD(Node gui, BitmapFont font, Texts texts, AssetManager assetManager,
               Camera cam, com.jme3.texture.Texture2D vignetteTexture) {
        this.gui = gui;
        this.font = font;
        this.texts = texts;
        this.assetManager = assetManager;
        this.w = cam.getWidth();
        this.h = cam.getHeight();

        // --- прицел: 4 линии и точка ---
        addCrosshairPart(0, 8, 2, 9);       // верх
        addCrosshairPart(0, -17, 2, 9);     // низ
        addCrosshairPart(-10, 0, 9, 2);     // лево
        addCrosshairPart(3, 0, 9, 2);       // право
        addCrosshairPart(-1, -1, 2, 2);     // точка
        crosshair.setLocalTranslation(w / 2f, h / 2f, 0.1f);
        hudRoot.attachChild(crosshair);

        // --- хит-маркер: 4 диагональных чёрточки ---
        addHitPart(-12, 8, 7, 2, 45);
        addHitPart(5, 8, 7, 2, -45);
        addHitPart(-12, -10, 7, 2, -45);
        addHitPart(5, -10, 7, 2, 45);
        hitMarker.setLocalTranslation(w / 2f, h / 2f, 0.12f);
        hitMarker.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudRoot.attachChild(hitMarker);

        // --- верх слева: волна и зомби ---
        waveText = makeText(22f, 1f);
        waveText.setLocalTranslation(20, h - 18, 1f);
        zombiesText = makeText(18f, 1f);
        zombiesText.setLocalTranslation(20, h - 54, 1f);

        // --- верх справа: очки ---
        scoreText = makeText(22f, 1f);
        scoreText.setLocalTranslation(w - 220, h - 18, 1f);

        // --- низ слева: полоска здоровья ---
        Material bgMat = unshaded(new ColorRGBA(0.05f, 0.05f, 0.05f, 0.7f));
        healthBarBg = new Geometry("hp_bg", new Quad(246, 24));
        healthBarBg.setMaterial(bgMat);
        healthBarBg.setQueueBucket(RenderQueue.Bucket.Gui);
        healthBarBg.setLocalTranslation(20, 26, 0f);
        hudRoot.attachChild(healthBarBg);

        healthFillMat = unshaded(new ColorRGBA(0.2f, 0.85f, 0.25f, 1f));
        healthBarFill = new Geometry("hp_fill", new Quad(242, 20));
        healthBarFill.setMaterial(healthFillMat);
        healthBarFill.setQueueBucket(RenderQueue.Bucket.Gui);
        healthBarFill.setLocalTranslation(22, 28, 0f);
        hudRoot.attachChild(healthBarFill);

        healthText = makeText(18f, 1f);
        healthText.setLocalTranslation(276, 26, 1f);

        // --- низ справа: оружие и патроны ---
        weaponText = makeText(20f, 1f);
        ammoText = makeText(26f, 1f);
        weaponText.setLocalTranslation(w - 220, 92, 1f);
        ammoText.setLocalTranslation(w - 220, 56, 1f);

        // --- центр: баннер волны, сообщения, подсказка ---
        bannerText = makeText(46f, 1.1f);
        messageText = makeText(20f, 1.1f);
        hintText = makeText(16f, 1.05f);

        // --- виньетка урона (fullscreen Picture с альфой) ---
        vignette = new Picture("vignette");
        vignette.setTexture(assetManager, vignetteTexture, true);
        vignette.setWidth(w);
        vignette.setHeight(h);
        vignette.setLocalTranslation(0, 0, 0.5f); // поверх HP-бара (z=0), под текстами
        vignette.getMaterial().setColor("Color", new ColorRGBA(1, 1, 1, 0f));
        hudRoot.attachChild(vignette);

        // --- пауза: затемнение + тексты ---
        Material dimMat = unshaded(new ColorRGBA(0, 0, 0, 0.6f));
        pauseDim = new Geometry("pause_dim", new Quad(w, h));
        pauseDim.setMaterial(dimMat);
        pauseDim.setQueueBucket(RenderQueue.Bucket.Gui);
        pauseDim.setLocalTranslation(0, 0, 0.8f);
        pauseDim.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        hudRoot.attachChild(pauseDim);

        pauseTitle = makeText(52f, 1.2f);
        pauseTitle.setText(texts.get("pause.title"));
        pauseHint1 = makeText(20f, 1.2f);
        pauseHint1.setText(texts.get("pause.resume"));
        pauseHint2 = makeText(20f, 1.2f);
        pauseHint2.setText(texts.get("pause.menu"));
        setPauseVisible(false);
        gui.attachChild(hudRoot);
    }

    // ------------------------------------------------------------
    //  Построение
    // ------------------------------------------------------------

    private BitmapText makeText(float size, float z) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(size);
        t.setLocalTranslation(0, 0, z); // z-слой: тексты выше виньетки/затемнения
        hudRoot.attachChild(t);
        return t;
    }

    private Material unshaded(ColorRGBA color) {
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return m;
    }

    private void addCrosshairPart(int dx, int dy, int qw, int qh) {
        Geometry g = new Geometry("ch", new Quad(qw, qh));
        g.setMaterial(unshaded(new ColorRGBA(1, 1, 1, 0.85f)));
        g.setQueueBucket(RenderQueue.Bucket.Gui);
        g.setLocalTranslation(dx, dy, 0);
        crosshair.attachChild(g);
    }

    private void addHitPart(int dx, int dy, int qw, int qh, float angleDeg) {
        Geometry g = new Geometry("hm", new Quad(qw, qh));
        g.setMaterial(unshaded(new ColorRGBA(1f, 0.3f, 0.25f, 0.95f)));
        g.setQueueBucket(RenderQueue.Bucket.Gui);
        g.setLocalTranslation(dx, dy, 0);
        g.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0, 0, (float) Math.toRadians(angleDeg)));
        hitMarker.attachChild(g);
    }

    // ------------------------------------------------------------
    //  События
    // ------------------------------------------------------------

    /** Большой баннер «ВОЛНА N» (по центру). */
    public void showWaveBanner(int wave) {
        bannerText.setText(texts.format("hud.waveBanner", wave));
        center(bannerText, h / 2f + 90f);
        bannerTimer = 2.6f;
    }

    /** Временное сообщение (подбор и т.п.). */
    public void showMessage(String msg, float seconds) {
        messageText.setText(msg);
        center(messageText, h / 2f + 130f);
        messageTimer = seconds;
    }

    /** Подсказка у прицела ([E] подобрать). */
    public void showHint(String msg) {
        hintText.setText(msg);
        hintText.setColor(new ColorRGBA(1f, 0.95f, 0.7f, 1f));
        center(hintText, h / 2f - 40f);
        hintTimer = 0.15f; // держим постоянно, обновляем каждый кадр из GameState
    }

    /** Вспышка урона на экране. */
    public void flashDamage() {
        vignetteAlpha = Math.max(vignetteAlpha, 0.55f);
    }

    /** Хит-маркер при попадании. */
    public void showHitMarker() {
        hitMarkerTimer = 0.12f;
    }

    /** Пауза-оверлей. */
    public void setPauseVisible(boolean visible) {
        if (visible) {
            center(pauseTitle, h / 2f + 60f);
            center(pauseHint1, h / 2f + 10f);
            center(pauseHint2, h / 2f - 24f);
        }
        pauseDim.setCullHint(visible ? com.jme3.scene.Spatial.CullHint.Dynamic
                : com.jme3.scene.Spatial.CullHint.Always);
        pauseTitle.setCullHint(visible ? com.jme3.scene.Spatial.CullHint.Dynamic
                : com.jme3.scene.Spatial.CullHint.Always);
        pauseHint1.setCullHint(visible ? com.jme3.scene.Spatial.CullHint.Dynamic
                : com.jme3.scene.Spatial.CullHint.Always);
        pauseHint2.setCullHint(visible ? com.jme3.scene.Spatial.CullHint.Dynamic
                : com.jme3.scene.Spatial.CullHint.Always);
    }

    /** Центрирует текст по горизонтали на высоте yFromBottom (верх текста там). */
    private void center(BitmapText t, float yFromBottom) {
        t.setLocalTranslation(w / 2f - t.getLineWidth() / 2f, yFromBottom, t.getLocalTranslation().z);
    }

    // ------------------------------------------------------------
    //  Кадр
    // ------------------------------------------------------------

    /**
     * Обновление HUD за кадр.
     * @param zombiesLeft всего живых/не заспавненных зомби
     */
    public void update(float tpf, Player player, WeaponSystem weapons,
                       WaveManager waves, int score, int zombiesLeft) {
        // тексты
        waveText.setText(texts.format("hud.wave", waves.getWave()));
        zombiesText.setText(texts.format("hud.zombies", zombiesLeft));
        scoreText.setText(texts.format("hud.score", score));
        scoreText.setLocalTranslation(w - 20 - scoreText.getLineWidth(), h - 18, 1f);

        Weapon weapon = weapons.getCurrent();
        weaponText.setText(weapon.getName());
        weaponText.setLocalTranslation(w - 20 - weaponText.getLineWidth(), 92, 1f);
        ammoText.setText(texts.format("hud.ammo", weapon.getAmmoInMag(), weapon.getReserve()));
        ammoText.setLocalTranslation(w - 20 - ammoText.getLineWidth(), 56, 1f);
        ammoText.setColor(weapon.getAmmoInMag() == 0
                ? new ColorRGBA(1f, 0.35f, 0.3f, 1f) : ColorRGBA.White);

        // полоска здоровья
        float hpFrac = player.getHealth() / (float) player.getMaxHealth();
        healthBarFill.setLocalScale(hpFrac, 1f, 1f);
        healthFillMat.setColor("Color", new ColorRGBA(
                0.9f - 0.7f * hpFrac, 0.15f + 0.7f * hpFrac, 0.2f, 1f));
        healthText.setText(String.valueOf(player.getHealth()));

        // виньетка: урон + пульс при низком здоровье
        float target = player.getHurtTimer() / 0.7f * 0.55f;
        if (player.getHealth() <= 30 && !player.isDead()) {
            target = Math.max(target, 0.18f + 0.10f * (float) Math.sin(System.currentTimeMillis() * 0.006));
        }
        vignetteAlpha = Math.max(target, vignetteAlpha - tpf * 0.9f);
        vignette.getMaterial().setColor("Color", new ColorRGBA(1, 1, 1, Math.min(1f, vignetteAlpha)));

        // хит-маркер
        if (hitMarkerTimer > 0) {
            hitMarkerTimer -= tpf;
            hitMarker.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
        } else {
            hitMarker.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }

        // баннер волны с плавным исчезновением
        if (bannerTimer > 0) {
            bannerTimer -= tpf;
            float a = Math.min(1f, bannerTimer / 0.6f);
            bannerText.setColor(new ColorRGBA(1f, 0.5f, 0.35f, a));
            bannerText.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
        } else {
            bannerText.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }

        // сообщение
        if (messageTimer > 0) {
            messageTimer -= tpf;
            float a = Math.min(1f, messageTimer / 0.4f);
            messageText.setColor(new ColorRGBA(1f, 1f, 0.8f, a));
            messageText.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
        } else {
            messageText.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }

        // подсказка
        if (hintTimer > 0) {
            hintTimer -= tpf;
            hintText.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
        } else {
            hintText.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }

        // обратный отсчёт между волнами
        if (waves.isIntermission() && waves.getWave() > 0) {
            hintText.setText(texts.format("hud.nextWave", waves.getIntermissionSeconds()));
            hintText.setColor(new ColorRGBA(0.8f, 0.85f, 1f, 0.9f));
            center(hintText, h / 2f - 40f);
            hintText.setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
        } else if (hintTimer <= 0) {
            hintText.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        }
    }

    /** Прицел скрыт в паузе. */
    public void setCrosshairVisible(boolean visible) {
        crosshair.setCullHint(visible ? com.jme3.scene.Spatial.CullHint.Dynamic
                : com.jme3.scene.Spatial.CullHint.Always);
    }

    /** Отключение HUD (detach только своего корня). */
    public void detach() {
        hudRoot.removeFromParent();
    }

    /** Тексты игры (для состояний меню/gameover). */
    public Texts getTexts() {
        return texts;
    }
}
