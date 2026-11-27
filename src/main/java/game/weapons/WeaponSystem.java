package game.weapons;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import game.assets.AssetLib;
import game.core.BoxCollider;
import game.entities.Zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Система оружия: владение (пистолет/дробовик), вид-модель от первого
 * лица (синхронизируется с камерой), отдача, анимация перезарядки,
 * вспышка выстрела (частицы + свет) и запуск рейкастов HitScanner.
 */
public class WeaponSystem {

    /** События выстрелов — обрабатывает GameState. */
    public interface Listener extends HitScanner.Listener {
        /** Выстрел произведён (текущее оружие, дуло). */
        void onShot(int weaponIndex, Vector3f muzzleWorld);

        /** Щелчок по пустому магазину. */
        void onEmptyClick();

        /** Началась перезарядка. */
        void onReloadStart();

        /** Сменено оружие. */
        void onSwitch();
    }

    private final List<Weapon> weapons = new ArrayList<>();
    private int currentIndex;

    private final Node holder = new Node("weaponHolder");
    private final List<Node> models = new ArrayList<>();

    private final Camera cam;
    private final Node zombiesRoot;
    private final Node worldRoot;
    private final Listener listener;
    private final Random rng = new Random();

    private ParticleEmitter flash;
    private PointLight flashLight;
    private float flashTimer;

    private float kick;           // 0..1 отдача
    private boolean fireRequested;

    public WeaponSystem(AssetLib assets, Camera cam, Node zombiesRoot, Node worldRoot, Listener listener) {
        this.cam = cam;
        this.zombiesRoot = zombiesRoot;
        this.worldRoot = worldRoot;
        this.listener = listener;

        weapons.add(new Pistol());
        weapons.add(new Shotgun());

        buildModels(assets);
        buildFlash(assets);
    }

