@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
"javac" -encoding UTF-8 -d out src\ru\edu\pr02\TimesheetAnalyzer.java src\ru\edu\pr02\SelfCheck.java
if errorlevel 1 exit /b 1
java -cp out ru.edu.pr02.SelfCheck %*
exit /b %errorlevel%
