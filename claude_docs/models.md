# Data Models

**Directory:** `tracker/models/`

---

## GameVersion.kt
```kotlin
enum class GameVersion {
    FIRE_RED, LEAF_GREEN, RUBY, SAPPHIRE, EMERALD, UNKNOWN
}
```
**Note:** Ruby/Sapphire battle addresses are UNVERIFIED PLACEHOLDERS — treat battle features as broken for those games.

---

## TrackerState.kt
Top-level state emitted by `TrackerPoller.state: StateFlow<TrackerState>`.

```kotlin
sealed class TrackerState {
    object Disconnected    // MemoryBridge.reader == null or ROM not loaded
    object NoGameLoaded    // ROM loaded but not a supported Gen III game
    data class Active(
        val game: GameVersion,
        val romVersion: Int,          // 0=v1.0, 1=v1.1, 2=v1.2
        val romTitle: String,         // 12-char from GBA header 0x080000A0
        val party: List<PokemonData>, // up to 6, non-null slots only
        val battle: BattleState,      // BattleState.NONE if not in battle
        val currentRoute: RouteInfo?,
        val stats: GameStats?,
        val bagDetail: BagDetailInfo?,
        val isGameOver: Boolean,
        val runAttempts: Int,
        val playerLearnset: LearnsetInfo?,
        val enemyLearnset: LearnsetInfo?,
        val routeEncounters: Map<Int, List<Int>>,   // mapLayoutId → species seen
        val routeVisitOrder: List<Int>              // insertion order
    )
}
```

---

## PokemonData.kt
Full decoded Pokemon from party struct.

```kotlin
data class PokemonData(
    val slot: Int,                    // 0–5 in party
    val speciesId: Int,
    val speciesName: String,
    val nickname: String,             // decoded from Gen III char map
    val level: Int,
    val currentHp: Int,
    val maxHp: Int,
    val type1: Int, val type2: Int,   // ROM type IDs (see data_addresses.md)
    val attack: Int, val defense: Int, val speed: Int,
    val spAtk: Int, val spDef: Int,   // computed in-battle stats
    val moves: List<MoveData>,
    val heldItemId: Int,
    val experience: Int,
    val statusCondition: Int,         // raw: bits 0–2=sleep, 3=PSN, 4=BRN, 5=FRZ, 6=PAR, 7=TOX
    val nature: Int,                  // 0–24 (personality % 25)
    val abilityIndex: Int,            // 0 or 1 (from bit 31 of IV word)
    val ability1Id: Int,
    val ability2Id: Int,
    val bst: Int,                     // Base Stat Total
    val expGroup: Int,                // 0–5 growth curve
    val gender: Gender,
    val isShiny: Boolean,
    val hasPokerus: Boolean,
    val ivHp: Int, val ivAtk: Int, val ivDef: Int,
    val ivSpe: Int, val ivSpA: Int, val ivSpD: Int,
    val evHp: Int, val evAtk: Int, val evDef: Int,
    val evSpe: Int, val evSpA: Int, val evSpD: Int,
    val friendship: Int,
    val hiddenPowerType: Int          // ROM type ID (computed from IVs)
)

enum class Gender { MALE, FEMALE, NONE }

data class MoveData(
    val moveId: Int,
    val moveName: String,
    val pp: Int, val maxPp: Int,
    val power: Int, val accuracy: Int,
    val moveType: Int                 // ROM type ID
)
```

**Computed properties:**
- `isAlive` = `currentHp > 0`
- `hpPercent` = `currentHp.toFloat() / maxHp`
- `abilityId` = if `abilityIndex == 0` then `ability1Id` else `ability2Id`
- `displayName` = nickname if set, else speciesName

---

## BattleState.kt

```kotlin
data class BattleState(
    val isActive: Boolean,
    val isWild: Boolean,              // false = trainer battle
    val enemy: EnemyData?,
    val weather: Weather,
    val playerReflect: Int,           // turns remaining (0 = inactive)
    val playerLightScreen: Int,
    val playerSafeguard: Int,
    val enemySpikes: Int,             // 0–3 layers on player's side
    val turnCount: Int,
    val lastMoveId: Int,
    val trainerOpponentId: Int,       // 0 if wild
    val playerStatStages: IntArray?   // [Atk,Def,SpA,SpD,Spe,Acc,Eva]; 6=neutral
) {
    companion object { val NONE = BattleState(isActive = false, ...) }
}

data class EnemyData(
    val speciesId: Int, val name: String, val level: Int,
    val type1: Int, val type2: Int,
    val ability1Id: Int, val ability2Id: Int, val bst: Int,
    val revealedMoveIds: List<Int>,   // moves seen (max 4)
    val ppByMoveId: Map<Int, Int>,    // current PP from enemy party struct
    val status: Int, val currentHp: Int, val maxHp: Int
) {
    val hpPercent get() = currentHp.toFloat() / maxHp
}

enum class Weather { NONE, RAIN, SAND, SUN, HAIL }
```
