package game.core;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import game.assets.AssetLib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Закрытая арена 60x60 метров: пол, периметр, колонны, два флигеля,
 * ящики, контейнеры, бочки и лампы. Всё построено процедурно из примитивов
 * с процедурными текстурами. Держит список AABB-коллайдеров и точки спавна.
 */
public class Arena implements game.ai.WaveManager.SpawnPoints {

    /** Половина размера арены (итого 60x60 м). */
    public static final float HALF = 30f;

    private final Node node = new Node("arena");
    private final List<BoxCollider> colliders = new ArrayList<>();
    private final List<Vector3f> spawnPoints = new ArrayList<>();
    private final Vector3f playerStart = new Vector3f(0f, 0f, 10f);
    private final Random rng = new Random();

    public Arena(AssetLib assets) {
        buildFloor(assets);
        buildPerimeter(assets);
        buildRooms(assets);
        buildObstacles(assets);
        buildLamps(assets);
        buildLights();
        buildSpawnPoints();
    }

    public Node getNode() {
        return node;
    }

    public List<BoxCollider> getColliders() {
        return colliders;
    }

    public Vector3f getPlayerStart() {
        return playerStart.clone();
    }

    /**
     * Подбирает точку спавна подальше от игрока (не ближе 14 м),
     * с лёгкой случайностью.
     */
    public Vector3f pickSpawnPoint(Vector3f awayFrom) {
        List<Vector3f> candidates = new ArrayList<>();
        for (Vector3f p : spawnPoints) {
            float dx = p.x - awayFrom.x;
            float dz = p.z - awayFrom.z;
            if (dx * dx + dz * dz > 14f * 14f) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            candidates = spawnPoints;
        }
        return candidates.get(rng.nextInt(candidates.size())).clone();
    }

    // ------------------------------------------------------------
    //  Построение
    // ------------------------------------------------------------

    private void buildFloor(AssetLib assets) {
        Box box = new Box(HALF + 1f, 0.25f, HALF + 1f);
        Mesh mesh = box;
        mesh.scaleTextureCoordinates(new Vector2f(24f, 24f));
        Geometry floor = new Geometry("floor", mesh);
        floor.setMaterial(assets.lighting("floor", ColorRGBA.White));
        floor.setLocalTranslation(0f, -0.25f, 0f);
        floor.setUserData("type", "world");
        node.attachChild(floor);
    }

    private void buildPerimeter(AssetLib assets) {
        // четыре стены высотой 3.5 м
        addBox(assets, "wall", new Vector3f(0, 1.75f, HALF + 0.5f), new Vector3f(HALF + 2, 3.5f, 1f));
        addBox(assets, "wall", new Vector3f(0, 1.75f, -HALF - 0.5f), new Vector3f(HALF + 2, 3.5f, 1f));
        addBox(assets, "wall", new Vector3f(HALF + 0.5f, 1.75f, 0), new Vector3f(1f, 3.5f, HALF + 2));
        addBox(assets, "wall", new Vector3f(-HALF - 0.5f, 1.75f, 0), new Vector3f(1f, 3.5f, HALF + 2));

        // угловые башни для атмосферы
        for (float sx = -1; sx <= 1; sx += 2) {
            for (float sz = -1; sz <= 1; sz += 2) {
                addBox(assets, "metal", new Vector3f(sx * (HALF - 0.4f), 2.2f, sz * (HALF - 0.4f)),
                        new Vector3f(1.6f, 4.4f, 1.6f));
            }
        }
    }

    private void buildRooms(AssetLib assets) {
        // два флигеля с дверными проёмами — «несколько комнат»
        // западный флигель (стены с проёмами)
        addBox(assets, "wall", new Vector3f(-21f, 1.75f, -7f), new Vector3f(0.5f, 3.5f, 8f));
        addBox(assets, "wall", new Vector3f(-21f, 1.75f, 9f), new Vector3f(0.5f, 3.5f, 6f));
        addBox(assets, "wall", new Vector3f(-17.5f, 1.75f, -11f), new Vector3f(4f, 3.5f, 0.5f));
        // восточный флигель
        addBox(assets, "wall", new Vector3f(21f, 1.75f, 7f), new Vector3f(0.5f, 3.5f, 8f));
        addBox(assets, "wall", new Vector3f(21f, 1.75f, -9f), new Vector3f(0.5f, 3.5f, 6f));
        addBox(assets, "wall", new Vector3f(17.5f, 1.75f, 11f), new Vector3f(4f, 3.5f, 0.5f));
    }

