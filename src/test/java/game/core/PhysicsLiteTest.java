package game.core;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты лёгкой физики (headless, без сцены jME). */
class PhysicsLiteTest {

    private static List<BoxCollider> oneWall(float minX, float minZ, float maxX, float maxZ) {
        List<BoxCollider> l = new ArrayList<>();
        l.add(new BoxCollider(new Vector3f(minX, 0, minZ), new Vector3f(maxX, 3, maxZ)));
        return l;
    }

    @Test
    void circleIsPushedOutOfWall() {
        List<BoxCollider> walls = oneWall(2, -1, 3, 1);
        Vector3f pos = new Vector3f(1.9f, 0, 0); // радиус 0.5, пересекает стену
        PhysicsLite.resolveCircleXZ(pos, 0.5f, walls);
        assertEquals(1.5f, pos.x, 1e-4, "круг должен быть вытолкнут влево до границы");
    }

    @Test
    void circleInsideWallIsPushedAlongSmallestAxis() {
        List<BoxCollider> walls = oneWall(-1, -1, 1, 1);
        Vector3f pos = new Vector3f(0, 0, 0);
        PhysicsLite.resolveCircleXZ(pos, 0.3f, walls);
        // минимальный выход — по любой оси, но точно за пределами
        boolean outside = Math.abs(pos.x) > 1f || Math.abs(pos.z) > 1f;
        assertTrue(outside, "позиция должна выйти за границы бокса: " + pos);
    }

    @Test
    void circleOutsideWallDoesNotMove() {
        List<BoxCollider> walls = oneWall(2, -1, 3, 1);
        Vector3f pos = new Vector3f(0, 0, 0);
        PhysicsLite.resolveCircleXZ(pos, 0.5f, walls);
        assertEquals(0f, pos.x, 1e-9);
        assertEquals(0f, pos.z, 1e-9);
    }

    @Test
    void lineOfSightBlockedAndClear() {
        List<BoxCollider> walls = oneWall(4, -1, 5, 1);
        // сквозь стену — заблокировано
        assertTrue(PhysicsLite.lineBlockedXZ(new Vector3f(0, 0, 0), new Vector3f(8, 0, 0), walls, 0.1f));
        // мимо стены — чисто
        assertFalse(PhysicsLite.lineBlockedXZ(new Vector3f(0, 0, 0), new Vector3f(8, 0, 6), walls, 0.1f));
        // вертикальный отрезок через стену
        assertTrue(PhysicsLite.lineBlockedXZ(new Vector3f(4.5f, 0, -3), new Vector3f(4.5f, 0, 3), walls, 0.0f));
    }

    @Test
    void separateKeepsCirclesApart() {
        List<Vector3f> positions = new ArrayList<>();
        positions.add(new Vector3f(0, 0, 0));
        positions.add(new Vector3f(0.3f, 0, 0));
        PhysicsLite.separate(positions, 0.4f);
        float dist = positions.get(0).distance(positions.get(1));
        assertTrue(dist >= 0.8f, "круги должны разойтись минимум на 2*r*0.97: " + dist);
    }

    @Test
    void clampToArena() {
        Vector3f p = new Vector3f(100, 0, -100);
        PhysicsLite.clampToArena(p, 30f, 1f);
        assertEquals(29f, p.x, 1e-6);
        assertEquals(-29f, p.z, 1e-6);
    }
}
