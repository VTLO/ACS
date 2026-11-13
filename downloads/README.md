# Téléchargements VTLOffmp3g 1.4

Toutes les commandes de [`tools/ffmpeg.sh`](../tools/ffmpeg.sh), prêtes à l'emploi, sur téléphone et sur PC.
Au premier lancement, un court guide animé en français explique comment s'en servir.

| Fichier | Pour | Taille |
|---|---|---|
| [`VTLOffmp3g-1.4-arm64-v8a.apk`](VTLOffmp3g-1.4-arm64-v8a.apk) | Téléphones Android récents (Pixel, Samsung…, Android 7 et plus) | 46 Mo |
| [`VTLOffmp3g-1.4-armeabi-v7a.apk`](VTLOffmp3g-1.4-armeabi-v7a.apk) | Anciens téléphones Android 32 bits | 46 Mo |
| [`VTLOffmp3g-web-1.4.zip`](VTLOffmp3g-web-1.4.zip) | PC (Windows, macOS, Linux), dans le navigateur | 10 Mo |

## Android

1. Téléchargez `VTLOffmp3g-1.4-arm64-v8a.apk` sur le téléphone (bouton **Download raw file** de GitHub).
2. Ouvrez-le et autorisez l'installation d'applications depuis cette source si Android le demande.
3. Si une ancienne version est installée, la 1.4 s'installe par-dessus.

## PC

1. Téléchargez `VTLOffmp3g-web-1.4.zip` et décompressez-le.
2. Dans le dossier `VTLOffmp3g-web` :
   - **Windows** : double-cliquez `Lancer-VTLOffmp3g.bat`.
   - **macOS / Linux** : lancez `./lancer.sh` (nécessite Python 3).
3. Le navigateur s'ouvre sur `http://localhost:8765/`. Laissez la fenêtre du lanceur ouverte pendant l'utilisation.

Le traitement se fait entièrement dans le navigateur (FFmpeg compilé en WebAssembly) : aucun fichier n'est envoyé sur
Internet. Les fichiers sont chargés en mémoire, donc restez sous ~1 à 2 Go par fichier. Le streaming RTMP n'est
disponible que dans l'application Android.

## Empreintes SHA-256

```
68ffde64b6134915ce22aa8247f0cc5a90a567f1085a1f9572407af88d3fd6bf  VTLOffmp3g-1.4-arm64-v8a.apk
91ee1d0ad9ca1ef9cdb8022ac1454b7116f613396153840ce40975afcc9cbf48  VTLOffmp3g-1.4-armeabi-v7a.apk
c776e1827301421e899bc5dccda1e1511b746d20929f3fd1245f7778ae5b132d  VTLOffmp3g-web-1.4.zip
```

Les APK sont signés avec une clé de développement : ils s'installent directement mais ne sont pas destinés au
Play Store.
