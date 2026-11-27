package game.assets;

import java.io.File;

/**
 * Каталоги для ресурсов, генерируемых процедурно при первом запуске игры.
 * Всё складывается в ~/.zombieattack, чтобы работать и в dev-режиме,
 * и внутри jpackage-приложения (папка установки обычно read-only).
 */
public final class DataDirs {

    private DataDirs() {
    }

    /** Корневой каталог данных игры. */
    public static File base() {
        return new File(System.getProperty("user.home", "."), ".zombieattack");
    }

    /** Каталог процедурных WAV-звуков. */
    public static File soundsDir() {
        return new File(base(), "sounds");
    }

    /** Каталог процедурного шрифта с кириллицей. */
    public static File fontDir() {
        return new File(base(), "fonts");
    }

    /** Каталог логов java.util.logging. */
    public static File logsDir() {
        return new File(base(), "logs");
    }

    /** Создаёт все каталоги (идемпотентно). */
    public static void ensureAll() {
        for (File f : new File[]{base(), soundsDir(), fontDir(), logsDir()}) {
            //noinspection ResultOfMethodCallIgnored
            f.mkdirs();
        }
    }
}
