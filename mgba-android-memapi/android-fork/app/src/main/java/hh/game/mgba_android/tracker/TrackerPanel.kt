package hh.game.mgba_android.tracker

import androidx.compose.animation.core.animateFloatAsState
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
import hh.game.mgba_android.tracker.models.*
import hh.game.mgba_android.tracker.tables.*
import hh.game.mgba_android.tracker.tables.MoveDescTable

// ── Color palette ────────────────────────────────────────────────────────────
private val PanelBg      = Color(0xFF0F1621)
private val HeaderBg     = Color(0xFF16213E)
private val CardBg       = Color(0xFF1A2540)
private val AccentRed    = Color(0xFFE94560)
private val AccentBlue   = Color(0xFF4090FF)
private val TextPrimary  = Color(0xFFEEEEEE)
private val TextSecondary= Color(0xFFAAAAAA)
private val HpHigh       = Color(0xFF4CAF50)
private val HpMid        = Color(0xFFFFEB3B)
private val HpLow        = Color(0xFFF44336)

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

// ── Main entry point ─────────────────────────────────────────────────────────
@Composable
fun TrackerPanel(state: TrackerState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg),
    ) {
        when (state) {
            is TrackerState.Disconnected -> StatusText("Loading…")
            is TrackerState.NoGameLoaded -> StatusText("No supported game loaded")
            is TrackerState.Active       -> ActivePanel(state)
        }
    }
}

@Composable
private fun StatusText(msg: String) {
    Text(
        text = msg, color = TextSecondary, fontSize = 11.sp,
        modifier = Modifier.padding(12.dp),
    )
}

// ── Active panel ──────────────────────────────────────────────────────────────
@Composable
private fun ActivePanel(state: TrackerState.Active) {
    // Carousel page: 0=Main, 1=Stats, 2=Defenses. Auto-show battle view when active.
    var page by remember { mutableStateOf(0) }

    // Header
    PanelHeader(state)

    // Route name
    state.currentRoute?.let {
        Text(
            text = it.name, color = AccentBlue, fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Lead Pokémon carousel
        state.leadPokemon?.let { lead ->
            // Carousel tab indicator
            CarouselTabs(page) { page = it }
            Spacer(Modifier.height(2.dp))

            when (page) {
                0 -> MainView(lead, state.battle)
                1 -> StatsView(lead)
                2 -> DefensesView(lead)
            }
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
private fun PanelHeader(state: TrackerState.Active) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "IRONMON", color = AccentRed, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${state.game.displayName.removePrefix("Pokémon ")} v1.${state.romVersion}",
            color = TextSecondary, fontSize = 9.sp,
        )
    }
}

@Composable
private fun CarouselTabs(current: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Main", "Stats", "Def")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { i, label ->
            Box(
                modifier = Modifier
                    .background(
                        if (i == current) AccentBlue else CardBg,
                        RoundedCornerShape(4.dp),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = label, color = TextPrimary, fontSize = 9.sp,
                    fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ── Page 0: Main view ─────────────────────────────────────────────────────────
@Composable
private fun MainView(pokemon: PokemonData, battle: BattleState) {
    var showMoveSheet by remember { mutableStateOf<MoveData?>(null) }
    var showAbilitySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        // Name row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    append(pokemon.speciesName)
                    when (pokemon.gender) {
                        Gender.MALE -> append(" ♂")
                        Gender.FEMALE -> append(" ♀")
                        else -> {}
                    }
                    if (pokemon.isShiny) append(" ✦")
                    if (pokemon.hasPokerus) append(" ✚")
                },
                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text("Lv.${pokemon.level}", color = TextSecondary, fontSize = 10.sp)
        }

        // Types
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TypeChip(pokemon.type1)
            if (pokemon.type2 != pokemon.type1) TypeChip(pokemon.type2)
        }
        Spacer(Modifier.height(2.dp))

        // HP bar
        HpBar(pokemon)
        Spacer(Modifier.height(2.dp))

        // Ability
        val abilityName = AbilityTable.name(pokemon.abilityId)
        Text(
            text = "Ability: $abilityName",
            color = AccentBlue, fontSize = 9.sp,
            modifier = Modifier.clickable { showAbilitySheet = true },
        )

        // Held item
        val itemName = if (pokemon.heldItemId > 0) ItemTable.get(pokemon.heldItemId) else "None"
        Text(text = "Item: $itemName", color = TextSecondary, fontSize = 9.sp)

        // Nature
        val nature = NatureTable.get(pokemon.nature)
        val natureMod = NatureTable.modifier(pokemon.nature)
        Text(
            text = "Nature: ${nature.name}${if (natureMod.isNotEmpty()) " ($natureMod)" else ""}",
            color = TextSecondary, fontSize = 9.sp,
        )

        Spacer(Modifier.height(3.dp))
        Divider(color = Color(0xFF303050), thickness = 0.5.dp)
        Spacer(Modifier.height(3.dp))

        // Moves with effectiveness badges
        pokemon.moves.forEach { move ->
            MoveRow(move, battle, modifier = Modifier.clickable { showMoveSheet = move })
        }
    }

    Spacer(Modifier.height(4.dp))

    // Sheets
    showMoveSheet?.let { move ->
        MoveDetailSheet(move, onDismiss = { showMoveSheet = null })
    }
    if (showAbilitySheet) {
        AbilityDetailSheet(
            abilityId = pokemon.abilityId,
            onDismiss = { showAbilitySheet = false },
        )
    }
}

