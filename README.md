# IoT Sentinel

Aplicación Android nativa desarrollada en Kotlin para la supervisión de sensores IoT/OT, la visualización de mediciones, la gestión de alertas y la comunicación Bluetooth local.

## Descripción del proyecto

IoT Sentinel es un prototipo académico orientado a la supervisión de variables ambientales y a la comunicación local entre dispositivos Android. Permite consultar mediciones simuladas, visualizar información de sensores y revisar el historial de registros.

El proyecto está diseñado como una base para un futuro sistema de monitorización ambiental con sensores físicos.

## Objetivos

- Desarrollar una aplicación Android nativa para supervisar variables ambientales.
- Simular mediciones de temperatura, humedad y calidad del aire.
- Implementar comunicación Bluetooth local entre dispositivos.
- Incorporar autenticación de usuarios y protección criptográfica de credenciales.
- Permitir la consulta y eliminación de registros del historial local.

## Funcionalidades implementadas

- Registro e inicio de sesión de usuarios.
- Panel principal con información de sensores.
- Simulación de mediciones asociadas a DHT11 y MQ-135.
- Gestión de alertas en la aplicación.
- Comunicación Bluetooth mediante RFCOMM.
- Conexión, desconexión y reconexión Bluetooth.
- Historial local con eliminación individual de registros.
- Configuración de apariencia con persistencia de los ajustes.
- Navegación entre las secciones principales de la aplicación.

## Tecnologías utilizadas

- Kotlin
- Android nativo
- Android Studio
- Material Design
- Bluetooth RFCOMM
- Android Keystore
- AES-GCM
- PBKDF2-HMAC-SHA256
- JSON para la representación de registros

## Arquitectura general

El código está organizado en componentes con responsabilidades diferenciadas:

- **data/local:** componentes relacionados con el almacenamiento local.
- **data/network:** servicios de comunicación Bluetooth.
- **data/repository:** acceso y gestión de datos.
- **data/security:** operaciones criptográficas.
- **domain:** lógica de alertas y simulación de sensores.
- **ui:** pantallas de autenticación, conexión, panel principal, historial, configuración y simulación.

## Seguridad

La implementación de autenticación y almacenamiento incluye los siguientes mecanismos:

- Derivación de hashes de contraseñas mediante PBKDF2-HMAC-SHA256, con sal y una configuración de 65.536 iteraciones indicada en el código.
- Comparación de hashes mediante `MessageDigest.isEqual()`.
- Cifrado de registros antes de almacenarlos en las preferencias locales mediante AES-GCM.
- Uso de Android Keystore para la gestión de claves criptográficas.

La implementación también contempla una clave alternativa cuando no se puede utilizar Android Keystore. Este mecanismo requiere revisión adicional para garantizar que los datos cifrados de forma persistente puedan recuperarse de manera segura en esas circunstancias.

Estas características describen los mecanismos encontrados en el código; no constituyen una auditoría de seguridad independiente.

## Pruebas realizadas

Las siguientes pruebas se realizaron en dos teléfonos Android 10:

- Instalación y apertura de la aplicación.
- Registro e inicio de sesión.
- Rechazo de una contraseña incorrecta.
- Actualización de valores simulados de temperatura, humedad y calidad del aire.
- Navegación entre las secciones principales.
- Conexión Bluetooth RFCOMM entre ambos teléfonos.
- Desconexión y reconexión Bluetooth.
- Persistencia del historial local y eliminación individual de registros.
- Persistencia de la configuración de apariencia.

## Limitaciones actuales

- Las mediciones de DHT11 y MQ-135 son simuladas; no se validó la conexión con sensores físicos.
- La comunicación Bluetooth se probó entre teléfonos Android, no con un ESP32.
- La estrategia de recuperación de claves alternativas requiere revisión adicional.

## Requisitos

- Android Studio.
- Android SDK compatible con la configuración del proyecto.
- Dispositivo Android compatible para instalar y probar la aplicación.
- Permisos Bluetooth requeridos por la versión de Android utilizada.

## Compilación

1. Clonar o descargar este repositorio.
2. Abrir el proyecto en Android Studio.
3. Esperar a que finalice la sincronización de Gradle.
4. Compilar y ejecutar la aplicación en un dispositivo Android compatible.

Los pasos específicos pueden variar según la versión de Android Studio y la configuración del proyecto.

## Autoría y propósito

Proyecto académico desarrollado para explorar el desarrollo Android nativo, la supervisión IoT, la comunicación Bluetooth local y la protección de datos.

## Enlaces

- **Repositorio GitHub:** https://github.com/VicMasterGS/IoT-Sentinel-GS
- **Proyecto en Google AI Studio:** https://ai.studio/apps/c4f32ca5-1a7d-4848-a5cf-8ac73e69a00f
