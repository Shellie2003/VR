# Sécurité Firebase — règles à publier et action à faire dans la console

Document destiné au **fournisseur** (pas au gérant). Il ne contient aucun secret.

> **Il y a DEUX bases Firebase, avec des règles opposées.** Les échanger serait pire que de ne rien
> faire : les règles des sauvegardes posées sur les licences rendraient les licences **écrivables**
> (n'importe qui s'auto-active gratuitement) ; celles des licences posées sur les sauvegardes
> rendraient `/backups` lisible **en entier** (un seul `GET /backups.json` ramène les données de
> tous les clients). Avant de publier, vérifier l'URL affichée en haut de la page *Rules* : elle
> doit correspondre à la constante du code — `LicenceManager.CENTRAL_DATABASE_URL` pour les
> licences, `FirebaseBackupManager.CENTRAL_BACKUP_DATABASE_URL` pour les sauvegardes.
>
> Les sections 1 à 5 concernent la base des **licences**. La section 6 concerne celle des
> **sauvegardes**.

---

## 1. Le problème, vérifié dans le code

Le nœud de licence `/licences/<numéroDeLicence>` contient un champ `recoveryCode` : la copie du
code de récupération des sauvegardes du client (voir AGENTS.md §40 et §43). Le fournisseur l'y
garde pour pouvoir aider un client qui a tout perdu — l'intention est bonne.

Mais deux choses se combinent mal :

1. **Le numéro de licence est l'ID d'installation à 6 chiffres**, donc énumérable : il n'y a qu'un
   million de valeurs possibles, et chaque client voit la sienne affichée sur son écran
   d'activation.
2. **Les règles donnent `/licences` en lecture à tous les clients** (c'est nécessaire : l'app doit
   pouvoir lire son propre nœud pour se déverrouiller).

Conséquence : qui parcourt `/licences/000000.json` … `/licences/999999.json` récupère les codes de
récupération de **toutes** les boutiques — et le code de récupération est l'adresse du coffre de
sauvegarde cloud. Il donne donc accès à la base complète de n'importe quelle boutique : ventes,
dettes, caisse, catalogue.

## 2. Pourquoi une règle sur le seul champ ne peut pas marcher

Dans Realtime Database, les règles **descendent en cascade et ne peuvent pas être resserrées plus
bas** : dès que `.read` vaut `true` sur `/licences`, tout son sous-arbre est lisible, et un
`.read: false` sur `/licences/$id/recoveryCode` **ne révoque rien**. Il n'y a donc pas de correctif
par règle seule : il faut **sortir la valeur du sous-arbre lisible**.

## 3. Ce que l'application lit réellement — vérifié dans `LicenceManager.verifierLicence`

| Champ | Lu par l'app ? |
|---|---|
| `actif` | **oui** |
| `expiration` | **oui** |
| `epicerie` | **oui** (nom affiché au déverrouillage) |
| `note` | **oui** (transporté dans l'objet `Licence`) |
| `appareils` | **oui** (liste des appareils autorisés) |
| `recoveryCode` | **non** |
| `maxAppareils` | **non** |
| `transferts` | **non** |

Les trois derniers peuvent donc être déplacés **sans changer une ligne de code** et sans risque de
casser l'activation d'un client.

## 4. À faire dans la console

**a. Déplacer les codes.** Pour chaque licence existante, déplacer la valeur de
`/licences/<numéro>/recoveryCode` vers `/codes_recuperation/<numéro>`, puis **supprimer** le champ
d'origine. Faire de même pour `maxAppareils` et `transferts` si vous voulez aussi les soustraire au
regard des autres clients (ce sont des informations commerciales, pas des secrets).

**b. Poser ces règles** sur la base des **licences** (pas celle des sauvegardes) :

```json
{
  "rules": {
    "licences": {
      ".read": true,
      ".write": false
    },
    "codes_recuperation": {
      ".read": false,
      ".write": false
    }
  }
}
```

La console garde l'accès à `codes_recuperation` : les règles ne s'appliquent pas à elle. Le code
reste donc à votre disposition pour dépanner un client, mais devient invisible depuis les
applications.

**c. Ne jamais remettre un code de récupération dans `note`** : ce champ est lu par l'application,
donc il est dans le sous-arbre lisible.

## 5. Ce que cela ne corrige pas

L'énumération reste possible, et expose encore le **nom de la boutique** (`epicerie`) et la **note
commerciale** (`note`) des autres clients. C'est une atteinte à la confidentialité, pas un accès
aux données de vente. Le palier suivant — déjà identifié dans AGENTS.md §39 — est Firebase
Authentication (anonyme) avec des règles par UID, de sorte qu'un client ne puisse lire que son
propre nœud. C'est un chantier à part, qui touche l'activation de tous les appareils en service.

Rappel de la limite de fond, inchangée : le verrouillage est fait par l'application, donc un APK
recompilé le contourne. L'objectif est la fraude ordinaire, pas le rétro-ingénieur.

---

## 6. L'autre base — les SAUVEGARDES

Règles à publier (base `FirebaseBackupManager.CENTRAL_BACKUP_DATABASE_URL`) :

```json
{
  "rules": {
    "backups": {
      "$token": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

**Le niveau est tout.** L'autorisation est donnée **sous `$token`**, jamais sur `backups`
lui-même. Ceci serait une fuite totale :

```
"backups": { ".read": true, ".write": true }
```

Un seul `GET /backups.json` renverrait alors la base complète de chaque boutique — ventes,
dettes, caisse, catalogue. Avec `$token`, il faut connaître le code de récupération de la boutique
(60 bits d'entropie, voir `RecoveryCode`) pour atteindre un nœud, et rien ne permet de lister les
autres.

Contrairement aux licences, cette base **doit** rester écrivable par les clients : c'est
l'application qui y dépose la sauvegarde (`PUT` sur `/backups/<code>.json`, voir
`FirebaseBackupManager.uploadBackup`).

### Piège à vérifier sur les deux bases : le mode test

Si une base est encore sur les règles par défaut de sa création, elles ressemblent à
`".read": "now < 1762...000"`. Elles **expirent à une date**, et ce jour-là les sauvegardes de
tous les clients s'arrêtent — ou les activations échouent — sans aucun message. Les règles données
ici n'expirent pas.
