@echo off
cd /d "%~dp0"
title FestVisuals Launcher - Dev Server

echo =========================================
echo       Starting FestVisuals Portal
echo =========================================

if not exist "node_modules\" (
    echo [!] Modules not found. Running npm install...
    call npm install
    if errorlevel 1 (
        echo [!] Error installing npm packages.
        pause
        exit /b 1
    )
)

echo [OK] Modules installed. Starting server...
echo [INFO] Запуск Electron приложения...
echo.

call npm run desktop

pause
