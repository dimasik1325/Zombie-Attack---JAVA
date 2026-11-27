package game.core;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

/**
 * База для игровых состояний: строгий жизненный цикл.
 * init() вызывается при attach, stateUpdate() — в кадре (если включено),
 * cleanupAll() — при detach. Подклассы не работают с API state manager.
 */
public abstract class AbstractAppStateBase extends AbstractAppState {

    @Override
    public final void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        init();
    }

    @Override
    public final void update(float tpf) {
        if (isEnabled()) {
            stateUpdate(tpf);
        }
    }

    @Override
    public final void cleanup() {
        cleanupAll();
        super.cleanup();
    }

    /** Построение состояния (сцена, ввод, HUD). */
    protected abstract void init();

    /** Логика кадра. */
    protected abstract void stateUpdate(float tpf);

    /** Полная очистка (сцена, ввод, процессоры). */
    protected abstract void cleanupAll();
}
