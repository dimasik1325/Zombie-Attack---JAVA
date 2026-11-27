package game.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import game.core.BoxCollider;
import game.core.PhysicsLite;

import java.util.List;

/**
 * Простое, но живое поведение зомби:
 *  - прямая погоня при видимости игрока (линия видимости через AABB);
 *  - при отсутствии видимости — обход препятствий тангенциальным
 *    рулением (влево или вправо, сторона выбирается и меняется при застревании);
 *  - расталкивание с соседями, чтобы толпа не сливалась в одну точку.
 */
public class ZombieAI {

    private int avoidSide = 1;
    private float stuckTimer;
    private final Vector3f lastPos = new Vector3f();
    private boolean firstRun = true;

    /**
     * Возвращает нормализованное направление движения (XZ).
     * @param pos        позиция зомби
     * @param target     позиция игрока
     * @param colliders  коллайдеры арены
     * @param others     позиции остальных зомби
     * @param radius     радиус зомби
     * @param tpf        время кадра
     * @param speedHint  ожидаемая скорость (для детекта застревания)
     */
    public Vector3f steer(Vector3f pos, Vector3f target, List<BoxCollider> colliders,
                          List<Vector3f> others, float radius, float tpf, float speedHint) {
        Vector3f to = new Vector3f(target.x - pos.x, 0f, target.z - pos.z);
        float dist = to.length();
        Vector3f dir = dist > 1e-5f ? to.mult(1f / dist) : new Vector3f(0f, 0f, 1f);

        // прямая видимость? идём напрямую; иначе — тангенциальный обход
        boolean visible = !PhysicsLite.lineBlockedXZ(pos, target, colliders, radius + 0.15f);
        if (!visible) {
            Vector3f perp = new Vector3f(-dir.z, 0f, dir.x).multLocal(avoidSide);
            dir = dir.mult(0.75f).addLocal(perp.mult(0.9f));
            dir.normalizeLocal();
        }

        // расталкивание от соседей
        if (others != null) {
            float minD = radius * 2.1f;
            for (Vector3f o : others) {
                float dx = pos.x - o.x;
                float dz = pos.z - o.z;
                float d2 = dx * dx + dz * dz;
                if (d2 < minD * minD && d2 > 1e-6f) {
                    float d = FastMath.sqrt(d2);
                    float push = (minD - d) / minD;
                    dir.addLocal(dx / d * push * 0.8f, 0f, dz / d * push * 0.8f);
                }
            }
            dir.normalizeLocal();
        }

        // детект застревания: если почти не двигаемся — меняем сторону обхода
        if (firstRun) {
            lastPos.set(pos);
            firstRun = false;
        }
        float moved = FastMath.abs(pos.x - lastPos.x) + FastMath.abs(pos.z - lastPos.z);
        lastPos.set(pos);
        if (moved < speedHint * tpf * 0.35f) {
            stuckTimer += tpf;
        } else {
            stuckTimer = Math.max(0f, stuckTimer - tpf * 2f);
        }
        if (stuckTimer > 0.9f) {
            avoidSide = -avoidSide;
            stuckTimer = 0f;
        }
        return dir;
    }
}
