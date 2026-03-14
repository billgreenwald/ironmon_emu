package hh.game.mgba_android.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.landscapist.glide.GlideImage
import hh.game.mgba_android.tracker.data.GameStats
import hh.game.mgba_android.tracker.models.*
import hh.game.mgba_android.tracker.tables.*
import hh.game.mgba_android.tracker.tables.MoveDescTable

// ── Color palette ────────────────────────────────────────────────────────────
private val PanelBg       = Color(0xFF0F1621)
private val HeaderBg      = Color(0xFF16213E)
private val CardBg        = Color(0xFF1A2540)
private val AccentRed     = Color(0xFFE94560)
private val AccentBlue    = Color(0xFF4090FF)
private val TextPrimary   = Color(0xFFEEEEEE)
private val TextSecondary = Color(0xFFAAAAAA)
private val HpHigh        = Color(0xFF4CAF50)
private val HpMid         = Color(0xFFFFEB3B)
private val HpLow         = Color(0xFFF44336)

// ── Type chip colors ──────────────────────────────────────────────────────────
private val TYPE_COLORS = mapOf(
    0  to Color(0xFFA8A878),  // Normal
    1  to Color(0xFFC03028),  // Fighting
    2  to Color(0xFF8EB8E0),  // Flying
    3  to Color(0xFFA040A0),  // Poison
    4  to Color(0xFFE0C068),  // Ground
    5  to Color(0xFFB8A038),  // Rock
    6  to Color(0xFFA8B820),  // Bug
    7  to Color(0xFF705898),  // Ghost
    8  to Color(0xFFB8B8D0),  // Steel
    10 to Color(0xFFF08030),  // Fire
    11 to Color(0xFF6890F0),  // Water
    12 to Color(0xFF78C850),  // Grass
    13 to Color(0xFFF8D030),  // Electric
    14 to Color(0xFF98D8D8),  // Ice
    15 to Color(0xFFF85888),  // Psychic
    16 to Color(0xFF7038F8),  // Dragon
    17 to Color(0xFF705848),  // Dark
)

private fun typeColor(typeId: Int) = TYPE_COLORS[typeId] ?: Color(0xFF888888)

// ── Move category (Gen III: type-based, matching MoveData.lua lines 103–119) ─
private val PHYSICAL_TYPES = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
private val SPECIAL_TYPES  = setOf(11, 12, 13, 14, 15, 16, 17, 18)

private fun moveCategory(typeId: Int, power: Int): String = when {
    power == 0               -> "—"
    typeId in PHYSICAL_TYPES -> "P"
    typeId in SPECIAL_TYPES  -> "S"
    else                     -> "—"
}

private fun moveCategoryColor(typeId: Int, power: Int): Color = when {
    power == 0               -> TextSecondary
    typeId in PHYSICAL_TYPES -> Color(0xFFF08030)  // orange
    typeId in SPECIAL_TYPES  -> Color(0xFF6890F0)  // blue
    else                     -> TextSecondary
}

// ── Main entry point ─────────────────────────────────────────────────────────
@Composable
fun TrackerPanel(state: TrackerState, onQuickload: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg),
    ) {
        when (state) {
            is TrackerState.Disconnected -> StatusText("Loading…")
            is TrackerState.NoGameLoaded -> StatusText("No supported game loaded")
            is TrackerState.Active       -> ActivePanel(state, onQuickload)
        }
    }
}

@Composable
private fun StatusText(msg: String) {
    Text(
        text = msg, color = TextSecondary, fontSize = 14.sp,
        modifier = Modifier.padding(12.dp),
    )
}

