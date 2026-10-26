# Registro de Clases para Android

Esta app es una "Trusted Web Activity": se instala como app normal, con su ícono y en pantalla completa, y por dentro abre tu Registro de Clases publicado en GitHub Pages usando el motor de Chrome.

Por eso:
- **No hay que tocar nada aquí cuando cambias la app.** Sigues subiendo `registro-clases-6.html` y `sw.js` a `registro-de-estudiantes` como siempre, y la app del teléfono se actualiza sola. El APK se instala una vez.
- **Los datos son los mismos que ya tienes en Chrome.** Funcionan igual el inicio de sesión con Google Drive, las descargas de Word y Excel, la cámara para los QR y el uso sin internet.
- **Necesita Chrome** (o otro navegador compatible) instalado en el teléfono, aunque no sea el navegador predeterminado.

## 1. Compilar el APK con GitHub (igual que Foco)

1. Crea un repositorio nuevo, por ejemplo `registro-clases-android`. Puede ser privado.
2. Sube **todo** el contenido de esta carpeta, incluida la carpeta oculta `.github`. No subas la carpeta `para-github-io`: esa va en otro repositorio (paso 2).
   - La forma más segura es con GitHub Desktop: clonas el repositorio vacío, copias los archivos dentro y haces "Commit" y "Push".
   - Si subes desde la web y no aparece `.github`, crea el archivo a mano: **Add file → Create new file**, escribe `.github/workflows/apk.yml` como nombre y pega el contenido de ese archivo.
3. En la pestaña **Actions** espera que termine "Compilar APK", unos 5 minutos.
4. En **Releases** aparece "Registro de Clases versión N". Ábrelo desde el teléfono, descarga `RegistroClases.apk` e instálalo. Android pedirá permitir "instalar apps desconocidas".

## 2. Quitar la barra de dirección (recomendado, se hace una sola vez)

Sin este paso la app funciona igual, pero muestra arriba una barra delgada con la dirección web. Para que Android confíe en la app y la quite:

1. Crea un repositorio **público** llamado exactamente `ggeorgetoledo-collab.github.io`.
2. Dentro crea dos archivos con **Add file → Create new file**:
   - Nombre `.nojekyll`, vacío. Sin este archivo, GitHub Pages no publica las carpetas que empiezan con punto.
   - Nombre `.well-known/assetlinks.json`, con el contenido del archivo del mismo nombre que está en `para-github-io`.
3. Espera uno o dos minutos y revisa que abra
   `https://ggeorgetoledo-collab.github.io/.well-known/assetlinks.json`.
4. Cierra la app en el teléfono y ábrela de nuevo: la barra ya no debería aparecer. Si sigue, desinstala la app y vuelve a instalarla.

Este repositorio no cambia tu Registro de Clases ni el portal de estudiantes; solo publica ese archivo de verificación.

## Opción B: Android Studio

Abre esta carpeta con **File → Open**, conecta el teléfono con "Depuración USB" activa y presiona ▶ Run.

## Archivos importantes

- `app/build.gradle`: aquí está la dirección de la app (`webUrl`). Si algún día cambia, se modifica solo esa línea.
- `app/registro.keystore`: firma fija de la app (contraseña `registro-clases`). No la cambies, o tendrías que desinstalar para actualizar y rehacer el `assetlinks.json`.
- `para-github-io/.well-known/assetlinks.json`: la verificación del paso 2. Su huella corresponde a `registro.keystore`.
