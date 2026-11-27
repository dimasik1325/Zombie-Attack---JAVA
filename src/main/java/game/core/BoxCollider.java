package game.core;

import com.jme3.math.Vector3f;

/**
 * Ось-выровненный 3D-коллайдер (AABB) — стены, ящики, колонны.
 * Движение персонажей считается в плоскости XZ (верх — по Y).
 */
public class BoxCollider {

    public final Vector3f min;
    public final Vector3f max;

    public BoxCollider(Vector3f min, Vector3f max) {
        this.min = min;
        this.max = max;
    }

    /** Коллайдер из центра и габаритов. */
    public static BoxCollider fromCenter(Vector3f center, Vector3f size) {
        Vector3f half = size.mult(0.5f);
        return new BoxCollider(center.subtract(half), center.add(half));
    }

    public float getMinX() {
        return min.x;
    }

    public float getMaxX() {
        return max.x;
    }

    public float getMinZ() {
        return min.z;
    }

    public float getMaxZ() {
        return max.z;
    }

    /** Точка внутри коллайдера (в XZ, с запасом padding). */
    public boolean containsXZ(float x, float z, float padding) {
        return x >= min.x - padding && x <= max.x + padding
                && z >= min.z - padding && z <= max.z + padding;
    }

    /** Ближайшая к точке точка коллайдера в XZ. */
    public float closestX(float x) {
        return Math.max(min.x, Math.min(max.x, x));
    }

    public float closestZ(float z) {
        return Math.max(min.z, Math.min(max.z, z));
    }
}
