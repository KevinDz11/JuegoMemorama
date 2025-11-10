# Memorama

**IPN  |  ESCOM**


**Unidad de Aprendizaje:** Desarrollo de Aplicaciones Móviles Nativas


**Proyecto:** Juego de Memoria (Memorama) con Temas Personalizables

## 📋 Descripción del Proyecto

Este proyecto es una implementación del clásico juego "Memorama" desarrollado nativamente para Android utilizando **Jetpack Compose**.

Aunque actualmente está enfocado en la experiencia para **un solo jugador**, el proyecto destaca por su sólida arquitectura técnica (**MVVM**, Inyección de Dependencias), manejo avanzado de estado y características adicionales como la persistencia de datos multiplataforma y un sistema de temas dinámicos (incluyendo temas institucionales del IPN y ESCOM).

## 🎯 Características Implementadas

### 🎮 Jugabilidad (Modo Un Jugador)
* **Mecánica Clásica:** Encuentra todos los pares de cartas en el menor tiempo y con el menor número de movimientos.
* **Dificultad Variable:**
    * 🟢 **Fácil:** 4x3 (6 pares).
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
* **Autoguardado:** La partida en curso se guarda automáticamente al salir (ver historial en curso).
* **Guardado Manual Multiformato:** Posibilidad de guardar partidas específicas (ganadas o en curso) en formato **JSON**.
* **Historial:** Carga de partidas anteriores (completadas o en progreso) desde el almacenamiento local.

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

### Menú Principal y Temas
| Tema IPN | Tema ESCOM |
| :---: | :---: |
| <img width="360" alt="Tema IPN" src="https://github.com/user-attachments/assets/9292babe-8896-4c32-a5f4-fd6671fb5b6b" /> | <img width="360" alt="Tema ESCOM" src="https://github.com/user-attachments/assets/3ff2e203-30a0-449c-b2fe-0dee71d64618" /> |

### Flujo de Juego (Un Jugador)
| 1. Selección de Dificultad | 2. Inicio de Partida (Fácil) |
| :---: | :---: |
| <img width="360" alt="Selección de Dificultad" src="https://github.com/user-attachments/assets/beddcdbf-6203-45c1-95b6-e165aec981f5" /> | <img width="360" alt="Inicio de Partida" src="https://github.com/user-attachments/assets/c3c2ca65-1b66-4103-a46d-fad780f045e8" /> |
| **3. Partida en Progreso** | **4. Ventana de Victoria** |
| <img width="360" alt="Partida en Progreso" src="https://github.com/user-attachments/assets/6cffe1d0-4622-42dd-a3f6-29d83ee80b32" /> | <img width="360" alt="Ventana de Victoria" src="https://github.com/user-attachments/assets/4920050f-fa81-4761-a46a-35e2af6baf3d" /> |

### Sistema de Guardado (Partida Terminada)
| 1. Diálogo de Guardado | 2. Validar (Archivo Existe) |
| :---: | :---: |
| <img width="360" alt="Diálogo de Guardado" src="https://github.com/user-attachments/assets/0fc73142-49d7-4667-a40a-456b30cc65b7" /> | <img width="360" alt="Archivo ya existe" src="https://github.com/user-attachments/assets/d2e799ae-4ff5-4ac8-bc63-5ff36241f213" /> |
| **3. Validar (Nombre Nuevo)** |
| <img width="360" alt="Nombre de archivo nuevo" src="https://github.com/user-attachments/assets/06c5ec85-33a6-4a4b-860b-21d5b32b6d52" /> |

### Sistema de Historial y Carga
| 1. Guardado en Curso (Pausa) | 2. Diálogo (Partida en Curso) |
| :---: | :---: |
| <img width="360" alt="Guardar partida sin terminar" src="https://github.com/user-attachments/assets/24214117-db61-4839-9ef1-c82f7bdee98e" /> | <img width="360" alt="Diálogo para guardar partida sin terminar" src="https://github.com/user-attachments/assets/ebc47dc3-20d3-4f0c-aea1-117eef6bf899" /> |
| **3. Historial (Partidas Ganadas)** | **4. Historial (Partidas en Curso)** |
| <img width="360" alt="Historial de partidas guardadas" src="https://github.com/user-attachments/assets/471c1c4d-d6d3-44e5-b402-9950cc878b6c" /> | <img width="360" alt="Historial de partidas en curso" src="https://github.com/user-attachments/assets/82a81293-c10d-4aa7-a945-ab6d7466bf23" /> |

## 🚀 Instrucciones de Ejecución

1.  **Clonar el repositorio:**
    ```bash
    git clone [URL_DE_TU_REPOSITORIO]
    ```
2.  **Abrir en Android Studio:** Requiere Android Studio (Iguana o superior recomendado).
3.  **Sincronizar:** Espera a que Gradle descargue todas las dependencias (incluyendo Hilt).
4.  **Ejecutar:** Selecciona un emulador (API 24+) o dispositivo físico y presiona `Run`.

## 📋 Requisitos

* **Android Min SDK:** 24 (Android 7.0)
* **Target SDK:** 35 (Android 15)
* **JDK:** 17

---
**Desarrollado por:** Díaz Fuentes Kevin y Beltrán Vidal Sol Jarelly






