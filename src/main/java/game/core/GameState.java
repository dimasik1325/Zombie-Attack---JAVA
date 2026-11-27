package game.core;

import com.jme3.audio.AudioNode;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Node;
import game.ai.WaveManager;
import game.assets.AssetLib;
import game.entities.Pickup;
import game.entities.Player;
import game.entities.Zombie;
import game.ui.HUD;
import game.ui.Texts;
import game.weapons.Weapon;
import game.weapons.WeaponSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Основное состояние игры: арена, игрок, зомби, волны, оружие, HUD.
 * Принимает весь ввод (WASD/мышь/1/2/R/E/Esc) и склеивает системы.
 */
public class GameState extends AbstractAppStateBase implements ActionListener, AnalogListener {

    private static final Logger LOG = Logger.getLogger(GameState.class.getName());

    private static final float MOUSE_SENS = 1.6f;
    private static final float PICKUP_RANGE = 1.8f;

    private final ZombieAttackApp app;

    private Node world = new Node("game_world");
    private final Node zombiesRoot = new Node("zombies");
    private final Node effectsRoot = new Node("effects");

    private Arena arena;
    private Player player;
    private WeaponSystem weapons;
    private HUD hud;
    private WaveManager waves;
    private Texts texts;
    private FilterPostProcessor fpp;

    private final List<Zombie> zombies = new ArrayList<>();
    private final Map<Integer, Zombie> zombieById = new HashMap<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Vector3f> zombiePositions = new ArrayList<>();
    private final List<AudioNode> tempAudio = new ArrayList<>();
    private final List<Float> tempAudioTtl = new ArrayList<>();
    private final Random rng = new Random();
    private int nextZombieId = 1;

    private int score;
    private boolean paused;
    private boolean gameOver;
    private float gameOverTimer;
    private AudioNode[] shotSounds = new AudioNode[2];

    public GameState(ZombieAttackApp app) {
        this.app = app;
    }

    // ============================================================
    //  Инициализация
    // ============================================================

    @Override
    protected void init() {
        AssetLib assets = app.getAssets();
        texts = new Texts(assets.hasCyrillic());

        // арена + игрок
        arena = new Arena(assets);
        world.attachChild(arena.getNode());
        player = new Player(arena.getPlayerStart());

        // зомби и эффекты
        world.attachChild(zombiesRoot);
        world.attachChild(effectsRoot);
        buildBloodAndImpact(assets);

        // оружие
        weapons = new WeaponSystem(assets, app.getCamera(), zombiesRoot, arena.getNode(), new WeaponListener());
        world.attachChild(weapons.getHolder());
        world.addLight(weapons.getFlashLight());

        // волны
        waves = new WaveManager((point, hp, speed, damage) -> spawnZombie(point, hp, speed, damage), arena);

        // HUD
        hud = new HUD(app.getGuiNode(), assets.getHudFont(), texts, assets.getAssetManager(),
                app.getCamera(), assets.texture("vignette"));

        // туман + лёгкий bloom для атмосферы
        fpp = new FilterPostProcessor(assets.getAssetManager());
        fpp.addFilter(new FogFilter(new ColorRGBA(0.05f, 0.06f, 0.09f, 1f), 1.0f, 62f));
        fpp.addFilter(new BloomFilter());
        app.getViewPort().addProcessor(fpp);

        // звуки
        buildSounds(assets);
        playAmbient();

        // ввод
        registerInput();

        // 3D-мир — в rootNode приложения
        app.getRootNode().attachChild(world);
        app.getInputManager().setCursorVisible(false);
    }

