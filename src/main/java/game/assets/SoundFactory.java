package game.assets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

/**
 * Процедурный синтез всех звуков игры (16-бит PCM WAV, 44100 Гц, моно).
 * Выстрелы, перезарядка, рычание зомби, урон, подбор предметов,
 * сирена волны, эмбиент — всё генерируется кодом, без внешних файлов.
 * WAV-файлы создаются один раз в ~/.zombieattack/sounds и подхватываются
 * asset-менеджером jME через RuntimeLocator.
 */
public final class SoundFactory {

    public static final int RATE = 44100;

    private SoundFactory() {
    }

    /** Все генерируемые звуки. */
    public static final String[] NAMES = {
            "shot_pistol", "shot_shotgun", "reload", "empty", "switch",
            "growl1", "growl2", "attack", "zombie_die",
            "hurt", "pickup", "wave_start", "wave_cleared", "gameover",
            "click", "ambient_menu", "ambient_game"
    };

    /**
     * Генерирует отсутствующие WAV-файлы в каталоге.
     * @return количество созданных файлов
     */
    public static int generateMissing(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            return 0;
        }
        int created = 0;
        for (String name : NAMES) {
            File out = new File(dir, name + ".wav");
            if (out.exists() && out.length() > 44) {
                continue;
            }
            try {
                Files.write(out.toPath(), wavBytes(name));
                created++;
            } catch (IOException e) {
                // Не критично: игра продолжит работать без этого звука
                System.err.println("Не удалось сгенерировать звук " + name + ": " + e.getMessage());
            }
        }
        return created;
    }

    /** WAV-байты звука по имени. */
    public static byte[] wavBytes(String name) {
        float[] samples = synth(name);
        return wav16(samples, RATE);
    }

    // ============================================================
    //  Синтез конкретных звуков (возвращает выборку -1..1)
    // ============================================================
    private static float[] synth(String name) {
        switch (name) {
            case "shot_pistol": return shotPistol();
            case "shot_shotgun": return shotShotgun();
            case "reload": return reload();
            case "empty": return emptyClick();
            case "switch": return switchClick();
            case "growl1": return growl(0.9f, 44100, 12345L);
            case "growl2": return growl(1.15f, 44100, 9999L);
            case "attack": return zombieAttack();
            case "zombie_die": return zombieDie();
            case "hurt": return playerHurt();
            case "pickup": return pickup();
            case "wave_start": return waveStart();
            case "wave_cleared": return waveCleared();
            case "gameover": return gameOver();
            case "click": return uiClick();
            case "ambient_menu": return ambient(0.16f, new float[]{41.0f, 61.5f, 82.0f}, 8.0f, 777L);
            case "ambient_game": return ambient(0.10f, new float[]{55.0f, 82.5f, 110.0f}, 6.0f, 555L);
            default: throw new IllegalArgumentException("Неизвестный звук: " + name);
        }
    }

    /** Выстрел пистолета: резкий шумовой хлопок + низкий удар. */
    private static float[] shotPistol() {
        int n = (int) (0.22f * RATE);
        float[] out = new float[n];
        Random rng = new Random(1);
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float env = (float) Math.exp(-t * 26);
            float noise = rng.nextFloat() * 2 - 1;
            lp = lp * 0.55f + noise * 0.45f;                       // лёгкий lowpass
            float thump = (float) Math.sin(t * 2 * Math.PI * 130) * (float) Math.exp(-t * 30);
            out[i] = (lp * 1.1f + thump * 0.7f) * env;
        }
        return normalize(out, 0.9f);
    }

    /** Выстрел дробовика: мощнее, длиннее, глуше. */
    private static float[] shotShotgun() {
        int n = (int) (0.5f * RATE);
        float[] out = new float[n];
        Random rng = new Random(2);
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float env = (float) Math.exp(-t * 12);
            float noise = rng.nextFloat() * 2 - 1;
            lp = lp * 0.30f + noise * 0.70f;                       // глухой lowpass
            float thump = (float) Math.sin(t * 2 * Math.PI * 85) * (float) Math.exp(-t * 16);
            out[i] = (lp * 1.4f + thump * 0.9f) * env;
        }
        return normalize(out, 1.0f);
    }

    /** Перезарядка: три щелчка (достал магазин, вставил, затвор). */
    private static float[] reload() {
        int n = (int) (0.7f * RATE);
        float[] out = new float[n];
        clickAt(out, 0.06f, 0.8f);
        clickAt(out, 0.32f, 0.7f);
        clickAt(out, 0.58f, 1.0f);
        return out;
    }

    private static void clickAt(float[] out, float atSec, float amp) {
        Random rng = new Random((long) (atSec * 1e6));
        int at = (int) (atSec * RATE);
        for (int i = 0; i < 1800 && at + i < out.length; i++) {
            float t = i / (float) RATE;
            float env = (float) Math.exp(-t * 120);
            out[at + i] += (rng.nextFloat() * 2 - 1) * env * amp * 0.8f;
        }
        // металлический резонанс
        for (int i = 0; i < 3600 && at + i < out.length; i++) {
            float t = i / (float) RATE;
            float env = (float) Math.exp(-t * 30);
            out[at + i] += (float) Math.sin(t * 2 * Math.PI * 1900) * env * amp * 0.25f;
        }
    }

    /** Сухой щелчок (нет патронов). */
    private static float[] emptyClick() {
        int n = (int) (0.12f * RATE);
        float[] out = new float[n];
        clickAt(out, 0.01f, 0.5f);
        return out;
    }

    /** Смена оружия. */
    private static float[] switchClick() {
        int n = (int) (0.2f * RATE);
        float[] out = new float[n];
        clickAt(out, 0.01f, 0.45f);
        clickAt(out, 0.10f, 0.55f);
        return out;
    }

    /** Рычание зомби: рычащий «голос» с вибрато и шумом. */
    private static float[] growl(float pitch, int rate, long seed) {
        int n = (int) (0.95f * rate);
        float[] out = new float[n];
        Random rng = new Random(seed);
        float phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) rate;
            // вибрато + случайные колебания частоты
            float vib = 1 + (float) Math.sin(t * 2 * Math.PI * 6.5) * 0.06f
                    + (rng.nextFloat() - 0.5f) * 0.04f;
            float freq = 82 * pitch * vib;
            phase += 2 * Math.PI * freq / rate;
            // «голосовые связки»: пила
            float saw = (float) (((phase / (2 * Math.PI)) % 1) * 2 - 1);
            // грубая фильтрация
            float body = saw * 0.6f + (float) Math.sin(phase * 2) * 0.25f + (float) Math.sin(phase * 3.02) * 0.15f;
            // дыхание/шипение
            float breath = (rng.nextFloat() * 2 - 1) * 0.18f;
            float env = (float) Math.min(1, t * 8) * (float) Math.exp(-t * 3.2);
            out[i] = (body * 0.8f + breath) * env;
        }
        return normalize(out, 0.85f);
    }

    /** Атака зомби: короткий яростный рык. */
    private static float[] zombieAttack() {
        int n = (int) (0.4f * RATE);
        float[] out = new float[n];
        Random rng = new Random(88);
        float phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float freq = 150 + t * 120;
            phase += 2 * Math.PI * freq / RATE;
            float saw = (float) (((phase / (2 * Math.PI)) % 1) * 2 - 1);
            float noise = (rng.nextFloat() * 2 - 1) * 0.35f;
            float env = (float) Math.min(1, t * 30) * (float) Math.exp(-t * 7);
            out[i] = (saw * 0.8f + noise) * env;
        }
        return normalize(out, 0.9f);
    }

    /** Смерть зомби: нисходящее рычание-стон. */
    private static float[] zombieDie() {
        int n = (int) (1.1f * RATE);
        float[] out = new float[n];
        float phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float freq = 110 * (1 - t * 0.55f);
            phase += 2 * Math.PI * freq / RATE;
            float saw = (float) (((phase / (2 * Math.PI)) % 1) * 2 - 1);
            float env = (float) Math.min(1, t * 10) * (float) Math.exp(-t * 3.4);
            out[i] = (saw * 0.7f + (float) Math.sin(phase * 2.1) * 0.3f) * env;
        }
        return normalize(out, 0.8f);
    }

    /** Урон игроку: глухой удар + короткий стон. */
    private static float[] playerHurt() {
        int n = (int) (0.45f * RATE);
        float[] out = new float[n];
        Random rng = new Random(13);
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float noise = rng.nextFloat() * 2 - 1;
            lp = lp * 0.2f + noise * 0.8f;
            float thump = (float) Math.sin(t * 2 * Math.PI * 70) * (float) Math.exp(-t * 18);
            float moan = (float) Math.sin(t * 2 * Math.PI * (180 - t * 120)) * (float) Math.exp(-t * 6) * 0.4f;
            float env = (float) Math.exp(-t * 9);
            out[i] = (lp * 0.7f + thump + moan) * env;
        }
        return normalize(out, 0.85f);
    }

    /** Подбор предмета: восходящий двухтональный бип. */
    private static float[] pickup() {
        int n = (int) (0.3f * RATE);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float f = t < 0.12f ? 620 : 930;
            float env = t < 0.12f
                    ? (float) Math.min(1, t * 40) * (float) Math.exp(-Math.max(0, t - 0.09) * 60)
                    : (float) Math.min(1, (t - 0.12) * 40) * (float) Math.exp(-Math.max(0, t - 0.24) * 60);
            out[i] = (float) Math.sin(t * 2 * Math.PI * f) * env * 0.5f;
        }
        return out;
    }

    /** Начало волны: тревожный низкий гудок-сирена. */
    private static float[] waveStart() {
        int n = (int) (1.6f * RATE);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            // частота плавно растёт и слегка вибрирует
            float f = 160 + t * 90 + (float) Math.sin(t * 2 * Math.PI * 3) * 8;
            float env = (float) Math.min(1, t * 4) * (float) Math.exp(-Math.max(0, t - 1.1) * 6);
            float tone = (float) Math.sin(t * 2 * Math.PI * f) * 0.6f
                    + (float) Math.sin(t * 2 * Math.PI * f * 1.5f) * 0.25f;
            out[i] = tone * env;
        }
        return normalize(out, 0.8f);
    }

    /** Волна пройдена: приятный аккорд из трёх нот. */
    private static float[] waveCleared() {
        int n = (int) (1.2f * RATE);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float env = (float) Math.min(1, t * 8) * (float) Math.exp(-t * 3);
            float tone = (float) Math.sin(t * 2 * Math.PI * 440) * 0.5f
                    + (float) Math.sin(t * 2 * Math.PI * 554) * 0.35f
                    + (float) Math.sin(t * 2 * Math.PI * 659) * 0.3f;
            out[i] = tone * env;
        }
        return normalize(out, 0.6f);
    }

    /** Game Over: нисходящая мрачная последовательность. */
    private static float[] gameOver() {
        int n = (int) (2.2f * RATE);
        float[] out = new float[n];
        float[] notes = {311, 261, 207, 155};
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            int note = (int) Math.min(3, t / 0.55f);
            float lt = t - note * 0.55f;
            float env = (float) Math.min(1, lt * 20) * (float) Math.exp(-lt * 5);
            float f = notes[note];
            out[i] = ((float) Math.sin(t * 2 * Math.PI * f) * 0.5f
                    + (float) Math.sin(t * 2 * Math.PI * f * 0.5f) * 0.4f) * env;
        }
        return normalize(out, 0.7f);
    }

    /** Щелчок UI. */
    private static float[] uiClick() {
        int n = (int) (0.1f * RATE);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float env = (float) Math.exp(-t * 60);
            out[i] = (float) Math.sin(t * 2 * Math.PI * 1200) * env * 0.4f;
        }
        return out;
    }

    /**
     * Эмбиент: медленный гул из нескольких низких тонов.
     * Длительность кратна циклам частот — луп получается бесшовным.
     */
    private static float[] ambient(float amp, float[] freqs, float dur, long seed) {
        int n = (int) (dur * RATE);
        float[] out = new float[n];
        Random rng = new Random(seed);
        float[] detune = new float[freqs.length];
        for (int i = 0; i < freqs.length; i++) {
            // расстройка кратна 1/длительности → бесшовный луп
            detune[i] = Math.round(rng.nextFloat() * 4 - 2) / dur;
        }
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            float v = 0;
            for (int k = 0; k < freqs.length; k++) {
                float f = freqs[k] + detune[k];
                float w = (float) Math.sin(t * 2 * Math.PI * f) / (k + 1.5f);
                v += w;
            }
            // медленная волна громкости
            float lfo = 0.75f + 0.25f * (float) Math.sin(t * 2 * Math.PI / dur * 2);
            out[i] = v * lfo * amp;
        }
        return normalize(out, 1.0f);
    }

    // ============================================================
    //  Формирование WAV (16 бит, моно, little-endian)
    // ============================================================
    private static float[] normalize(float[] in, float target) {
        float max = 1e-6f;
        for (float v : in) max = Math.max(max, Math.abs(v));
        float k = target / max;
        for (int i = 0; i < in.length; i++) {
            in[i] = Math.max(-1f, Math.min(1f, in[i] * k));
        }
        return in;
    }

    /** Канонический 44-байтный WAV-заголовок + данные PCM. */
    public static byte[] wav16(float[] samples, int rate) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(44 + samples.length * 2);
        int dataLen = samples.length * 2;
        leInt(baos, 0x52494646);            // "RIFF"
        leInt(baos, 36 + dataLen);          // размер файла - 8
        leInt(baos, 0x57415645);            // "WAVE"
        leInt(baos, 0x666D7420);            // "fmt "
        leInt(baos, 16);                    // размер fmt
        leShort(baos, 1);                   // PCM
        leShort(baos, 1);                   // каналы: моно
        leInt(baos, rate);                  // частота
        leInt(baos, rate * 2);              // байт/сек
        leShort(baos, 2);                   // блок
        leShort(baos, 16);                  // бит
        leInt(baos, 0x64617461);            // "data"
        leInt(baos, dataLen);
        for (float s : samples) {
            int v = (int) (s * 32767);
            leShort(baos, v);
        }
        return baos.toByteArray();
    }

    private static void leShort(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void leInt(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }

    /** Проверка существования сгенерированного набора (для SelfCheck/тестов). */
    public static boolean isGenerated(File dir) {
        for (String name : NAMES) {
            File f = new File(dir, name + ".wav");
            if (!f.exists() || f.length() <= 44) {
                return false;
            }
        }
        return true;
    }

    /** Пример сохранения в файл (используется генератором). */
    @SuppressWarnings("unused")
    private static void save(File out, float[] samples) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(wav16(samples, RATE));
        }
    }
}
