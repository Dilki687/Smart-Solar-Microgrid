@echo off
REM ============================================================
REM  dev-tunnel.bat -- Smart Solar Microgrid Android dev helper
REM
REM  Keeps the adb reverse mapping from the phone's port 5147
REM  back to the developer PC's port 5147 alive for as long as
REM  this window is open.
REM
REM  Why: adb reverse mappings live only in the adb server's
REM  memory and get wiped whenever the phone re-plugs, adb server
REM  restarts, or Android Studio bounces its own adb. This loop
REM  restores the mapping within a few seconds after any of that.
REM
REM  Usage:
REM    - Double-click this file at the start of your work session.
REM    - Leave the window open in the background.
REM    - Close the window when you're done for the day.
REM
REM  Optional -- run at every Windows login without a click:
REM    Win+R -> shell:startup -> paste a shortcut to this file.
REM ============================================================

setlocal
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "PORT=5147"

if not exist "%ADB%" (
    echo [dev-tunnel] adb not found at "%ADB%"
    echo             Update the ADB path at the top of this file.
    pause
    exit /b 1
)

title Smart Solar dev tunnel :%PORT%
echo Watching adb reverse tcp:%PORT% -^> host tcp:%PORT%
echo Leave this window open. Close it to stop.
echo.

:loop
"%ADB%" get-state 1>nul 2>nul
if errorlevel 1 (
    echo [%TIME%] no device attached -- waiting.
) else (
    "%ADB%" reverse --list 2>nul | findstr /C:"tcp:%PORT% tcp:%PORT%" 1>nul
    if errorlevel 1 (
        "%ADB%" reverse tcp:%PORT% tcp:%PORT% 1>nul 2>nul
        if errorlevel 1 (
            echo [%TIME%] adb reverse failed -- will retry.
        ) else (
            echo [%TIME%] tunnel re-established.
        )
    )
)

REM 3 second heartbeat
ping -n 4 127.0.0.1 1>nul
goto loop
