#!/usr/bin/env bash
# ZOMBIE ATTACK — сборка под Linux/macOS (нативное приложение через jpackage).
# На Windows используйте build-exe.bat.
set -e
cd "$(dirname "$0")"

echo "=== ZOMBIE ATTACK: сборка (Linux/macOS) ==="

GRADLE_CMD="./gradlew"
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
    echo "[!] gradle-wrapper.jar не найден — используется системный gradle"
    GRADLE_CMD="gradle"
fi

"$GRADLE_CMD" clean build installDist
"$GRADLE_CMD" packageDist

echo ""
echo "=== Готово ==="
if [ "$(uname)" = "Darwin" ]; then
    echo "Запуск: dist/ZombieAttack/ZombieAttack.app"
else
    echo "Запуск: dist/ZombieAttack/bin/ZombieAttack"
fi
