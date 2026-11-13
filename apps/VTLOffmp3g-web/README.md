# VTLOffmp3g pour PC (application web)

La version navigateur de VTLOffmp3g : les mêmes 24 recettes que l'application Android (exportées depuis
`../VTLOffmp3g/app/src/main/java/com/vtlo/offmp3g/Recipe.java`), le même design, et le même guide animé en
français au premier lancement. FFmpeg 5.1 tourne dans le navigateur grâce à
[ffmpeg.wasm](https://github.com/ffmpegwasm/ffmpeg.wasm) : les fichiers ne quittent jamais l'ordinateur.

## Utiliser

Le dossier [`dist/`](dist/) contient l'application prête à l'emploi (aussi disponible en
[zip](../../downloads/VTLOffmp3g-web-1.4.zip)).

- **Windows** : double-cliquez `dist/Lancer-VTLOffmp3g.bat` (petit serveur PowerShell intégré à Windows, rien à installer).
- **macOS / Linux** : `dist/lancer.sh` (Python 3).
- **N'importe quel serveur web statique** : servez `dist/` tel quel (`.js` en `text/javascript`, `.wasm` en
  `application/wasm`). Ouvrir `index.html` directement par double-clic ne fonctionne pas : les navigateurs bloquent
  les modules JavaScript et les Web Workers en `file://`.
- **GitHub Pages** : activez Pages (*Settings → Pages → Source : GitHub Actions*), puis lancez le workflow
  **VTLOffmp3g web (GitHub Pages)** depuis l'onglet *Actions*.

## Fonctionnement

- Choisissez une recette, ajoutez vos fichiers, réglez les paramètres ou modifiez la commande, puis **Run**.
- La jauge, la télémétrie (fps et débit) et le journal se mettent à jour pendant l'encodage.
- Le résultat s'affiche en aperçu (vidéo, audio, image) avec un bouton **Download**. Les captures d'images
  (recette *Dump frames*) sont regroupées dans un `.zip`.
- Différences avec Android : le streaming RTMP n'est pas possible depuis une page web, et les presets libvpx sont
  chargés avec `-fpre` (ffmpeg.wasm n'a pas de dossier de presets), ce qui est fait automatiquement.
- Les fichiers sont traités en mémoire : comptez ~1 à 2 Go maximum par fichier.

## Construire

```sh
python3 build.py     # nécessite python3, un JDK (javac/java) et npm
```

`build.py` exporte les recettes depuis `Recipe.java`, copie `src/`, récupère ffmpeg.wasm depuis npm
(`@ffmpeg/ffmpeg` 0.12.15, `@ffmpeg/core` 0.12.10) et les presets libvpx de l'application Android, puis écrit
`dist/`.

## Licences

- ffmpeg.wasm : `@ffmpeg/ffmpeg` sous licence MIT, `@ffmpeg/core` (FFmpeg avec x264, x265…) sous GPL-2.0-or-later.
- Police DejaVu Sans (sous-titres incrustés) : licence Bitstream Vera, voir `src/fonts/LICENSE-DejaVu.txt`.
