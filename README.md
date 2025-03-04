# Jaak Android KYC Example

## Descripción

El proyecto jaak-android-kyc-example es un ejemplo de implementación de un proceso KYC (Know Your Customer), cuyo objetivo es validar la autenticidad de documentos de identificación y verificar la identidad de una persona dentro de un flujo KYC. Esto garantiza que los documentos sean válidos, verídicos y cumplan con las normativas establecidas.

Este proyecto está desarrollado en Android, siguiendo la arquitectura MVVM (Model-View-ViewModel), e implementa dos SDKs creados por Jaak:

Document Detector SDK: Se encarga de capturar una o dos imágenes con la cámara del dispositivo, dependiendo del tipo de documento, y convertirlas en formato base64.

Face Detector SDK: Permite la grabación de un video con la cámara del dispositivo para verificar que el rostro capturado sea humano. Si el dispositivo lo permite, este proceso se realiza de manera automática.

Además, el ejemplo incluye la integración con servicios REST para:

- Validar los documentos capturados.

- Verificar la autenticidad del rostro.

- Comparar los documentos con la identidad de la persona.

Para la comunicación con los servicios REST, se utiliza Retrofit como cliente HTTP principal.

Este proyecto sirve como referencia para entender la implementación y validación de un proceso KYC utilizando Jaak como proveedor de tecnología.


## Requerimientos

- Android Studio: Versión recomendada Iguana.

- Gradle: Versión recomendada 8.4.

- Android Gradle Plugin: Versión recomendada 8.3.2.

- Compilación del SDK: SDK 35.

- Compatibilidad de Source y Target: Java 18.

- minSdkVersion: 26.

- targetSdkVersion: 33.

- Groovy DSL: La configuración de build.gradle debe estar en Groovy DSL.


## Licencia
Este proyecto se distribuye bajo la licencia JAAK.