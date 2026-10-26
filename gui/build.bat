@echo off
rem Build giao dien vao gui\build\GameTournamentGUI.jar roi mo chuong trinh.
rem Chay tu thu muc GameTournament de dung chung du lieu data\gtms.dat voi ban console.
cd /d "%~dp0.."

if exist gui\out rmdir /s /q gui\out
javac -encoding UTF-8 -nowarn -d gui\out -sourcepath "src;gui\src" gui\src\gtms\gui\GuiApp.java
if errorlevel 1 (
    echo Bien dich loi.
    pause
    exit /b 1
)

rem Cong cu jar nam trong thu muc bin cua JDK, thuong khong co san trong PATH
set "JAR=jar"
where jar >nul 2>nul
if not errorlevel 1 goto pack
for /f "tokens=2 delims==" %%h in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do set "JHOME=%%h"
set "JAR=%JHOME:~1%\bin\jar.exe"

:pack
if not exist gui\build mkdir gui\build
"%JAR%" --create --file gui\build\GameTournamentGUI.jar --main-class gtms.gui.GuiApp -C gui\out .
if errorlevel 1 (
    echo Khong tao duoc file jar, chay truc tiep tu gui\out.
    start "" javaw -cp gui\out gtms.gui.GuiApp
    exit /b 0
)

echo Da build gui\build\GameTournamentGUI.jar
start "" javaw -jar gui\build\GameTournamentGUI.jar
