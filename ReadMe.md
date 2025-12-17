# 🧩 HAI913I_TP1_Part2_JDT — Analyseur de Code Java  
*(Console, JavaFX, Web)*

## 📖 Description du projet

Ce projet propose un **analyseur statique de code Java** basé sur **Eclipse JDT (Java Development Tools)**.  
Il permet de parcourir, analyser et visualiser la structure interne d’un projet Java sous différentes formes :

- 🖥 **Mode console** pour tester le graphe ou les statistiques analytiques en renseignant le chemin du projet via le code source
- 🎨 **Interface JavaFX** pour une visualisation interactive sur bureau
- 🌐 **Application Web** Spring Boot + Thymeleaf pour une utilisation moderne depuis un navigateur

---

## ⚙️ Architecture du projet

```text
HAI913I_TP1_Part2_JDT/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── graph/
│   │   │   │   ├── JDTParser.java                 # Console : Graphe d’appel
│   │   │   │   └── ClassMethodCallVisitor.java
│   │   │   ├── visiteurs/
│   │   │   │   ├── Parser.java                    # Console : Statistiques globales
│   │   │   │   └── *.java                         # Visiteurs JDT (compteurs, analyseurs)
│   │   │   ├── gui/
│   │   │   │   ├── JDTAnalyzerGUI.java             # JavaFX : Statistiques
│   │   │   │   └── JDTCallGraphGUI.java            # JavaFX : Graphe
│   │   │   └── webanalyzer/
│   │   │       ├── WebAnalyzerApplication.java     # Application Web Spring Boot
│   │   │       ├── controller/ProjectController.java
│   │   │       ├── model/GlobalStats.java
│   │   │       └── parser/Parser.java
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/style.css
│   │       │   └── images/background.avif
│   │       └── templates/
│   │           ├── index.html
│   │           └── analysis.html
````

---

## 🧪 1️⃣ Mode console — Graphe des appels

### 🔹 Classe principale

```java
graph.JDTParser
```

### ▶ Exécution

```bash
mvn exec:java -Dexec.mainClass="graph.JDTParser"
```

### 📸 Résultat attendu

Affiche dans la console la liste des appels entre classes et méthodes sous forme de graphe logique.

#### Exemple de sortie console

```text
Classe: MethodeA
 -> appelle : methodeB(Type)
```

---

## 📊 2️⃣ Mode console — Statistiques analytiques

### 🔹 Classe principale

```java
visiteurs.Parser
```

### ▶ Exécution

```bash
mvn exec:java -Dexec.mainClass="visiteurs.Parser"
```

### 📈 Données affichées

* Nombre total de classes
* Nombre total de méthodes
* Moyenne d’attributs / méthodes par classe
* Classes avec plus de X méthodes
* Classes contenant les méthodes les plus longues
* Etc.

#### Exemple de sortie console

```text
===== ANALYSE STATISTIQUE DU PROJET =====
Total Classes : 27
Total Méthodes : 154
Moyenne Méthodes / Classe : 5.7
Classe avec le plus de méthodes : UserManager (12)
Classes avec plus de X=2 méthodes : [Y, Z]
```

---

## 💻 3️⃣ Interface JavaFX — Analyse Statistique

### 🔹 Classe principale

```java
gui.JDTAnalyzerGUI
```

### ▶ Exécution

```bash
mvn exec:java -Dexec.mainClass="gui.JDTAnalyzerGUI"
```

### 🖼 Fonctionnalités

* Affichage des statistiques sous forme de tableau
* Filtrage des résultats
* Visualisation directe

---

## 🕸 4️⃣ Interface JavaFX — Graphe d’appels

### 🔹 Classe principale

```java
gui.JDTCallGraphGUI
```

### ▶ Exécution

```bash
mvn exec:java -Dexec.mainClass="gui.JDTCallGraphGUI"
```

### 🖼 Fonctionnalités

* Visualisation dynamique du graphe d’appels entre classes et méthodes
* Interaction utilisateur en JavaFX

---

## 🌐 5️⃣ Application Web — JDT Analyzer Web

### 🔹 Classe principale

```java
webanalyzer.WebAnalyzerApplication
```

### ▶ Exécution

```bash
mvn spring-boot:run
```

### 🌍 Accès

```
http://localhost:8081
```

### 🧩 Fonctionnalités

* Formulaire de sélection du projet
* Affichage des statistiques globales
* Visualisation graphique interactive du graphe d’appels
* Utilisation de **Cytoscape.js**

---

## 🧰 Dépendances principales (`pom.xml`)

| Dépendance                      | Utilisation                     |
| ------------------------------- | ------------------------------- |
| `org.eclipse.jdt.core`          | Analyse syntaxique du code Java |
| `commons-io`                    | Manipulation de fichiers        |
| `spring-boot-starter-web`       | Application web REST            |
| `spring-boot-starter-thymeleaf` | Templates HTML                  |
| `jackson-databind`              | Sérialisation JSON              |
| `lombok`                        | Réduction du code boilerplate   |

---

## 🔧 Compilation et exécution globale

### 📦 Compilation

```bash
mvn clean install
```

### 🧠 Exécution Web (via exec-maven-plugin)

```bash
mvn exec:java
```

---

## 🧩 Auteur

👩‍💻 **Fama COUNDOUL**
🎓 Université de Montpellier — Master Génie Logiciel
📘 TP2 — Analyse Statique de Code avec Eclipse JDT
