# Memorama

**IPN  |  ESCOM**


**Unidad de Aprendizaje:** Desarrollo de Aplicaciones Móviles Nativas


**Proyecto:** Juego de Memoria (Memorama) con Temas Personalizables

## 📋 Descripción del Proyecto

Este proyecto es una implementación del clásico juego "Memorama" desarrollado nativamente para Android utilizando **Jetpack Compose**.

Aunque actualmente está enfocado en la experiencia para **un solo jugador**, el proyecto destaca por su sólida arquitectura técnica (**MVVM**, Inyección de Dependencias), manejo avanzado de estado y características adicionales como la persistencia de datos multiplataforma y un sistema de temas dinámicos (incluyendo temas institucionales del IPN y ESCOM).

## 🎯 Características Implementadas

### 🎮 Jugabilidad (Modo Un Jugador)
* **Mecánica Clásica:** Encuentra todos los pares de cartas en el menor tiempo y con el menor número de movimientos.
* **Dificultad Variable:**
    * 🟡 **Medio:** 6x5 (15 pares).
* **Retroalimentación Visual y Auditiva:** Animaciones fluidas al voltear cartas y efectos de sonido para aciertos, fallos y victoria*.
* **Estadísticas en Tiempo Real:** Contador de movimientos, pares encontrados, puntuación y temporizador.

### 🎨 Personalización y UI (Temas)
El proyecto implementa un sistema de temas avanzado que cumple con requisitos de personalización institucional:
* **Soporte Modo Claro/Oscuro:** Adaptación automática según la configuración del sistema.
* **Tema IPN:** Paleta de colores institucional (Guinda).
* **Tema ESCOM:** Paleta de colores representativa (Azules).
* **Selección Dinámica:** El usuario puede cambiar el tema en tiempo real desde el menú principal.

### 💾 Persistencia de Datos
Sistema robusto para no perder el progreso:
* **Autoguardado:** La partida en curso se guarda automáticamente al salir.
* **Guardado Manual Multiformato:** Posibilidad de guardar partidas específicas en formato **JSON**.
* **Historial:** Carga de partidas anteriores desde el almacenamiento local.

## 🛠️ Stack Tecnológico

El proyecto sigue las mejores prácticas de desarrollo moderno en Android:

* **Arquitectura:** MVVM (Model-View-ViewModel) siguiendo principios de Clean Architecture.
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3).
* **Inyección de Dependencias:** [Dagger Hilt](https://dagger.dev/hilt/).
* **Concurrencia:** Kotlin Coroutines y StateFlow para un manejo reactivo del estado de la UI.
* **Almacenamiento:**
    * [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore): Para guardar la preferencia de tema del usuario.
    * **Sistema de Archivos & Kotlinx Serialization:** Para el guardado manual de partidas en formato JSON.

## 📸 Capturas de Pantalla

| Menú Principal | Tema IPN (Claro) | Tema ESCOM (Oscuro) |
| :---: | :---: | :---: |
| *(Inserta captura aquí)* | *(Inserta captura aquí)* | *(Inserta captura aquí)* |

| Gameplay | Diálogo de Guardado | Victoria |
| :---: | :---: | :---: |
| *(Inserta captura aquí)* | *(Inserta captura aquí)* | *(Inserta captura aquí)* |

## 🚀 Instrucciones de Ejecución

1.  **Clonar el repositorio:**
    ```bash
    git clone [URL_DE_TU_REPOSITORIO]
    ```
2.  **Abrir en Android Studio:** Requiere Android Studio.
3.  **Sincronizar:** Espera a que Gradle descargue todas las dependencias.
4.  **Ejecutar:** Selecciona un emulador (API 24+) o dispositivo físico y presiona `Run`.

## 📋 Requisitos

* **Android Min SDK:** 24 (Android 7.0)
* **Target SDK:** 35 (Android 15)
* **JDK:** 17

---
**Desarrollado por:** Díaz Fuentes Kevin y Beltrán Vidal Sol Jarelly
