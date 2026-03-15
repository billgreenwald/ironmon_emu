# Lookup Tables

**Directory:** `tracker/tables/`

All tables are `object` singletons with hardcoded data. These are **reference data only** — no logic.

> **Important:** Table contents were sourced from the Lua ironmon tracker and Gen III ROM data. Do not change IDs/values without verifying against the Lua source or ROM dumps.

---

## Species & Moves

| File | Contents | Key Type |
|------|----------|----------|
| `SpeciesNames.kt` | 386 Pokemon names | `Map<Int, String>` — species ID → name |
| `MoveNames.kt` | 354 move names | `Map<Int, String>` — move ID → name |
| `MoveStatsTable.kt` | 354 moves: power, accuracy, type (ROM ID), PP | `Map<Int, MoveStats>` |
| `MoveDescTable.kt` | Move descriptions (for detail sheets) | `Map<Int, String>` |

---

## Pokemon Data

| File | Contents | Key Type |
|------|----------|----------|
| `NatureTable.kt` | 25 natures with stat modifiers (atk/def/spA/spD/spe: +1.1/×1.0/×0.9) | `Map<Int, NatureData>` |
| `AbilityTable.kt` | 76 Gen III ability names + descriptions | `Map<Int, AbilityData>` |
| `BstTable.kt` | Base Stat Totals for all 386 species | `Map<Int, Int>` |
| `ExperienceTable.kt` | 6 growth curves (xpForLevel, xpProgress %) | functions by group index (0–5) |
| `EvolutionTable.kt` | Evolution chains by species ID | `Map<Int, List<EvolutionLevel>>` |
| `EvolutionLevel.kt` | Evolution entry: method, param (level/item ID), target species | data class |

---

## Items & Types

| File | Contents | Key Type |
|------|----------|----------|
| `ItemTable.kt` | 376 Gen III items by ID | `Map<Int, String>` |
| `TypeChart.kt` | 18×18 effectiveness matrix (Gen III type IDs) | `Array<FloatArray>` indexed by [attackType][defenseType] |

---

## Routes

| File | Contents | Key Type |
|------|----------|----------|
| `RouteNames.kt` | FR/LG map group/num → location name + Hoenn (Ruby/Sapphire/Emerald) | Two separate maps (FR/LG + Hoenn) |
| `RouteEncounterSlots.kt` | mapLayoutId → list of species IDs per encounter slot | `Map<Int, List<Int>>` — partially populated |

---

## Character Encoding

| File | Contents |
|------|----------|
| `GenIIICharMap.kt` | GBA byte → Unicode character mapping for Pokemon nicknames |

**Key values:** `0xFF` = end of string, `0x00` = null (skip), printable chars in `0xA1–0xEE` range.

---

## Type IDs (ROM → Display)
Gen III uses its own type ID numbering:
```
0=Normal  1=Fighting  2=Flying   3=Poison   4=Ground  5=Rock
6=Bug     7=Ghost     8=Steel    11=Fire    12=Water  13=Grass
14=Electric 15=Ice    16=Psychic 17=Dragon  18=Dark
(9 and 10 are unused)
```
`TypeChart.kt` is indexed by these ROM IDs — don't remap to 0-based before looking up.

---

## Adding / Updating Tables
1. Verify the new values against the **Lua ironmon tracker source** first
2. Check all 5 games — don't hardcode Emerald-only species/move changes
3. If adding a new table, add a reference to it in `CLAUDE_ARCHITECTURE.md`
