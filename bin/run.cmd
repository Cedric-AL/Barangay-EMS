@echo off
setlocal
cd /d "%~dp0"

if exist "lib\gson.jar" goto bundled
where mvn >nul 2>nul
if %errorlevel%==0 goto maven

echo Gson dependency not found. Install Maven, or place Gson 2.10.1 at lib\gson.jar.
exit /b 1

:bundled
if exist out rmdir /s /q out
mkdir out
for /r src %%f in (*.java) do echo "%%f">>out\sources.txt
javac -encoding UTF-8 -cp lib\gson.jar -d out @out\sources.txt
if errorlevel 1 exit /b 1
java -cp "out;lib\gson.jar" barangayems.Main
exit /b %errorlevel%

:maven
mvn -q compile exec:java
exit /b %errorlevel%
