package game.entities;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import game.core.BoxCollider;
import game.core.PhysicsLite;

import java.util.List;
import java.util.Random;

/**
 * Игрок от первого лица: позиция (ноги), здоровье, WASD-движение,
 * прыжок с гравитацией, скольжение по стенам, headbob и тряска камеры.
 */
public class Player {

    public static final float EYE_HEIGHT = 1.62f;
    public static final float RADIUS = 0.38f;
    public static final float WALK_SPEED = 4.4f;
    public static final float RUN_SPEED = 7.2f;
    public static final float JUMP_VELOCITY = 5.4f;
    public static final float GRAVITY = 16f;

    private final Vector3f position = new Vector3f();   // уровень ног
    private float yaw, pitch;
    private float velocityY;
    private boolean onGround = true;

    private int health = 100;
    private final int maxHealth = 100;
    private float regenDelay;
    private float hurtTimer;      // для виньетки урона
    private float shake;          // тряска камеры
    private float bobPhase;       // покачивание камеры
    private boolean justHurt;     // флаг «только что получил урон» для GameState

    // Флаги ввода (устанавливает GameState)
    public boolean moveForward, moveBack, moveLeft, moveRight, running, jumpRequested;

    private final Random rng = new Random();
    private final Quaternion tmpQuat = new Quaternion();

    public Player(Vector3f start) {
        position.set(start);
    }

    /** Обновление движения и состояния. */
    public void update(float tpf, List<BoxCollider> colliders, float arenaHalf) {
        // желаемое направление в плоскости XZ
        Vector3f wish = new Vector3f(0, 0, 0);
        Vector3f fwd = forward();
        Vector3f left = left();
        if (moveForward) wish.addLocal(fwd);
        if (moveBack) wish.addLocal(fwd.negate());
        if (moveLeft) wish.addLocal(left);
        if (moveRight) wish.addLocal(left.negate());
        if (wish.lengthSquared() > 1e-6f) {
            wish.normalizeLocal();
        }

        float speed = running ? RUN_SPEED : WALK_SPEED;
        Vector3f delta = wish.mult(speed * tpf);

        // прыжок / гравитация
        if (jumpRequested && onGround) {
            velocityY = JUMP_VELOCITY;
            onGround = false;
        }
        jumpRequested = false;
        velocityY -= GRAVITY * tpf;
        if (velocityY < -30f) {
            velocityY = -30f;
        }
        float ny = position.y + velocityY * tpf;
        if (ny <= 0f) {
            ny = 0f;
            velocityY = 0f;
            onGround = true;
        }
        position.y = ny;

        // движение + столкновения со скольжением
        position.addLocal(delta);
        PhysicsLite.resolveCircleXZ(position, RADIUS, colliders);
        PhysicsLite.clampToArena(position, arenaHalf, RADIUS + 0.2f);

        // headbob
        float horiz = wish.length() * speed;
        bobPhase += horiz * tpf * 1.6f;

        // пассивная регенерация после паузы в уроне
        if (regenDelay > 0f) {
            regenDelay -= tpf;
        } else if (health < maxHealth) {
            health = Math.min(maxHealth, health + (int) Math.ceil(3f * tpf));
        }

        // затухание эффектов
        hurtTimer = Math.max(0f, hurtTimer - tpf);
        shake = Math.max(0f, shake - tpf * 1.3f);
        justHurt = false;
    }

    /** Применяет позицию/поворот к камере (с headbob и тряской). */
    public void applyTo(com.jme3.renderer.Camera cam) {
        float bob = onGround ? FastMath.sin(bobPhase * 2f) * 0.035f : 0f;
        Vector3f eye = new Vector3f(position.x, position.y + EYE_HEIGHT + bob, position.z);
        if (shake > 0f) {
            eye.addLocal((rng.nextFloat() - 0.5f) * shake * 0.5f,
                    (rng.nextFloat() - 0.5f) * shake * 0.5f,
                    (rng.nextFloat() - 0.5f) * shake * 0.5f);
        }
        cam.setLocation(eye);
        tmpQuat.fromAngles(pitch, yaw, 0f);
        cam.setRotation(tmpQuat);
    }

    /** Получить урон. */
    public void damage(int amount) {
        if (amount <= 0) {
            return;
        }
        health = Math.max(0, health - amount);
        regenDelay = 5.5f;
        hurtTimer = 0.7f;
        shake = 0.5f;
        justHurt = true;
    }

    /** Лечение. */
    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public boolean isDead() {
        return health <= 0;
    }

    // ------------------------------------------------------------
    //  Геометрия
    // ------------------------------------------------------------

    /** Горизонтальное направление взгляда. */
    public Vector3f forward() {
        return new Vector3f(-FastMath.sin(yaw), 0f, -FastMath.cos(yaw));
    }

    /** Вектор влево (горизонтальный). */
    public Vector3f left() {
        return new Vector3f(-FastMath.cos(yaw), 0f, FastMath.sin(yaw));
    }

    public Vector3f getPosition() {
        return position.clone();
    }

    public float getX() {
        return position.x;
    }

    public float getZ() {
        return position.z;
    }

    // ------------------------------------------------------------
    //  Параметры для HUD/логики
    // ------------------------------------------------------------

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public float getHurtTimer() {
        return hurtTimer;
    }

    public boolean isJustHurt() {
        return justHurt;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void addYaw(float delta) {
        this.yaw += delta;
    }

    public void setPitch(float pitch) {
        this.pitch = FastMath.clamp(pitch, -1.35f, 1.35f);
    }

    public float getPitch() {
        return pitch;
    }

    public void addPitch(float delta) {
        setPitch(this.pitch + delta);
    }

    public boolean isRunning() {
        return running;
    }
}
