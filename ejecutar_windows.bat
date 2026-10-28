@echo off
title Sistema de Gestion de Almacen (Java + SQL)
cd /d "%~dp0"
echo ======================================================================
echo Iniciando Sistema de Gestion de Almacen...
echo ======================================================================

if exist "%~dp0AlmacenApp.jar" (
    java -jar "%~dp0AlmacenApp.jar"
    if errorlevel 1 goto error_ejecucion
    goto fin
)

if exist "%~dp0bin\almacen\Principal.class" (
    java -cp "%~dp0bin;%~dp0lib\hsqldb.jar" almacen.Principal
    if errorlevel 1 goto error_ejecucion
    goto fin
)

echo.
echo [ERROR] No se encontro el archivo AlmacenApp.jar ni la carpeta bin.
echo.
echo Causa mas comun:
echo - El archivo ZIP se abrio directamente sin extraer.
echo.
echo SOLUCION:
echo 1. Haz clic derecho sobre "SistemaAlmacenJava_ProyectoCompleto.zip".
echo 2. Selecciona "Extraer todo..." o "Extraer aqui".
echo 3. Entra a la carpeta extraida y ejecuta nuevamente este archivo .bat.
echo.
pause
exit /b 1

:error_ejecucion
echo.
echo [AVISO] Ocurrio un error al ejecutar con Java.
echo Asegurate de tener instalado Java (JRE o JDK 11 o superior).
echo.
pause
exit /b 1

:fin
