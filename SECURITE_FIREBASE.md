# Sécurité Firebase — ce qu'il faut publier, et dans quelle base

Document destiné au **fournisseur** (pas au gérant). Il ne contient aucun secret.

---

## Il y a DEUX bases Firebase, avec des règles opposées

| Projet | Rôle | Reconnaissable à |
|---|---|---|
| **varotra-8c4d8** | **Licences** (activation) | `LicenceManager.CENTRAL_DATABASE_URL` |
| **varotra-1528a** | **Sauvegardes** (données des boutiques) | `FirebaseBackupManager.CENTRAL_BACKUP_DATABASE_URL` |

Retenir les cinq caractères : **`8c4d8` = licences**, **`1528a` = sauvegardes**. L'adresse est
affichée en haut de la page Realtime Database — la vérifier avant chaque publication.

**Les échanger serait pire que de ne rien faire.** Les règles des sauvegardes posées sur les
licences les rendraient **écrivables** (n'importe qui s'auto-active gratuitement) ; celles des
licences posées sur les sauvegardes rendraient `/backups` lisible **en entier** (un seul
`GET /backups.json` ramène les données de tous les clients).

Chemin dans la console, identique pour les deux : **choisir le projet → menu de gauche
« Créer » / « Build » → « Realtime Database » → onglet « Règles »**.

---

## Étape 1 — Publier les règles (5 minutes, aucune donnée à toucher)

C'est la seule étape urgente, et elle est **indépendante** de l'étape 2. À elle seule, elle ferme
les deux trous les plus graves.

### Base des LICENCES (`varotra-8c4d8`)

```json
{
  "rules": {
    "licences": {
      ".read": true,
      ".write": false
    }
  }
}
```

- `.read: true` est **obligatoire** : l'application doit lire son propre nœud pour se déverrouiller.
- `.write: false` est **la seule chose** qui empêche un client de s'auto-activer, ou de s'ajouter
  dans la liste `appareils`. Si une activation échoue un jour, ne jamais le passer à `true` : la
  cause est ailleurs (clé du nœud différente de l'ID d'installation à 6 chiffres, voir AGENTS §55).
  L'application ne fait **aucune** écriture sur cette base — vérifié, aucun PUT/POST/PATCH dans
  `LicenceManager`.

### Base des SAUVEGARDES (`varotra-1528a`)

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

**Le niveau est tout.** L'autorisation est donnée **sous `$token`**, jamais sur `backups`.
Ceci serait une fuite totale :

```
"backups": { ".read": true, ".write": true }
```

