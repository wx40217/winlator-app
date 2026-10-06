---
version: 1
slug: "java-com-winlator-shortcutsfragment-java-d4e9619c"
primary_target: "app/src/main/java/com/winlator/ShortcutsFragment.java"
related_targets: ["app/src/main/java/com/winlator/SettingsFragment.java"]
---

Mode: Operate. Target: native Android game library and layout preference. Approved by the user: icon grid by default, compact list available from Settings and persisted.

## Direction contract

THESIS: Make existing shortcuts an immediately usable game library: search, scope by container, launch by tapping the whole item, and return to the same browsing context.

OWN-WORLD: Blue navigation surfaces, cool light/dark reading surfaces, system type, aligned two-line titles, 48dp actions, and clearly subordinate container labels. Android Material navigation and controls carry the tasks.

STORY: Open the library, find a program, launch through the existing runtime, and find it again in recent launches. Favorites and recent launch records are local metadata, not runtime or compatibility claims.

FIRST VIEWPORT: Main app bar; name search; All/Favorites and container filter; recent launches only when real records exist; adaptive icon grid with independent 48dp overflow controls; persistent Library/Containers/Settings navigation. Empty and filtered states explain the next action.

FORM: Collection catalog, grounded candidate 5, direction seed 88d1c4f4. User selected the icon grid and requested the alternative compact list in Settings. Signature interaction: a launched item's recent placement updates while browsing context remains stable across runtime-driven process restart. Code-led build; conversation mockup is a concept reference, not an approved pixel comp.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
