package game.core;

import com.jme3.system.AppSettings;
import game.assets.DataDirs;
import game.assets.IconFactory;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Запуск игры: конфигурация логов, окна и передача управления
 * в ZombieAttackApp (SimpleApplication).
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        configureLogging();

        // Любое необработанное исключение — в лог И на экран (окно с ошибкой),
        // чтобы игрок видел причину, а не молчаливое закрытие приложения
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOG.log(Level.SEVERE, "Необработанное исключение в потоке " + t.getName(), e);
            ZombieAttackApp.showErrorWindow("Ошибка приложения",
                    "Игра остановилась из-за ошибки. Подробности ниже и в лог-файле.", e);
        });

        try {
            AppSettings settings = new AppSettings(true);
            settings.setTitle("ZOMBIE ATTACK");
            settings.setResolution(1280, 720);
            settings.setVSync(true);
            // Сглаживание отключено: мультисемплинг-FBO на части Windows-драйверов
            // (старые Intel HD) приводит к падению на первом кадре.
            settings.setSamples(0);
            settings.setAudioRenderer(AppSettings.LWJGL_OPENAL);
            settings.setIcons(IconFactory.createIcons()); // процедурная иконка окна

            LOG.info("Java " + System.getProperty("java.version")
                    + " | " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                    + " | " + System.getProperty("os.arch"));

            ZombieAttackApp app = new ZombieAttackApp();
            app.setSettings(settings);
            app.setShowSettings(false);
            app.start();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Не удалось запустить игру", e);
            System.exit(1);
        }
    }

    /** Файловый лог в ~/.zombieattack/logs + консоль. */
    private static void configureLogging() {
        try {
            DataDirs.ensureAll();
            File dir = DataDirs.logsDir();
            FileHandler fh = new FileHandler(
                    new File(dir, "zombie-attack-%u.log").getAbsolutePath().replace('\\', '/'),
                    1_000_000, 3, false);
            fh.setFormatter(new SimpleFormatter());
            fh.setEncoding("UTF-8");
            Logger root = Logger.getLogger("");
            root.addHandler(fh);
        } catch (Exception e) {
            // файловый лог недоступен — играем с консольным
            System.err.println("Файловый лог недоступен: " + e.getMessage());
        }
    }
}
