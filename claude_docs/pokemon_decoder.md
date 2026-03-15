# PokemonDecoder

**File:** `tracker/data/PokemonDecoder.kt`

## Purpose
Decrypts the 100-byte Gen III Pokemon struct and extracts all fields into `PokemonData`. This is the most complex single piece of the tracker — implements XOR decryption and the 24 substructure permutation system.

## Key Function
```kotlin
object PokemonDecoder {
    fun decode(
        slot: Int,
        raw: ByteArray,           // 100 bytes from party slot
        nameTable: Map<Int,String>,
        moveTable: Map<Int,String>,
        baseStatsReader: (Int) -> ByteArray?  // reads 28 bytes at ROM offset
    ): PokemonData?
}
```

## Decode Steps (in order)
1. Read personality (u32) and OT ID (u32) from offsets 0x00 and 0x04
2. If `personality == 0` → empty slot, return null
3. Compute XOR key: `personality XOR otId`
4. Decrypt 12 words at offsets 0x20–0x4F: `word[i] = word[i] XOR key`
5. Look up substructure order: `SUBSTRUCTURE_ORDER[personality % 24]` → array of 4 ints (0=G, 1=A, 2=E, 3=M)
6. Extract G (growth): species ID, held item ID, experience, friendship
7. Extract A (attacks): 4 move IDs + 4 PP values
8. Extract E (effort): EVs — HP, Atk, Def, Speed, SpAtk, SpDef
9. Extract M (misc): Pokérus byte at +0, IVs at +4 (5 bits each packed into 32-bit word, bit 31 = ability slot)
10. Compute hidden power type from IVs → `HP_TYPE_ROM_IDS[type_index]` → ROM type ID
11. Decode nickname from unencrypted bytes 0x08–0x11 (10 bytes, 0xFF terminator) via `GenIIICharMap`
12. Read unencrypted fields: status (0x50), level (0x54), current stats (0x56–)
13. Compute nature: `personality % 25`
14. Compute shiny: `(otId XOR pid XOR (pid ushr 16) XOR (otId ushr 16)) < 8`
15. Read base stats ROM entry (28 bytes) via `baseStatsReader(speciesId)`
    - types, gender ratio, exp group, abilities
16. Compute gender from `personality and 0xFF` vs genderRatio
17. Return `PokemonData` with all fields populated

## Substructure Layout
Each substructure is 12 bytes. Their position in the 48-byte block is determined by `SUBSTRUCTURE_ORDER[personality % 24]`.

**G (Growth) at `order[0] * 12`:**
```
+0  speciesId (u16)
+2  heldItemId (u16)
+4  experience (u32)
+8  ppBonuses (u8)
+9  friendship (u8)
```

**A (Attacks) at `order[1] * 12`:**
```
+0  move1 (u16), +2 move2 (u16), +4 move3 (u16), +6 move4 (u16)
+8  pp1 (u8), +9 pp2 (u8), +10 pp3 (u8), +11 pp4 (u8)
```

**E (Effort) at `order[2] * 12`:**
```
+0 evHp, +1 evAtk, +2 evDef, +3 evSpe, +4 evSpA, +5 evSpD (each u8, 0–255)
```

**M (Misc) at `order[3] * 12`:**
```
+0  Pokérus (u8)  +1 Pokérus days
+4–+7  IV word (u32):
    bits  0–4  = ivHp
    bits  5–9  = ivAtk
    bits 10–14 = ivDef
    bits 15–19 = ivSpe
    bits 20–24 = ivSpA
    bits 25–29 = ivSpD
    bit  31    = ability slot (0 → ability1, 1 → ability2)
```

## Troubleshooting
- **Garbage species / stats:** XOR key wrong, or substructure ordering wrong — check personality%24 index
- **Wrong moves:** Substructure A offset wrong; verify `order[1] * 12` math
- **Nickname garbled:** Check `GenIIICharMap.kt` — 0xFF = string end, 0x00 = '\0'
- **Wrong ability:** Check bit 31 of IV word in M substructure
- **Wrong gender:** `genderRatio == 0xFF` means male-only; `0xFE` means female-only; `0x00` means genderless
