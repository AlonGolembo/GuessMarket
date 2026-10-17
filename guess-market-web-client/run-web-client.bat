@echo off
rem Starts the Guess Market web client on http://localhost:3000 and opens it in the browser.
rem Prerequisite: Tomcat is running on localhost:8080 with guess-market-server.war deployed.
cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
    echo Node.js was not found on PATH. Please install Node.js and run this file again.
    pause
    exit /b 1
)

rem The client uses only Node's built-in modules; npm install is a harmless no-op kept for completeness.
call npm install --no-audit --no-fund --loglevel=error >nul 2>nul

title Guess Market Web Client
node server.js --open
pause