// ── Active panel ──────────────────────────────────────────────────────────────
@Composable
private fun ActivePanel(state: TrackerState.Active, onQuickload: (() -> Unit)?) {
    PanelHeader(state, onQuickload)

    // Game over banner
    if (state.isGameOver) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFB00020))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("GAME OVER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .background(Color(0xFF7B0020), RoundedCornerShape(4.dp))
                    .clickable {
                        TrackerPoller.resetGameOver()
                        onQuickload?.invoke()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Run ${state.runAttempts + 1} →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Route name
    state.currentRoute?.let {
        Text(
            text = it.name, color = AccentBlue, fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        state.leadPokemon?.let { lead ->
            MainView(lead, state.battle, state.stats)
        } ?: run {
            StatusText("Party empty")
        }

        // Battle panel
        if (state.battle.isActive) {
            Spacer(Modifier.height(4.dp))
            BattlePanel(state.battle)
        }
    }
}

@Composable
private fun PanelHeader(state: TrackerState.Active, onQuickload: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "IRONMON", color = AccentRed, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            )
            if (state.romTitle.isNotEmpty()) {
                Text(text = state.romTitle, color = TextSecondary, fontSize = 8.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "${state.game.displayName.removePrefix("Pokémon ")} v1.${state.romVersion}",
            color = TextSecondary, fontSize = 9.sp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Run ${state.runAttempts + 1}",
            color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        )
        if (onQuickload != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(AccentRed, RoundedCornerShape(4.dp))
                    .clickable { onQuickload() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Next Run →", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Main view ─────────────────────────────────────────────────────────────────
@Composable
private fun MainView(pokemon: PokemonData, battle: BattleState, stats: GameStats? = null) {
    var showMoveSheet by remember { mutableStateOf<MoveData?>(null) }
    var showAbilitySheet by remember { mutableStateOf(false) }
    var showDefenseSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        // Header row: sprite + name/types/HP
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            GlideImage(
                imageModel = { "file:///android_asset/sprites/${pokemon.speciesId}.gif" },
                modifier = Modifier.size(80.dp),
                failure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("#${pokemon.speciesId}", color = TextSecondary, fontSize = 12.sp)
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(pokemon.speciesName)
                            when (pokemon.gender) {
                                Gender.MALE   -> append(" ♂")
                                Gender.FEMALE -> append(" ♀")
                                else          -> {}
                            }
                            if (pokemon.isShiny) append(" ✦")
                            if (pokemon.hasPokerus) append(" ✚")
                        },
                        color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text("Lv.${pokemon.level}", color = TextSecondary, fontSize = 16.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.clickable { showDefenseSheet = true },
                ) {
                    TypeChip(pokemon.type1)
                    if (pokemon.type2 != pokemon.type1) TypeChip(pokemon.type2)
                }
                Spacer(Modifier.height(2.dp))
                HpBar(pokemon)
            }
        }

        Spacer(Modifier.height(4.dp))

        val abilityName = AbilityTable.name(pokemon.abilityId)
        Text(
            text = "Ability: $abilityName",
            color = TextPrimary, fontSize = 14.sp,
            modifier = Modifier.clickable { showAbilitySheet = true },
        )

        val nature = NatureTable.get(pokemon.nature)
        val natureMod = NatureTable.modifier(pokemon.nature)
        Text(
            text = "Nature: ${nature.name}${if (natureMod.isNotEmpty()) " ($natureMod)" else ""}",
            color = TextSecondary, fontSize = 12.sp,
        )

        val itemName = if (pokemon.heldItemId > 0) ItemTable.get(pokemon.heldItemId) else "None"
        Text(text = "Item: $itemName", color = TextSecondary, fontSize = 12.sp)

        stats?.let { gs ->
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("%,d steps".format(gs.steps), color = TextSecondary, fontSize = 12.sp)
                Text("${gs.totalBattles} battles", color = TextSecondary, fontSize = 12.sp)
                Text("${gs.pokemonCenterVisits} centers", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(4.dp))
        Divider(color = Color(0xFF303050), thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))

        StatsTable(pokemon)

        Spacer(Modifier.height(4.dp))
        Divider(color = Color(0xFF303050), thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))

        MoveTable(pokemon.moves, battle, onClick = { showMoveSheet = it })
    }

    Spacer(Modifier.height(4.dp))

    showMoveSheet?.let { move ->
        MoveDetailSheet(move, onDismiss = { showMoveSheet = null })
    }
    if (showAbilitySheet) {
        AbilityDetailSheet(
            abilityId = pokemon.abilityId,
            onDismiss = { showAbilitySheet = false },
        )
    }
    if (showDefenseSheet) {
        TypeDefenseSheet(pokemon.type1, pokemon.type2, onDismiss = { showDefenseSheet = false })
    }
}

// ── Stats table (2 rows × 3 cols, inline in MainView) ────────────────────────
@Composable
private fun StatsTable(pokemon: PokemonData) {
    val nature = NatureTable.get(pokemon.nature)
    // Row 1: HP | Atk | Def
    Row(modifier = Modifier.fillMaxWidth()) {
        StatCell("HP",  pokemon.maxHp,   nature, statIdx = -1, Modifier.weight(1f))
        StatCell("Atk", pokemon.attack,  nature, statIdx = 0,  Modifier.weight(1f))
        StatCell("Def", pokemon.defense, nature, statIdx = 1,  Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
    // Row 2: SpA | SpD | Spd
    Row(modifier = Modifier.fillMaxWidth()) {
        StatCell("SpA", pokemon.spAtk, nature, statIdx = 2, Modifier.weight(1f))
        StatCell("SpD", pokemon.spDef, nature, statIdx = 3, Modifier.weight(1f))
        StatCell("Spe", pokemon.speed, nature, statIdx = 4, Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
    Row {
        Text("BST", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text("${pokemon.bst}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCell(label: String, value: Int, nature: NatureInfo, statIdx: Int, modifier: Modifier) {
    val valueColor = when {
        statIdx >= 0 && statIdx == nature.boostedStat -> Color(0xFF4CAF50)  // green = boosted
        statIdx >= 0 && statIdx == nature.reducedStat -> Color(0xFFFF6B6B)  // red = reduced
        else                                          -> TextPrimary
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label,   color = TextSecondary, fontSize = 11.sp)
        Text("$value", color = valueColor,   fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Move table ────────────────────────────────────────────────────────────────
@Composable
private fun MoveTable(moves: List<MoveData>, battle: BattleState, onClick: (MoveData) -> Unit) {
    // Column header
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Cat", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(22.dp))
        Text("Move", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("Pwr", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(34.dp))
        Text("Eff", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp))
        Text("PP",  color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp))
    }
    Spacer(Modifier.height(2.dp))
    moves.forEach { move ->
        MoveTableRow(move, battle, onClick = { onClick(move) })
    }
}

@Composable
private fun MoveTableRow(move: MoveData, battle: BattleState, onClick: () -> Unit) {
    val effectiveness = battle.enemy?.let { enemy ->
        TypeChart.effectiveness(move.moveType, enemy.type1, enemy.type2)
    }
    val cat      = moveCategory(move.moveType, move.power)
    val catColor = moveCategoryColor(move.moveType, move.power)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category
        Text(
            text = cat, color = catColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp),
        )
        // Type dot + name
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(typeColor(move.moveType), CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = move.moveName, color = TextPrimary, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // Power
        Text(
            text = if (move.power > 0) "${move.power}" else "—",
            color = TextSecondary, fontSize = 14.sp,
            modifier = Modifier.width(34.dp),
        )
        // Effectiveness arrows
        val (effText, effColor) = when (effectiveness) {
            null  -> "" to TextSecondary
            0.0f  -> "✕"  to Color(0xFF888888)
            0.25f -> "▼▼" to Color(0xFFFF4444)
            0.5f  -> "▼"  to Color(0xFFFF8866)
            1.0f  -> ""   to TextSecondary
            2.0f  -> "▲"  to Color(0xFF66CC44)
            4.0f  -> "▲▲" to Color(0xFF00DD44)
            else  -> ""   to TextSecondary
        }
        Text(
            text = effText, color = effColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp),
        )
        // PP
        Text(
            text = "${move.pp}", color = TextSecondary, fontSize = 14.sp,
            modifier = Modifier.width(28.dp),
        )
    }
}

// ── Battle panel ──────────────────────────────────────────────────────────────
@Composable
private fun BattlePanel(battle: BattleState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A0E20), RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (battle.isWild) "WILD" else "TRAINER",
                color = AccentRed, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            if (battle.weather != Weather.NONE) {
                Spacer(Modifier.width(6.dp))
                Text(battle.weather.displayName, color = AccentBlue, fontSize = 14.sp)
            }
        }

        battle.enemy?.let { enemy ->
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${enemy.name} Lv.${enemy.level}",
                    color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TypeChip(enemy.type1)
                    if (enemy.type2 != enemy.type1) TypeChip(enemy.type2)
                }
            }
            HpBar(enemy.hpPercent, enemy.currentHp, enemy.maxHp)
            Spacer(Modifier.height(1.dp))
            Text("BST: ${enemy.bst}", color = TextSecondary, fontSize = 12.sp)
            if (enemy.revealedMoveIds.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("Moves:", color = TextSecondary, fontSize = 12.sp)
                enemy.revealedMoveIds.forEach { moveId ->
                    Text(
                        "  • ${MoveNames.get(moveId)}",
                        color = TextSecondary, fontSize = 12.sp,
                    )
                }
            }
        }

        // Side conditions
        if (battle.playerReflect > 0 || battle.playerLightScreen > 0 ||
            battle.enemySpikes > 0 || battle.playerSafeguard > 0) {
            Spacer(Modifier.height(3.dp))
            Divider(color = Color(0xFF303050), thickness = 0.5.dp)
            Spacer(Modifier.height(2.dp))
            if (battle.playerReflect > 0)     Text("Reflect (${battle.playerReflect}t)", color = TextSecondary, fontSize = 12.sp)
            if (battle.playerLightScreen > 0) Text("Light Screen (${battle.playerLightScreen}t)", color = TextSecondary, fontSize = 12.sp)
            if (battle.enemySpikes > 0)       Text("Spikes ×${battle.enemySpikes}", color = TextSecondary, fontSize = 12.sp)
            if (battle.playerSafeguard > 0)   Text("Safeguard (${battle.playerSafeguard}t)", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────
@Composable
private fun HpBar(pokemon: PokemonData) {
    HpBar(pokemon.hpPercent, pokemon.currentHp, pokemon.maxHp)
}

@Composable
private fun HpBar(pct: Float, current: Int, max: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = pct, color = hpColor(pct), trackColor = Color(0xFF303050),
            modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(4.dp))
        Text("$current/$max", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun TypeChip(typeId: Int, small: Boolean = false) {
    val name = TypeChart.typeName(typeId)
    Box(
        modifier = Modifier
            .background(typeColor(typeId).copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            .padding(horizontal = if (small) 4.dp else 6.dp, vertical = if (small) 1.dp else 2.dp),
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = if (small) 11.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun hpColor(pct: Float): Color = when {
    pct > 0.5f -> HpHigh
    pct > 0.2f -> HpMid
    else        -> HpLow
}

// ── Move detail sheet ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDetailSheet(move: MoveData, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(move.moveName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                TypeChip(move.moveType)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Power: ${if (move.power > 0) move.power.toString() else "—"}",
                    color = TextSecondary, fontSize = 13.sp)
                Text("Acc: ${if (move.accuracy > 0) "${move.accuracy}%" else "—"}",
                    color = TextSecondary, fontSize = 13.sp)
                Text("PP: ${move.pp}", color = TextSecondary, fontSize = 13.sp)
            }
            val desc = MoveDescTable.get(move.moveId)
            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(desc, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Ability detail sheet ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbilityDetailSheet(abilityId: Int, onDismiss: () -> Unit) {
    val info = AbilityTable.get(abilityId)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(16.dp)) {
            Text(info.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(info.desc, color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Type defense sheet ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeDefenseSheet(type1: Int, type2: Int, onDismiss: () -> Unit) {
    val chart = TypeChart.defenseChart(type1, type2)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text("Type Defenses: ", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TypeChip(type1); Spacer(Modifier.width(4.dp))
                if (type2 != type1) TypeChip(type2)
            }
            Spacer(Modifier.height(8.dp))
            chart.entries.sortedBy { it.value }.forEach { (typeId, mult) ->
                val multStr = when (mult) {
                    0.0f  -> "0×"
                    0.25f -> "¼×"
                    0.5f  -> "½×"
                    2.0f  -> "2×"
                    4.0f  -> "4×"
                    else  -> "${mult}×"
                }
                val multColor = when {
                    mult == 0.0f -> Color(0xFF888888)
                    mult < 1.0f  -> AccentBlue
                    mult > 1.0f  -> Color(0xFFFF8C00)
                    else         -> TextSecondary
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeChip(typeId)
                    Spacer(Modifier.weight(1f))
                    Text(multStr, color = multColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
