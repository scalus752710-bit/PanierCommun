# Panier Commun — application Android

Ce dossier est un projet Android complet : l'application affiche votre page Panier Commun en plein écran.
Les données restent enregistrées sur le téléphone, comme dans le navigateur.

## Option 1 — Sans rien installer (GitHub, gratuit)

1. Créez un compte sur github.com, puis un nouveau dépôt (bouton « New »).
2. Sur la page du dépôt : « Add file » → « Upload files », glissez **tout le contenu** de ce dossier
   (y compris le dossier `.github`), puis « Commit changes ».
3. Ouvrez l'onglet **Actions** : la compilation démarre seule (3 à 5 minutes).
4. Cliquez sur la compilation terminée (coche verte), puis sur **PanierCommun-apk** en bas de page.
5. Décompressez le zip téléchargé : vous obtenez `app-debug.apk`.

Astuce : si le dossier `.github` ne passe pas au glisser-déposer, créez le fichier à la main avec
« Add file » → « Create new file », nommé `.github/workflows/build-apk.yml`, et collez son contenu.

## Option 2 — Avec Android Studio

1. Installez Android Studio (developer.android.com/studio).
2. « Open » → choisissez ce dossier. Laissez la synchronisation se terminer.
3. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.

## Installer l'APK sur le téléphone

Copiez `app-debug.apk` sur le téléphone, ouvrez-le, et autorisez « Installer des applications
inconnues » quand Android le demande.

## Scanner avec la caméra (version 1.1)

En Caisse, choisissez un foyer puis touchez « Scanner avec la caméra ».
La première fois, Android demande l'accès à la caméra : touchez **Autoriser**.
Si vous avez refusé : Paramètres du téléphone → Applications → Panier Commun → Autorisations → Caméra → Autoriser.
Une connexion internet est nécessaire la première fois que le lecteur de codes-barres se charge.

## Bon à savoir

- **Internet** : les polices et la création du PDF d'étiquettes se chargent en ligne. Sans connexion,
  l'application fonctionne mais avec la police du système, et l'impression d'étiquettes est indisponible.
- **PDF d'étiquettes** : ils sont enregistrés dans le dossier Téléchargements du téléphone et s'ouvrent directement.
- **Données** : elles sont propres à chaque téléphone. Désinstaller l'application les efface.
  Si vous aviez installé la version 1.0, les données saisies dedans ne sont pas reprises par la 1.1.
- **Mettre à jour la page** : remplacez `app/src/main/assets/index.html`, augmentez `versionCode`
  dans `app/build.gradle.kts`, puis recompilez.
- Pour publier sur le Play Store, il faudra une version « release » signée avec votre propre clé.
