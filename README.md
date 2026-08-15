# Deviz — convertisseur de devises

Deviz est un petit convertisseur de devises en Kotlin, decline en deux
applications :

- **`desktop/`** — une application pour ordinateur (Kotlin + Swing)
- **`android/`** — une application mobile Android (Kotlin + Jetpack Compose)

Les deux affichent les memes taux de change en direct, dans les memes
langues (francais / anglais), avec le meme comportement.

Ce document explique **comment c'est construit** et **pourquoi**, pas
seulement comment le lancer — l'objectif est que tu puisses relire le code
en t'appuyant dessus.

---

## 1. Vue d'ensemble

```
Deviz/
├── core/            module Kotlin pur : toute la logique de conversion
│   ├── src/main/kotlin/deviz/core/
│   │   ├── Devise.kt          les devises geree (EUR, USD, GBP, ...)
│   │   ├── Langue.kt          francais/anglais + tous les textes de l'appli
│   │   ├── TauxDeChange.kt    taux fixes, taux en direct, repli automatique
│   │   └── Convertisseur.kt   le calcul de conversion en lui-meme
│   └── src/test/kotlin/...    tests unitaires (8 tests)
│
├── desktop/         application Swing qui utilise `core`
│   └── src/main/kotlin/deviz/desktop/Main.kt
│
├── android/         application Android (projet Gradle independant)
│   └── app/src/main/kotlin/
│       ├── deviz/core/        COPIE des 4 fichiers de `core/` (voir §4)
│       └── deviz/mobile/MainActivity.kt
│
├── gradlew, gradlew.bat, gradle/   wrapper Gradle pour desktop/ + core/
└── README.md        ce fichier
```

Il y a donc deux "projets Gradle" independants : un a la racine
(`core` + `desktop`, buildable directement) et un dans `android/` (a ouvrir
avec Android Studio). Ils ne se "voient" pas entre eux — voir §4 pour
pourquoi.

---

## 2. Le module `core` : toute la logique, sans interface graphique

C'est la partie la plus interessante a lire. Elle ne depend ni de Swing ni
d'Android : juste du Kotlin standard, plus une petite lib JSON (`org.json`).

### `Devise.kt`

Un `enum class` avec une entree par devise geree (EUR, USD, GBP, CHF, JPY,
CAD, AUD, CNY). Chaque devise connait son libelle en francais, son libelle
en anglais, et son symbole (`$`, `£`, ...). Le nom Kotlin de la constante
(`USD`, `GBP`...) sert aussi de code pour interroger l'API de taux de
change, donc il suit volontairement le format ISO 4217.

### `Langue.kt`

Definit `enum class Langue { FRANCAIS, ANGLAIS }` et un objet `Traductions`
qui contient **tous** les textes affiches par l'application (titre, labels,
messages d'erreur...), pour chaque langue. Chaque texte est un champ nomme
d'une `data class Textes` plutot qu'une cle de traduction texte libre
(`getString("montant")`) : si un texte manque pour une langue, ca ne
compile pas. C'est une astuce simple pour eviter les oublis de traduction.

### `TauxDeChange.kt` — le coeur du sujet "taux en temps reel"

Trois classes qui implementent toutes la meme interface `SourceDeTaux`
(une seule methode : `taux(devise) -> Double`) :

- **`TauxStatiques`** — une table de taux fixes, ecrite en dur dans le
  code. Elle sert de solution de secours.
