package game.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

/**
 * Экранная кнопка меню: фон-квадрат + подпись, подсветка при наведении,
 * клик по прямоугольнику. Координаты задаются от ЛЕВОГО ВЕРХНЕГО угла
 * экрана (как привычно верстать), а в gui-координаты (низ-лево, Y вверх)
 * переводятся при attachTo().
 */
public class Button {

    /** Обратный вызов клика. */
    public interface Action {
        void run();
    }

    private final Node root = new Node("button");
    private final Geometry background;
    private final BitmapText label;
    private final int x, yTop, w, h;
    private final Action action;
    private final Runnable onClickSound;
    private final Material matNormal;
    private final Material matHover;

    public Button(AssetManager assetManager, BitmapFont font, float size, float z,
                  int x, int yTop, int w, int h, String text,
                  Action action, Runnable onClickSound) {
        this.x = x;
        this.yTop = yTop;
        this.w = w;
        this.h = h;
        this.action = action;
        this.onClickSound = onClickSound;

        // фон с прозрачностью
        matNormal = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matNormal.setColor("Color", new ColorRGBA(0.10f, 0.12f, 0.10f, 0.72f));
        matNormal.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        matHover = matNormal.clone();
        matHover.setColor("Color", new ColorRGBA(0.36f, 0.14f, 0.10f, 0.85f));

        background = new Geometry("button_bg", new Quad(w, h));
        background.setMaterial(matNormal);
        background.setQueueBucket(RenderQueue.Bucket.Gui);
        root.attachChild(background);

        label = new BitmapText(font, false);
        label.setSize(size);
        label.setText(text);
        label.setColor(ColorRGBA.White);
        root.attachChild(label);
    }

    /**
     * Прикрепляет кнопку к узлу с учётом высоты экрана
     * (переводит «верхние» координаты в gui-координаты снизу-вверх).
     */
    public void attachTo(Node gui, int screenH) {
        // Quad растёт вверх от точки — нижний левый угол кнопки:
        background.setLocalTranslation(x, screenH - yTop - h, 0f);
        // текст: верх строки чуть ниже верха кнопки
        label.setLocalTranslation(x + 14f, screenH - yTop - 12f, 0.01f);
        gui.attachChild(root);
    }

    public void detach() {
        root.removeFromParent();
    }

    /** Подсветка по позиции курсора. */
    public void setHover(Vector2f cursor, int screenH) {
        boolean hover = contains(cursor, screenH);
        background.setMaterial(hover ? matHover : matNormal);
        label.setColor(hover ? new ColorRGBA(1f, 0.85f, 0.7f, 1f) : ColorRGBA.White);
    }

    /** Обработка клика: если курсор внутри — выполнить действие. */
    public boolean click(Vector2f cursor, int screenH) {
        if (contains(cursor, screenH)) {
            if (onClickSound != null) {
                onClickSound.run();
            }
            action.run();
            return true;
        }
        return false;
    }

    /** Курсор внутри прямоугольника? (курсор: px слева, py СНИЗУ экрана) */
    private boolean contains(Vector2f cursor, int screenH) {
        float fromTop = screenH - cursor.y;
        return cursor.x >= x && cursor.x <= x + w
                && fromTop >= yTop && fromTop <= yTop + h;
    }
}
