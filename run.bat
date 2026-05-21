@echo off
REM Script para ejecutar ObraTech en Windows

echo.
echo ========================================
echo   ObraTech - Gestor de Proyectos
echo ========================================
echo.

REM Verificar si el JAR existe
if not exist "target\obratech-0.0.1-SNAPSHOT.jar" (
    echo [INFO] JAR no encontrado. Compilando proyecto...
    call mvnw.cmd clean package -DskipTests
    if errorlevel 1 (
        echo [ERROR] La compilacion fallo
        pause
        exit /b 1
    )
)

echo [INFO] Iniciando aplicacion...
echo.

java -jar "target\obratech-0.0.1-SNAPSHOT.jar"
if errorlevel 1 (
    echo [ERROR] La aplicacion fallo
    pause
    exit /b 1
)

echo [INFO] Aplicacion finalizada
pause
