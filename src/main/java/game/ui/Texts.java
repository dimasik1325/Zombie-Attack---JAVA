package game.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Все надписи игры. Русские строки показываются, когда доступен
 * процедурный шрифт с кириллицей; иначе — английские аналоги
 * (стандартный Default.fnt не содержит кириллических глифов).
 */
public final class Texts {

    private static final Map<String, String> RU = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        // Главное меню
        RU.put("menu.title", "ZOMBIE ATTACK");
        RU.put("menu.subtitle", "Выживи как можно дольше. Волны не заканчиваются.");
        RU.put("menu.start", "Начать игру");
        RU.put("menu.exit", "Выход");
        RU.put("menu.enterHint", "Enter — начать, Esc — выход");
        RU.put("menu.controls", "Управление:\n"
                + "WASD — движение, мышь — камера\n"
                + "Shift — бег, Space — прыжок\n"
                + "ЛКМ — стрелять, R — перезарядка\n"
                + "1/2 — пистолет/дробовик\n"
                + "E — подобрать предмет, Esc — пауза");

        // HUD
        RU.put("hud.wave", "Волна: %d");
        RU.put("hud.zombies", "Зомби: %d");
        RU.put("hud.score", "Очки: %d");
        RU.put("hud.ammo", "Патроны: %d / %d");
        RU.put("hud.waveBanner", "— ВОЛНА %d —");
        RU.put("hud.waveCleared", "Волна %d пройдена! +%d очков");
        RU.put("hud.nextWave", "Следующая волна через %d...");
        RU.put("hud.pickupHint", "[E] — подобрать %s");
        RU.put("hud.ammoGot", "Патроны пополнены: +%d");
        RU.put("hud.healthGot", "Аптечка использована: +%d HP");
        RU.put("hud.reserveFull", "Боезапас полон");
        RU.put("hud.ammoName", "патроны");
        RU.put("hud.medkitName", "аптечку");
        RU.put("hud.relaxed", "Отдых...");
        RU.put("hud.coming", "Они идут...");

        // Пауза
        RU.put("pause.title", "ПАУЗА");
        RU.put("pause.resume", "Esc — продолжить");
        RU.put("pause.menu", "Q — выйти в меню");

        // Game Over
        RU.put("gameover.title", "ВЫ ПОГИБЛИ");
        RU.put("gameover.score", "Итоговый счёт: %d");
        RU.put("gameover.wave", "Волна: %d");
        RU.put("gameover.restart", "Рестарт");
        RU.put("gameover.menu", "В меню");
        RU.put("gameover.hint", "R — рестарт, M — меню");

        EN.put("menu.title", "ZOMBIE ATTACK");
        EN.put("menu.subtitle", "Survive as long as you can. The waves never end.");
        EN.put("menu.start", "Start Game");
        EN.put("menu.exit", "Exit");
        EN.put("menu.enterHint", "Enter — start, Esc — exit");
        EN.put("menu.controls", "Controls:\n"
                + "WASD — move, mouse — camera\n"
                + "Shift — run, Space — jump\n"
                + "LMB — shoot, R — reload\n"
                + "1/2 — pistol/shotgun\n"
                + "E — pick up item, Esc — pause");

        EN.put("hud.wave", "Wave: %d");
        EN.put("hud.zombies", "Zombies: %d");
        EN.put("hud.score", "Score: %d");
        EN.put("hud.ammo", "Ammo: %d / %d");
        EN.put("hud.waveBanner", "— WAVE %d —");
        EN.put("hud.waveCleared", "Wave %d cleared! +%d score");
        EN.put("hud.nextWave", "Next wave in %d...");
        EN.put("hud.pickupHint", "[E] — pick up %s");
        EN.put("hud.ammoGot", "Ammo replenished: +%d");
        EN.put("hud.healthGot", "Medkit used: +%d HP");
        EN.put("hud.reserveFull", "Ammo reserve full");
        EN.put("hud.ammoName", "ammo");
        EN.put("hud.medkitName", "medkit");
        EN.put("hud.relaxed", "Relax...");
        EN.put("hud.coming", "They are coming...");

        EN.put("pause.title", "PAUSED");
        EN.put("pause.resume", "Esc — resume");
        EN.put("pause.menu", "Q — back to menu");

        EN.put("gameover.title", "YOU ARE DEAD");
        EN.put("gameover.score", "Final score: %d");
        EN.put("gameover.wave", "Wave: %d");
        EN.put("gameover.restart", "Restart");
        EN.put("gameover.menu", "Menu");
        EN.put("gameover.hint", "R — restart, M — menu");
    }

    private final boolean russian;

    public Texts(boolean russian) {
        this.russian = russian;
    }

    /** Строка по ключу. */
    public String get(String key) {
        Map<String, String> map = russian ? RU : EN;
        return map.getOrDefault(key, key);
    }

    /** Форматированная строка. */
    public String format(String key, Object... args) {
        return String.format(get(key), args);
    }
}
