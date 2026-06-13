# Realistic Block Physics Fixer - Analyse und Fix-Notizen

## Analysiertes Referenzrepo

Referenz: `xBigEllx/realistic-block-physics-mirror`, Branch `mc-1.20.x`.

Die Analyse konzentrierte sich auf den Forge-1.20.x-Codepfad rund um:

- `xbigellx.rbp.RealisticBlockPhysics`
- `xbigellx.rbp.internal.physics.BlockPhysicsHandler`
- `xbigellx.rbp.internal.physics.BlockOperationScheduler`
- `xbigellx.rbp.internal.entity.RealisticFallingBlockEntity`
- `xbigellx.rbp.internal.physics.BlockStabilityManager`

## Gefundene technische Schwachstellen

### 1. Client-only Klassen werden im Mod-Hauptcode importiert

`RealisticBlockPhysics` importiert den Client-Renderer direkt in derselben Klasse, die auch auf Dedicated Servern geladen wird. Obwohl die Renderer-Registrierung in einer `Dist.CLIENT`-Subscriber-Klasse steckt, bleibt der Top-Level-Import und die verschachtelte Klasse ein unnötiges Dedicated-Server-Risiko.

**Risiko:** `NoClassDefFoundError`/Classloading-Probleme auf Dedicated Servern, abhängig von Forge-Classloading und Optimierungen.

**Fixer-Strategie:** Der Fixer hält sämtlichen Common-Code frei von `net.minecraft.client`-Imports und verlinkt nicht direkt gegen RBP-Klassen.

### 2. Level-Tick-Handler prüft die Tick-Phase nicht

`BlockPhysicsHandler#onLevelTick` ruft Chunk-Analyse, Stabilitätsmanager und Operation-Scheduler ohne `START`/`END`-Phasenfilter auf.

**Risiko:** Die Arbeit kann pro Tick doppelt laufen. Das verschlechtert TPS, erhöht die Wahrscheinlichkeit großer Falling-Block-Spitzen und belastet Explosionen besonders stark.

**Fixer-Strategie:** Fixer-Module laufen über den eigenen serverseitigen Scheduler nur in `ServerTickEvent.Phase.END` und haben ein globales Arbeitsbudget.

### 3. Explosionen lösen unmittelbare, potentiell große Physics-Revalidierungen aus

`ExplosionEvent.Detonate` wird direkt an die Physics-Engine weitergereicht. Große Explosionen können sehr viele Nachbarschafts-/Stabilitätsprüfungen und Queue-Einträge auslösen.

**Risiko:** Tick-Spikes, riesige Operation-Queues, massenhaft fallende Blöcke, Chunk-Grenzprobleme und Server-Stalls.

**Fixer-Strategie:** Die Explosion-Fix-Logik sammelt nur sichere Koordinaten, plant verzögert/budgetiert, lädt keine Chunks, ignoriert BlockEntities/Fluids optional und dedupliziert Update-Ziele.

### 4. Falling-Block-Entities werden hart nach 600 Ticks entfernt

`RealisticFallingBlockEntity#tick` verwirft aktive Physics-Blöcke nach einer festen Lebensdauer. Bei Lag, großen Explosionen, Kollisionen, Slabs/Replaceables oder temporär ungünstiger Chunk-/Entity-Situation können Blöcke verschwinden, bevor sie sauber landen.

**Risiko:** Schlechteres Spielerlebnis durch verschwindende Strukturen/Ressourcen; besonders sichtbar bei großen Explosionen oder vielen fallenden Blöcken.

**Fixer-Strategie:** Neues Modul `falling_block_entity_guard` erkennt RBP-Falling-Block-Entities ohne direkte RBP-Dependency und setzt den privaten `fallTime`-Zähler nur bei sicheren, geladenen, airborne Entities rechtzeitig zurück.

### 5. Unsichere Casts und fehlende Null-Guards in Entity-Speicherung

Beim Laden einer `RealisticFallingBlockEntity` wird das Level aus dem Physics-Handler auf `RBPLevel` gecastet. Außerdem wird beim Speichern `blockPhysics.getName()` verwendet, was bei fehlerhafter Entity-Initialisierung null-anfällig ist.

**Risiko:** Crash beim Laden beschädigter Entities, bei Mod-/Config-Wechseln oder wenn Physics-Handler nicht wie erwartet initialisiert ist.

**Fixer-Strategie:** Der Fixer selbst nutzt defensive Checks und Reflection nur mit Fallback. Direkte Korrektur des Original-Entity-Serialisierungswegs wäre ein Mixin-Thema und bleibt ein offenes Risiko.

### 6. BlockEntity-Handling beim Fallen ist riskant

Beim Summon wird vorhandene BlockEntity-NBT zwischengespeichert, die BlockEntity entfernt und später in eine neue Position geschrieben. Bei anderen Mods, geändertem BlockState oder unerwarteter NBT können Datenverlust oder fehlerhafte BlockEntity-Zustände entstehen.

