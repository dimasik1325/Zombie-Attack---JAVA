package game.ai;

import com.jme3.math.Vector3f;

/**
 * Менеджер бесконечных волн: считает количество и параметры зомби,
 * определяет моменты спавна и межволновые паузы. Сложность растёт
 * с номером волны (здоровье, скорость, урон, плотность толпы).
 *
 * Класс не зависит от jME-сцены: спавн и выбор точек делегированы
 * интерфейсам, поэтому тестируется в headless-режиме.
 */
public class WaveManager {

    /** Делегат спавна зомби (реализует GameState). */
    public interface Spawner {
        void spawn(Vector3f point, float hp, float speed, int damage);
    }

    /** Поставщик точек спавна (реализует Arena). */
    public interface SpawnPoints {
        Vector3f pickSpawnPoint(Vector3f awayFrom);
    }

    private static final int START_COUNT = 4;
    private static final int COUNT_PER_WAVE = 3;
    private static final int MAX_COUNT = 42;
    private static final float BASE_HP = 60f;
    private static final float HP_PER_WAVE = 14f;
    private static final float BASE_SPEED = 1.7f;
    private static final float SPEED_PER_WAVE = 0.09f;
    private static final float MAX_SPEED = 3.5f;
    private static final int BASE_DAMAGE = 8;
    private static final float INTERMISSION = 5f;

    private final Spawner spawner;
    private final SpawnPoints spawnPoints;

    private int wave;
    private int toSpawn;
    private int aliveSpawned;
    private float spawnTimer;
    private float intermissionTimer = 3f;
    private boolean intermission = true;

    public WaveManager(Spawner spawner, SpawnPoints spawnPoints) {
        this.spawner = spawner;
        this.spawnPoints = spawnPoints;
    }

    /**
     * Обновление волны. Вызывает спавн по таймеру и переключает паузы.
     * @param playerPos позиция игрока (для выбора точки спавна)
     */
    public void update(float tpf, Vector3f playerPos) {
        if (intermission) {
            intermissionTimer -= tpf;
            if (intermissionTimer <= 0f) {
                startWave();
            }
            return;
        }
        if (toSpawn > 0) {
            spawnTimer -= tpf;
            if (spawnTimer <= 0f) {
                spawnTimer = Math.max(0.4f, 1.3f - wave * 0.05f);
                Vector3f point = spawnPoints.pickSpawnPoint(playerPos);
                spawner.spawn(point, hpForWave(), speedForWave(), damageForWave());
                toSpawn--;
                aliveSpawned++;
            }
        } else if (aliveSpawned <= 0) {
            // волна пройдена
            intermission = true;
            intermissionTimer = INTERMISSION;
        }
    }

    private void startWave() {
        wave++;
        toSpawn = Math.min(MAX_COUNT, START_COUNT + COUNT_PER_WAVE * (wave - 1));
        spawnTimer = 0.2f;
        intermission = false;
        waveStartPending = true;
    }

    // ------------------------------------------------------------
    //  Формулы сложности
    // ------------------------------------------------------------

    public float hpForWave() {
        return BASE_HP + HP_PER_WAVE * (wave - 1);
    }

    public float speedForWave() {
        return Math.min(MAX_SPEED, BASE_SPEED + SPEED_PER_WAVE * (wave - 1));
    }

    public int damageForWave() {
        return BASE_DAMAGE + wave;
    }

    /** Бонус очков за волну. */
    public int clearBonus() {
        return 50 * wave;
    }

    /** Зомби, вызванный смертью/полным исчезновением одного врага. */
    public void onZombieGone() {
        aliveSpawned = Math.max(0, aliveSpawned - 1);
    }

    // ------------------------------------------------------------
    //  Состояние для HUD
    // ------------------------------------------------------------

    public int getWave() {
        return wave;
    }

    public int getPendingSpawn() {
        return toSpawn;
    }

    public int getAliveSpawned() {
        return aliveSpawned;
    }

    public boolean isIntermission() {
        return intermission;
    }

    /** Сколько секунд до следующей волны (для обратного отсчёта). */
    public int getIntermissionSeconds() {
        return (int) Math.ceil(intermissionTimer);
    }

    /** Флаг «волна только что началась» — забирается HUD/звуком ровно один раз. */
    private boolean waveStartPending;

    /** Забирает событие старта волны (true только один раз). */
    public boolean consumeWaveStart() {
        if (waveStartPending) {
            waveStartPending = false;
            return true;
        }
        return false;
    }
}
