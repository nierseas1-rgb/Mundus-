# Plugins Mundus (esprit Kodi)

Les plugins sont la brique « ouverte » de Mundus : ils ajoutent des chaînes
(Vavoo, sites de streaming, API perso…) et savent transformer une entrée en flux
lisible au moment de la lecture.

## Le contrat

Un plugin implémente [`MundusPlugin`](../app/src/main/java/com/mundus/plugin/MundusPlugin.kt) :

```kotlin
interface MundusPlugin {
    val manifest: PluginManifest
    suspend fun init(context: PluginContext) {}
    suspend fun getChannels(context: PluginContext): List<Channel>
    suspend fun resolveStream(context: PluginContext, channel: Channel): ResolvedStream =
        ResolvedStream(channel.streamUrl)
    suspend fun search(context: PluginContext, query: String): List<Channel> = /* défaut */
}
```

- **`manifest`** — identité, sections couvertes, capacités, schéma de config.
- **`getChannels`** — renvoie les chaînes ; doit être **résilient** (renvoyer ce qu'il
  peut plutôt que lever une exception, pour ne pas casser la bibliothèque fusionnée).
- **`resolveStream`** — appelé juste avant lecture quand `Channel.requiresResolution`
  est `true` (URLs signées / éphémères).
- **`PluginContext`** — fournit `http` (OkHttp partagé), la `config` de l'utilisateur
  et un `log`. Volontairement **sans type UI Android**.

## Écrire un plugin (intégré)

1. Créez une classe dans `com.mundus.plugin.builtin` qui implémente `MundusPlugin`.
2. Renseignez un `PluginManifest` (id unique, `configSchema` pour les champs attendus).
3. Enregistrez-le dans [`PluginRegistry`](../app/src/main/java/com/mundus/plugin/PluginRegistry.kt) :

```kotlin
init {
    register(VavooPlugin())
    register(MonSitePlugin()) // ← votre plugin
}
```

4. Dans l'app : onglet **Plugins → Activer**. Il devient une *source* et ses chaînes
   sont fusionnées avec le reste, avec inférence de section (TV/Film/Série/Animé).

## Exemple : le plugin Vavoo

Voir [`VavooPlugin`](../app/src/main/java/com/mundus/plugin/builtin/VavooPlugin.kt). Points clés :

- **URL de catalogue configurable** (`catalog_url`) plutôt que codée en dur — ces
  endpoints bougent et leur format varie ; on parse défensivement.
- **Résolution paresseuse** via `resolver_url` optionnel.
- **Repli hors-ligne** : sans réseau, un petit jeu de démo garde l'UI utilisable.

Pour du contenu réel, renseignez `catalog_url` (et `resolver_url` si nécessaire) au
moment d'ajouter la source.

## Feuille de route : plugins externes

L'objectif « façon Kodi » à terme :

1. **Sources déclaratives** (sûr, court terme) : un manifeste JSON distant décrivant des
   endpoints (M3U/Xtream/API) — pas de code exécuté, donc pas de risque d'exécution
   arbitraire. Idéal pour des « dépôts » communautaires.
2. **Plugins de code** (moyen terme) : chargement via `DexClassLoader` depuis un APK/DEX
   signé, avec permissions explicites et exécution isolée. Le contrat `MundusPlugin`
   restant stable, le code des plugins n'aurait pas à changer.

La sécurité (signature, permissions réseau, isolation) est un prérequis avant d'activer
l'exécution de code tiers.