- **`TauxEnLigne`** — va chercher les vrais taux du jour sur
  [Frankfurter](https://frankfurter.dev), une API gratuite et sans cle
  basee sur les taux de reference publies chaque jour ouvre par la Banque
  Centrale Europeenne. Sa methode `rafraichir()` fait un appel reseau
  **bloquant** : elle ne doit jamais tourner sur le thread d'interface
  (voir §3 et §4 pour comment chaque appli s'en occupe).
- **`SourceAvecSecours`** — une classe "combinatoire" : elle essaie une
  source principale (`TauxEnLigne`), et si ca echoue pour n'importe quelle
  raison (pas d'internet, API en panne, taux pas encore charges...), elle
  se rabat silencieusement sur une source de secours (`TauxStatiques`).
  C'est ce que les deux applications utilisent en pratique :

  ```kotlin
  val source = SourceAvecSecours(principale = TauxEnLigne(), secours = TauxStatiques)
  val convertisseur = Convertisseur(source)
  ```

  Grace a cette petite couche, `Convertisseur` n'a jamais besoin de savoir
  si on est en ligne ou pas : il appelle `source.taux(devise)` et ca
  marche toujours, avec un resultat approximatif en dernier recours.

### `Convertisseur.kt`

La classe qui fait le calcul. L'astuce classique : on convertit toujours
**via l'euro** comme devise pivot (`montant / taux(depart) * taux(arrivee)`),
ce qui evite de stocker un taux pour chaque paire de devises possible
(8 devises = 28 paires, mais seulement 8 taux a connaitre).

### Les tests (`core/src/test/`)

`ConvertisseurTest` (conversions, montant negatif refuse),
`SourceAvecSecoursTest` (bascule bien sur le secours en cas d'echec) et
`TraductionsTest` (chaque langue a un texte pour chaque cle). Lance-les
avec :

```bash
./gradlew :core:test
```

---

## 3. L'application desktop (`desktop/`, Swing)

Une seule fenetre (`FenetreDeviz`, dans `Main.kt`) construite avec Swing —
la boite a outils graphique livree avec le JDK, sans dependance externe.
Elle est decoupee en trois zones (`BorderLayout.NORTH/CENTER/SOUTH`) :

- **En-tete** : titre + deux boutons FR/EN pour changer de langue.
- **Formulaire** (au centre, dans une "carte" blanche) : montant, devise de
  depart, bouton *inverser* (⇄), devise d'arrivee.
- **Pied de page** : bouton *Convertir*, resultat, et une ligne de statut
  qui indique si les taux sont a jour ("Taux en direct du 2026-08-13...")
  ou si l'appli est hors ligne, plus un bouton pour rafraichir manuellement.

Points a noter dans le code :

- `SwingUtilities.invokeLater { ... }` dans `main()` : Swing exige que
  toute la construction/modification de l'interface se fasse sur un thread
  dedie (l'*Event Dispatch Thread*). C'est une regle a respecter partout.
- `rafraichirTaux()` lance un `Thread { ... }` classique pour l'appel
  reseau (bloquant), puis revient sur `SwingUtilities.invokeLater { ... }`
  pour mettre a jour les labels — seul le thread Swing a le droit de
  toucher aux composants graphiques.
- `RenduDevise`, une classe interne qui personnalise l'affichage des
  `JComboBox` pour montrer le libelle dans la langue courante au lieu du
  nom Kotlin brut (`USD`).

### Lancer l'application desktop

```bash
cd Deviz
./gradlew run
```

### Lancer seulement les tests

```bash
./gradlew test
```

La toute premiere execution telecharge Gradle et les dependances
(quelques dizaines de Mo) : ca prend un peu de temps, les fois suivantes
sont rapides.

---

## 4. L'application mobile (`android/`, Jetpack Compose)

### Pourquoi le code de `core` est copie, pas partage

Kotlin permet de vraiment partager du code entre desktop et Android via
*Kotlin Multiplatform* (KMP). C'est une vraie techno, mais elle ajoute pas
mal de complexite de configuration (source sets `commonMain`/`jvmMain`/
`androidMain`, cibles multiples...) pour un projet de cette taille. Pour
rester simple et lisible, j'ai choisi de **dupliquer** les 4 fichiers du
dossier `core/` dans `android/app/src/main/kotlin/deviz/core/` : c'est
exactement le meme code, copie tel quel (`Devise.kt`, `Langue.kt`,
`TauxDeChange.kt`, `Convertisseur.kt`). Si tu modifies la logique de
conversion, pense a repercuter le changement dans les deux dossiers.

### L'interface (`MainActivity.kt`)

Contrairement a Swing (imperatif : on cree des composants puis on les
modifie a la main), Jetpack Compose est **declaratif** : on decrit a quoi
l'ecran doit ressembler *en fonction de l'etat actuel*, et Compose se
charge de redessiner ce qui a change. Par exemple :

```kotlin
var langue by remember { mutableStateOf(Langue.FRANCAIS) }
```

Des que `langue` change, tout ce qui en depend (les textes, l'affichage
des devises...) se redessine automatiquement — pas besoin d'appeler
manuellement un equivalent de `repaint()` comme cote desktop.

L'ecran reprend les memes elements que la version desktop, en Material 3 :
titre + selecteur FR/EN, une `Card` avec le formulaire (montant, devise de
depart, bouton inverser, devise d'arrivee), un bouton *Convertir*, le
resultat, et en bas soit un petit indicateur de chargement soit le statut
des taux + un bouton pour les rafraichir.

Difference cote reseau : Android interdit categoriquement les appels
reseau sur le thread principal (ca plante l'appli). On utilise donc des
**coroutines** plutot qu'un `Thread` brut :

```kotlin
portee.launch {
    withContext(Dispatchers.IO) { tauxEnLigne.rafraichir() }
    // le code ici reprend automatiquement sur le thread principal
}
```

C'est l'equivalent "moderne Android" du `Thread { ... } / invokeLater { }`
utilise cote desktop : deux ecritures differentes pour le meme probleme
(ne jamais bloquer le thread d'interface).

### Lancer l'application mobile

Ce projet est un projet Android standard. Il n'y a pas d'Android SDK ni
d'emulateur disponibles dans l'environnement ou j'ai prepare ce projet,
mais je l'ai quand meme **compile et teste reellement** en installant
temporairement les outils en ligne de commande d'Android (SDK
platform 34, build-tools) : `./gradlew assembleDebug` et
`./gradlew testDebugUnitTest` passent tous les deux. Le code est donc
verifie, pas juste relu.

Pour le lancer chez toi :

1. Installe [Android Studio](https://developer.android.com/studio) si ce
   n'est pas deja fait.
2. `Fichier > Ouvrir...` puis selectionne le dossier `Deviz/android/`.
3. Laisse Android Studio synchroniser le projet (il proposera d'installer
   automatiquement le SDK/les composants manquants si besoin).
4. Cree un emulateur (`Device Manager`) ou branche un telephone Android en
   mode debogage USB.
5. Clique sur *Run* (▶).

---

## 5. Les taux de change en direct

Source : [api.frankfurter.dev](https://api.frankfurter.dev) — gratuite,
sans cle d'API, basee sur les taux de reference quotidiens de la BCE.
Exemple de requete (celle utilisee par l'appli) :

```
GET https://api.frankfurter.dev/v1/latest?base=EUR&symbols=USD,GBP,CHF,JPY,CAD,AUD,CNY
```

Comportement :

- Au demarrage, chaque application tente de recuperer les taux du jour.
- En cas de succes, la ligne de statut affiche la date des taux.
- En cas d'echec (pas d'internet, API indisponible...), l'application
  bascule automatiquement sur les taux fixes de `TauxStatiques` (voir §2)
  et l'affiche clairement ("hors ligne : taux approximatifs").
- Un bouton "rafraichir" permet de retenter a tout moment.

---

## 6. Limites connues / pistes d'amelioration

- Seulement 8 devises (facile a etendre : une ligne dans `Devise.kt` +
  une ligne dans `TauxStatiques`, l'API en direct suit automatiquement).
- La langue choisie n'est pas sauvegardee entre deux lancements.
- Pas de gestion des tres grands nombres (formatage `%.2f` simple).
- Le code de `core` est duplique entre desktop et Android plutot que
  partage via Kotlin Multiplatform (choix assume, voir §4).
