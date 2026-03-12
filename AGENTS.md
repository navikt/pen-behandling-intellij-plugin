# AGENTS.md (pen-behandling-intellij-plugin)

Denne filen beskriver hvordan en automatisert kodeagent (og mennesker) bør jobbe i dette repoet.

## Prosjektoversikt

IntelliJ-plugin for PEN's behandlingsløsning. Genererer kode for behandlinger, aktiviteter og tilhørende boilerplate i henhold til konvensjonene i [pensjon-pen](https://github.com/navikt/pensjon-pen).

## Teknologi

- **Språk**: Kotlin 2.x
- **Bygg**: Gradle med [IntelliJ Platform Plugin 2.x](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- **Target**: IntelliJ IDEA 2024.3+ (build 243+), K2-kompatibel
- **JDK**: 21

## Struktur

```
src/main/kotlin/no/nav/pensjon/pen/plugin/
├── action/          # AnAction-er (New → PEN Behandling)
├── dialog/          # DialogWrapper-dialoger og datamodeller
├── generator/       # Kodegeneratorer (streng-basert, ingen PSI-manipulasjon)
├── inspection/      # LocalInspectionTool-inspeksjoner
└── intention/       # IntentionAction-er (Alt+Enter-handlinger)
src/main/resources/META-INF/
└── plugin.xml       # Plugin-registrering (extensions, actions)
```

## Arkitekturmønstre

### Intention Actions (Alt+Enter)

Alle intention actions følger samme mønster:

1. **`isAvailable()`** — Sjekker filnavn/innhold for å avgjøre om handlingen skal tilbys
2. **Vis dialog** — Bruker en `DialogWrapper`-subklasse for brukerinput
3. **`WriteCommandAction`** — Utfører kodeendringer innenfor en undo-bar skrivekommando

Viktig: `startInWriteAction()` returnerer alltid `false` fordi dialoger må vises utenfor write lock.

### Kodegenerering

Generatorene (`BehandlingGenerator`, `AktivitetGenerator`) er rene streng-baserte — de bruker `buildString` til å generere Kotlin-kildekode. Ingen PSI-manipulasjon.

`ParameterCodeModifier` bruker regex og streng-manipulasjon for å modifisere eksisterende filer (legge til parametere, imports, etc.).

### Kodeendringer i eksisterende filer

Endringer i eksisterende filer gjøres via `PsiDocumentManager`:

1. Hent `Document` fra `PsiFile`
2. Manipuler teksten (streng-replace)
3. `doc.setText(newText)`
4. `psiDocManager.commitDocument(doc)`
5. Filnavn-endring: `virtualFile.rename(this, newName)`

### Dialoger

- Alle dialoger arver `DialogWrapper`
- Bruk `initValidation()` i `init`-blokken for kontinuerlig validering (ellers låser OK-knappen seg etter valideringsfeil)
- `doValidate()` returnerer `ValidationInfo` ved feil, `null` ved gyldig input

## Konvensjoner

### Navngivning

- Intention actions: `{Verb}{Substantiv}IntentionAction` (f.eks. `AddInputParameterIntentionAction`)
- Dialoger: `{Kontekst}Dialog` (f.eks. `NewBehandlingDialog`, `RenameAktivitetDialog`)
- Generatorer: `{Type}Generator` (f.eks. `BehandlingGenerator`)

### Registrering

Alle extensions registreres i `plugin.xml`:
- `<intentionAction>` for intention actions
- `<localInspection>` for inspeksjoner
- `<action>` for menypunkter

### Diskriminatorverdier

Diskriminatorverdier er databasenøkler og skal **ikke** endres uten at brukeren eksplisitt velger det. Endring brekker bakoverkompatibilitet.

## PEN-konvensjoner som pluginen håndhever

Pluginen genererer kode i henhold til pensjon-pen's behandlingsdomene:

- **Behandling**: `@Entity`, `@DiscriminatorValue("{Navn}")`, `@ForvalgtAnsvarligTeam`
- **Aktivitet**: `@Entity`, `@DiscriminatorValue("{Behandling}_{Beskrivelse}")`, entity + processor i samme fil
- **Filnavn**: `{Navn}Behandling.kt`, `A{###}_{Beskrivelse}.kt`
- **Klassenavn**: `{Behandling}A{###}{Beskrivelse}Aktivitet`, `...AktivitetProcessor`
- **Input/Output**: `@Lob @Column(name = "INPUT"/"OUTPUT")`, `@Serializable data class`, `kotlinx.serialization`

Se [behandlingsløsningens dokumentasjon](https://pensjon-dokumentasjon.ansatt.dev.nav.no/pen/Behandlingsloesningen/Behandlingslosningen.html) for fullstendige konvensjoner.

## Bygg og test

```bash
./gradlew buildPlugin          # Bygger plugin-ZIP i build/distributions/
./gradlew runIde               # Starter IntelliJ med pluginen installert
./gradlew verifyPlugin         # Verifiserer plugin-kompatibilitet
```

## Release

GitHub Actions workflow (`release.yml`) bygger og publiserer plugin-ZIP som GitHub Release. Versjon styres i `build.gradle.kts`.
