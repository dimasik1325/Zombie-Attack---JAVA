package game.assets;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты процедурного синтеза WAV (headless, без аудио-устройства). */
class SoundFactoryTest {

    @Test
    void allSoundsAreValidWav() {
        for (String name : SoundFactory.NAMES) {
            byte[] wav = SoundFactory.wavBytes(name);
            assertTrue(wav.length > 44, name + " должен содержать данные");
            // заголовок RIFF
            assertEquals('R', wav[0], name);
            assertEquals('I', wav[1], name);
            assertEquals('F', wav[2], name);
            assertEquals('F', wav[3], name);
            // формат WAVE
            assertEquals('W', wav[8], name);
            assertEquals('A', wav[9], name);
            assertEquals('V', wav[10], name);
            assertEquals('E', wav[11], name);
            // PCM 16 бит моно
            int audioFormat = (wav[20] & 0xFF) | ((wav[21] & 0xFF) << 8);
            assertEquals(1, audioFormat, name + ": PCM");
            int bits = (wav[34] & 0xFF) | ((wav[35] & 0xFF) << 8);
            assertEquals(16, bits, name + ": 16 бит");
        }
    }

    @Test
    void generateMissingWritesAllFiles() throws Exception {
        Path dir = Files.createTempDirectory("za-sounds");
        int created = SoundFactory.generateMissing(dir.toFile());
        assertEquals(SoundFactory.NAMES.length, created, "все звуки должны быть созданы");
        assertTrue(SoundFactory.isGenerated(dir.toFile()));
        // повторный вызов — ничего не пересоздаёт
        assertEquals(0, SoundFactory.generateMissing(dir.toFile()));
    }

    @Test
    void ambientsAreLongEnoughForSeamlessLoop() {
        byte[] wav = SoundFactory.wavBytes("ambient_game");
        int dataLen = (wav[40] & 0xFF) | ((wav[41] & 0xFF) << 8)
                | ((wav[42] & 0xFF) << 16) | ((wav[43] & 0xFF) << 24);
        int samples = dataLen / 2;
        float seconds = samples / (float) SoundFactory.RATE;
        assertTrue(seconds >= 5.9f, "эмбиент должен быть длинным лупом, а не щелчком: " + seconds);
    }

    @Test
    void unknownSoundFailsClearly() {
        assertThrows(IllegalArgumentException.class, () -> SoundFactory.wavBytes("no_such_sound"));
    }

    @Test
    void fileLayoutUnderUserHome() {
        // каталоги должны быть определены детерминированно
        File base = DataDirs.base();
        assertTrue(base.getName().equals(".zombieattack") || base.equals(new File(".")),
                "корень данных: " + base);
        assertEquals("sounds", DataDirs.soundsDir().getName());
        assertEquals("fonts", DataDirs.fontDir().getName());
    }
}
