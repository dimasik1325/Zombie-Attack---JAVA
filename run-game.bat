@echo off
setlocal
chcp 65001 >nul
title ZOMBIE ATTACK — быстрый запуск (без сборки exe)
cd /d "%~dp0"

rem Быстрый запуск в окне разработки: gradlew run
set GRADLE_CMD=.\gradlew.bat
if not exist gradle\wrapper\gradle-wrapper.jar set GRADLE_CMD=gradle

call %GRADLE_CMD% run
pause
