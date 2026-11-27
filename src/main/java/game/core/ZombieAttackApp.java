package game.core;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.StatsAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import game.assets.AssetLib;
import game.ui.GameOverState;
import game.ui.MenuState;

import java.util.logging.Logger;

/**
 * Точка входа jMonkeyEngine: настройка окна, ресурсов и переключение
 * между состояниями (меню → игра → game over → меню).
 */
public class ZombieAttackApp extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(ZombieAttackApp.class.getName());

    private AssetLib assetLib;
    /** Текущее игровое состояние (меню/игра/game over) — для переключения. */
    private com.jme3.app.state.AppState current;

    public ZombieAttackApp() {
        // Статистика нужна для setDisplayFps/setDisplayStatView (выключаем ниже)
        super(new StatsAppState());
    }

    @Override
    public void simpleInitApp() {
        // Отключаем встроенный Esc-выход и статистику — управляем всем сами
        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        setDisplayFps(false);
        setDisplayStatView(false);
        flyCam.setEnabled(false);
        setPauseOnLostFocus(false);

        viewPort.setBackgroundColor(new ColorRGBA(0.045f, 0.05f, 0.075f, 1f));

        // Ресурсы: процедурные звуки/шрифт/текстуры + локаторы
        assetLib = new AssetLib(assetManager);

        inputManager.setCursorVisible(true);
        LOG.info("ZOMBIE ATTACK запущена. Разрешение: "
                + settings.getWidth() + "x" + settings.getHeight());

        showMenu();
    }

    // ------------------------------------------------------------
    //  Переключение состояний
    // ------------------------------------------------------------

    /** Начать новую игру (или рестарт после смерти). */
    public void startGame() {
        attachCurrent(new GameState(this));
        LOG.info("Начата новая игра");
    }

    /** Главное меню. */
    public void showMenu() {
        attachCurrent(new MenuState(this));
    }

    /** Экран проигрыша. */
    public void showGameOver(int score, int wave) {
        attachCurrent(new GameOverState(this, score, wave));
    }

    /** Снимает предыдущее состояние (если было) и ставит новое. */
    private void attachCurrent(AppState next) {
        if (current != null) {
            stateManager.detach(current);
            current = null;
        }
        current = next;
        stateManager.attach(next);
        inputManager.setCursorVisible(true);
    }

    // ------------------------------------------------------------
    //  Доступ для состояний
    // ------------------------------------------------------------

    public AssetLib getAssets() {
        return assetLib;
    }
}
