<div align="center">

<img src="apps/VTLOffmp3g/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="">

# VTLOffmp3g

Toutes les commandes de la cheatsheet [`tools/ffmpeg.sh`](tools/ffmpeg.sh) en application,
sur téléphone Android et sur PC, sans connexion et sans envoyer vos fichiers.

par **VTLO**

</div>

## 📲 Télécharger

| | Fichier | |
|---|---|---|
| **Android** (téléphones récents) | [`VTLOffmp3g-1.4-arm64-v8a.apk`](downloads/VTLOffmp3g-1.4-arm64-v8a.apk) | 46 Mo |
| **Android** (anciens téléphones 32 bits) | [`VTLOffmp3g-1.4-armeabi-v7a.apk`](downloads/VTLOffmp3g-1.4-armeabi-v7a.apk) | 46 Mo |
| **PC** (Windows, macOS, Linux) | [`VTLOffmp3g-web-1.4.zip`](downloads/VTLOffmp3g-web-1.4.zip) | 10 Mo |

Installation et empreintes SHA-256 : [`downloads/`](downloads/).
Au premier lancement, un court guide animé en français explique comment s'en servir.

## ✨ Fonctions

- **24 recettes** couvrant toute la cheatsheet : conversion MP4 / WebM (VP8 avec presets libvpx, VP9), remux,
  compression CRF et deux passes, extraction et remplacement de l'audio, GIF optimisé, découpe et assemblage,
  redimensionnement, recadrage, filigrane, sous-titres incrustés, captures d'images, streaming RTMP (Android),
  plus une commande libre.
- Choix des fichiers, paramètres modifiables et **commande FFmpeg éditable** avant de lancer.
- Suivi en direct : jauge de progression, **télémétrie** (fps et débit), vitesse, taille, journal.
- Résultat à enregistrer, partager ou ouvrir (Android), ou à prévisualiser et télécharger (PC).
- Android : rapport de plantage au lancement suivant, travail en arrière-plan avec notification.
- PC : FFmpeg tourne dans le navigateur (WebAssembly), les fichiers ne quittent jamais l'ordinateur.

## 🗂️ Organisation

| Dossier | Contenu |
|---|---|
| [`apps/VTLOffmp3g`](apps/VTLOffmp3g) | Application Android (Java, FFmpegKit full-gpl, Android 7 et plus) |
| [`apps/VTLOffmp3g-web`](apps/VTLOffmp3g-web) | Application PC dans le navigateur (ffmpeg.wasm) ; `dist/` est prêt à l'emploi |
| [`downloads/`](downloads) | APK et zip PC prêts à installer |
| [`tools/ffmpeg.sh`](tools/ffmpeg.sh) | La cheatsheet FFmpeg dont sont tirées les recettes |
| [`.github/workflows`](.github/workflows) | Construction des APK à chaque modification, publication web sur GitHub Pages |

Les recettes sont définies une seule fois, dans
[`Recipe.java`](apps/VTLOffmp3g/app/src/main/java/com/vtlo/offmp3g/Recipe.java) ;
l'application web les exporte de là au moment de la construction.

## 🛠️ Construire

**Android** (JDK 17+, Android SDK, `patchelf`) :

```sh
cd apps/VTLOffmp3g
./gradlew assembleRelease        # APK dans app/build/outputs/apk/release/
./gradlew testDebugUnitTest      # tests (dont rendus d'écran dans app/build/screenshots)
```

**PC** (Python 3, JDK, npm) :

```sh
cd apps/VTLOffmp3g-web
python3 build.py                 # écrit dist/
python3 dist/serve.py            # ouvre l'application sur http://localhost:8765/
```

Détails : [`apps/VTLOffmp3g/README.md`](apps/VTLOffmp3g/README.md) et
[`apps/VTLOffmp3g-web/README.md`](apps/VTLOffmp3g-web/README.md).

## 📄 Licences

- FFmpeg (FFmpegKit full-gpl sur Android, ffmpeg.wasm sur PC) inclut x264 et est sous **GPL** : les applications
  distribuées le sont donc aussi.
- [`tools/ffmpeg.sh`](tools/ffmpeg.sh) provient de [awesome-cheatsheets](https://github.com/LeCoupa/awesome-cheatsheets),
  sous licence MIT ([`LICENSE`](LICENSE)).
- Police DejaVu Sans (version PC) : licence Bitstream Vera.
