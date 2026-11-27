package game.ui;

import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector2f;
import com.jme3.ui.Picture;
import game.core.AbstractAppStateBase;
import game.core.ZombieAttackApp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Главное меню: процедурный фон, заголовок, управление, кнопки
 * «Начать игру» и «Выход» (мышь + Enter/Esc). Тихий эмбиент.
 */
public class MenuState extends AbstractAppStateBase implements ActionListener, AnalogListener {

    private static final Logger LOG = Logger.getLogger(MenuState.class.getName());

    private final ZombieAttackApp app;
    private Texts texts;
    private AudioNode ambient;
    private Picture background;
    private final List<Button> buttons = new ArrayList<>();
    private Button btnStart;
    private Button btnExit;

    private int w, h;

    public MenuState(ZombieAttackApp app) {
        this.app = app;
    }

    @Override
    protected void init() {
        texts = new Texts(app.getAssets().hasCyrillic());
        com.jme3.renderer.Camera cam = app.getCamera();
        w = cam.getWidth();
        h = cam.getHeight();
        com.jme3.scene.Node gui = app.getGuiNode();
        com.jme3.asset.AssetManager am = app.getAssets().getAssetManager();

        // фон
        background = new Picture("menu_bg");
        background.setTexture(am, app.getAssets().texture("menuBg"), false);
        background.setWidth(w);
        background.setHeight(h);
        background.setPosition(0, 0);
        gui.attachChild(background);

        // заголовок и подзаголовок
        com.jme3.font.BitmapFont font = app.getAssets().getHudFont();
        com.jme3.font.BitmapText title = new com.jme3.font.BitmapText(font, false);
        title.setSize(72f);
        title.setText(texts.get("menu.title"));
        // тень
        com.jme3.font.BitmapText shadow = new com.jme3.font.BitmapText(font, false);
        shadow.setSize(72f);
        shadow.setText(texts.get("menu.title"));
        shadow.setColor(new com.jme3.math.ColorRGBA(0, 0, 0, 0.75f));
        shadow.setLocalTranslation(w / 2f - title.getLineWidth() / 2f + 4f, h - 150f - 4f, 0.02f);
        gui.attachChild(shadow);
        title.setLocalTranslation(w / 2f - title.getLineWidth() / 2f, h - 150f, 0.05f);
        title.setColor(new com.jme3.math.ColorRGBA(0.55f, 1f, 0.45f, 1f));
        gui.attachChild(title);

        com.jme3.font.BitmapText subtitle = new com.jme3.font.BitmapText(font, false);
        subtitle.setSize(22f);
        subtitle.setText(texts.get("menu.subtitle"));
        subtitle.setLocalTranslation(w / 2f - subtitle.getLineWidth() / 2f, h - 205f, 0.05f);
        gui.attachChild(subtitle);

        // управление (слева снизу)
        com.jme3.font.BitmapText controls = new com.jme3.font.BitmapText(font, false);
        controls.setSize(16f);
        controls.setText(texts.get("menu.controls"));
        controls.setLocalTranslation(28f, 250f, 0.05f);
        controls.setColor(new com.jme3.math.ColorRGBA(0.8f, 0.85f, 0.9f, 0.85f));
        gui.attachChild(controls);

        // версия
        com.jme3.font.BitmapText version = new com.jme3.font.BitmapText(font, false);
        version.setSize(14f);
        version.setText("ZOMBIE ATTACK v1.0.0 · jMonkeyEngine 3.6.1");
        version.setLocalTranslation(w - 20f - version.getLineWidth(), 26f, 0.05f);
        version.setColor(new com.jme3.math.ColorRGBA(0.5f, 0.55f, 0.6f, 0.8f));
        gui.attachChild(version);

        // подсказка Enter/Esc
        com.jme3.font.BitmapText enterHint = new com.jme3.font.BitmapText(font, false);
        enterHint.setSize(16f);
        enterHint.setText(texts.get("menu.enterHint"));
        enterHint.setLocalTranslation(w / 2f - enterHint.getLineWidth() / 2f, h - 420f, 0.05f);
        enterHint.setColor(new com.jme3.math.ColorRGBA(0.7f, 0.75f, 0.8f, 0.8f));
        gui.attachChild(enterHint);

        // кнопки
        int bw = 300, bh = 52;
        int bx = w / 2 - bw / 2;
        btnStart = new Button(am, font, 26f, 0.06f, bx, h - 460, bw, bh,
                texts.get("menu.start"), app::startGame, this::playClick);
        btnExit = new Button(am, font, 26f, 0.06f, bx, h - 460 + 70, bw, bh,
                texts.get("menu.exit"), app::stop, this::playClick);
        buttons.add(btnStart);
        buttons.add(btnExit);
        for (Button b : buttons) {
            b.attachTo(gui, h);
        }

        // эмбиент
        ambient = new AudioNode(am, "ambient_menu.wav");
        ambient.setLooping(true);
        ambient.setVolume(0.4f);
        gui.attachChild(ambient); // не-позиционный, но узлу нужно быть в графе
        try {
            ambient.play();
        } catch (Exception e) {
            LOG.warning("Аудио недоступно: " + e.getMessage());
        }

        // ввод
        com.jme3.input.InputManager im = app.getInputManager();
        im.addMapping("menu_click", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        im.addMapping("menu_start", new KeyTrigger(KeyInput.KEY_RETURN));
        im.addMapping("menu_exit", new KeyTrigger(KeyInput.KEY_ESCAPE));
        im.addMapping("menu_move", new com.jme3.input.controls.MouseAxisTrigger(MouseInput.AXIS_X, true),
                new com.jme3.input.controls.MouseAxisTrigger(MouseInput.AXIS_X, false),
                new com.jme3.input.controls.MouseAxisTrigger(MouseInput.AXIS_Y, true),
                new com.jme3.input.controls.MouseAxisTrigger(MouseInput.AXIS_Y, false));
        // this реализует и ActionListener, и AnalogListener
        im.addListener(this, "menu_click", "menu_start", "menu_exit", "menu_move");
        im.setCursorVisible(true);

        LOG.info("Главное меню");
    }

    private void playClick() {
        try {
            AudioNode click = new AudioNode(app.getAssets().getAssetManager(), "click.wav");
            click.setVolume(0.7f);
            app.getGuiNode().attachChild(click);
            click.playInstance();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }
        switch (name) {
            case "menu_start" -> app.startGame();
            case "menu_exit" -> app.stop();
            case "menu_click" -> {
                Vector2f cursor = app.getInputManager().getCursorPosition();
                for (Button b : buttons) {
                    if (b.click(cursor, h)) {
                        break;
                    }
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // подсветка кнопок при движении мыши
        if (name.equals("menu_move")) {
            Vector2f cursor = app.getInputManager().getCursorPosition();
            for (Button b : buttons) {
                b.setHover(cursor, h);
            }
        }
    }

    @Override
    public void stateUpdate(float tpf) {
        // меню статично
    }

    @Override
    protected void cleanupAll() {
        com.jme3.input.InputManager im = app.getInputManager();
        im.removeListener(this);
        for (String n : new String[]{"menu_click", "menu_start", "menu_exit", "menu_move"}) {
            if (im.hasMapping(n)) {
                im.deleteMapping(n);
            }
        }
        app.getGuiNode().detachAllChildren();
        for (Button b : buttons) {
            b.detach();
        }
        buttons.clear();
    }
}
