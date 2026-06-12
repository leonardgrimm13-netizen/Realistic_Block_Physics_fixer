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
- überspringt unsichere Positionen, ungeladene Chunks, gelandete oder entfernte Entities,
- bietet optionalen, standardmäßig deaktivierten Emergency-Discard als letzte Crash-Sicherung.

## Neue Config-Werte

Unter `modules.fallingBlockEntityGuard`:

- `enabled`
- `scanIntervalTicks`
- `maxEntitiesScannedPerLevel`
- `softLimitPerLevel`
- `hardLimitPerLevel`
- `emergencyDiscardAboveHardLimit`
- `keepAliveEnabled`
- `keepAliveResetAtTicks`
- `keepAliveResetToTicks`
- `stuckKeepAliveAfterTicks`
- `trackingTtlTicks`

## Offene Risiken

- Ohne Mixin kann der Fixer nicht verhindern, dass RBP direkt in seinem eigenen Entity-Tick vor einem Guard-Scan discarded.
- Die privaten Feldnamen von RBP können sich ändern. Dann deaktiviert sich die Keep-Alive-Funktion per Warnung statt zu crashen.
- BlockEntity-NBT-Probleme im Originalmod werden reduziert, aber nicht vollständig gepatcht.
- Sehr große Explosionen können weiterhin Last erzeugen, wenn RBP selbst bereits viele Operationen queued, bevor der Fixer nachsteuert.
- Das optionale Emergency-Discard sollte nur genutzt werden, wenn Crash-Vermeidung wichtiger ist als das vollständige Erhalten jedes Physics-Blocks.
