package game.entities;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import game.ai.ZombieAI;
import game.assets.AssetLib;
import game.core.Arena;
import game.core.PhysicsLite;

import java.util.Random;

/**
 * Зомби: процедурная модель (туловище/голова/руки/ноги с пивотами),
 * процедурная анимация ходьбы и атак, подъём из-под земли при спавне,
 * падение при смерти. Поведение вынесено в game.ai.ZombieAI.
 */
public class Zombie {

    public enum State {RISE, CHASE, ATTACK, DYING, DEAD}

    /** Звуковые события зомби (реализует GameState через позиционные AudioNode). */
    public interface Sounds {
        void growl(Vector3f position, int variant);

        void attack(Vector3f position);

        void death(Vector3f position);
    }

    public static final float RADIUS = 0.42f;
    public static final float ATTACK_RANGE = 1.5f;
    private static final float ATTACK_HIT_TIME = 0.24f;
    private static final float ATTACK_DURATION = 0.55f;

    private final Node root = new Node("zombie");
    private final Node body = new Node("zombie_body");
    private final Node armPivotL = new Node("armL");
    private final Node armPivotR = new Node("armR");
    private final Node legPivotL = new Node("legL");
    private final Node legPivotR = new Node("legR");

    private final Material skinMat;
    private final Sounds sounds;
    private final ZombieAI ai = new ZombieAI();
    private final Random rng = new Random();

    private final int id;
    private float hp;
    private final float speed;
    private final int damage;

    private State state = State.RISE;
    private float attackCooldown;
    private float attackTimer;
    private boolean attackLanded;
    private float walkPhase;
    private float growlIn;
    private float dyingTimer;
    private float flashTimer;
    private float riseProgress;

    public Zombie(AssetLib assets, int id, Vector3f spawn, float hp, float speed, int damage, Sounds sounds) {
        this.id = id;
        this.hp = hp;
        this.speed = speed;
        this.damage = damage;
        this.sounds = sounds;

        // каждый зомби получает свою копию материала (для вспышки урона и оттенка)
        skinMat = assets.lighting("skin", ColorRGBA.White);
        float tint = 0.8f + rng.nextFloat() * 0.35f;
        skinMat.setColor("Diffuse", new ColorRGBA(tint, tint, tint, 1));

        buildModel();
        root.setLocalTranslation(spawn);
        root.setLocalTranslation(spawn.x, -1.7f, spawn.z); // начинает под землёй
        growlIn = 2f + rng.nextFloat() * 4f;
    }

    /** Собирает модель из примитивов. Вперед — ось +Z. */
    private void buildModel() {
        // туловище: слегка сгорбленное
        Box torsoMesh = new Box(0.24f, 0.35f, 0.15f);
        Geometry torso = new Geometry("zombie_part", torsoMesh);
        torso.setMaterial(skinMat);
        torso.setLocalTranslation(0f, 1.18f, 0f);
        torso.setUserData("zombieId", id);
        body.attachChild(torso);

        // таз
        Box pelvisMesh = new Box(0.2f, 0.16f, 0.13f);
        Geometry pelvis = new Geometry("zombie_part", pelvisMesh);
        pelvis.setMaterial(skinMat);
        pelvis.setLocalTranslation(0f, 0.86f, 0f);
        pelvis.setUserData("zombieId", id);
        body.attachChild(pelvis);

        // голова — помечена отдельно (хедшоты)
        Sphere headMesh = new Sphere(8, 10, 0.17f);
        Geometry head = new Geometry("zombie_part", headMesh);
        head.setMaterial(skinMat);
        head.setLocalTranslation(0f, 1.72f, 0.02f);
        head.setUserData("zombieId", id);
        head.setUserData("isHead", Boolean.TRUE);
        body.attachChild(head);

        // руки: пивот у плеча, сами руки вытянуты вперёд (классическая поза зомби)
        Box armMesh = new Box(0.09f, 0.42f, 0.09f);
        Geometry armL = new Geometry("zombie_part", armMesh);
        armL.setMaterial(skinMat);
        armL.setLocalTranslation(0f, -0.21f, 0f);
        armL.setUserData("zombieId", id);
        armPivotL.setLocalTranslation(-0.3f, 1.42f, 0f);
        armPivotL.attachChild(armL);

        Geometry armR = new Geometry("zombie_part", armMesh);
        armR.setMaterial(skinMat);
        armR.setLocalTranslation(0f, -0.21f, 0f);
        armR.setUserData("zombieId", id);
        armPivotR.setLocalTranslation(0.3f, 1.42f, 0f);
        armPivotR.attachChild(armR);

        // ноги: пивот у бедра
        Box legMesh = new Box(0.11f, 0.39f, 0.11f);
        Geometry legL = new Geometry("zombie_part", legMesh);
        legL.setMaterial(skinMat);
        legL.setLocalTranslation(0f, -0.39f, 0f);
        legL.setUserData("zombieId", id);
        legPivotL.setLocalTranslation(-0.13f, 0.8f, 0f);
        legPivotL.attachChild(legL);

        Geometry legR = new Geometry("zombie_part", legMesh);
        legR.setMaterial(skinMat);
        legR.setLocalTranslation(0f, -0.39f, 0f);
        legR.setUserData("zombieId", id);
        legPivotR.setLocalTranslation(0.13f, 0.8f, 0f);
        legPivotR.attachChild(legR);

        // небольшой рандомный рост и сгорбленность
        float scale = 0.95f + rng.nextFloat() * 0.15f;
        body.setLocalScale(scale);
        body.setLocalRotation(new Quaternion().fromAngles(0.14f, 0f, 0f));

        root.attachChild(body);
        root.attachChild(armPivotL);
        root.attachChild(armPivotR);
        root.attachChild(legPivotL);
        root.attachChild(legPivotR);
    }

