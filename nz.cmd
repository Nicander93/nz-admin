@echo off
setlocal
node "%~dp0tools\nz-cli\bin\nz.mjs" %*
exit /b %errorlevel%
