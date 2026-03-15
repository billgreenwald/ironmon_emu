# Data Addresses & Game Settings

**Files:** `tracker/data/DataHelper.kt`, `tracker/data/GameSettings.kt`

## Purpose
Cross-game memory address constants. **CRITICAL: Always verify addresses against the Lua ironmon tracker source before changing anything here.**

## GameSettings.kt — ROM Detection
```
ROM_GAME_CODE_ADDR  = 0x080000AC  (4-byte game code, e.g. "BPRE")
ROM_VERSION_BYTE    = 0x080000BC  (0 = v1.0, 1 = v1.1, 2 = v1.2)
```

**Game code sets:**
- `FIRE_RED_CODES` — "BPRE" (English), "BPRI" (Italian), "BPRS" (Spanish), "BPRF" (French), "BPRD" (German), "BPRJ" (Japanese)
- `LEAF_GREEN_CODES` — "BPGE", "BPGI", "BPGS", "BPGF", "BPGD", "BPGJ"
- `RUBY_CODES` — "AXVE" + variants
- `SAPPHIRE_CODES` — "AXPE" + variants
- `EMERALD_CODES` — "BPEE"

## DataHelper.kt — Address Sets

### `GameAddresses` fields (per-game):
```
partyCount          — number of live party members
partyBase           — first byte of 6-slot party array (each slot 100 bytes)
baseStatsTable      — ROM address of base stats entries (28 bytes each)
levelUpLearnsets    — ROM pointer table for level-up moves
enemyParty          — enemy party slots (100 bytes each, for PP tracking)
battleTypeFlags     — u32 flags; bit 3 = TRAINER (0 = wild)
battleMons          — gBattleMons array; slot 0=player, slot 1=enemy
battlersCount       — active battler count
battleWeather       — current weather enum
sideStatuses        — u32 per side (reflect, light screen, safeguard bits)
sideTimers          — turn counts for side conditions
battleOutcome       — battle result code
battleResults       — includes current/last move info
gMapHeader          — pointer to current map header (u16 mapLayoutId at +0x12)
saveBlock1Ptr       — pointer (FR/LG/Emerald) or direct (Ruby/Sapphire)
saveBlock1IsPointer — true for FR/LG/Emerald; false for Ruby/Sapphire
gameStatsOffset     — offset into SaveBlock1 for stats array
saveBlock2Ptr       — address of SaveBlock2 (for XOR encryption key)
encryptionKeyOffset — offset within SaveBlock2 for 32-bit XOR key
bagPocket_Items_offset/size       — items pocket in SaveBlock1
bagPocket_Berries_offset/size     — berries pocket in SaveBlock1
trainerBattleOpponent             — current trainer opponent ID
```

### `addressesFor(game, romVersion, gameCode)` returns correct `GameAddresses` or null

**Supported variants:**
- FR/LG English v1.0 and v1.1 (different `baseStatsTable`)
- FR/LG non-English (Japanese/Spanish/Italian/French/German — different `saveBlock2Ptr`)
- Ruby/Sapphire v1.0 and v1.1 (direct SaveBlock1, no XOR encryption)
- Emerald (single version, XOR-encrypted)
- **Ruby/Sapphire battle addresses are UNVERIFIED PLACEHOLDERS** — treat as broken

## Pokemon Struct Layout (100 bytes)
```
0x00–0x03   Personality (u32) — XOR key source, nature = %25, gender, shiny
0x04–0x07   OT ID (u32) — XOR key source, shiny
0x08–0x11   Nickname (10 bytes, 0xFF terminator, Gen III char encoding)
0x12–0x1F   (other unencrypted fields: language, marks, etc.)
0x20–0x4F   48 bytes encrypted (4x 12-byte substructures, order = personality%24)
0x50        Status condition (u8)
0x54        Level (u8)
0x56–0x5D   Stats: HP, Atk, Def, Speed, SpAtk, SpDef (u16 each, LE)
```

## Substructure Order (personality % 24)
Each of 24 orderings arranges G/A/E/M substructures:
- **G (Growth):** species ID, held item, experience, friendship
- **A (Attacks):** 4 move IDs + 4 PP values
- **E (Effort):** EVs (HP, Atk, Def, Speed, SpAtk, SpDef)
- **M (Misc):** Pokérus (bytes 0-1), IVs + ability (bytes 4-7; bit 31 = ability slot)

## XOR Decryption
```
key = personality XOR otId
for each of 12 words (u32) in the 48 encrypted bytes:
    word[i] = word[i] XOR key
```

## Battle Struct (gBattleMons, 0x58 = 88 bytes per slot)
```
slot 0 = player, slot 1 = enemy
moves at 0x0C (NOT 0x14!)
species at 0x00
level at 0x1C
status at 0x28
current HP at 0x22, max HP at 0x24
stat stages at 0x18 (7 bytes: Atk, Def, SpA, SpD, Spe, Acc, Eva; 6 = neutral)
```

## Key Formulas
- **Shiny:** `(otId XOR personality XOR (personality ushr 16) XOR (otId ushr 16)) < 8`
- **Nature:** `personality % 25`
- **Gender:** `(personality and 0xFF) < genderRatio` → FEMALE; genderRatio 0xFF = always male, 0xFE = always female
- **Hidden Power type:** derived from IVs via `HP_TYPE_ROM_IDS` in PokemonDecoder

## Base Stats ROM Entry (28 bytes per species at `baseStatsTable + speciesId * 28`)
```
+0  HP base stat
+1  Attack
+2  Defense
+3  Speed
+4  SpAtk
+5  SpDef
+6  type1 (ROM type ID)
+7  type2 (ROM type ID)
+8  catch rate
+9  base exp
+10–+11 EV yield
+12–+13 item1, item2
+14 gender ratio
+15 egg steps
+16 base friendship
+17 exp group (0–5)
+18–+19 egg groups
+20 ability1
+21 ability2
```

## Type IDs (ROM)
`0=Normal 1=Fighting 2=Flying 3=Poison 4=Ground 5=Rock 6=Bug 7=Ghost 8=Steel 11=Fire 12=Water 13=Grass 14=Electric 15=Ice 16=Psychic 17=Dragon 18=Dark`
(9 and 10 unused in Gen III)

## Troubleshooting
- **Wrong species / garbage data:** XOR key wrong, or substructure order lookup wrong — check personality%24
- **Stats offset:** Only worry about DataHelper if addresses changed between ROM versions
- **Non-English ROM not detected:** Check `isNonEnglish()` and add to the game code sets in GameSettings
