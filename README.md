# IronMon Emu

A GBA emulator for Android with a built-in [IronMon Tracker](https://github.com/besteon/Ironmon-Tracker) — no PC, no setup, no second screen required.

<p align="center">
  <img src="screenshots/top%20screenshot.png" alt="IronMon Emu" width="600"/>
</p>

---

## Full IronMon Tracker Built In

IronMon Emu replicates the full feature set of the [Ironmon-Tracker](https://github.com/besteon/Ironmon-Tracker) Lua script — live, on your Android device. Everything you would see on a second monitor or stream overlay is shown in the right-hand panel while you play.

The tracker panel has three tabs, which you can select by tapping the tab name or by swiping left/right anywhere on the tracker panel:

- **MY MON** — Your lead Pokémon's species, level, nature (with stat impact highlighted), ability, held item, base stats, moves with power/accuracy/PP, type chips, shiny/Pokérus/gender indicators, HP bar, XP progress, and a **GachaMon star rating** (see below).
- **OPPONENT** — When a battle starts, the opponent tab automatically populates with the enemy Pokémon's species, level, type, BST, stat markings, and moves as they are revealed in the same column layout as the player's move table (category icon, type dot, move name, Pwr, Eff, Acc, PP). Wild vs. trainer battles are detected automatically.
- **ROUTES** — Shows the current route and all Pokémon that can be encountered in it, with trainer defeat counts, so you can plan ahead without leaving the app.

<p align="center">
  <img src="screenshots/opponent%20view.png" alt="Opponent View" width="600"/>
  <br/><em>Opponent tab showing enemy stats and revealed moves mid-battle</em>
</p>

<p align="center">
  <img src="screenshots/route%20view.png" alt="Route View" width="600"/>
  <br/><em>Routes tab showing current location, trainer counts, and possible encounters</em>
</p>

The header bar always shows your current game, version, and run number. A **Next Run** button in the Tools menu starts a fresh run at any time without leaving the emulator.

---

## GachaMon Star Rating

The main tracker tab displays a ★★★★☆ star rating (1–5 stars, or 5+) for your lead Pokémon, computed from the same formula used by the Lua Ironmon Tracker's GachaMon system. The score accounts for ability quality, move ratings (with STAB, accuracy, and recoil bonuses), offensive/defensive/speed base stat thresholds, and nature. The numeric score is shown alongside the stars (e.g. `★★★☆☆ (38)`).

In **Emulator Settings**, a **Rating Ruleset** dropdown lets you choose which ruleset to evaluate against: Standard, Ultimate, Kaizo, Survival, Super Kaizo, or Subpar. Banned and adjusted moves/abilities are scored at 0 or 50% per the selected ruleset. Defaults to Standard and is persisted across sessions.

---

## Live Battle Type Tracking

During battle, the tracker reads Pokémon types directly from the live battle struct (`gBattleMons`) instead of static ROM data. This means type-changing moves and abilities automatically update the displayed types and type effectiveness in real time:

- **Conversion** — user's type changes to one of their move types
- **Conversion 2** — user's type changes to resist the last move taken
- **Camouflage** — user's type changes to match the terrain
- **Color Change / Kecleon** — user's type changes to the type of the move just received

Both player and enemy types update live. Types revert to base stats at battle end.

---

## Variable-Power Move Calculations in Battle

Move power is computed live during battle for HP-based, weight-based, friendship-based, and weather-dependent moves. Calculated values display in gold to distinguish them from static labels:

- **Flail / Reversal** — shows actual power (200/150/100/80/40/20) based on player's current HP
- **Eruption / Water Spout** — shows computed power (1–150) based on player's current HP
- **Low Kick** — shows weight bracket (20/40/60/80/100/120) based on enemy species weight
- **Return / Frustration** — shows computed power when friendship is near max (≥100)
- **Hidden Power** — shows power (30–70) computed from player's IVs
- **Weather Ball** — shows 50 (clear) or 100 (active weather)

Outside battle, all moves continue showing static labels (`>HP`, `<HP`, `WT`, `VAR`, etc.) unchanged.

---

## Starter Ball Randomizer

When a new run begins and the player is in the starter lab with no Pokémon, the tracker displays a ball picker showing Left / Middle / Right positions. One is randomly highlighted with a ▼ arrow to tell you which starter to grab. A **Reroll** button lets you re-randomize the choice. The picker dismisses automatically once you pick up your starter — mirroring the Lua tracker's ball picker feature.

---

## Gamepad Detection

When a Bluetooth or USB gamepad is connected, the on-screen touch controls automatically hide so they don't cover the game. Disconnect the controller and they reappear instantly.

<p align="center">
  <img src="screenshots/gamepad%20detection.png" alt="Gamepad Detection" width="600"/>
  <br/><em>On-screen controls hidden when a gamepad is connected</em>
</p>

---

## How to set up ROMs, and the concept of ROM Families

When you load the app for the first time, click the folder in the top status bar to locate your ROMs.  This will open a file browser.   The folder you select is where you should store any ROMs you want the app to load; they can be nested within subdirectories if you want.  The app is designed to be used with the Bulk Generation mode of the randomizer app (either Android based, or the java one on a computer and then you manually transfer the ROMs to your computer).  Once you have a folder with a batch of ROMs in it selected, the app will autoscan and create ROM families from them for you.

The home screen groups your ROMs into **families** rather than listing every file individually. A family is a set of ROMs that were batch generated together (or in later batches) with the ironmon randomizer.  Families are detected by name, and then sequenced by number (i.e. all "FireRed1" "FireRed2" ... will be grouped into a single family called "FireRed". The card shows how many ROMs are in the family and which run you were on last, and when you pick it will automatically start your last played rom.  Quickload is auto tracked per family.

<p align="center">
  <img src="screenshots/rom%20family%20grouping.png" alt="ROM Family Grouping" width="300"/>
  <br/><em>ROM families grouped on the home screen, in this case ROMs were named "KaizoFR#"</em>
</p>

**To pick a specific ROM from a family**, long-press the family card. A list of all ROMs in that family will appear so you can choose the exact file to load.

**To add ROMs**, tap the folder icon in the top bar and point it at a directory. The app scans recursively, so you can organize your ROMs in subfolders however you like.

### UPR Mode (Re-randomize Each Run)

Every family card shows a **BATCH** or **UPR** badge. In **UPR mode**, the family holds a single base ROM that gets re-randomized via [UPR-Android](https://github.com/Brogawon/UPR-Android) on every "Next Run" — no need to pre-generate hundreds of ROMs. The app binds to UPR-Android's `OverwriteService`, receives the randomized ROM, writes it back to disk, and reloads automatically.

To switch a family to UPR mode, long-press its card and choose **Family Mode** in the settings dialog. If UPR-Android is not installed, the ROM list shows a "✗ Randomizer not installed" status at the bottom and the UPR option is disabled. When UPR-Android is detected, it shows "✓ Randomizer installed" in green.

**Requirements for UPR mode:** UPR-Android installed, Android 8.1+ (API 27) for SharedMemory support.

---

## Route Images

For Fire Red and Leaf Green, tapping any route name that has associated images opens a full-screen tabbed gallery overlay with two tabs:

- **Maps** — dungeon/building maps (Mt. Moon, Rock Tunnel, Power Plant, Rocket Hideout, Safari Zone, Seafoam Islands, Silph Co., Victory Road, Pokémon Mansion, Saffron Gym, S.S. Anne, and more)
- **Hidden Items** — hidden item location images for 46 FRLG locations

Routes with available images show a `↗` indicator in both the header and the Routes tab. Use the ◀ ▶ arrows to navigate between images.

---

## Tappable Info in the Tracker

Most elements in the tracker panel are interactive — tap them to get a detail sheet with more information.

| Tap target | What you get |
|---|---|
| **Move name** | Move detail sheet: full description, type, power, accuracy, PP |
| **Ability name** | Ability detail sheet: full in-game ability description |
| **Type chip** (on your Pokémon or opponent) | Type defense chart: full 2× / 0.5× / 0× breakdown against all 18 types |
| **Moves** | List of what level each move is learned |
| **Stats** | EVs, Friendship, and Hidden Power Type |
| **Heals** | Full bag status, PP heal %, status heals |
| **Route name** (FR/LG) | Full-screen map and hidden item image gallery |
| **Wild Pokémon** in Routes tab | Detail sheet: types, BST, evolutions, abilities |

<p align="center">
  <img src="screenshots/pop%20up%20info.png" alt="Pop-up Info" width="600"/>
  <br/><em>Tapping a move name shows its full detail sheet</em>
</p>

---

## Emulator Settings

Open **Emulator Settings** from the ROM list page or from the in-game Tools menu. Settings are organized into five sections:

### Speed
- **Fast Forward Speed** — multiplier applied when fast forward is active
- **L button = Fast Forward** — intercepts the physical L button for fast forward instead of sending it to the GBA emulator (disables GBA L)
- **Fast Forward: Toggle** — press once to activate / press again to deactivate, instead of holding

### Input
- **Button Bindings** — bind any physical controller button or keyboard key to emulator actions (Fast Forward, Save State, Load State, Tracker Open/Close, Next Run, Mute Toggle, Tools Menu) and all 10 GBA buttons (A, B, L, R, Start, Select, D-pad). Volume keys can also be captured and bound.

### Layout
- **Tracker Size** — choose any 10%-increment split from 100%/0% to 0%/100% without restarting the game. Includes **Game Overlay** (game full-screen, tracker slides over) and **Tracker Overlay** (tracker full-screen, game slides under) modes. Font scale adjusts automatically.
- **Collapsible tracker panel** — adds a ◀/▶ arrow strip to hide/show the tracker panel with a tap or swipe
- **Hide collapse button** — removes the arrow strip; collapse/expand is then driven exclusively by the "Tracker Open/Close" key binding
- **Always show on-screen controls** — disables auto-hide when a gamepad is detected
- **Always hide on-screen controls** — permanently hides the padboard (for keyboard or gamepad-only users)
- **Controls opacity** — slider (0–100%) to adjust on-screen controls transparency; default 70%
- **Controls scale** — slider (50–150%) to resize the on-screen controls; default 100%

### Tracker
- **Game Over Condition** — choose what triggers a run-end: "Lead Pokémon faints" (default), "Highest level faints", or "Entire party faints". Mirrors the Lua Ironmon Tracker's Game Over settings.
- **Rating Ruleset** — choose which GachaMon ruleset to score against: Standard, Ultimate, Kaizo, Survival, Super Kaizo, or Subpar

### Display
- **Show FPS** — displays a live frames-per-second counter

---

## Installation

IronMon Emu is distributed as an APK for sideloading. It is not on the Play Store.

### Steps

1. **Download** the latest `ironmon_emulator.apk` from the [Latest Release](../../releases/latest) page on this GitHub repo from your phone.

2. **Enable sideloading** on your device:
   - On Android 8+: when you open the APK, Android will prompt you to allow installs from that source (your browser or Files app). Tap **Allow**.
   - On older Android: go to **Settings → Security → Unknown sources** and enable it.

3. **Open the APK** from your Files app and tap **Install**.

4. **Grant storage permission** on first launch so the app can scan for ROMs.

5. Tap the **folder icon** in the top bar, select the directory where your ROMs live, and you're ready to go.  ROMs can be nested within this folder if you want further filesystem organization, the app will automatically scan within.

### Requirements

- Android 8.0 (Oreo) or later
- A GBA ROM (`.gba`) — Fire Red, Leaf Green, Ruby, Sapphire, or Emerald
- ~200 MB free storage

### Notes

- Save states and battery saves are stored in the app's private storage and persist across updates.
- If Android warns that the file may be harmful, this is a standard warning shown for all sideloaded APKs — tap **Install anyway**.
- The app checks for updates on each launch. If a newer version is available, a banner appears on the ROM list page with a tap-to-download link.

## FAQ

- Will you make an ios version
  - I dont have an iphone, and I havent dealt with jailbreaking ios devices in a decade, and since I'm not releasing an app with Nintedo assets into a real store, there is no plan.  Feel free to put a PR in
- Will you put this in the actual play store
 - No, for the same reasons of Nintendo assets.
- Something doesnt work
  - Make an issue on github, and ill get to it!
- Did you use AI to make this?
  - You betchya, its all vibe coded.  If you are against the concept of using AI in any way, you should avoid this app.
- Can we donate in some way?
  - No.  I have some other apps that will be coming to the app store (ios and android) that you could buy so you get use out of it too, but otherwise no.
  