    private void buildObstacles(AssetLib assets) {
        // круглые колонны
        float[][] pillars = {{-10, -10}, {10, -10}, {-10, 10}, {10, 10}, {0, -18}, {0, 18}};
        for (float[] p : pillars) {
            Cylinder cyl = new Cylinder(10, 14, 0.7f, 4f);
            Geometry g = new Geometry("pillar", cyl);
            g.setMaterial(assets.lighting("wall", ColorRGBA.White));
            g.setLocalTranslation(p[0], 2f, p[1]);
            g.setUserData("type", "world");
            node.attachChild(g);
            colliders.add(BoxCollider.fromCenter(new Vector3f(p[0], 0, p[1]), new Vector3f(1.5f, 1f, 1.5f)));
        }

        // большие контейнеры
        addBox(assets, "metal", new Vector3f(13f, 1.1f, -14f), new Vector3f(2.4f, 2.2f, 4.6f));
        addBox(assets, "metal", new Vector3f(-13f, 1.1f, 14f), new Vector3f(2.4f, 2.2f, 4.6f));

        // кластеры ящиков (несколько штук, один сверху)
        float[][] clusters = {{6, -6}, {-6, 6}, {18, 8}, {-18, -8}, {8, 16}, {-8, -16}, {24, 24}, {-24, -24}};
        for (float[] c : clusters) {
            addBox(assets, "crate", new Vector3f(c[0], 0.6f, c[1]), new Vector3f(1.2f, 1.2f, 1.2f));
            addBox(assets, "crate", new Vector3f(c[0] + 1.4f, 0.6f, c[1] + 0.3f), new Vector3f(1.2f, 1.2f, 1.2f));
            addBox(assets, "crate", new Vector3f(c[0] + 0.7f, 1.8f, c[1] + 0.15f), new Vector3f(1.1f, 1.1f, 1.1f));
        }

        // бочки
        float[][] barrels = {{12, -10}, {-12, 10}, {15, -16}, {-15, 16}, {3, -12}, {-3, 12}};
        for (float[] b : barrels) {
            Cylinder cyl = new Cylinder(8, 12, 0.36f, 1.0f);
            Geometry g = new Geometry("barrel", cyl);
            g.setMaterial(assets.lighting("metal", ColorRGBA.White));
            g.setLocalTranslation(b[0], 0.5f, b[1]);
            g.setUserData("type", "world");
            node.attachChild(g);
            colliders.add(BoxCollider.fromCenter(new Vector3f(b[0], 0, b[1]), new Vector3f(0.8f, 1f, 0.8f)));
        }
    }

    private void buildLamps(AssetLib assets) {
        // столбы с тёплыми лампами — главный источник атмосферы
        float[][] lamps = {{0, 0}, {12, -12}, {-12, 12}, {22, 16}, {-22, -16}, {20, -20}, {-20, 20}};
        for (float[] l : lamps) {
            Box post = new Box(0.12f, 1.4f, 0.12f);
            Geometry postG = new Geometry("lamp_post", post);
            postG.setMaterial(assets.lightingColor(new ColorRGBA(0.16f, 0.17f, 0.19f, 1),
                    new ColorRGBA(0.1f, 0.1f, 0.1f, 1), 8f));
            postG.setLocalTranslation(l[0], 2.2f, l[1]);
            postG.setUserData("type", "world");
            node.attachChild(postG);

            Box head = new Box(0.22f, 0.14f, 0.22f);
            Geometry headG = new Geometry("lamp_head", head);
            headG.setMaterial(assets.unshadedColor(new ColorRGBA(1f, 0.85f, 0.6f, 1)));
            headG.setLocalTranslation(l[0], 3.6f, l[1]);
            node.attachChild(headG);

            PointLight light = new PointLight();
            light.setPosition(new Vector3f(l[0], 3.4f, l[1]));
            light.setRadius(19f);
            light.setColor(new ColorRGBA(1f, 0.78f, 0.5f, 1));
            node.addLight(light);

            colliders.add(BoxCollider.fromCenter(new Vector3f(l[0], 0, l[1]), new Vector3f(0.5f, 1f, 0.5f)));
        }
    }

    private void buildLights() {
        // лунный свет + слабый ambient, туман добавляет GameState
        DirectionalLight moon = new DirectionalLight();
        moon.setDirection(new Vector3f(-0.4f, -0.85f, -0.3f).normalizeLocal());
        moon.setColor(new ColorRGBA(0.38f, 0.44f, 0.62f, 1));
        node.addLight(moon);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.32f, 0.34f, 0.42f, 1));
        node.addLight(ambient);
    }

    private void buildSpawnPoints() {
        float e = HALF - 2.5f;
        spawnPoints.add(new Vector3f(e, 0, e));
        spawnPoints.add(new Vector3f(-e, 0, e));
        spawnPoints.add(new Vector3f(e, 0, -e));
        spawnPoints.add(new Vector3f(-e, 0, -e));
        spawnPoints.add(new Vector3f(e, 0, 0));
        spawnPoints.add(new Vector3f(-e, 0, 0));
        spawnPoints.add(new Vector3f(0, 0, e));
        spawnPoints.add(new Vector3f(0, 0, -e));
        spawnPoints.add(new Vector3f(e * 0.6f, 0, e * 0.9f));
        spawnPoints.add(new Vector3f(-e * 0.6f, 0, -e * 0.9f));
        spawnPoints.add(new Vector3f(e * 0.9f, 0, -e * 0.6f));
        spawnPoints.add(new Vector3f(-e * 0.9f, 0, e * 0.6f));
    }

    /** Добавляет стену/ящик: геометрия + AABB-коллайдер. */
    private void addBox(AssetLib assets, String texture, Vector3f center, Vector3f size) {
        Box box = new Box(size.x * 0.5f, size.y * 0.5f, size.z * 0.5f);
        Geometry g = new Geometry("block", box);
        g.setMaterial(assets.lighting(texture, ColorRGBA.White));
        g.setLocalTranslation(center);
        g.setUserData("type", "world");
        // масштаб UV по размеру грани, чтобы текстура не тянулась
        Mesh mesh = g.getMesh();
        float scale = Math.max(1f, Math.max(size.x, size.z)) / 3.2f;
        mesh.scaleTextureCoordinates(new Vector2f(scale, scale));
        node.attachChild(g);

        colliders.add(BoxCollider.fromCenter(center, size));
    }

    /** Список точек спавна (только для чтения). */
    public List<Vector3f> getSpawnPoints() {
        return Collections.unmodifiableList(spawnPoints);
    }

    /** Угол для ориентации зомби/декора (не используется в физике). */
    public static float yawTo(Vector3f from, Vector3f to) {
        return FastMath.atan2(to.x - from.x, to.z - from.z);
    }
}
