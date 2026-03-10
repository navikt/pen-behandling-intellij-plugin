# PEN Behandling IntelliJ Plugin

[![Build](https://github.com/navikt/pen-behandling-intellij-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/navikt/pen-behandling-intellij-plugin/actions/workflows/build.yml)

IntelliJ-plugin som gjør det enklere å opprette nye behandlinger og aktiviteter i PEN's behandlingsløsning.

## Funksjoner

### Ny Behandling (New → PEN Behandling)

Oppretter en komplett behandling med:
- `{Navn}Behandling.kt` — Behandlingsklassen med riktige annotasjoner (`@Entity`, `@DiscriminatorValue`, `@ForvalgtAnsvarligTeam`)
- `A{nr}_{Beskrivelse}.kt` — Initiell aktivitet med `Aktivitet`-entity og `AktivitetProcessor`

![ny_behandling.png](docs/images/ny_behandling.png)

Dialogen lar deg velge:
- **Navn** — Navnet på behandlingen (uten "Behandling"-suffiks)
- **Team** — Ansvarlig team (`PESYS_FELLES`, `PESYS_ALDER`, `PESYS_UFORE`)
- **Prioritet** — `ONLINE`, `ONLINE_BATCH` eller `BATCH`
- **Input-parametere** — Parametre som serialiseres til JSON i `INPUT`-kolonnen
- **Initiell aktivitet** — Beskrivelse for første aktivitet
- 
![ny_behandling_dialog.png](docs/images/ny_behandling_dialog.png)

### Ny Aktivitet (Alt+Enter i en Behandling- eller Aktivitet-fil)

Legg til en ny aktivitet direkte fra koden med **Alt+Enter** → *"Add new Aktivitet to this Behandling"*.

- **Fra en `*Behandling.kt`-fil**: Ny aktivitet legges til etter den høyeste eksisterende
- **Fra en `A###_*.kt`-fil**: Ny aktivitet settes inn rett etter den nåværende

![ny_aktivitet.png](docs/images/ny_aktivitet.png)

![ny_aktivitet_dialog.png](docs/images/ny_aktivitet_dialog.png)

Nummereringen håndteres automatisk. Hvis du setter inn en aktivitet midt i flyten, renummereres alle etterfølgende aktiviteter automatisk (filnavn og klassenavn oppdateres i alle filer i mappen).

### Gi nytt navn til Aktivitet (Alt+Enter i en Aktivitet-fil)

**Alt+Enter** → *"Rename Aktivitet (PEN conventions)"* oppdaterer:
- Filnavn (`A101_GammeltNavn.kt` → `A101_NyttNavn.kt`)
- Klassenavn (entity og processor)
- Diskriminatorverdi
- Alle referanser i filer i samme mappe

![rename_aktivitet.png](docs/images/rename_aktivitet.png)

![rename_aktivitet_dialog.png](docs/images/rename_aktivitet_dialog.png)

### Inspeksjoner

Pluginen har tre konsoliderte inspeksjoner som sjekker PEN-konvensjoner:

#### Behandling-inspeksjon
| Sjekk | Alvorlighet |
|---|---|
| Manglende `@Entity` | ERROR |
| Manglende `@DiscriminatorValue` | ERROR |
| `@DiscriminatorValue` matcher ikke klassenavn-konvensjonen | WARNING |
| Manglende `@ForvalgtAnsvarligTeam` | WARNING |

#### Aktivitet-inspeksjon
| Sjekk | Alvorlighet |
|---|---|
| Manglende `@Entity` | ERROR |
| Manglende `@DiscriminatorValue` | ERROR |
| `@DiscriminatorValue` matcher ikke konvensjonen (`{Behandling}_{Beskrivelse}`) | WARNING |
| Klassenavn slutter ikke med "Aktivitet" | WARNING |
| Ingen `AktivitetProcessor` i samme fil | WARNING |

#### Processor-inspeksjon
| Sjekk | Alvorlighet |
|---|---|
| Manglende `@Component` | WARNING |
| Refererer feil `Behandling`-type (matcher ikke mappen) | WARNING |
| Refererer `Aktivitet`-type som ikke finnes i samme fil | WARNING |

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

1. **Ny behandling**: Høyreklikk på en mappe → **New → PEN Behandling** → fyll inn skjemaet
2. **Ny aktivitet**: Åpne en behandlings- eller aktivitetsfil → **Alt+Enter** → *"Add new Aktivitet to this Behandling"*
3. **Gi nytt navn**: Åpne en aktivitetsfil → **Alt+Enter** → *"Rename Aktivitet (PEN conventions)"*
4. **Inspeksjoner**: Aktiveres automatisk i alle Kotlin-filer med Behandling/Aktivitet/Processor-klasser
