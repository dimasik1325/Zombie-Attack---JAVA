package game.ai;

import com.jme3.math.Vector3f;
import game.core.BoxCollider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты менеджера волн без jME-сцены. Спавн и выбор точек делегированы
 * интерфейсам, поэтому время можно «проматывать» крупными tpf.
 */
class WaveManagerTest {

    @Test
    void firstWaveSpawnsConfiguredCount() {
        AtomicInteger spawned = new AtomicInteger();
        WaveManager waves = new WaveManager(
                (p, hp, speed, dmg) -> spawned.incrementAndGet(),
                away -> new Vector3f(20, 0, 20));

        // межволновая пауза (3 c) + спавн всей волны с интервалом 1.25 c
        for (int i = 0; i < 60; i++) {
            waves.update(0.25f, new Vector3f());
        }
        assertEquals(4, spawned.get(), "волна 1 = 4 зомби");
        assertEquals(1, waves.getWave());
        assertFalse(waves.isIntermission(), "волна в процессе");
    }

    @Test
    void difficultyGrowsWithWave() {
        WaveManager waves = new WaveManager((p, hp, speed, dmg) -> {
        }, away -> new Vector3f(20, 0, 20));

        // стартуем волну 1 (межволновая пауза 3 с)
        for (int i = 0; i < 15; i++) {
            waves.update(0.25f, new Vector3f());
        }
        assertEquals(1, waves.getWave());
        assertEquals(60f, waves.hpForWave(), 1e-3);
        assertEquals(9, waves.damageForWave());

        // проматываем несколько волн: заспавненные мгновенно «убиваются»
        for (int i = 0; i < 150; i++) {
            waves.update(0.5f, new Vector3f());
            waves.onZombieGone();
        }
        assertTrue(waves.getWave() >= 2, "волны должны сменяться");
        assertTrue(waves.hpForWave() > 60f, "здоровье растёт");
        assertTrue(waves.speedForWave() > 1.7f, "скорость растёт");
        assertTrue(waves.damageForWave() > 8, "урон растёт");
    }

    @Test
    void intermissionAfterClearingWave() {
        AtomicInteger alive = new AtomicInteger();
        WaveManager waves = new WaveManager(
                (p, hp, speed, dmg) -> alive.incrementAndGet(),
                away -> new Vector3f(20, 0, 20));

        boolean sawIntermission = false;
        for (int i = 0; i < 200; i++) {
            waves.update(0.25f, new Vector3f());
            if (alive.get() > 0) {
                alive.decrementAndGet();
                waves.onZombieGone();
            }
            if (waves.isIntermission() && waves.getWave() >= 1) {
                sawIntermission = true;
            }
        }
        assertTrue(sawIntermission, "после зачистки волны должна начаться пауза");
        assertTrue(waves.getWave() >= 2, "после паузы начинается следующая волна");
        assertTrue(waves.getIntermissionSeconds() >= 0);
    }

    @Test
    void waveStartConsumedOnce() {
        WaveManager waves = new WaveManager((p, hp, speed, dmg) -> {
        }, away -> new Vector3f(20, 0, 20));
        for (int i = 0; i < 30; i++) {
            waves.update(0.25f, new Vector3f());
        }
        assertTrue(waves.consumeWaveStart(), "событие старта волны должно сработать");
        assertFalse(waves.consumeWaveStart(), "и только один раз");
    }

    @Test
    void boxColliderFromCenter() {
        BoxCollider c = BoxCollider.fromCenter(new Vector3f(10, 0, 10), new Vector3f(2, 1, 4));
        assertEquals(9, c.getMinX(), 1e-6);
        assertEquals(11, c.getMaxX(), 1e-6);
        assertEquals(8, c.getMinZ(), 1e-6);
        assertEquals(12, c.getMaxZ(), 1e-6);
    }
}
