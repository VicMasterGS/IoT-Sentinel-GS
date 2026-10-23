# IoT Sentinel

Aplicación Android para la monitorización de variables ambientales y la comunicación entre dispositivos IoT.

## Descripción

IoT Sentinel es un proyecto académico desarrollado en Kotlin para Android. Permite visualizar lecturas simuladas de sensores ambientales, administrar registros locales y probar la comunicación inalámbrica entre dispositivos Android.

## Funcionalidades

- Registro e inicio de sesión de usuarios.
- Protección de credenciales y gestión de sesiones.
- Simulación de temperatura, humedad y calidad del aire.
- Comunicación entre dispositivos mediante Bluetooth Classic.
- Consulta y eliminación de registros del historial local.
- Configuración de apariencia y consulta de permisos.
- Persistencia local de información.

## Tecnologías

- Kotlin
- Android SDK
- Jetpack Compose
- Material Design
- Room
- Bluetooth Classic (RFCOMM)
- Android Keystore y almacenamiento cifrado
- Gradle

## Sensores

La aplicación contempla variables asociadas a los sensores DHT11 y MQ-135. En esta versión, las lecturas se simulan mediante software y no representan mediciones físicas reales.

## Requisitos

- Android Studio.
- JDK y Android SDK compatibles con la configuración del proyecto.
- Un dispositivo Android compatible o un emulador.
- Dos dispositivos Android compatibles para probar la comunicación Bluetooth.

## Abrir y ejecutar el proyecto

1. Descargar o clonar este repositorio.
2. Abrir la carpeta del proyecto desde Android Studio.
3. Esperar a que termine la sincronización de Gradle.
4. Revisar las instrucciones de configuración del proyecto y las variables de entorno requeridas.
5. Ejecutar la aplicación en un dispositivo o emulador compatible.

## Generar el APK

1. Abrir el proyecto en Android Studio.
2. Esperar a que Gradle sincronice correctamente.
3. Seleccionar la opción de compilación de APK disponible en el menú **Build**.
4. Comprobar el resultado de la compilación.
5. Instalar y probar el APK generado.

La compilación de distribución puede requerir configuración de firma adicional.

## Pruebas funcionales

Se han realizado pruebas de autenticación, navegación, actualización de lecturas simuladas, comunicación Bluetooth entre dispositivos, recuperación de la conexión, consulta y eliminación del historial y persistencia de preferencias.

## Limitaciones

- Las lecturas de los sensores son simuladas.
- La comunicación Bluetooth depende de la compatibilidad y configuración de los dispositivos.
- El proyecto es una demostración académica y no un sistema industrial certificado ni un sistema de seguridad crítica.

## Referencia de Google AI Studio

[Proyecto original en Google AI Studio](https://ai.studio/apps/c4f32ca5-1a7d-4848-a5cf-8ac73e69a00f)

## Autoría

Proyecto académico desarrollado como parte de una evaluación.

## Licencia

No se ha definido una licencia de distribución para este proyecto.