@Composable
private fun MoveRow(move: MoveData, battle: BattleState, modifier: Modifier = Modifier) {
    val effectiveness = battle.enemy?.let { enemy ->
        TypeChart.effectiveness(move.moveType, enemy.type1, enemy.type2)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(typeColor(move.moveType), CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = move.moveName, color = TextPrimary, fontSize = 9.sp,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (move.power > 0) {
            Text("${move.power}", color = TextSecondary, fontSize = 8.sp)
            Spacer(Modifier.width(2.dp))
        }
        Text("(${move.pp})", color = TextSecondary, fontSize = 8.sp)
        effectiveness?.let { eff ->
            if (eff != 1.0f) {
                Spacer(Modifier.width(3.dp))
                val effText = when {
                    eff == 0.0f -> "0×"
                    eff < 1.0f  -> "½×"
                    eff >= 4.0f -> "4×"
                    else        -> "2×"
                }
                val effColor = when {
                    eff == 0.0f -> Color(0xFF888888)
                    eff < 1.0f  -> Color(0xFFFF6B35)
                    else        -> Color(0xFF4CAF50)
                }
                Text(effText, color = effColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Page 1: Stats view ────────────────────────────────────────────────────────
@Composable
private fun StatsView(pokemon: PokemonData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Current Stats", color = TextSecondary, fontSize = 9.sp, modifier = Modifier.weight(1f))
            Text("BST: ${pokemon.bst}", color = TextSecondary, fontSize = 9.sp)
        }
        Spacer(Modifier.height(3.dp))

        StatBar("HP",  pokemon.maxHp,   Color(0xFFFF5555), pokemon.nature, -1)
        StatBar("Atk", pokemon.attack,  Color(0xFFF08030), pokemon.nature, 0, 0)
        StatBar("Def", pokemon.defense, Color(0xFFF8D030), pokemon.nature, 0, 1)
        StatBar("SpA", pokemon.spAtk,   Color(0xFF6890F0), pokemon.nature, 0, 2)
        StatBar("SpD", pokemon.spDef,   Color(0xFF78C850), pokemon.nature, 0, 3)
        StatBar("Spd", pokemon.speed,   Color(0xFFF85888), pokemon.nature, 0, 4)

        Spacer(Modifier.height(4.dp))
        // EXP bar
        val expPct = ExperienceTable.xpProgress(pokemon.expGroup, pokemon.level, pokemon.experience.toLong())
        Text("EXP", color = TextSecondary, fontSize = 8.sp)
        LinearProgressIndicator(
            progress = expPct,
            color = Color(0xFF8080FF),
            trackColor = Color(0xFF303050),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun StatBar(label: String, value: Int, barColor: Color, nature: Int, statIndex: Int, boostedStatIndex: Int = -1) {
    val natureMod = if (boostedStatIndex >= 0) {
        val info = NatureTable.get(nature)
        when (boostedStatIndex) {
            info.boostedStat -> 1.1f
            info.reducedStat -> 0.9f
            else -> 1.0f
        }
    } else 1.0f

    val displayColor = when {
        natureMod > 1.0f -> Color(0xFFFF6B6B)
        natureMod < 1.0f -> Color(0xFF6B9FFF)
        else -> barColor
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, fontSize = 8.sp, modifier = Modifier.width(28.dp))
        Text(
            "$value", color = displayColor, fontSize = 8.sp,
            modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold,
        )
        val frac = (value / 500f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = frac, color = displayColor, trackColor = Color(0xFF303050),
            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(2.dp)),
        )
    }
    Spacer(Modifier.height(1.dp))
}

// ── Page 2: Type defenses view ────────────────────────────────────────────────
@Composable
private fun DefensesView(pokemon: PokemonData) {
    var showSheet by remember { mutableStateOf(false) }

    val chart = TypeChart.defenseChart(pokemon.type1, pokemon.type2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .padding(8.dp)
            .clickable { showSheet = true },
    ) {
        Text("Type Defenses (tap for full chart)", color = TextSecondary, fontSize = 9.sp)
        Spacer(Modifier.height(4.dp))

        // Group by multiplier
        val immune   = chart.filter { it.value == 0.0f }
        val quarter  = chart.filter { it.value == 0.25f }
        val half     = chart.filter { it.value == 0.5f }
        val double   = chart.filter { it.value == 2.0f }
        val quad     = chart.filter { it.value >= 4.0f }

        if (immune.isNotEmpty()) DefenseRow("0×", Color(0xFF888888), immune.keys)
        if (quarter.isNotEmpty()) DefenseRow("¼×", Color(0xFFADD8E6), quarter.keys)
        if (half.isNotEmpty()) DefenseRow("½×", Color(0xFF87CEEB), half.keys)
        if (double.isNotEmpty()) DefenseRow("2×", Color(0xFFFF8C00), double.keys)
        if (quad.isNotEmpty()) DefenseRow("4×", Color(0xFFFF4500), quad.keys)
    }

    if (showSheet) {
        TypeDefenseSheet(pokemon.type1, pokemon.type2, onDismiss = { showSheet = false })
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun DefenseRow(label: String, labelColor: Color, types: Set<Int>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = labelColor, fontSize = 9.sp, modifier = Modifier.width(22.dp),
            fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            types.forEach { typeId ->
                TypeChip(typeId, small = true)
            }
        }
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
                color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            )
            if (battle.weather != Weather.NONE) {
                Spacer(Modifier.width(6.dp))
                Text(battle.weather.displayName, color = AccentBlue, fontSize = 9.sp)
            }
        }

        battle.enemy?.let { enemy ->
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${enemy.name} Lv.${enemy.level}",
                    color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TypeChip(enemy.type1)
                    if (enemy.type2 != enemy.type1) TypeChip(enemy.type2)
                }
            }
            HpBar(enemy.hpPercent, enemy.currentHp, enemy.maxHp)
            Spacer(Modifier.height(1.dp))
            Text("BST: ${enemy.bst}", color = TextSecondary, fontSize = 8.sp)
            if (enemy.revealedMoveIds.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("Moves:", color = TextSecondary, fontSize = 8.sp)
                enemy.revealedMoveIds.forEach { moveId ->
                    Text(
                        "  • ${hh.game.mgba_android.tracker.tables.MoveNames.get(moveId)}",
                        color = TextSecondary, fontSize = 8.sp,
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
            if (battle.playerReflect > 0)      Text("Reflect (${battle.playerReflect}t)", color = TextSecondary, fontSize = 8.sp)
            if (battle.playerLightScreen > 0)  Text("Light Screen (${battle.playerLightScreen}t)", color = TextSecondary, fontSize = 8.sp)
            if (battle.enemySpikes > 0)        Text("Spikes ×${battle.enemySpikes}", color = TextSecondary, fontSize = 8.sp)
            if (battle.playerSafeguard > 0)    Text("Safeguard (${battle.playerSafeguard}t)", color = TextSecondary, fontSize = 8.sp)
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
            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(4.dp))
        Text("$current/$max", color = TextSecondary, fontSize = 8.sp)
    }
}

@Composable
private fun TypeChip(typeId: Int, small: Boolean = false) {
    val name = TypeChart.typeName(typeId)
    Box(
        modifier = Modifier
            .background(typeColor(typeId).copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            .padding(horizontal = if (small) 3.dp else 5.dp, vertical = if (small) 1.dp else 2.dp),
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = if (small) 7.sp else 8.sp,
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
