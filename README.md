<img src="assets/icon.svg" width="64" height="64" alt="Kova">

# Kova — Entreno y Progreso para Android

Aplicación nativa de Android (**Kotlin + Jetpack Compose + Material 3 + Room SQLite**) diseñada para registrar tus entrenamientos de gimnasio con precisión, claridad y velocidad.

## Funcionalidades principales
- **Rutinas personalizadas por días** (Push, Pull, Leg o tu propia división) con objetivo diario de ejercicios.
- **Ejercicios obligatorios y opcionales**: completa todos los obligatorios y rellena el objetivo diario con tus opcionales favoritos.
- **Sesión activa guiada**: registra series, repeticiones y kilos (o peso corporal / lastre) con referencia de tu última sesión y ajuste rápido (`±2.5 kg` / `±1 rep`).
- **Temporizador de descanso tipo Dynamic Island**: píldora flotante automática tras completar cada serie con `+15s / -15s`, preajustes y aviso háptico/sonoro.
- **Gráficas interactivas de progreso**: alterna entre **1RM estimado**, **Peso máximo**, **Volumen total** y **Repeticiones**, con detección automática de **PR (récord)** y **estancamiento**.
- **Calendario semanal inteligente**: semanas de lunes a domingo en verde al cumplir el objetivo semanal y en rojo si se cierra una semana por debajo del mínimo, con rachas y detalle de cada entreno.
- **Base de datos local SQLite (Room)** + copia de seguridad completa en JSON y arquitectura lista para sincronización con **Google Drive (`appDataFolder`)**.

## Compilar el APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleRelease
```
El APK compilado se genera en `app/build/outputs/apk/release/app-release.apk` (y la versión debug en `app/build/outputs/apk/debug/app-debug.apk`).
