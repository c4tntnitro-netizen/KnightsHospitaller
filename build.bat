@echo off
setlocal enabledelayedexpansion
rem ---- KoH Lore build script ----
rem Compiles src\ into jars\KoHLore.jar using the JDK bundled with Starsector.

set "ROOT=%~dp0"
set "JDK=%ROOT%..\..\jdk-23+7\bin"
set "CORE=%ROOT%..\..\starsector-core"

set "CP=%CORE%\starfarer.api.jar;%CORE%\json.jar;%CORE%\lwjgl.jar;%CORE%\lwjgl_util.jar;%CORE%\log4j-1.2.9.jar"

echo Classpath: %CP%

if exist "%ROOT%out" rmdir /s /q "%ROOT%out"
mkdir "%ROOT%out"

del "%ROOT%sources.txt" 2>nul
for /r "%ROOT%src" %%f in (*.java) do (
  set "p=%%f"
  echo "!p:\=/!">>"%ROOT%sources.txt"
)

"%JDK%\javac.exe" -encoding UTF-8 --release 17 -cp "%CP%" -d "%ROOT%out" @"%ROOT%sources.txt"
if errorlevel 1 (
  echo.
  echo BUILD FAILED
  del "%ROOT%sources.txt" 2>nul
  exit /b 1
)

if not exist "%ROOT%jars" mkdir "%ROOT%jars"
"%JDK%\jar.exe" cf "%ROOT%jars\KoHLore.jar" -C "%ROOT%out" .
if errorlevel 1 (
  echo.
  echo JAR PACKAGING FAILED
  del "%ROOT%sources.txt" 2>nul
  exit /b 1
)

del "%ROOT%sources.txt" 2>nul
echo.
echo BUILD OK -^> jars\KoHLore.jar
endlocal
