@echo off
setlocal
chcp 65001 >nul
title ZOMBIE ATTACK — сборка .exe
cd /d "%~dp0"

echo.
echo ════════════════════════════════════════════════════════════
echo   ZOMBIE ATTACK — автоматическая сборка нативного .exe
echo   Шаг 1: Gradle clean build (компиляция + тесты)
echo   Шаг 2: jpackage — приложение ZombieAttack.exe с JRE внутри
echo   Результат: dist\ZombieAttack\ZombieAttack.exe
echo ════════════════════════════════════════════════════════════
echo.

rem ── Проверка Java ────────────────────────────────────────────
where java >nul 2>nul
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена в PATH.
    echo Установите JDK 17+ ^(Adoptium Temurin: https://adoptium.net^)
    echo и убедитесь, что java и jpackage доступны в PATH или задан JAVA_HOME.
    pause
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    if not exist "%JAVA_HOME%\bin\jpackage.exe" (
        echo [ОШИБКА] jpackage не найден. Нужен полный JDK 17+ ^(не JRE^).
        echo Проще всего установить Temurin JDK 17 с сайта https://adoptium.net
        echo и выбрать опцию "Add to PATH".
        pause
        exit /b 1
    )
)

rem ── Выбор команды Gradle ─────────────────────────────────────
set GRADLE_CMD=.\gradlew.bat
if not exist gradle\wrapper\gradle-wrapper.jar (
    echo [!] gradle-wrapper.jar не найден — используется системный Gradle.
    set GRADLE_CMD=gradle
)

rem ── Шаг 1: сборка и тесты ────────────────────────────────────
echo [1/2] Сборка: %GRADLE_CMD% clean build installDist
call %GRADLE_CMD% clean build installDist
if errorlevel 1 (
    echo.
    echo [ОШИБКА] Сборка Gradle завершилась с ошибкой — см. вывод выше.
    pause
    exit /b 1
)

rem ── Шаг 2: jpackage ─────────────────────────────────────────
echo.
echo [2/2] Упаковка: jpackage app-image -^> dist\ZombieAttack\ZombieAttack.exe
call %GRADLE_CMD% packageDist
if errorlevel 1 (
    echo.
    echo [ОШИБКА] jpackage не удался. Проверьте, что установлен JDK 17+.
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════════
echo   ГОТОВО!
echo   Исполняемый файл:  dist\ZombieAttack\ZombieAttack.exe
echo   (портативная версия: можно скопировать папку целиком
echo    куда угодно — JRE встроена, установка не требуется)
echo ════════════════════════════════════════════════════════════
echo.
pause
