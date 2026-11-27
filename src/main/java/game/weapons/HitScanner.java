package game.weapons;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import java.util.Random;

/**
 * Лучевой сканер попаданий: по пуль на выстрел — рейкаст из камеры
 * с разбросом против сцены зомби и мира. Ближайшее пересечение решает:
 * попали в зомби (по userData zombieId), в голову (isHead) или в стену.
 */
public final class HitScanner {

    private HitScanner() {
    }

    /** Результат одного выстрела, передаётся в GameState. */
    public interface Listener {
        /** Попадание в зомби. */
        void onZombieHit(int zombieId, Vector3f point, float damage, boolean headshot);

        /** Попадание в стену/пол/препятствие. */
        void onWorldHit(Vector3f point);
    }

    /**
     * Выполняет выстрел из камеры.
     * @param spread  разброс
     * @param pellets количество лучей
     * @param damage  урон за луч
     */
    public static void fire(Camera cam, Node zombiesRoot, Node worldRoot,
                            float spread, int pellets, float damage, float headshotMult,
                            Listener listener, Random rng) {
        Vector3f origin = cam.getLocation();
        Vector3f dir = cam.getDirection().clone();
        Vector3f left = cam.getLeft().clone();
        Vector3f up = cam.getUp().clone();

        for (int i = 0; i < pellets; i++) {
            Vector3f d = dir.clone()
                    .addLocal(left.mult((rng.nextFloat() * 2f - 1f) * spread))
                    .addLocal(up.mult((rng.nextFloat() * 2f - 1f) * spread))
                    .normalizeLocal();

            Ray ray = new Ray(origin, d);

            // пересечения с миром (стены/пол/ящики)
            CollisionResults worldHits = new CollisionResults();
            worldRoot.collideWith(ray, worldHits);
            float worldDist = worldHits.size() > 0
                    ? worldHits.getClosestCollision().getDistance()
                    : Float.MAX_VALUE;

            // пересечения с зомби (сортированы по расстоянию)
            CollisionResults zombieHits = new CollisionResults();
            zombiesRoot.collideWith(ray, zombieHits);

            boolean hitSomething = false;
            for (int j = 0; j < zombieHits.size(); j++) {
                com.jme3.collision.CollisionResult c = zombieHits.getCollision(j);
                if (c.getDistance() >= worldDist) {
                    break; // дальше стены — пуля в ней
                }
                Integer zombieId = c.getGeometry().getUserData("zombieId");
                if (zombieId != null) {
                    Boolean head = c.getGeometry().getUserData("isHead");
                    boolean headshot = Boolean.TRUE.equals(head);
                    listener.onZombieHit(zombieId, c.getContactPoint(),
                            damage * (headshot ? headshotMult : 1f), headshot);
                    hitSomething = true;
                    break;
                }
            }
            if (!hitSomething && worldDist < 90f) {
                listener.onWorldHit(worldHits.getClosestCollision().getContactPoint());
            }
        }
    }
}