    /** Вид-модель: корпус оружия держится справа-снизу от центра экрана. */
    private void buildModels(AssetLib assets) {
        // --- пистолет ---
        Node pistol = new Node("pistolModel");
        Material darkMetal = assets.lightingColor(new ColorRGBA(0.14f, 0.15f, 0.17f, 1),
                new ColorRGBA(0.35f, 0.35f, 0.4f, 1), 40f);
        Material grip = assets.lightingColor(new ColorRGBA(0.1f, 0.09f, 0.08f, 1),
                new ColorRGBA(0.1f, 0.1f, 0.1f, 1), 10f);

        Geometry slide = new Geometry("pistol_slide", new Box(0.032f, 0.032f, 0.19f));
        slide.setMaterial(darkMetal);
        slide.setLocalTranslation(0f, 0.03f, -0.02f);
        pistol.attachChild(slide);

        Geometry frame = new Geometry("pistol_frame", new Box(0.03f, 0.026f, 0.17f));
        frame.setMaterial(grip);
        frame.setLocalTranslation(0f, 0f, 0f);
        pistol.attachChild(frame);

        Geometry handle = new Geometry("pistol_grip", new Box(0.028f, 0.1f, 0.045f));
        handle.setMaterial(grip);
        handle.setLocalTranslation(0f, -0.06f, 0.05f);
        handle.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0.28f, 0f, 0f));
        pistol.attachChild(handle);

        Node pistolMuzzle = new Node("pistol_muzzle");
        pistolMuzzle.setLocalTranslation(0f, 0.03f, -0.13f);
        pistol.attachChild(pistolMuzzle);
        pistol.setUserData("muzzle", pistolMuzzle);
        models.add(pistol);

        // --- дробовик ---
        Node shotgun = new Node("shotgunModel");
        Material wood = assets.lightingColor(new ColorRGBA(0.4f, 0.25f, 0.12f, 1),
                new ColorRGBA(0.15f, 0.1f, 0.05f, 1), 16f);

        Geometry barrel = new Geometry("shotgun_barrel", new Box(0.04f, 0.04f, 0.5f));
        barrel.setMaterial(darkMetal);
        barrel.setLocalTranslation(0f, 0.02f, -0.16f);
        shotgun.attachChild(barrel);

        Geometry pump = new Geometry("shotgun_pump", new Box(0.048f, 0.048f, 0.14f));
        pump.setMaterial(wood);
        pump.setLocalTranslation(0f, -0.03f, -0.2f);
        shotgun.attachChild(pump);

        Geometry stock = new Geometry("shotgun_stock", new Box(0.042f, 0.09f, 0.16f));
        stock.setMaterial(wood);
        stock.setLocalTranslation(0f, -0.045f, 0.14f);
        stock.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(0.16f, 0f, 0f));
        shotgun.attachChild(stock);

        Node shotgunMuzzle = new Node("shotgun_muzzle");
        shotgunMuzzle.setLocalTranslation(0f, 0.02f, -0.42f);
        shotgun.attachChild(shotgunMuzzle);
        shotgun.setUserData("muzzle", shotgunMuzzle);
        models.add(shotgun);

        // оба держим в одном пивоте со смещением от камеры
        for (Node m : models) {
            m.setLocalTranslation(0.24f, -0.2f, -0.5f);
            holder.attachChild(m);
        }
        models.get(1).setCullHint(com.jme3.scene.Spatial.CullHint.Always); // дробовик скрыт
    }

    /** Вспышка выстрела: частицы у дула + точечный свет. */
    private void buildFlash(AssetLib assets) {
        flash = new ParticleEmitter("muzzle_flash", ParticleMesh.Type.Triangle, 12);
        flash.setMaterial(assets.particle("flash"));
        flash.setImagesX(1);
        flash.setImagesY(1);
        flash.setStartColor(new ColorRGBA(1f, 0.95f, 0.7f, 1f));
        flash.setEndColor(new ColorRGBA(1f, 0.5f, 0.1f, 0f));
        flash.setStartSize(0.22f);
        flash.setEndSize(0.02f);
        flash.setLowLife(0.04f);
        flash.setHighLife(0.09f);
        flash.setGravity(0f, 0f, 0f);
        flash.setParticlesPerSec(0f);
        flash.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 0f, -1.2f));
        flash.setVelocityVariation(0.4f);
        flash.setInWorldSpace(true);
        holder.attachChild(flash);

        flashLight = new PointLight();
        flashLight.setColor(new ColorRGBA(1f, 0.85f, 0.5f, 1f));
        flashLight.setRadius(9f);
        flashLight.setEnabled(false);
    }

    /** Свет вспышки должен быть в сцене — добавляет GameState. */
    public PointLight getFlashLight() {
        return flashLight;
    }

    public Node getHolder() {
        return holder;
    }

    public Weapon getCurrent() {
        return weapons.get(currentIndex);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<Weapon> getWeapons() {
        return weapons;
    }

    /** Запрос на выстрел (клик мыши, обрабатывается в update). */
    public void requestFire() {
        fireRequested = true;
    }

    /** Перезарядка текущего оружия. */
    public void reload() {
        if (getCurrent().startReload()) {
            listener.onReloadStart();
        }
    }

    /** Смена оружия (0 — пистолет, 1 — дробовик). */
    public void switchTo(int index) {
        if (index == currentIndex || index < 0 || index >= weapons.size()) {
            return;
        }
        getCurrent().cancelReload();
        models.get(currentIndex).setCullHint(com.jme3.scene.Spatial.CullHint.Always);
        currentIndex = index;
        models.get(currentIndex).setCullHint(com.jme3.scene.Spatial.CullHint.Dynamic);
        kick = Math.max(kick, 0.5f);
        listener.onSwitch();
    }

    /**
     * Обновление: синхронизация с камерой, анимации отдачи/перезарядки,
     * обработка запроса на выстрел, таймеры вспышки.
     */
    public void update(float tpf, Node zombiesRoot, List<Zombie> zombies, List<BoxCollider> worldColliders) {
        holder.setLocalTranslation(cam.getLocation());
        holder.setLocalRotation(cam.getRotation());

        Weapon weapon = getCurrent();
        weapon.update(tpf);

        // отдача затухает
        kick = Math.max(0f, kick - tpf * 6f);

        // анимация: базовое положение + отдача + «нырок» перезарядки
        Node model = models.get(currentIndex);
        float reloadDip = weapon.isReloading()
                ? (float) Math.sin(weapon.getReloadProgress() * Math.PI) : 0f;
        model.setLocalTranslation(0.24f, -0.2f - reloadDip * 0.12f, -0.5f + kick * 0.07f);
        model.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(
                kick * 0.16f - reloadDip * 0.6f, 0f, 0f));

        // выстрел по запросу
        if (fireRequested) {
            fireRequested = false;
            if (weapon.isEmpty()) {
                listener.onEmptyClick();
            } else if (weapon.canFire()) {
                fire(weapon, zombiesRoot);
            }
        }

        // таймер вспышки
        if (flashTimer > 0f) {
            flashTimer -= tpf;
            if (flashTimer <= 0f) {
                flashLight.setEnabled(false);
            }
        }
    }

    private void fire(Weapon weapon, Node zombiesRoot) {
        weapon.consume();
        kick = 1f;

        // вспышка у дула
        Node model = models.get(currentIndex);
        Node muzzleNode = model.getUserData("muzzle");
        Vector3f muzzleWorld = muzzleNode.getWorldTranslation();
        flash.setLocalTranslation(holder.worldToLocal(muzzleWorld, null));
        flash.emitAllParticles();
        flashLight.setPosition(muzzleWorld);
        flashLight.setEnabled(true);
        flashTimer = 0.05f;

        listener.onShot(currentIndex, muzzleWorld);

        // лучи урона
        HitScanner.fire(cam, zombiesRoot, worldRoot, weapon.getSpread(),
                weapon.getPellets(), weapon.getDamage(), weapon.getHeadshotMult(),
                listener, rng);
    }

    /** Имена для HUD. */
    public String currentName() {
        return getCurrent().getName();
    }
}
