# OP Tools – 0.2 Alpha

Open-Source-Fabric-Mod für den Minecraft-Server **OPSUCHT** (`opsucht.net`).
Lokal-first, modular und vorbereitet für ein optionales, selbst gehostetes Backend.

| | |
|---|---|
| Minecraft | 1.21.11 (Fabric) |
| Abhängigkeiten | Fabric Loader ≥ 0.16.10, Fabric API |
| Java | 21 |
| Lizenz | MIT |

## Installation

1. Fabric Loader für Minecraft 1.21.11 installieren.
2. [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner legen.
3. `OP-Tools-0.2-Alpha.jar` in den `mods`-Ordner legen.
4. Beim ersten Start erscheint der Einrichtungsbildschirm → **„Lokal verwenden“**.

Menü: Taste **O** (änderbar) oder `/optools`.

## Funktionen

### Job-Tracker
* Wertet die Job-Anzeige automatisch aus – unabhängig vom Übertragungsweg: Actionbar (beide Pakettypen enden in
  `Gui#setOverlayMessage`), Titel/Untertitel, Bossbars und Systemchat laufen über einen zentralen Router
  (`OpTools#onIncoming`). Job, Level, Fortschritt, erhaltene XP und Geld werden reihenfolgeunabhängig erkannt,
  z. B. `+2.5 XP · +12.73$ · Holzfäller · Level 58 · [...] · 11.91%`.
* Jede bezahlte Aktion zählt einzeln – auch wenn OPSUCHT viele identische Meldungen im selben Tick schickt
  (z. B. Timber-Axt: eine Actionbar pro Block).
* Sessionzeit (nur aktive Zeit), XP/h, Geld/h, geschätzte XP bis zum Level-Up und ETA.
* AFK-Erkennung: ohne Job-Aktion länger als die eingestellte Zeit (Schieberegler 1–20 s, Standard 5 s) zeigt das
  HUD „AFK“, die Pause zählt nicht in die aktive Zeit und verfälscht den Stundenschnitt nicht.
* Zahlenformat einstellbar: kompakt (`12,3k`, `1,2 Mio`) oder vollständig (`12.345,67`).
* Sessions enden nach einstellbarer Inaktivität, beim Verlassen des Servers oder mit `/optools session neu`
  und werden in der Historie gespeichert.
* Statistiken: Gesamtwerte, Bestwerte, pro Job, Verlauf der aktuellen Session (Graph), XP/h bzw. $/h der letzten
  Sessions (Balkendiagramm).
* HUD-Widget: frei verschiebbar (HUD-Editor, `/optools hud`), Größe, Deckkraft und angezeigte Zeilen einstellbar.

### Finanzbuch
* Erfasst eingehende und ausgehende Zahlungen (`/pay`) aus Systemnachrichten.
* Jobeinnahmen werden gebündelt gebucht (Standard: ein Eintrag pro Job und 10 Minuten).
* Buchungsverlauf mit Filtern, Einnahmen/Ausgaben/Netto (heute, 7 Tage, 30 Tage, gesamt),
  Nettoentwicklung (Liniendiagramm) und Tagesbalken. Falsch erkannte Buchungen lassen sich löschen.
* **Schutz vor Fake-Nachrichten:** Zeilen, die als Spieler-Chat erkannt werden oder deren Präfix kein bekanntes
  System-Präfix ist (z. B. `Spieler » Du hast … überwiesen`), werden nie gebucht. Signierter Spieler-Chat wird
  gar nicht ausgewertet.

### Markt- und Shard-Daten (offizielle OPSUCHT-API)
Ausschließlich diese, live geprüften Endpunkte von `https://api.opsucht.net` werden genutzt:

| Endpunkt | Inhalt | Verwendung |
|---|---|---|
| `GET /market/prices` | `{Kategorie: {MATERIAL: [{orderSide, activeOrders, price}]}}` | Preisliste (BUY / SELL) |
| `GET /market/categories` | `[{name, material, icon}]` | Kategorie-Filter |
| `GET /market/history/{material}` | `{HOURLY, DAILY, WEEKLY, MONTHLY: [{avgPrice, minPrice, maxPrice, items, transactions, timestamp}]}` | Marktplatz-Preisverlauf |
| `GET /merchant/rates` | `[{source, target, base, exchangeRate}]` | Rohstoffhändler-Kurse (OPShards / Redcoins) |

Die Auktionshaus-Endpunkte werden bewusst **nicht** verwendet (keine durchschnittlichen AH-Preise).
Die API cached 60 s, deshalb fragt die Mod höchstens alle 60 s neu an (nur solange der Markt-Tab offen ist).

### Command-QoL
Client-Kurzbefehle aus `config/optools/shortcuts.json` (Argumente werden angehängt oder bei `{args}` eingesetzt):

| Kurzbefehl | Sendet | Status |
|---|---|---|
| `/cb1` … `/cb6` | `/nav cb1` … `/nav cb6` | Navigator-Ziele der CityBuilds |
| `/fw` | `/farm` (Farmwelt-Navigator) | laut Wiki |
| `/rw` | `/redstone` | laut Wiki |
| `/lux` | `/nav luxury-island` | laut Wiki |

Neue Kurzbefehle: Eintrag in `shortcuts.json` ergänzen → `/optools reload` → neu verbinden.
Alte Einträge `nav citybuild-N` aus 0.1 Alpha werden beim Start automatisch auf `nav cbN` korrigiert.

### RTP-Tracker
* Erkennt RTP-/Biom-Teleport-Meldungen (Anmeldung/Hinzufügung zu einem Biom, Warteschlangen-Position, Countdown,
  Teleport, Abbruch, Abklingzeit) aus Chat, Actionbar, Titeln und Bossbars.
* Keine fest verdrahteten Einzelsätze: eine Zeile muss RTP-Bezug haben (`rtpContext`) und wird dann über
  Schlüsselwort-Patterns eingeordnet; Biom, Position und Sekunden werden unabhängig extrahiert.
* Eigenes HUD-Widget „RTP / Biom-Teleport“ (nur sichtbar, solange etwas läuft), `/optools rtp` zeigt den Status.

### Shard-Kurse im Item-Tooltip
Für Items, die der Rohstoffhändler annimmt, steht der aktuelle Kurs unter dem normalen Tooltip
(pro Stück, für den ganzen Stapel und im Vergleich zur Basis). Eigene Items wie Gräbergemisch werden über
Name bzw. `custom_model_data` erkannt. Abschaltbar unter Einstellungen → Darstellung.

### Chat-QoL
* Spielernamen im Chat sind anklickbar → bereitet `/msg <Name> ` vor (optional direkt ausführen,
  Befehlsvorlage einstellbar). Nicknames (`~Nick`) werden erkannt.
* `/ah` bzw. `/auktionshaus` in Spielernachrichten wird zu einer klickbaren Aktion `/ah <Name>`
  (ein explizites `/ah Steve` wird respektiert).
* Erweiterbar: neue Aktion = Klasse, die `ChatAction` implementiert, in `ChatActionRegistry` registrieren.

### Einstellungen & First-Start
* Module einzeln (de)aktivierbar, HUD, Tracking-, Finanz-, Chat- und Darstellungsoptionen (Akzentfarbe).
* Einrichtungsbildschirm: „Lokal verwenden“ (aktiv) und „Eigenen OP Tools Server verbinden“ mit
  IP-/Domain-Feld – deutlich als **Coming Soon** markiert, Verbinden deaktiviert. Die Adresse wird nur gemerkt.

## Befehle

| Befehl | Funktion |
|---|---|
| `/optools [jobs\|finanzen\|markt\|shards\|einstellungen]` | Menü / Tab öffnen |
| `/optools hud` | HUD-Editor |
| `/optools session neu` / `pause` | Session beenden / pausieren |
| `/optools reload` | Patterns & Kurzbefehle neu laden |
| `/optools rtp` | Aktueller RTP-Status |

## Dateien

```
config/optools/
├── config.json             Einstellungen
├── opsucht-patterns.json   ALLE OPSUCHT-spezifischen Texte/Regex (zentral anpassbar)
├── shortcuts.json          Kurzbefehle
└── data/
    ├── meta.json           Schema-Version + zufällige Geräte-ID
    ├── job-sessions.json   Job-Historie
    └── finance.json        Finanzbuch
```

Alle Dateien werden atomar geschrieben (temp-Datei + move); defekte Dateien werden gesichert statt überschrieben.

## Architektur

```
de.optools
├── OpTools                Entrypoint, verdrahtet Module mit Fabric-Events
├── opsucht/               ⟵ einzige Stelle mit OPSUCHT-Wissen
│   ├── OpsuchtPatterns    Default-Regex (Actionbar, Zahlungen, Chat, /ah, Server-Adressen, Jobnamen)
│   ├── PatternRepository  lädt/aktualisiert opsucht-patterns.json
│   └── parse/             JobActionbarParser, PaymentParser, ChatLineParser (reines Java, getestet)
├── jobs/                  JobTracker (Sessions, Raten, ETA), JobStatistics
├── finance/               FinanceBook (Buchungen, Aggregationen)
├── market/                OpsuchtApiClient + MarketService (Cache)
├── commands/              Kurzbefehle + /optools
├── chat/                  ChatAction-System (ComponentRewriter erhält Formatierung)
├── hud/                   HudManager + Widgets (Job, Finanzen)
├── storage/               DataProvider-Abstraktion, LocalDataProvider, RemoteDataProvider (Coming Soon)
├── config/                Konfiguration
├── gui/                   eigenes UI-Toolkit, Tabs, Charts, First-Start, HUD-Editor
└── mixin/                 GuiMixin (Actionbar), ChatComponentMixin (Chat-Aktionen)
```

**Backend-Vorbereitung:** Alle persistierten Datensätze erben von `SyncRecord` (`id` UUID, `createdAt`,
`updatedAt`, `deviceId`, `deleted`-Tombstone). Feature-Module sprechen nur mit `DataStore`; ein späterer
`RemoteDataProvider` kann per `id`/`updatedAt` synchronisieren, ohne die Module anzufassen.

## Was noch nicht verifiziert ist

Das Job-Format (`+2,5 XP • +12,73$ • Holzfäller Level 58 • […] 13,85%`) ist mit echten OPSUCHT-Daten geprüft.
Die Texte für Zahlungen und RTP sind nicht öffentlich dokumentiert und beruhen auf toleranten Mustern. Falls etwas
nicht erkannt wird, das passende Pattern in `config/optools/opsucht-patterns.json` anpassen, `"customized": true`
setzen und `/optools reload` ausführen.

## Getestet (0.2 Alpha)

* 23 Unit-Tests für Zahlen-, Job-, Zahlungs-, Chat- und RTP-Parser (inkl. echter OPSUCHT-Actionbar-Zeilen), Fake-Zahlungs-Schutz, Merchant-Daten, Timber-Bursts, AFK-Zeit und Tracker-Mathematik.
* Im echten Client (Dev- und Produktionsumgebung mit dem gebauten Jar) mit simulierten OPSUCHT-Nachrichten:
  Actionbar → HUD/Tracker, Zahlungen → Finanzbuch (Fake-Zeile abgewiesen), klickbare Namen und `/ah`-Links,
  Live-Daten der OPSUCHT-API in Markt- und Shard-Tab, HUD-Editor, First-Start, Persistenz über Neustarts.
* Job-Tracker auf dem echten OPSUCHT-Server bestätigt (0.1 Alpha nach Fix); Zahlungs- und RTP-Texte noch nicht.

## Bauen

```bash
./gradlew build      # → build/libs/OP-Tools-0.2-Alpha.jar
./gradlew test       # Parser- und Tracker-Tests
```
