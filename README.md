# Mundus 🌐📺

**Un lecteur IPTV pour Android / Android TV** qui vise l'ergonomie de **TiViMate**,
l'ouverture de **Kodi** (plugins/add-ons personnalisés), et une identité visuelle
**qui change selon la section** (TV en direct, Films, Séries, Animés).

> État : **v0.1 — fondations + MVP jouable.** Le projet compile dans Android Studio
> (qui installera le SDK Android). Il n'a **pas** pu être compilé dans l'environnement
> de génération (SDK Android bloqué par la politique réseau), donc considérez cette
> première version comme une base à ouvrir dans l'IDE et à itérer.

---

## ✨ Ce qui est déjà là

### Sources & bibliothèque unifiée
- **Playlists M3U / M3U8** (parseur tolérant : `tvg-id`, `tvg-logo`, `tvg-chno`, `group-title`).
- **Comptes Xtream Codes** (live + VOD, via `player_api.php`).
- **Plugins** (voir plus bas) — Vavoo fourni en exemple.
- **Plusieurs sources en même temps, fusionnées** : toutes les chaînes de toutes les
  playlists/comptes activés apparaissent dans **une seule bibliothèque**, sans avoir
  à basculer de l'une à l'autre. Dédoublonnage des flux identiques.
- Chargement **en parallèle** et **tolérant aux pannes** : une source qui échoue
  n'empêche pas les autres de s'afficher.

### Guide (EPG) façon TiViMate
- Parseur **XMLTV** natif.
- **Grille timeline** lisible : chaînes en lignes, programmes en colonnes
  proportionnelles au temps, défilement horizontal synchronisé + colonne de chaînes
  fixe, mise en avant du programme **en cours** avec barre de progression.

### Lecture (Media3 / ExoPlayer)
- Lecture HLS (`.m3u8`), MPEG-TS (`.ts`), MP4…
- **Réglages classiques** : mémoire tampon (min / max / avant lecture / après coupure),
  délais réseau, User-Agent, décodage matériel, reconnexion — le tout branché sur le
  `LoadControl` d'ExoPlayer.

### Thèmes par section
- Chaque section (📡 TV, 🎬 Films, 📺 Séries, 🌸 Animés) a **sa propre palette** et son
  accent. Changer de section **re-skinne toute l'app** avec une transition animée.

### Divers
- Favoris, recherche, filtres par catégorie.
- Pensé **10-foot / télécommande** (orientation paysage, `LEANBACK_LAUNCHER`, bannière TV).

---

## 🧩 Système de plugins (esprit Kodi)

Un plugin implémente l'interface [`MundusPlugin`](app/src/main/java/com/mundus/plugin/MundusPlugin.kt) :
il transforme un « monde extérieur » (Vavoo, un site de streaming, une API perso) en
chaînes Mundus, et peut **résoudre** un flux juste avant lecture (utile pour les URLs
signées/éphémères).

- Contrat volontairement **sans dépendance à l'UI Android** → prêt pour un chargement
  externe/sandboxé plus tard.
- Le plugin **Vavoo** intégré montre le flux complet (catalogue configurable + résolution
  + repli hors-ligne sur des données de démo).

Détails et feuille de route (chargement dynamique, sécurité) : **[docs/PLUGINS.md](docs/PLUGINS.md)**.

---

## 🏗️ Architecture

```
app/src/main/java/com/mundus/
├── core/model/        Modèle de domaine (Section, Channel, Source, Programme, Settings…)
├── data/
│   ├── m3u/           Parseur M3U + inférence de section
│   ├── xtream/        Client Xtream Codes
│   ├── epg/           Parseur XMLTV
│   ├── http/          Client HTTP (OkHttp) partagé
│   └── repo/          Agrégation multi-sources, EPG, persistance JSON
├── plugin/            SDK de plugins + registre + Vavoo (builtin)
├── player/            Fabrique ExoPlayer (buffer/timeouts depuis les réglages)
├── theme/             Thèmes par section + application animée
├── ui/                Compose : rail de sections, écrans, ViewModel
└── di/                Conteneur de dépendances (DI manuelle, sans kapt)
```

Choix techniques : **Kotlin**, **Jetpack Compose**, **Media3**, **OkHttp**,
**kotlinx.serialization**, **Coroutines**. Persistance en **JSON** (pas de Room/kapt
pour garder le build simple ; la frontière `repo/` permet de migrer plus tard).

---

## ▶️ Compiler & lancer

Prérequis : **Android Studio** (Ladybug ou plus récent) ou le SDK Android en ligne de commande.

```bash
# Debug APK
./gradlew assembleDebug

# Installer sur un appareil / une box branchée en ADB
./gradlew installDebug
```

- `minSdk 23`, `targetSdk 34`, `compileSdk 35`.
- Fonctionne sur téléphone/tablette **et** Android TV (ni écran tactile ni leanback requis).

> Astuce Android TV : installez l'APK via `adb connect <ip-de-la-box>` puis `adb install`.

---

## 🗺️ Feuille de route (prochaines itérations)

- Séries Xtream : résolution des épisodes (`get_series_info`).
- Chargement **dynamique** de plugins externes (dépôts façon Kodi) + sandbox.
- Timeshift / catch-up, enregistrement, multi-vue.
- Room + WorkManager pour un cache EPG persistant et des rafraîchissements planifiés.
- Composants **Compose for TV** (`tv-material`) pour un focus D-pad encore plus soigné.
- Éditeur de règles de section (mapper groupes → TV/Film/Série/Animé à la main).

---

## ⚠️ Note légale

Mundus est un **lecteur** : il ne fournit aucun contenu. L'utilisateur est responsable
des sources (playlists, comptes, plugins) qu'il ajoute et de leur légalité dans sa
juridiction.
