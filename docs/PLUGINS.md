# Plugins Mundus (esprit Kodi) — data-driven, rien d'intégré

Les plugins sont la brique « ouverte » de Mundus. **Aucun n'est fourni avec l'app** :
l'utilisateur ajoute n'importe quel plugin depuis l'onglet **Plugins** en collant une URL.

## Deux façons d'ajouter un plugin

1. **Un lien `.m3u` / `.m3u8`** → transformé automatiquement en plugin M3U.
2. **Une URL vers une définition JSON** (le format ci-dessous) → plugin M3U, JSON ou Xtream.

Vavoo, par exemple, s'ajoute exactement comme ça — ce n'est qu'un plugin parmi d'autres.

## Format d'une définition (`PluginDefinition`)

```json
{
  "id": "mon-plugin",
  "name": "Mon Plugin",
  "version": "1.0.0",
  "description": "Chaînes de démonstration",
  "type": "JSON",                     // M3U | JSON | XTREAM
  "catalogUrl": "https://exemple.tld/catalog.json",
  "requiresResolution": false,
  "resolverUrl": "https://exemple.tld/resolve?url=",
  "userAgent": "MonUA/1.0",
  "referer": "https://exemple.tld",
  "json": {
    "root": "channels",               // clé contenant le tableau (optionnel)
    "name":  ["name", "title"],
    "url":   ["url", "stream"],
    "logo":  ["logo", "icon"],
    "group": ["group", "category"],
    "id":    ["id"]
  }
}
```

- **`type: M3U`** → `catalogUrl` pointe vers une playlist M3U (parsée par `M3uParser`).
- **`type: JSON`** → `catalogUrl` renvoie un tableau d'objets ; `json` mappe les champs
  (chaque entrée est une liste de clés candidates, essayées dans l'ordre).
- **`type: XTREAM`** → `catalogUrl` = hôte de base `http://serveur:port`, avec
  `xtreamUsername` / `xtreamPassword`.
- **`requiresResolution` + `resolverUrl`** → si le flux doit être « signé » juste avant
  lecture : Mundus appelle `resolverUrl + streamUrl` et attend l'URL jouable en réponse.
- **`userAgent` / `referer`** → en-têtes HTTP ajoutés aux requêtes du plugin.

La section (TV / Films / Séries / Animés) de chaque chaîne est déduite automatiquement du
groupe/nom (comme pour les playlists M3U).

## Comment ça marche en interne

- [`PluginRepository`](../app/src/main/java/com/mundus/plugin/PluginRepository.kt) télécharge
  et interprète l'URL (JSON ou repli M3U), et renvoie une `PluginDefinition`.
- La définition est **persistée** ; le plugin apparaît dans l'onglet Plugins.
- **Activer** un plugin crée une source fusionnée dans la bibliothèque.
- [`PluginEngine`](../app/src/main/java/com/mundus/plugin/PluginEngine.kt) exécute la
  définition (chargement des chaînes + résolution des flux).

## Feuille de route

1. **Dépôts de plugins** : une URL renvoyant une liste de définitions (façon dépôt Kodi).
2. **Plugins de code** (avancé) : chargement d'un module signé via `DexClassLoader`, avec
   permissions explicites et isolation — pour les cas qui dépassent le modèle déclaratif.
   La sécurité (signature, permissions, sandbox) est un prérequis avant d'exécuter du code
   tiers.
