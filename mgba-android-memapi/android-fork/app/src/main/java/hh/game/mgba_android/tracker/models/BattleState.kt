package hh.game.mgba_android.tracker.models

data class EnemyData(
    val speciesId: Int,
    val name: String,
    val level: Int,
    val type1: Int,
    val type2: Int,
    val ability1Id: Int,
    val ability2Id: Int,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val baseSpd: Int,
    val baseSpAtk: Int,
    val baseSpDef: Int,
    val revealedMoveIds: List<Int>,  // move IDs seen so far in battle
    val status: Int,                 // status condition byte (0=none)
    val currentHp: Int,
    val maxHp: Int,
) {
    val hpPercent: Float get() = if (maxHp > 0) currentHp.toFloat() / maxHp else 0f
    val bst: Int get() = baseHp + baseAtk + baseDef + baseSpd + baseSpAtk + baseSpDef
}

data class BattleState(
    val isActive: Boolean,
    val isWild: Boolean,
    val enemy: EnemyData?,
    val weather: Weather,
    val playerReflect: Int,       // turns remaining (0 = none)
    val playerLightScreen: Int,
    val enemySpikes: Int,          // 0–3 layers on player's side
    val playerSafeguard: Int,
    val turnCount: Int,
    val lastMoveId: Int,           // 0 = none
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
