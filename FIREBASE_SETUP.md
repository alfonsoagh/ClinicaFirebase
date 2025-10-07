<<<<<<< HEAD
# Configuración de Firebase (solo google-services.json)

Esta app ahora usa únicamente la inicialización automática de Firebase mediante `app/google-services.json` y el plugin `com.google.gms.google-services`.

Qué significa:
- No se usa ya `app/src/main/assets/firebase_config.json` ni inicialización manual.
- No hay clase `Application` personalizada para inicializar Firebase.
- Con colocar `google-services.json` en `app/` y compilar, Firebase queda listo.

Pasos rápidos
1) En Firebase Console, registra tu app Android con package `com.example.clinicasx` y descarga `google-services.json`.
2) Coloca el archivo en la carpeta del módulo: `app/google-services.json`.
3) Asegúrate de que el plugin esté aplicado (ya lo está en `app/build.gradle.kts`).
4) Compila el proyecto.

Reglas recomendadas
- Firestore (colección por usuario `users/{uid}/...`).
- Storage (imágenes por usuario `users/{uid}/images/...`).

Privacidad
- `.gitignore` ya ignora cualquier `google-services.json` y archivos de config locales. Si en algún momento lo subiste al repo, ejecútalo una vez:
  - `git rm --cached app/google-services.json`
  - `git commit -m "stop tracking google-services.json"`

Notas
- Si existía `assets/firebase_config.json`, ya no se utiliza. Puedes borrarlo si quieres; está ignorado por Git.
- Si cambiaste de proyecto Firebase, descarga el nuevo `google-services.json` y reemplázalo en `app/`.
=======
# Configuración de Firebase para ClinicaSX

Este proyecto ya incluye dependencias de Firebase (Auth, Firestore y Storage), un flujo de login con Email/Password y un sistema de sincronización que sube/descarga pacientes por usuario. Solo necesitas vincularlo con tu proyecto de Firebase.

Resumen técnico de la app:
- Inicialización manual de Firebase en `Application` leyendo `app/src/main/assets/firebase_config.json` (NO usa google-services.json por defecto).
- Autenticación: Email/Password (`LoginActivity`).
- Firestore: colecciones por usuario en `users/{uid}/pacientes` y subcolección `history` por paciente.
- Storage: imágenes en `users/{uid}/images/{id}/...`.
- Sync: `SyncWorker` sube locales “sucios” y baja cambios por `updatedAt` (ISO-8601 UTC) y se programa cada 6 horas.

---

## 1) Crea el proyecto y el App Android en Firebase
1. Ve a https://console.firebase.google.com y crea un proyecto (o usa uno existente).
2. En Configuración del proyecto → pestaña General → Tus apps → Agregar app → Android.
3. Nombre del paquete (obligatorio): `com.example.clinicasx` (exacto).
4. Alias de la app y SHA no son necesarios para Email/Password (puedes agregarlos si quieres).
5. Finaliza el registro de la app. Se te ofrecerá descargar `google-services.json` (no es obligatorio en este proyecto).

## 2) Obtén los valores para `firebase_config.json`
La app inicializa Firebase con estos campos (plantilla en `app/src/main/assets/firebase_config.json`):

```json
{
  "applicationId": "REEMPLAZA_APP_ID",
  "apiKey": "REEMPLAZA_API_KEY",
  "projectId": "REEMPLAZA_PROJECT_ID",
  "storageBucket": "REEMPLAZA.appspot.com",
  "gcmSenderId": "REEMPLAZA_SENDER_ID",
  "databaseUrl": ""
}
```

Dónde conseguir cada valor:
- applicationId: es el "App ID" de Firebase (campo `mobilesdk_app_id`), NO es el package name. Lo ves en Configuración del proyecto → General → Tus apps → App Android.
- apiKey: API key del proyecto (en Configuración del proyecto → General, sección "Tus apps" o dentro de `google-services.json` como `current_key`).
- projectId: ID del proyecto (p. ej. `tu-proyecto-123`).
- storageBucket: normalmente `<projectId>.appspot.com`.
- gcmSenderId (opcional): "Project number" (también llamado Sender ID / `project_number`).
- databaseUrl (opcional): para Realtime Database (esta app usa Firestore, puedes dejarlo vacío).

TIP: Si descargaste `google-services.json`, puedes abrirlo y mapear:
- `mobilesdk_app_id` → applicationId
- `project_info.project_id` → projectId
- `project_info.storage_bucket` → storageBucket
- `project_info.project_number` → gcmSenderId
- `api_key[0].current_key` → apiKey

Después, reemplaza en `app/src/main/assets/firebase_config.json` todos los `REEMPLAZA_*` con tus valores.

## 3) Habilita productos necesarios
1. Authentication → Métodos de inicio de sesión → habilita Email/Password.
2. Firestore Database → Crear base de datos → elige una ubicación. Crea en modo restringido (recomendado) y pega reglas (debajo).
3. Storage → configurar bucket (se crea por defecto) y pega reglas (debajo).

## 4) Reglas de seguridad recomendadas
Las reglas limitan el acceso a los recursos del usuario autenticado.

Firestore rules (Reglas → Firestore):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Storage rules (Reglas → Storage):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/images/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## 5) Ejecuta y valida
1. Compila e instala la app en un dispositivo/emulador Android.
2. En la pantalla de login:
   - Regístrate con Email/Password o inicia sesión con una cuenta ya creada.
3. Al abrir `MainActivity`, se dispara una sincronización y se agenda otra periódica (cada 6 h).
4. En el menú (⋮), usa “Sincronizar” para forzar una sync manual.
5. Verifica en Firestore:
   - Colección `users/{tuUid}/pacientes` con documentos creados/actualizados.
   - Subcolección `history` en cada paciente (snapshots históricos).
6. Verifica en Storage:
   - Ruta `users/{tuUid}/images/{id}/...` con imágenes subidas (si el paciente tenía imagen local).

Logs útiles (Logcat):
- `ClinicaApp`: "Firebase inicializado desde assets" si cargó la config.
- `SyncWorker`: progreso de subida/descarga y advertencias.

## 6) Alternativa (google-services.json)
Si prefieres el método estándar de Google Services en lugar de `firebase_config.json`:
1. Coloca `google-services.json` en `app/`.
2. En `settings.gradle.kts` y `build.gradle.kts` (raíz) agrega el classpath del plugin si aún no está, y en `app/build.gradle.kts` aplica `com.google.gms.google-services`.
3. Elimina la inicialización manual en `ClinicaApp` para evitar doble init.

Este proyecto ya está preparado para la vía "manual"; no necesitas esta alternativa a menos que la prefieras.

## 7) Notas y solución de problemas
- Si ves "Sin usuario autenticado; se omite sync": inicia sesión primero.
- Si ves "No se pudo inicializar Firebase desde assets": revisa que `app/src/main/assets/firebase_config.json` existe y tiene valores correctos.
- `updatedAt` usa ISO-8601 en UTC. No cambies el formato si haces integraciones externas.
- Email/Password no requiere SHA-1. Otros proveedores (Google, teléfono) sí.
- Asegúrate de tener Internet y que el dispositivo tenga hora/fecha correctas.

Con esto, la app puede subir y bajar pacientes por usuario autenticado, incluyendo imágenes en Storage.
>>>>>>> b13913cb9112d657361d7d7debe8e152d43fb196
