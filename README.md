# PEN Behandling IntelliJ Plugin

IntelliJ-plugin som gjør det enklere å opprette nye behandlinger og aktiviteter i PEN's behandlingsløsning.

## Funksjoner

### Ny Behandling (New → PEN Behandling → Behandling)

Oppretter en komplett behandling med:
- `{Navn}Behandling.kt` — Behandlingsklassen med riktige annotasjoner (`@Entity`, `@DiscriminatorValue`, `@ForvalgtAnsvarligTeam`)
- `A101_{Beskrivelse}.kt` — Initiell aktivitet med `Aktivitet`-klasse og `AktivitetProcessor`

Dialogen lar deg velge:
- **Navn** — Navnet på behandlingen (uten "Behandling"-suffiks)
- **Team** — Ansvarlig team (`PESYS_FELLES`, `PESYS_ALDER`, `PESYS_UFORE`)
- **Prioritet** — `ONLINE`, `ONLINE_BATCH` eller `BATCH`
- **Input-parametere** — Parametre som serialiseres til JSON i `INPUT`-kolonnen
- **Initiell aktivitet** — Nummer og beskrivelse for første aktivitet

### Ny Aktivitet (New → PEN Behandling → Aktivitet)

Oppretter et aktivitet/prosessor-par:
- `A{nr}_{Beskrivelse}.kt` — Aktivitet- og AktivitetProcessor-klasse

Pluginen gjetter automatisk:
- Behandlingsnavn fra eksisterende `*Behandling.kt`-filer i mappen
- Neste aktivitetsnummer basert på eksisterende `A\d{3}_*.kt`-filer

### Inspeksjoner

| Inspeksjon | Beskrivelse |
|---|---|
| `@DiscriminatorValue` suffix | Advarer hvis diskriminatorverdien ender med "Behandling" |
| Manglende `@ForvalgtAnsvarligTeam` | Advarer hvis en `Behandling`-subklasse mangler teamannotasjon |
| Manglende `@Component` | Advarer hvis en `AktivitetProcessor`-subklasse mangler `@Component` |

## Bygging

```bash
./gradlew buildPlugin
```

Plugin-filen havner i `build/distributions/`.

## Installasjon

1. Bygg pluginen med `./gradlew buildPlugin`
2. I IntelliJ: **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. Velg ZIP-filen fra `build/distributions/`
4. Restart IntelliJ

## Bruk

1. Høyreklikk på en mappe i PEN-prosjektet (under `domain/`)
2. Velg **New → PEN Behandling → Behandling** eller **Aktivitet**
3. Fyll inn skjemaet og klikk OK
4. Filene opprettes automatisk med korrekt boilerplate