    /**
     * Обновление: подъём из земли, погоня, атака, смерть.
     * @param otherPositions позиции остальных зомби для расталкивания
     */
    public void update(float tpf, Player player, Arena arena, java.util.List<Vector3f> otherPositions) {
        Vector3f pos = root.getLocalTranslation();
        Vector3f playerPos = player.getPosition();

        // вспышка урона
        if (flashTimer > 0f) {
            flashTimer -= tpf;
            if (flashTimer <= 0f) {
                skinMat.setColor("Diffuse", ColorRGBA.White);
            }
        }

        switch (state) {
            case RISE: {
                riseProgress += tpf / 1.2f;
                float y = -1.7f * (1f - Math.min(1f, riseProgress));
                root.setLocalTranslation(pos.x, y, pos.z);
                animateWalk(tpf, 0.5f);
                if (riseProgress >= 1f) {
                    state = State.CHASE;
                    root.setLocalTranslation(pos.x, 0f, pos.z);
                }
                break;
            }
            case CHASE: {
                float dist = distance2D(pos, playerPos);
                if (dist < ATTACK_RANGE && attackCooldown <= 0f) {
                    startAttack();
                } else {
                    // Steering от ZombieAI
                    Vector3f dir = ai.steer(pos, playerPos, arena.getColliders(),
                            otherPositions, RADIUS, tpf, speed);
                    Vector3f next = pos.add(dir.mult(speed * tpf));
                    PhysicsLite.resolveCircleXZ(next, RADIUS, arena.getColliders());
                    PhysicsLite.clampToArena(next, Arena.HALF, RADIUS + 0.2f);
                    next.y = 0f;
                    root.setLocalTranslation(next);
                    // поворачиваем лицом к игроку (модель смотрит в +Z)
                    float yaw = FastMath.atan2(playerPos.x - pos.x, playerPos.z - pos.z);
                    root.setLocalRotation(new Quaternion().fromAngles(0f, yaw, 0f));
                    animateWalk(tpf, 1f);
                }
                attackCooldown -= tpf;
                break;
            }
            case ATTACK: {
                attackTimer += tpf;
                animateAttack();
                if (!attackLanded && attackTimer >= ATTACK_HIT_TIME) {
                    attackLanded = true;
                    // урон, если игрок всё ещё рядом
                    if (distance2D(pos, playerPos) < ATTACK_RANGE + 0.5f) {
                        player.damage(damage);
                    }
                }
                if (attackTimer >= ATTACK_DURATION) {
                    state = State.CHASE;
                    attackCooldown = 1.15f;
                    resetArms();
                }
                break;
            }
            case DYING: {
                dyingTimer += tpf;
                // падение вперёд
                float fall = Math.min(1.45f, dyingTimer * 4.2f);
                body.setLocalRotation(new Quaternion().fromAngles(fall, 0f, 0f));
                // после падения — плавно уйти под землю
                if (dyingTimer > 0.6f) {
                    float y = root.getLocalTranslation().y - tpf * 0.8f;
                    root.setLocalTranslation(pos.x, y, pos.z);
                }
                if (dyingTimer > 2.4f) {
                    state = State.DEAD;
                }
                break;
            }
            case DEAD:
            default:
                break;
        }

        // периодическое рычание
        if (state == State.CHASE || state == State.RISE) {
            growlIn -= tpf;
            if (growlIn <= 0f) {
                growlIn = 3f + rng.nextFloat() * 5f;
                if (sounds != null) {
                    sounds.growl(root.getLocalTranslation(), rng.nextInt(2));
                }
            }
        }
    }

