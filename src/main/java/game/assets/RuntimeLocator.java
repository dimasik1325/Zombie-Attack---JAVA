package game.assets;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetLocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Локатор ресурсов, который понимает АБСОЛЮТНЫЕ пути.
 *
 * Нужен потому, что BitmapFontLoader строит ключ текстуры страницы
 * как «папка шрифта + имя файла», а папка шрифта у нас — абсолютный
 * путь в ~/.zombieattack/fonts. Стандартный FileLocator склеивает
 * корень и имя, поэтому абсолютные имена не находит.
 */
public class RuntimeLocator implements AssetLocator {

    private String root = "";

    @Override
    public void setRootPath(String rootPath) {
        this.root = rootPath == null ? "" : rootPath;
    }

    @Override
    public AssetInfo locate(final AssetManager manager, final AssetKey key) {
        String name = key.getName();
        File file = resolve(name);
        if (file != null && file.exists() && file.isFile()) {
            return new AssetInfo(manager, key) {
                @Override
                public InputStream openStream() {
                    try {
                        return new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }
            };
        }
        return null;
    }

    private File resolve(String name) {
        // 1) абсолютный путь как есть
        File f = new File(name);
        if (f.isAbsolute()) {
            return f;
        }
        // 2) относительно корня локатора
        if (!root.isEmpty()) {
            f = new File(root, name);
            if (f.exists()) {
                return f;
            }
        }
        // 3) относительно текущего каталога процесса
        f = new File(name);
        return f.exists() ? f : null;
    }
}
