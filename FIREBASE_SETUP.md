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
