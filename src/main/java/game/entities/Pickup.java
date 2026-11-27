package game.entities;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import game.assets.AssetLib;

import java.util.Random;

/**
 * Предмет-подбор: патроны (жёлтый ящик) или аптечка (белая с крестом).
 * Парит и вращается; игрок подбирает клавишей E.
 */
public class Pickup {

    public enum Kind {AMMO, HEALTH}

    private final Kind kind;
    private final Node node = new Node("pickup");
    private final Random rng = new Random();
    private float phase;
    private boolean taken;

    public Pickup(AssetLib assets, Kind kind, Vector3f position) {
        this.kind = kind;
        position.y = 0f;
        node.setLocalTranslation(position);
        phase = rng.nextFloat() * FastMath.TWO_PI;

        if (kind == Kind.AMMO) {
            Box box = new Box(0.18f, 0.18f, 0.18f);
            Geometry g = new Geometry("pickup_ammo", box);
            g.setMaterial(assets.lightingColor(new ColorRGBA(0.75f, 0.62f, 0.14f, 1),
                    new ColorRGBA(0.4f, 0.35f, 0.1f, 1), 24f));
            g.setUserData("type", "pickup");
            node.attachChild(g);
        } else {
            Box box = new Box(0.2f, 0.2f, 0.2f);
            Geometry g = new Geometry("pickup_health", box);
            g.setMaterial(assets.lighting("health", ColorRGBA.White));
            g.setUserData("type", "pickup");
            node.attachChild(g);
        }
    }

    /** Парение и вращение. */
    public void update(float tpf) {
        phase += tpf * 2.2f;
        node.setLocalTranslation(node.getLocalTranslation().x,
                0.35f + FastMath.sin(phase) * 0.08f,
                node.getLocalTranslation().z);
        node.rotate(0f, tpf * 1.6f, 0f);
    }

    /** Достаточно ли близко к игроку. */
    public boolean inRange(Player player, float range) {
        Vector3f p = node.getLocalTranslation();
        float dx = p.x - player.getX();
        float dz = p.z - player.getZ();
        return dx * dx + dz * dz <= range * range;
    }

    public Kind getKind() {
        return kind;
    }

    public Node getNode() {
        return node;
    }

    public Vector3f getPosition() {
        return node.getLocalTranslation();
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }
}
