# Data Readers

Subsystem readers called by `TrackerPoller` each tick. All read via `MemoryBridge`.

---

## RouteReader
**File:** `tracker/data/RouteReader.kt`

Reads current map location.

```kotlin
data class RouteInfo(val mapLayoutId: Int, val name: String)
object RouteReader {
    fun read(game: GameVersion, addresses: GameAddresses): RouteInfo?
}
```

**How it works:**
1. Read `gMapHeader` pointer
2. Read u16 at `gMapHeader + 0x12` → mapLayoutId
3. Look up name in `RouteNames` (FR/LG table or Hoenn table based on game)

**Troubleshooting:** Wrong location name → check `RouteNames.kt` for the mapLayoutId; FR/LG and Hoenn use separate tables.

---

## StatsReader
**File:** `tracker/data/StatsReader.kt`

Reads game stats (steps, battles, Pokemon Center visits) from SaveBlock1 with XOR decryption.

```kotlin
data class GameStats(val steps: Long, val totalBattles: Long, val pokemonCenterVisits: Long)
object StatsReader {
    fun read(addresses: GameAddresses): GameStats?
}
```

**How it works:**
1. Resolve SaveBlock1:
   - FR/LG/Emerald: read pointer at `saveBlock1Ptr`, dereference to get real address
   - Ruby/Sapphire: use `saveBlock1Ptr` directly (no pointer)
2. Read 32-bit XOR key from SaveBlock2 at `encryptionKeyOffset`
   - Ruby/Sapphire: key = 0 (unencrypted)
3. Read stats array (4-byte dwords, XOR-decrypt each)
   - Index 5 = steps
   - Index 7 = total battles
   - Index 15 = Pokemon Center visits
4. XOR formula: `decryptedValue = rawValue XOR key`

**Troubleshooting:** Wrong stats → verify dword indices against Lua tracker source for the target game.

---

## BagReader
**File:** `tracker/data/BagReader.kt`

Reads Items and Berries pockets, computes healing summary for lead Pokemon.

```kotlin
data class BagDetailInfo(
    val hpHealPercent: Float,  // total heal as % of lead max HP
    val hpHealCount: Int,
    val hpItems: List<BagItemEntry>,
    val ppItems: List<BagItemEntry>,
    val statusItems: List<BagItemEntry>,
    val battleItems: List<BagItemEntry>
)
object BagReader {
    fun read(addresses: GameAddresses, maxHp: Int): BagDetailInfo
}
```

**How it works:**
1. Resolve SaveBlock1 (same pointer logic as StatsReader)
2. Scan items pocket: `bagPocket_Items_offset` + `bagPocket_Items_size` slots × 4 bytes (u16 itemId + u16 quantity)
3. Scan berries pocket: same format at `bagPocket_Berries_offset`
4. XOR-decrypt quantity for FR/LG/Emerald using 16-bit encryption key (lower 16 bits of SaveBlock2 key)
5. Categorize 19+ known healing items by type (HP, PP, Status, Battle)
6. Compute total HP heal % for lead; caps at 9999% / 99 items

**Troubleshooting:** Wrong quantities → XOR key extraction; Ruby/Sapphire don't encrypt quantities.

---

## LearnsetReader
**File:** `tracker/data/LearnsetReader.kt`

Reads level-up learnset from ROM.

```kotlin
data class LearnsetInfo(
    val learnedCount: Int,
    val totalCount: Int,
    val nextMoveLevel: Int?,
    val nextMoveName: String?,
    val allMoveLevels: List<Pair<Int,String>>  // (level, moveName)
)
object LearnsetReader {
    fun read(speciesId: Int, currentLevel: Int, addresses: GameAddresses): LearnsetInfo?
}
```

**How it works:**
1. Read pointer at `levelUpLearnsets + speciesId * 4` → pointer to learnset data
2. Read 2-byte entries until `0xFFFF` sentinel (max 100 entries failsafe)
   - Bits 0–8: moveId
   - Bits 9–15: level
3. Count moves learned (level ≤ currentLevel)
4. Find next unlearned move (lowest level > currentLevel)

**Troubleshooting:** Wrong learnset → verify `levelUpLearnsets` address in DataHelper for the specific game version. Different versions have different ROM offsets.
