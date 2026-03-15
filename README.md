# IronMon Emu

A GBA emulator for Android with a built-in [IronMon Tracker](https://github.com/besteon/Ironmon-Tracker) — no PC, no setup, no second screen required.

<p align="center">
  <img src="screenshots/top%20screenshot.png" alt="IronMon Emu" width="600"/>
</p>

---

## Full IronMon Tracker Built In

IronMon Emu replicates the full feature set of the [Ironmon-Tracker](https://github.com/besteon/Ironmon-Tracker) Lua script — live, on your Android device. Everything you would see on a second monitor or stream overlay is shown in the right-hand panel while you play.

The tracker panel has three tabs, which you can select by tapping the tab name or by swiping left/right anywhere on the tracker pannel:

- **MY MON** — Your lead Pokémon's species, level, nature (with stat impact highlighted), ability, held item, base stats, moves with power/accuracy/PP, type chips, shiny/Pokérus/gender indicators, HP bar, and XP progress.
- **OPPONENT** — When a battle starts, the opponent tab automatically populates with the enemy Pokémon's species, level, type, BST, stat markings, and moves as they are revealed. Wild vs. trainer battles are detected automatically.
- **ROUTE** — Shows the current route and all Pokémon that can be encountered in it, so you can plan ahead without leaving the app.

<p align="center">
  <img src="screenshots/opponent%20view.png" alt="Opponent View" width="600"/>
  <br/><em>Opponent tab showing enemy stats and revealed moves mid-battle</em>
</p>

<p align="center">
  <img src="screenshots/route%20view.png" alt="Route View" width="600"/>
  <br/><em>Route tab showing current location and possible encounters</em>
</p>

The header bar always shows your current game, version, and run number. A **Next Run** button lets you start a fresh run at any time without leaving the emulator.

---

## Gamepad Detection

When a Bluetooth or USB gamepad is connected, the on-screen touch controls automatically hide so they don't cover the game. Disconnect the controller and they reappear instantly.

<p align="center">
  <img src="screenshots/gamepad%20detection.png" alt="Gamepad Detection" width="600"/>
  <br/><em>On-screen controls hidden when a gamepad is connected</em>
</p>

---

## ROM Families

The home screen groups your ROMs into **families** rather than listing every file individually. A family is a set of ROMs that were batch generated together (or in later batches) with the ironmon randomizer.  Families are detected by name, and then sequenced by number (i.e. all "FireRed1" "FireRed2" ... will be grouped into a single family called "FireRed". The card shows how many ROMs are in the family and which run you were on last, and when you pick it will automatically start your last played rom.  Quickload is auto tracked per family.

<p align="center">
  <img src="screenshots/rom%20family%20grouping.png" alt="ROM Family Grouping" width="300"/>
  <br/><em>ROM families grouped on the home screen, in this case ROMs were named "KaizoFR#"</em>
</p>

**To pick a specific ROM from a family**, long-press the family card. A list of all ROMs in that family will appear so you can choose the exact file to load.

**To add ROMs**, tap the folder icon in the top bar and point it at a directory. The app scans recursively, so you can organize your ROMs in subfolders however you like.

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

<p align="center">
  <img src="screenshots/pop%20up%20info.png" alt="Pop-up Info" width="600"/>
  <br/><em>Tapping a move name shows its full detail sheet</em>
</p>

---

## Installation

IronMon Emu is distributed as an APK for sideloading. It is not on the Play Store.

### Steps

1. **Download** the latest `IronMonEmu.apk` from the [Releases](../../releases) page on this GitHub repo from your phone.

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
  