    // ------------------------------------------------------------
    //  Анимации (процедурные)
    // ------------------------------------------------------------

    private void animateWalk(float tpf, float intensity) {
        walkPhase += tpf * speed * 2.4f * intensity;
        float s = FastMath.sin(walkPhase);
        legPivotL.setLocalRotation(new Quaternion().fromAngles(s * 0.6f, 0f, 0f));
        legPivotR.setLocalRotation(new Quaternion().fromAngles(-s * 0.6f, 0f, 0f));
        // руки вытянуты вперёд и слегка раскачиваются
        float base = -1.3f;
        armPivotL.setLocalRotation(new Quaternion().fromAngles(base + s * 0.12f, 0f, 0.1f + s * 0.06f));
        armPivotR.setLocalRotation(new Quaternion().fromAngles(base - s * 0.12f, 0f, -0.1f - s * 0.06f));
        // лёгкое покачивание корпуса
        body.setLocalTranslation(0f, FastMath.abs(s) * 0.04f, 0f);
    }

    private void animateAttack() {
        // руки вскидываются вверх, затем рубящий удар вниз
        float t = attackTimer / ATTACK_DURATION;
        float angle;
        if (t < 0.45f) {
            angle = -1.3f - 1.0f * (t / 0.45f);           // замах
        } else {
            angle = -2.3f + 1.8f * ((t - 0.45f) / 0.55f); // удар
        }
        armPivotL.setLocalRotation(new Quaternion().fromAngles(angle, 0f, 0.12f));
        armPivotR.setLocalRotation(new Quaternion().fromAngles(angle, 0f, -0.12f));
        // корпус подаётся вперёд
        body.setLocalRotation(new Quaternion().fromAngles(0.14f + FastMath.sin(t * FastMath.PI) * 0.2f, 0f, 0f));
    }

    private void resetArms() {
        armPivotL.setLocalRotation(new Quaternion().fromAngles(-1.3f, 0f, 0.1f));
        armPivotR.setLocalRotation(new Quaternion().fromAngles(-1.3f, 0f, -0.1f));
        body.setLocalRotation(new Quaternion().fromAngles(0.14f, 0f, 0f));
    }

    private void startAttack() {
        state = State.ATTACK;
        attackTimer = 0f;
        attackLanded = false;
        if (sounds != null) {
            sounds.attack(root.getLocalTranslation());
        }
    }

    // ------------------------------------------------------------
    //  Урон и смерть
    // ------------------------------------------------------------

    /**
     * Нанести урон.
     * @return true, если зомби умер
     */
    public boolean takeDamage(float amount) {
        if (state == State.DYING || state == State.DEAD) {
            return false;
        }
        hp -= amount;
        flashTimer = 0.1f;
        skinMat.setColor("Diffuse", new ColorRGBA(2.5f, 0.6f, 0.5f, 1)); // красная вспышка
        if (hp <= 0f) {
            state = State.DYING;
            dyingTimer = 0f;
            if (sounds != null) {
                sounds.death(root.getLocalTranslation());
            }
            return true;
        }
        return false;
    }

    public boolean isDying() {
        return state == State.DYING || state == State.DEAD;
    }

    public boolean isDead() {
        return state == State.DEAD;
    }

    public State getState() {
        return state;
    }

    public Node getNode() {
        return root;
    }

    public Vector3f getPosition() {
        return root.getLocalTranslation();
    }

    public int getId() {
        return id;
    }

    public int getDamage() {
        return damage;
    }

    public float getSpeed() {
        return speed;
    }

    private static float distance2D(Vector3f a, Vector3f b) {
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        return FastMath.sqrt(dx * dx + dz * dz);
    }
}
