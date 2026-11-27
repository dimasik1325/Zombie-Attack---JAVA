package game.ui;

import com.jme3.audio.AudioNode;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.ui.Picture;
import game.core.AbstractAppStateBase;
import game.core.ZombieAttackApp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Экран Game Over: итоговый счёт, волна, кнопки «Рестарт» и «В меню»
 * (мышь + клавиши R/M). Тёмно-красный процедурный фон.
 */
public class GameOverState extends AbstractAppStateBase implements ActionListener {

    private static final Logger LOG = Logger.getLogger(GameOverState.class.getName());

    private final ZombieAttackApp app;
    private final int finalScore;
    private final int finalWave;

    private Texts texts;
    private final List<Button> buttons = new ArrayList<>();
    private int w, h;

    public GameOverState(ZombieAttackApp app, int score, int wave) {
        this.app = app;
        this.finalScore = score;
        this.finalWave = wave;
    }

    @Override
    protected void init() {
        texts = new Texts(app.getAssets().hasCyrillic());
        com.jme3.renderer.Camera cam = app.getCamera();
        w = cam.getWidth();
        h = cam.getHeight();
        com.jme3.scene.Node gui = app.getGuiNode();
        com.jme3.asset.AssetManager am = app.getAssets().getAssetManager();
        BitmapFont font = app.getAssets().getHudFont();

        // фон: виньетка, растянутая во весь экран
        Picture bg = new Picture("gameover_bg");
        bg.setTexture(am, app.getAssets().texture("vignette"), true);
        bg.getMaterial().setColor("Color", new ColorRGBA(1f, 0.75f, 0.7f, 1f)); // подкрасить в красный
        bg.setWidth(w);
        bg.setHeight(h);
        bg.setPosition(0, 0);
        gui.attachChild(bg);

        // заголовок
        BitmapText title = new BitmapText(font, false);
        title.setSize(64f);
        title.setText(texts.get("gameover.title"));
        title.setColor(new ColorRGBA(1f, 0.25f, 0.2f, 1f));
        title.setLocalTranslation(w / 2f - title.getLineWidth() / 2f, h / 2f + 150f, 0.05f);
        gui.attachChild(title);

        // статистика
        BitmapText stats = new BitmapText(font, false);
        stats.setSize(26f);
        stats.setText(texts.format("gameover.score", finalScore) + "\n"
                + texts.format("gameover.wave", finalWave));
        stats.setLocalTranslation(w / 2f - 120f, h / 2f + 70f, 0.05f);
        gui.attachChild(stats);

        // подсказка по клавишам
        BitmapText hint = new BitmapText(font, false);
        hint.setSize(16f);
        hint.setText(texts.get("gameover.hint"));
        hint.setColor(new ColorRGBA(0.85f, 0.85f, 0.9f, 0.85f));
        hint.setLocalTranslation(w / 2f - hint.getLineWidth() / 2f, h / 2f - 170f, 0.05f);
        gui.attachChild(hint);

        // кнопки
        int bw = 280, bh = 50;
        int bx = w / 2 - bw / 2;
        Button btnRestart = new Button(am, font, 24f, 0.06f, bx, h / 2 + 40, bw, bh,
                texts.get("gameover.restart"), app::startGame, this::playClick);
        Button btnMenu = new Button(am, font, 24f, 0.06f, bx, h / 2 + 106, bw, bh,
                texts.get("gameover.menu"), app::showMenu, this::playClick);
        buttons.add(btnRestart);
        buttons.add(btnMenu);
        for (Button b : buttons) {
            b.attachTo(gui, h);
        }

        // звук проигрыша
        try {
            AudioNode snd = new AudioNode(am, "gameover.wav");
            snd.setVolume(0.9f);
            gui.attachChild(snd);
            snd.playInstance();
        } catch (Exception e) {
            LOG.warning("Аудио недоступно: " + e.getMessage());
        }

        // ввод
        com.jme3.input.InputManager im = app.getInputManager();
        im.addMapping("go_restart", new KeyTrigger(KeyInput.KEY_R));
        im.addMapping("go_menu", new KeyTrigger(KeyInput.KEY_M));
        im.addMapping("go_click", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        im.addListener(this, "go_restart", "go_menu", "go_click");
        im.setCursorVisible(true);

        LOG.info("Game Over: очки=" + finalScore + ", волна=" + finalWave);
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
            case "go_restart" -> app.startGame();
            case "go_menu" -> app.showMenu();
            case "go_click" -> {
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
    public void stateUpdate(float tpf) {
        // экран статичен
    }

    @Override
    protected void cleanupAll() {
        com.jme3.input.InputManager im = app.getInputManager();
        im.removeListener(this);
        for (String n : new String[]{"go_restart", "go_menu", "go_click"}) {
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
