@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle --no-daemon %*
  exit /b %ERRORLEVEL%
)
echo Gradle 8.x is required. On Windows, install Gradle 8.8 or run from an environment where gradle is on PATH.
exit /b 1