**Risiko:** Inventar-/Maschinendatenverlust oder NBT-Inkompatibilität bei fallenden modded BlockEntities.

**Fixer-Strategie:** Explosion-Fix ignoriert BlockEntities standardmäßig als Update-Ziele. Das reduziert die Wahrscheinlichkeit, dass sensible BlockEntities in Explosion-Nachläufen unnötig in Physics geraten.

### 7. Queue- und Player-Cache-Code kann bei vielen Spielern/Chunks teuer werden

`BlockOperationScheduler` iteriert spielernahe Chunks und Queues, nutzt pro Spieler Cache-Zustand und arbeitet weiter, bis Rate-/Prioritätsbedingungen greifen.

**Risiko:** Mit vielen Spielern, hoher `chunkUpdateRange` oder großen Explosionen wächst die Tick-Last stark.

**Fixer-Strategie:** Der Fixer nutzt harte pro-Tick Budgets, Scan-Intervalle, Soft-Limits, Deduplikation und Backpressure, statt selbst neue Entities zu erzeugen.

### 8. Geladene Chunks werden nicht überall gleich defensiv behandelt

Der Scheduler prüft `chunkExists` beim Einplanen, aber andere Pfade greifen über Physics-/Level-Wrapper auf BlockStates/Kontexte zu. Explosionen und Nachbarschaftsupdates an Chunk-Grenzen bleiben heikel.

**Risiko:** Chunkloads, falsche Zustände an Chunk-Grenzen oder verworfene Operationen.

**Fixer-Strategie:** Alle Fixer-Scans und Update-Dispatches prüfen geladene Chunks und laden keine Chunks nach.

## Eingebaute Fixes in diesem Arbeitsrepo

### Explosion Physics Update Fix

Bereits vorhandenes Modul, jetzt dokumentiert als Kernfix gegen große Explosionen:

- verzögerte Explosion-Scans,
- getrennte Normal-/Large-Explosion-Limits,
- optionale async Planung nur mit unveränderlichen Koordinaten,
- keine async World-Zugriffe,
- keine Chunkloads,
- BlockEntity-/Fluid-Filter,
- deduplizierte Update-Ziele,
- per-Tick-Budgets und Queue-Limits.

### Falling Block Entity Guard

Neu hinzugefügt:

- erkennt RBP-Falling-Block-Entities über Registry-ID `rbp:falling_block` oder Klassenname,
- keine direkte RBP- oder Client-Dependency,
- scannt nur auf dem Serverthread,
- respektiert globale und modulinterne Budgets,
- warnt bei zu vielen RBP-Falling-Block-Entities pro Level,
- kann den `fallTime`-Zähler sicher zurücksetzen, bevor RBP aktive Blöcke hart entfernt,
- nutzt zusätzlich einen optionalen Mixin-Hook am Anfang von `RealisticFallingBlockEntity#tick`, damit der Zähler vor RBP's eigenem `fallTime > 600`-Discard gekappt wird,
- zählt Keep-Alive nur dann als erfolgreich, wenn das private Feld wirklich gefunden und gesetzt wurde,
- überspringt unsichere Positionen, ungeladene Chunks, gelandete oder entfernte Entities,
- bricht den Fallback-Entity-Scan nach `maxEntitiesVisitedPerLevel` hart ab, weil Forge ohne RBP-Abhängigkeit keinen globalen RBP-Entity-Index bereitstellt,
- bietet optionalen, standardmäßig deaktivierten Emergency-Discard als letzte Crash-Sicherung.

## Neue Config-Werte

Unter `modules.fallingBlockEntityGuard`:

- `enabled`
- `scanIntervalTicks`
- `maxEntitiesScannedPerLevel`
- `maxEntitiesVisitedPerLevel`
- `softLimitPerLevel`
- `hardLimitPerLevel`
- `emergencyDiscardAboveHardLimit`
- `keepAliveEnabled`
- `mixinKeepAliveEnabled`
- `keepAliveResetAtTicks`
- `keepAliveResetToTicks`
- `stuckKeepAliveAfterTicks`
- `trackingTtlTicks`

## Test-/Check-Punkte im Spiel

1. Server mit RBP und RBPF starten und eine kontrollierte größere Sand-/Kies- oder Explosionssituation erzeugen.
2. `/rbpf fallingblocks stats` ausführen. Erwartet werden Zähler wie `seen`, `visited`, `keptAlive` und bei aktivem Mixin `mixinKeepAliveResets`.
3. Mit `debug.debugLogging=true` oder `/rbpf debug on` sollten Keep-Alive-Resets als Debug-Zeilen erscheinen, zum Beispiel mit Quelle `mixin` oder `scan` und dem alten/neuen `fallTime`.
4. Wenn `reflectionUnavailable=1` oder `keepAliveReflectionUnavailable` steigt, wurde das private `fallTime`-Feld nicht gefunden/gesetzt; dann zählt der Guard keine falschen Keep-Alive-Erfolge und die RBP-Version muss separat geprüft werden.
5. Wenn `scanVisitLimitReached` steigt, wurde der harte Entity-Visit-Abbruch erreicht. Dann `maxEntitiesVisitedPerLevel` nur erhöhen, wenn die Server-TPS stabil bleibt.

