@echo off
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0run.ps1"
if %ERRORLEVEL% neq 0 pause
