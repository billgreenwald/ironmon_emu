package hh.game.mgba_android.tracker.models

data class EnemyData(
    val speciesId: Int,
    val name: String,
    val level: Int,
    val type1: Int,
    val type2: Int,
    val ability1Id: Int,
    val ability2Id: Int,
    val bst: Int,                            // Base Stat Total from BstTable (static lookup)
    val revealedMoveIds: List<Int>,          // move IDs seen so far in battle
    val ppByMoveId: Map<Int, Int> = emptyMap(), // moveId → current PP (from enemy party struct)
    val status: Int,                         // status condition byte (0=none)
    val currentHp: Int,
    val maxHp: Int,
) {
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
}

data class BattleState(
    val isActive: Boolean,
    val isWild: Boolean,
    val enemy: EnemyData?,
    val weather: Weather,
    val playerReflect: Int,           // turns remaining (0 = none)
    val playerLightScreen: Int,
    val enemySpikes: Int,              // 0–3 layers on player's side
    val playerSafeguard: Int,
    val turnCount: Int,
    val lastMoveId: Int,               // 0 = none
    val trainerOpponentId: Int = 0,    // gTrainerBattleOpponent_A; 0 for wild
    val playerStatStages: IntArray? = null, // [Atk,Def,SpA,SpD,Spe,Acc,Eva] 0-12; 6=neutral
    val playerType1: Int = -1,         // live from gBattleMons slot 0; -1 = not in battle
    val playerType2: Int = -1,         // updated by Conversion, Camouflage, etc.
) {
    companion object {
        val NONE = BattleState(
            isActive = false, isWild = false, enemy = null,
            weather = Weather.NONE, playerReflect = 0, playerLightScreen = 0,
            enemySpikes = 0, playerSafeguard = 0, turnCount = 0, lastMoveId = 0,
        )
    }
}

enum class Weather(val displayName: String) {
    NONE("Clear"),
    RAIN("Rain"),
    SAND("Sandstorm"),
    SUN("Harsh Sun"),
    HAIL("Hail"),
}