## Offene Risiken

- Wenn der optionale Mixin-Hook deaktiviert ist oder wegen einer geänderten RBP-Zielklasse nicht angewendet werden kann, bleibt nur der fallback Tick-Scan; dann kann RBP in Extremfällen vor dem nächsten Scan discardet haben.
- Die privaten Feldnamen von RBP können sich ändern. Dann deaktiviert sich die Keep-Alive-Funktion per Warnung statt zu crashen.
- BlockEntity-NBT-Probleme im Originalmod werden reduziert, aber nicht vollständig gepatcht.
- Sehr große Explosionen können weiterhin Last erzeugen, wenn RBP selbst bereits viele Operationen queued, bevor der Fixer nachsteuert.
- Das optionale Emergency-Discard sollte nur genutzt werden, wenn Crash-Vermeidung wichtiger ist als das vollständige Erhalten jedes Physics-Blocks.

## Produktionshärtung PR #4

### Dedicated-Server-Starttest und Artifact

Die GitHub-Action `Build and dedicated server smoke test` baut den Mod mit Java 17, prüft die Mixin-Konfiguration im Jar/Manifest, lädt die gebaute Jar-Datei bereits vor dem Smoke-Test als Artifact hoch, startet danach einen Forge Dedicated-Server-Dev-Run ohne RBP und wartet auf RBPF-Startup plus den vanilla `Done`-Ready-Marker. RBP bleibt absichtlich optional, damit RBPF auch auf Servern ohne RBP nicht crasht. Die Mixin-Config wird im Jar geprüft, weil Mixin den Confignamen je nach Forge/Mixin-Logging nicht zuverlässig im Serverlog ausgibt.

Lokaler RBP-Test:

1. `./gradlew build`
2. RBPF-Jar und RBP-Jar nach `run/mods/` kopieren.
3. `echo eula=true > run/eula.txt`
4. `./gradlew runServer --no-daemon -Dmixin.debug.verbose=true`
5. Log prüfen: RBPF-Startup, vanilla `Done`-Ready-Marker, kein Crash. Die Mixin-Config selbst im gebauten Jar/Manifest prüfen, falls sie nicht im Serverlog erscheint.

### Mixin-Kompatibilitätsprüfung

Der Mixin-Plugin-Zeitpunkt liegt früh in der Forge/Mixin-Initialisierung. Normales `ModList.get()` ist dort nicht garantiert verfügbar. Deshalb prüft `RBPFMixinPlugin` zuerst reflektiv Forge `LoadingModList` gegen bekannte RBP-Mod-IDs und fällt danach auf einen nicht-initialisierenden Zielklassen-Check zurück. Wenn RBP fehlt, anders heißt oder die Zielklasse entfernt wurde, ist die Mixin-Config `required=false` und der Mixin wird übersprungen statt den Server zu crashen.

### Reflection-Diagnose

`FallingBlockGuardSupport` unterscheidet `available`, `unavailable` und `unknown_until_rbp_entity_seen`. `/rbpf fallingblocks health` zeigt den Status, Mixin-Resets, Scan-Keep-Alives, Emergency-Discards und den letzten Fehler. Wenn `fallTime` nicht gefunden oder nicht gesetzt werden kann, wird eine klare Warnung mit Handlungsempfehlung geloggt und `health` meldet `BROKEN`, solange Keep-Alive aktiv ist.

### Produktions-Defaults

- `keepAliveResetAtTicks=560`: 40 Ticks Sicherheitsabstand zum RBP-Discard bei `fallTime > 600`; mit aktivem Mixin wird direkt vor RBPs Tick-Logik gekappt.
- `keepAliveResetToTicks=120`: reduziert Reset-Spam und hält die Entity weit genug vom Discard entfernt.
- `maxEntitiesVisitedPerLevel=4000`: verhindert unbegrenzte Fallback-Scans auf großen Servern.
- `maxEntitiesScannedPerLevel=512`: begrenzt Tracking/Reflection-Arbeit; der Mixin übernimmt den zeitkritischen Pfad.
- `softLimitPerLevel=350`: frühe Warnung bei großen Collapses.
- `hardLimitPerLevel=1200`: hohe Schwelle nur für explizit aktiviertes Emergency-Discard.
- `emergencyDiscardAboveHardLimit=false`: keine aggressiven Discards im Default.