Un seul `GET /backups.json` renverrait alors la base complète de chaque boutique — ventes, dettes,
caisse, catalogue. Avec `$token`, il faut connaître le code de récupération de la boutique
(60 bits d'entropie, voir `RecoveryCode`) pour atteindre un nœud, et rien ne permet de lister les
autres. Contrairement aux licences, cette base **doit** rester écrivable : c'est l'application qui
y dépose la sauvegarde.

### Avant de publier : regarder ce qui est déjà là

Ouvrir l'onglet Règles et **lire** sans rien remplacer. Deux cas méritent attention :

- **Mode test** — des règles du genre `".read": "now < 1762...000"`. Elles **expirent à une date**,
  et ce jour-là les sauvegardes de tous les clients s'arrêtent, ou les activations échouent, sans
  aucun message. Les règles ci-dessus n'expirent pas.
- **D'autres nœuds** que `licences` (base 8c4d8) ou `backups` (base 1528a) : les règles ci-dessus
  remplacent tout le bloc, donc elles couperaient l'accès à ce qui n'est pas listé.

### Vérifier, sans attendre un appel client

Dans l'onglet Règles, bouton **« Simulateur »** (Rules playground) :

| Base | Type | Chemin | Attendu |
|---|---|---|---|
| 8c4d8 | Lecture | `/licences/123456` | autorisé |
| 8c4d8 | Écriture | `/licences/123456` | refusé |
| 1528a | Lecture | `/backups` | **refusé** |

Le dernier est le plus important : s'il est autorisé, l'autorisation est posée au mauvais niveau et
toutes les boutiques sont lisibles d'un coup.

---

## Étape 2 — Les codes de récupération (sans urgence, et plus simple qu'il n'y paraît)

### Le problème

Le nœud de licence contient un champ `recoveryCode` : la copie du code de récupération des
sauvegardes du client (AGENTS §40 et §43). Or le numéro de licence **est** l'ID d'installation à
6 chiffres, donc énumérable (un million de valeurs), et `/licences` doit rester lisible. Qui
parcourt `/licences/000000.json` … `/licences/999999.json` récupère donc les codes de **toutes** les
boutiques — et un code de récupération donne accès à la sauvegarde cloud complète d'une boutique.

**Aucune règle ne peut masquer ce seul champ.** Dans Realtime Database les règles descendent en
cascade et ne peuvent pas être resserrées plus bas : un `.read: false` sur
`/licences/$id/recoveryCode` sous un `/licences` lisible ne révoque rien. La valeur doit **sortir**
du sous-arbre lisible.

### La méthode : sortez-la de Firebase, pas ailleurs dans Firebase

**L'application ne lit jamais ce champ** — vérifié dans `LicenceManager.verifierLicence`, qui ne
lit que `actif`, `expiration`, `epicerie`, `note` et `appareils`. Ce n'est donc qu'un pense-bête
pour dépanner un client qui a tout perdu. Un pense-bête n'a aucune raison de vivre dans Firebase,
où il est justement la seule copie énumérable.

```
1 clic   Onglet Données, sur le nœud « licences » : ⋮ → Exporter JSON
         Ce fichier contient TOUS les codes. C'est votre copie.
         Le ranger avec les documents de gestion (pas dans Firebase).

puis     Supprimer le champ recoveryCode des nœuds de licence.
```

Rien à créer, aucune valeur à recopier à la main.

**Moins d'une dizaine de clients :** déplier `licences` → `<numéro>`, survoler la ligne
`recoveryCode`, cliquer le **×**. Deux clics par client.

**Beaucoup de clients :** retirer le champ du fichier exporté, puis, sur le nœud `licences`,
**⋮ → Importer JSON**. Un import sur **ce** nœud ne remplace que lui. Ne **jamais** importer à la
racine, ce qui écraserait toute la base. Pour retirer le champ sans éditer le fichier à la main
(rien ne quitte la machine) :

```bash
python3 -c "import json,sys; d=json.load(open('licences.json'));
[n.pop('recoveryCode',None) for n in d.values() if isinstance(n,dict)];
json.dump(d,open('licences-sans-codes.json','w'),indent=2,ensure_ascii=False)"
```

`maxAppareils` et `transferts` peuvent partir de la même façon si l'on veut les soustraire au
regard des autres clients — l'application ne les lit pas davantage. Ce sont des informations
commerciales, pas des secrets.

### Pour les nouveaux clients

Ne plus inscrire le code de récupération dans le nœud de licence. Le garder dans le fichier de
gestion, à côté de l'ID d'installation. Et **ne pas le mettre dans `note`** : ce champ est lu par
l'application, donc il est dans le sous-arbre lisible.

---

## Ce que cela ne corrige pas

L'énumération des 10⁶ numéros reste possible et expose encore le **nom de la boutique**
(`epicerie`) et la **note commerciale** (`note`) des autres clients. C'est une atteinte à la
confidentialité, plus un accès aux données de vente. Le palier suivant — déjà identifié dans
AGENTS §39 — est Firebase Authentication (anonyme) avec des règles par UID, de sorte qu'un client
ne puisse lire que son propre nœud. C'est un chantier à part, qui touche l'activation de tous les
appareils en service.

Rappel de la limite de fond, inchangée : le verrouillage est fait par l'application, donc un APK
recompilé le contourne. L'objectif est la fraude ordinaire, pas le rétro-ingénieur.
