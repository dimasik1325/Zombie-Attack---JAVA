package game.core;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.List;

/**
 * Лёгкая «физика» для арены: столкновение кругов с AABB-коллайдерами
 * (со скольжением по стенам), расталкивание зомби, проверка линии
 * видимости. Полноценный Bullet здесь не нужен: арена плоская, а
 * собственная 2D-физика в XZ быстрее, детерминированнее и не тянет
 * нативные библиотеки в .exe.
 */
public final class PhysicsLite {

    private PhysicsLite() {
    }

    /**
     * Выталкивает круг из всех пересечённых коллайдеров (в XZ).
     * Дает естественное скольжение вдоль стен при повторении каждый кадр.
     */
    public static void resolveCircleXZ(Vector3f pos, float radius, List<BoxCollider> colliders) {
        for (BoxCollider c : colliders) {
            float cx = c.closestX(pos.x);
            float cz = c.closestZ(pos.z);
            float dx = pos.x - cx;
            float dz = pos.z - cz;
            float d2 = dx * dx + dz * dz;
            if (d2 >= radius * radius) {
                continue;
            }
            if (d2 > 1e-8f) {
                float d = FastMath.sqrt(d2);
                float push = (radius - d) / d;
                pos.x += dx * push;
                pos.z += dz * push;
            } else {
                // центр внутри бокса — выталкиваем по минимальной оси
                float left = pos.x - (c.getMinX() - radius);
                float right = (c.getMaxX() + radius) - pos.x;
                float up = pos.z - (c.getMinZ() - radius);
                float down = (c.getMaxZ() + radius) - pos.z;
                float m = Math.min(Math.min(left, right), Math.min(up, down));
                if (m == left) {
                    pos.x = c.getMinX() - radius;
                } else if (m == right) {
                    pos.x = c.getMaxX() + radius;
                } else if (m == up) {
                    pos.z = c.getMinZ() - radius;
                } else {
                    pos.z = c.getMaxZ() + radius;
                }
            }
        }
    }

    /**
     * Пересекается ли отрезок AB с каким-либо коллайдером в XZ
     * (алгоритм Лианга-Барски). Используется зомби-ИИ для проверки
     * прямой видимости игрока.
     */
    public static boolean lineBlockedXZ(Vector3f a, Vector3f b, List<BoxCollider> colliders, float padding) {
        for (BoxCollider c : colliders) {
            if (segmentHitsRect(a.x, a.z, b.x, b.z,
                    c.getMinX() - padding, c.getMinZ() - padding,
                    c.getMaxX() + padding, c.getMaxZ() + padding)) {
                return true;
            }
        }
        return false;
    }

    /** Лианг-Барски: пересечение отрезка с прямоугольником. */
    private static boolean segmentHitsRect(float ax, float az, float bx, float bz,
                                           float minX, float minZ, float maxX, float maxZ) {
        float dx = bx - ax;
        float dz = bz - az;
        float t0 = 0f, t1 = 1f;

        // клип по четырём полуплоскостям
        float[][] edges = {
                {-dx, ax - minX},   // левая грань
                { dx, maxX - ax},   // правая грань
                {-dz, az - minZ},   // нижняя грань
                { dz, maxZ - az}    // верхняя грань
        };
        for (float[] e : edges) {
            float p = e[0], q = e[1];
            if (p == 0f) {
                if (q < 0f) {
                    return false;   // параллельны и снаружи
                }
            } else {
                float r = q / p;
                if (p < 0f) {
                    if (r > t1) return false;
                    if (r > t0) t0 = r;
                } else {
                    if (r < t0) return false;
                    if (r < t1) t1 = r;
                }
            }
        }
        return t0 <= t1;
    }

    /**
     * Расталкивание кругов между собой: пары ближе 2*radius
     * отталкиваются симметрично. Модифицирует позиции на месте.
     */
    public static void separate(List<Vector3f> positions, float radius) {
        float minD = radius * 2.05f;
        float minD2 = minD * minD;
        for (int i = 0; i < positions.size(); i++) {
            Vector3f a = positions.get(i);
            for (int j = i + 1; j < positions.size(); j++) {
                Vector3f b = positions.get(j);
                float dx = a.x - b.x;
                float dz = a.z - b.z;
                float d2 = dx * dx + dz * dz;
                if (d2 > minD2 || d2 < 1e-8f) {
                    continue;
                }
                float d = FastMath.sqrt(d2);
                float push = (minD - d) * 0.5f / d;
                a.x += dx * push;
                a.z += dz * push;
                b.x -= dx * push;
                b.z -= dz * push;
            }
        }
    }

    /** Держит позицию в пределах арены. */
    public static void clampToArena(Vector3f p, float halfSize, float margin) {
        p.x = FastMath.clamp(p.x, -halfSize + margin, halfSize - margin);
        p.z = FastMath.clamp(p.z, -halfSize + margin, halfSize - margin);
    }
}