    /** Кровь и искры попаданий — два burst-эмиттера. */
    private void buildBloodAndImpact(AssetLib assets) {
        bloodFx = new ParticleEmitter("blood", ParticleMesh.Type.Triangle, 26);
        bloodFx.setMaterial(assets.particle("blood"));
        bloodFx.setImagesX(1);
        bloodFx.setImagesY(1);
        bloodFx.setStartColor(new ColorRGBA(0.6f, 0.05f, 0.04f, 1f));
        bloodFx.setEndColor(new ColorRGBA(0.4f, 0.02f, 0.02f, 0f));
        bloodFx.setStartSize(0.1f);
        bloodFx.setEndSize(0.02f);
        bloodFx.setLowLife(0.25f);
        bloodFx.setHighLife(0.55f);
        bloodFx.setGravity(0f, -5f, 0f);
        bloodFx.setParticlesPerSec(0f);
        bloodFx.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 1.6f, 0f));
        bloodFx.setVelocityVariation(1.1f);
        bloodFx.setInWorldSpace(true);
        effectsRoot.attachChild(bloodFx);

        impactFx = new ParticleEmitter("impact", ParticleMesh.Type.Triangle, 14);
        impactFx.setMaterial(assets.particle("flash"));
        impactFx.setImagesX(1);
        impactFx.setImagesY(1);
        impactFx.setStartColor(new ColorRGBA(0.8f, 0.75f, 0.6f, 0.9f));
        impactFx.setEndColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 0f));
        impactFx.setStartSize(0.05f);
        impactFx.setEndSize(0.01f);
        impactFx.setLowLife(0.15f);
        impactFx.setHighLife(0.3f);
        impactFx.setGravity(0f, -6f, 0f);
        impactFx.setParticlesPerSec(0f);
        impactFx.getParticleInfluencer().setInitialVelocity(new Vector3f(0f, 1.4f, 0f));
        impactFx.setVelocityVariation(1.4f);
        impactFx.setInWorldSpace(true);
        effectsRoot.attachChild(impactFx);
    }

    private ParticleEmitter bloodFx;
    private ParticleEmitter impactFx;

    /** Предзагрузка не-позиционных звуков (щелчки выстрелов и т.п.). */
    private void buildSounds(AssetLib assets) {
        for (int i = 0; i < shotSounds.length; i++) {
            shotSounds[i] = new AudioNode(assets.getAssetManager(),
                    (i == 0 ? "shot_pistol.wav" : "shot_shotgun.wav"));
            shotSounds[i].setVolume(i == 0 ? 0.8f : 1f);
            world.attachChild(shotSounds[i]);
        }
        sndReload = makeSound("reload.wav", 0.8f);
        sndEmpty = makeSound("empty.wav", 0.7f);
        sndSwitch = makeSound("switch.wav", 0.7f);
        sndHurt = makeSound("hurt.wav", 0.9f);
        sndPickup = makeSound("pickup.wav", 0.8f);
        sndWaveStart = makeSound("wave_start.wav", 0.9f);
        sndWaveCleared = makeSound("wave_cleared.wav", 0.8f);
    }

    private AudioNode makeSound(String file, float volume) {
        AudioNode n = new AudioNode(app.getAssets().getAssetManager(), file);
        n.setVolume(volume);
        world.attachChild(n);
        return n;
    }

    private AudioNode sndReload, sndEmpty, sndSwitch, sndHurt, sndPickup, sndWaveStart, sndWaveCleared;
    private AudioNode ambientGame;

    /** Эмбиент арены — тихий низкий гул, лупится. */
    private void playAmbient() {
        ambientGame = makeSound("ambient_game.wav", 0.35f);
        ambientGame.setLooping(true);
        playSafe(ambientGame);
    }

    // ============================================================
    //  Ввод
    // ============================================================

    private void registerInput() {
        com.jme3.input.InputManager im = app.getInputManager();
        im.addMapping("za_fwd", new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        im.addMapping("za_back", new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
        im.addMapping("za_left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        im.addMapping("za_right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        im.addMapping("za_run", new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        im.addMapping("za_jump", new KeyTrigger(KeyInput.KEY_SPACE));
        im.addMapping("za_fire", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        im.addMapping("za_reload", new KeyTrigger(KeyInput.KEY_R));
        im.addMapping("za_pickup", new KeyTrigger(KeyInput.KEY_E));
        im.addMapping("za_w1", new KeyTrigger(KeyInput.KEY_1));
        im.addMapping("za_w2", new KeyTrigger(KeyInput.KEY_2));
        im.addMapping("za_pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
        im.addMapping("za_quit", new KeyTrigger(KeyInput.KEY_Q));
        im.addMapping("za_lookL", new MouseAxisTrigger(MouseInput.MOUSE_X, true));
        im.addMapping("za_lookR", new MouseAxisTrigger(MouseInput.MOUSE_X, false));
        im.addMapping("za_lookU", new MouseAxisTrigger(MouseInput.MOUSE_Y, true));
        im.addMapping("za_lookD", new MouseAxisTrigger(MouseInput.MOUSE_Y, false));
        im.addListener(this,
                "za_fwd", "za_back", "za_left", "za_right", "za_run", "za_jump",
                "za_fire", "za_reload", "za_pickup", "za_w1", "za_w2", "za_pause", "za_quit",
                "za_lookL", "za_lookR", "za_lookU", "za_lookD");
    }

    private void unregisterInput() {
        com.jme3.input.InputManager im = app.getInputManager();
        String[] names = {"za_fwd", "za_back", "za_left", "za_right", "za_run", "za_jump",
                "za_fire", "za_reload", "za_pickup", "za_w1", "za_w2", "za_pause", "za_quit",
                "za_lookL", "za_lookR", "za_lookU", "za_lookD"};
        im.removeListener(this);
        for (String n : names) {
            if (im.hasMapping(n)) {
                im.deleteMapping(n);
            }
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        // пауза работает всегда
        if (name.equals("za_pause") && isPressed) {
            if (gameOver) {
                return;
            }
            if (paused) {
                resume();
            } else {
                pause();
            }
            return;
        }
        if (name.equals("za_quit") && isPressed && paused) {
            app.showMenu();
            return;
        }
        if (paused || gameOver) {
            return;
        }

        switch (name) {
            case "za_fwd" -> player.moveForward = isPressed;
            case "za_back" -> player.moveBack = isPressed;
            case "za_left" -> player.moveLeft = isPressed;
            case "za_right" -> player.moveRight = isPressed;
            case "za_run" -> player.running = isPressed;
            case "za_jump" -> {
                if (isPressed) {
                    player.jumpRequested = true;
                }
            }
            case "za_fire" -> {
                if (isPressed) {
                    weapons.requestFire();
                }
            }
            case "za_reload" -> {
                if (isPressed) {
                    weapons.reload();
                }
            }
            case "za_w1" -> {
                if (isPressed) {
                    weapons.switchTo(0);
                }
            }
            case "za_w2" -> {
                if (isPressed) {
                    weapons.switchTo(1);
                }
            }
            case "za_pickup" -> {
                if (isPressed) {
                    tryPickup();
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (paused || gameOver) {
            return;
        }
        switch (name) {
            case "za_lookL" -> player.addYaw(value * MOUSE_SENS);
            case "za_lookR" -> player.addYaw(-value * MOUSE_SENS);
            case "za_lookU" -> player.addPitch(value * MOUSE_SENS);
            case "za_lookD" -> player.addPitch(-value * MOUSE_SENS);
            default -> {
            }
        }
    }

    // ============================================================
    //  Кадр
    // ============================================================

    @Override
    public void stateUpdate(float tpf) {
        if (gameOver) {
            // короткая пауза на экран смерти, чтобы прозвучал звук
            gameOverTimer -= tpf;
            if (gameOverTimer <= 0f) {
                app.showGameOver(score, waves.getWave());
            }
            return;
        }
        if (paused) {
            return;
        }

        // урон мог случиться в прошлом кадре (после player.update) — фиксируем до сброса
        boolean hurtThisFrame = player.isJustHurt();

        // игрок и камера
        player.update(tpf, arena.getColliders(), Arena.HALF);
        player.applyTo(app.getCamera());

        // оружие
        weapons.update(tpf, zombiesRoot, zombies, arena.getColliders());

        // зомби: обновление + расталкивание
        zombiePositions.clear();
        for (Zombie z : zombies) {
            if (!z.isDying()) {
                zombiePositions.add(z.getPosition());
            }
        }
        PhysicsLite.separate(zombiePositions, Zombie.RADIUS);
        int i = 0;
        for (Zombie z : zombies) {
            if (!z.isDying()) {
                Vector3f p = zombiePositions.get(i++);
                z.getNode().setLocalTranslation(p.x, z.getNode().getLocalTranslation().y, p.z);
            }
        }
        for (Zombie z : zombies) {
            z.update(tpf, player, arena, zombiePositions);
        }
        // убрать полностью умерших
        for (int k = zombies.size() - 1; k >= 0; k--) {
            if (zombies.get(k).isDead()) {
                Zombie z = zombies.remove(k);
                zombieById.remove(z.getId());
                zombiesRoot.detachChild(z.getNode());
                waves.onZombieGone();
            }
        }

        // волны
        waves.update(tpf, player.getPosition());
        if (waves.consumeWaveStart()) {
            hud.showWaveBanner(waves.getWave());
            playSafe(sndWaveStart);
            waveInProgress = true;
        }
        if (waveInProgress && waves.getPendingSpawn() == 0
                && waves.getAliveSpawned() == 0 && zombies.isEmpty()) {
            int bonus = waves.clearBonus();
            score += bonus;
            waveInProgress = false;
            playSafe(sndWaveCleared);
            hud.showMessage(texts.format("hud.waveCleared", waves.getWave(), bonus), 2.2f);
        }

        // подборы
        for (Pickup p : pickups) {
            p.update(tpf);
        }
        Pickup near = nearestPickup();
        if (near != null) {
            hud.showHint(texts.format("hud.pickupHint",
                    near.getKind() == Pickup.Kind.AMMO ? texts.get("hud.ammoName") : texts.get("hud.medkitName")));
        }

        // звук урона
        if (hurtThisFrame) {
            playSafe(sndHurt);
            hud.flashDamage();
        }

        // жизнь временных позиционных звуков
        for (int k = tempAudio.size() - 1; k >= 0; k--) {
            float ttl = tempAudioTtl.get(k) - tpf;
            if (ttl <= 0f) {
                world.detachChild(tempAudio.get(k));
                tempAudio.remove(k);
                tempAudioTtl.remove(k);
            } else {
                tempAudioTtl.set(k, ttl);
            }
        }

        // HUD
        int aliveCount = zombies.size() + waves.getPendingSpawn();
        hud.update(tpf, player, weapons, waves, score, aliveCount);
        hud.setCrosshairVisible(!paused);

        // смерть игрока — короткая задержка и экран Game Over
        if (player.isDead()) {
            gameOver = true;
            gameOverTimer = 1.5f;
            app.getInputManager().setCursorVisible(true);
        }
    }

    /** Флаг: текущая волна ещё «в процессе» (для подсчёта бонуса). */
    private boolean waveInProgress;

    // ============================================================
    //  Зомби: спавн, попадания, звуки
    // ============================================================

    private void spawnZombie(Vector3f point, float hp, float speed, int damage) {
        int id = nextZombieId++;
        Zombie z = new Zombie(app.getAssets(), id, point, hp, speed, damage, new ZombieSounds());
        zombies.add(z);
        zombieById.put(id, z);
        zombiesRoot.attachChild(z.getNode());
    }

    /** Позиционные звуки зомби (объект-адаптер). */
    private class ZombieSounds implements Zombie.Sounds {
        @Override
        public void growl(Vector3f position, int variant) {
            playPositional((variant == 0 ? "growl1.wav" : "growl2.wav"), position, 0.8f);
        }

        @Override
        public void attack(Vector3f position) {
            playPositional("attack.wav", position, 0.9f);
        }

        @Override
        public void death(Vector3f position) {
            playPositional("zombie_die.wav", position, 0.9f);
        }
    }

    /** Одноразовый позиционный звук с авто-удалением узла. */
    private void playPositional(String file, Vector3f position, float volume) {
        try {
            AudioNode n = new AudioNode(app.getAssets().getAssetManager(), file);
            n.setPositional(true);
            n.setRefDistance(3f);
            n.setMaxDistance(30f);
            n.setVolume(volume);
            n.setLocalTranslation(position);
            world.attachChild(n);
            n.playInstance();
            tempAudio.add(n);
            tempAudioTtl.add(3f);
        } catch (Exception e) {
            // звук не критичен
        }
    }

    private void playSafe(AudioNode n) {
        try {
            if (n != null) {
                n.playInstance();
            }
        } catch (Exception ignored) {
            // аудиосистема недоступна
        }
    }

    /** События выстрелов от WeaponSystem. */
    private class WeaponListener implements WeaponSystem.Listener {
        @Override
        public void onZombieHit(int zombieId, Vector3f point, float damage, boolean headshot) {
            Zombie z = zombieById.get(zombieId);
            if (z == null) {
                return;
            }
            bloodFx.setLocalTranslation(point);
            bloodFx.emitAllParticles();
            hud.showHitMarker();

            boolean killed = z.takeDamage(damage);
            if (killed) {
                score += 100 + 25 * (waves.getWave() - 1) + (headshot ? 60 : 0);
                maybeDropPickup(z.getPosition());
            }
        }

        @Override
        public void onWorldHit(Vector3f point) {
            impactFx.setLocalTranslation(point);
            impactFx.emitAllParticles();
        }

        @Override
        public void onShot(int weaponIndex, Vector3f muzzleWorld) {
            playSafe(shotSounds[weaponIndex]);
        }

        @Override
        public void onEmptyClick() {
            playSafe(sndEmpty);
        }

        @Override
        public void onReloadStart() {
            playSafe(sndReload);
        }

        @Override
        public void onSwitch() {
            playSafe(sndSwitch);
        }
    }

    /** Шанс выбросить боеприпас/аптечку после смерти зомби. */
    private void maybeDropPickup(Vector3f position) {
        float roll = rng.nextFloat();
        if (roll < 0.32f) {
            pickups.add(new Pickup(app.getAssets(), Pickup.Kind.AMMO, position.clone()));
            effectsRoot.attachChild(pickups.get(pickups.size() - 1).getNode());
        } else if (roll < 0.50f) {
            pickups.add(new Pickup(app.getAssets(), Pickup.Kind.HEALTH, position.clone()));
            effectsRoot.attachChild(pickups.get(pickups.size() - 1).getNode());
        }
    }

    private Pickup nearestPickup() {
        Pickup best = null;
        float bestD = PICKUP_RANGE;
        for (Pickup p : pickups) {
            if (p.isTaken()) {
                continue;
            }
            float dx = p.getPosition().x - player.getX();
            float dz = p.getPosition().z - player.getZ();
            float d = (float) Math.sqrt(dx * dx + dz * dz);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Подбор предмета по клавише E. */
    private void tryPickup() {
        Pickup p = nearestPickup();
        if (p == null) {
            return;
        }
        if (p.getKind() == Pickup.Kind.AMMO) {
            int added = 0;
            for (Weapon w : weapons.getWeapons()) {
                added += w.addReserve(w == weapons.getWeapons().get(0) ? 24 : 8);
            }
            if (added == 0) {
                hud.showMessage(texts.get("hud.reserveFull"), 1.4f);
                return;
            }
            hud.showMessage(texts.format("hud.ammoGot", added), 1.6f);
        } else {
            player.heal(35);
            hud.showMessage(texts.format("hud.healthGot", 35), 1.6f);
        }
        playSafe(sndPickup);
        p.setTaken(true);
        effectsRoot.detachChild(p.getNode());
        pickups.remove(p);
    }

    // ============================================================
    //  Пауза
    // ============================================================

    private void pause() {
        paused = true;
        app.getInputManager().setCursorVisible(true);
        hud.setPauseVisible(true);
        hud.setCrosshairVisible(false);
    }

    private void resume() {
        paused = false;
        app.getInputManager().setCursorVisible(false);
        hud.setPauseVisible(false);
        hud.setCrosshairVisible(true);
    }

    // ============================================================
    //  Жизненный цикл состояния
    // ============================================================

    @Override
    protected void cleanupAll() {
        unregisterInput();
        app.getRootNode().detachChild(world);
        if (fpp != null) {
            app.getViewPort().removeProcessor(fpp);
        }
        hud.detach();
        app.getInputManager().setCursorVisible(true);
    }
}
