# ZOMBIE ATTACK

Полноценный 3D-экшен от первого лица на **Java 17 + jMonkeyEngine 3.6.1**: бесконечные волны зомби на ночной арене. Вся графика, звук и шрифт генерируются **процедурно в коде** — в проекте нет ни одного внешнего бинарного ассета.

![жанр](https://img.shields.io/badge/genre-FPS%20survival-red) ![java](https://img.shields.io/badge/Java-17+-orange) ![engine](https://img.shields.io/badge/jMonkeyEngine-3.6.1-blue)

---

## 🎮 Геймплей

- **FPS от первого лица**: WASD + мышь (mouse-look), Shift — бег, Space — прыжок, ЛКМ — стрельба, R — перезарядка, E — подобрать предмет.
- **Два оружия** (клавиши 1/2): пистолет (точный, быстрый) и дробовик (6 дробин, ураган вблизи). Ограниченный боезапас, хедшоты дают ×2+ урона.
- **Зомби с ИИ**: преследование по прямой при видимости, **обход препятствий** тангенциальным рулением при потере видимости, расталкивание в толпе, процедурные анимации ходьбы/атаки/падения, подъём из-под земли.
- **Бесконечные волны** с растущей сложностью (здоровье/скорость/урон/количество), баннер волны и обратный отсчёт.
- **Арена 60×60 м**: бетон с трещинами, колонны, два флигеля-«комнаты», контейнеры, ящики, бочки, тёплые лампы + лунный свет, туман и bloom.
- **HUD**: полоска здоровья, патроны, счёт, номер волны, счётчик зомби, прицел, хит-маркер, красная виньетка урона.
- **Меню и состояния**: главное меню (мышь + Enter), пауза (Esc), Game Over с рестартом.
- **Звук** полностью синтезирован: выстрелы, перезарядка, рычание, сирена волны, эмбиент-лупы.

## 🚀 Как запустить (для новичка)

1. Установите **JDK 17+** (рекомендую [Adoptium Temurin 17](https://adoptium.net) — при установке отметьте *Set JAVA_HOME* и *Add to PATH*).
2. Скачайте/клонируйте этот репозиторий.
3. **Windows**: двойной клик по `run-game.bat` — игра скачает зависимости и запустится (первый запуск ~2-3 минуты, дальше мгновенно).
   **Linux/macOS**: `./build.sh` (или `./gradlew run`).
4. В главном меню нажмите **Начать игру**.

> Первый запуск также генерирует процедурные звуки и шрифт в `~/.zombieattack/` — это нормально, без них игра тоже стартует (просто без звука/русских надписей).

## 📦 Как собрать готовый .exe

**Одна команда:**

```bat
build-exe.bat
```

Скрипт сам: `gradlew clean build` → `jpackage` (нативный лаунчер + встроенная JRE) → результат в **`dist\ZombieAttack\ZombieAttack.exe`** (портативный, установку не требует). Требуется JDK 17+ (jpackage входит в JDK).

Опционально: `gradlew packageInstaller` — классический установщик (нужен WiX 3).

Под Linux/macOS: `./build.sh` → `dist/ZombieAttack/bin/ZombieAttack` (или `ZombieAttack.app` на macOS).

**Альтернатива без установки чего-либо:** CI этого репозитория собирает exe при каждом push — вкладка **Actions → CI Build → ZombieAttack-windows-exe** (или релиз v1.0.0).

## 🕹 Управление

| Клавиша | Действие |
|---|---|
| W A S D | движение |
| Мышь | обзор |
| Shift | бег |
| Space | прыжок |
| ЛКМ | стрельба |
| R | перезарядка |
| 1 / 2 | пистолет / дробовик |
| E | подобрать патроны/аптечку |
| Esc | пауза (Q в паузе — выйти в меню) |

## 🏗 Структура проекта

```
zombie-attack/
├── build.gradle                 # сборка + таски packageExe/packageInstaller/packageDist
├── settings.gradle
├── gradlew / gradlew.bat        # Gradle wrapper 8.5
├── gradle/wrapper/
├── build-exe.bat                # ОДНА команда для готового .exe (Windows)
├── run-game.bat                 # быстрый запуск без сборки exe
├── build.sh                     # сборка под Linux/macOS
├── .github/workflows/build.yml  # CI: тесты + сборка exe + артефакт/релиз
└── src/
    ├── main/java/
    │   ├── game/core/           # Main, ZombieAttackApp (SimpleApplication), GameState,
    │   │                        # Arena (уровень), PhysicsLite (AABB-физика), BoxCollider,
    │   │                        # AbstractAppStateBase
    │   ├── game/entities/       # Player, Zombie (модель+анимации), Pickup
    │   ├── game/ai/             # ZombieAI (погоня/обход препятствий), WaveManager
    │   ├── game/weapons/        # Weapon, Pistol, Shotgun, WeaponSystem (вид-модель,
    │   │                        # отдача, вспышка), HitScanner (рейкасты)
    │   ├── game/ui/             # HUD (здоровье/патроны/волна/счёт/прицел), MenuState,
    │   │                        # GameOverState, Button, Texts (RU/EN)
    │   └── game/assets/         # ProcTex (текстуры), SoundFactory (WAV-синтез),
    │                            # FontFactory (bitmap-шрифт с кириллицей), AssetLib,
    │                            # IconFactory, DataDirs, RuntimeLocator
    └── test/java/               # headless-тесты: физика, волны, оружие, звук, текстуры, шрифт
```

## 🧪 Тесты

```bash
./gradlew test          # или gradlew test на Windows
```

Headless-тесты покрывают: выталкивание из стен и линию видимости (`PhysicsLite`), прогрессию волн (`WaveManager`), амуницию/перезарядку (`Weapon`), корректность WAV-заголовков (`SoundFactory`), наличие глифов/альфа-каналов (`ProcTex`, `FontFactory`).

## ❓ Устранение проблем

| Симптом | Решение |
|---|---|
| `jpackage не найден` при сборке exe | Установите полный JDK 17+ (не JRE): adoptium.net |
| Gradle «Could not resolve jme3-core» | Нужен доступ в интернет для первого скачивания зависимостей (Maven Central) |
| Нет звука | Проверьте аудиоустройство; лог в `~/.zombieattack/logs/` |
| Английские надписи вместо русских | В системе нет шрифта с кириллицей — игра автоматически откатилась на Default.fnt |
| Игра вылетает на старте | Смотрите `~/.zombieattack/logs/zombie-attack-*.log` |

## 🧠 Технические решения

- **Движок**: jMonkeyEngine 3.6.1 (ядро + LWJGL 3) — стабильная JVM-сцена, встроенные GUI-шрифты, пост-фильтры (FogFilter/BloomFilter) и OpenAL-звук.
- **Физика**: своя лёгкая AABB-физика в XZ (скольжение по стенам, Лианг-Барски для линии видимости) вместо Bullet — арена плоская, а отсутствие нативных `.dll` упрощает jpackage и кроссплатформенность.
- **Ассеты**: 100% процедурные — `BufferedImage` → `Texture2D`, DSP-синтез WAV 44.1 кГц, bitmap-шрифт с кириллицей из системного SansSerif (AngelCode `.fnt`).
- **Упаковка**: jpackage `app-image` (без WiX) — папка с `ZombieAttack.exe` + встроенный runtime, готовая к копированию.
