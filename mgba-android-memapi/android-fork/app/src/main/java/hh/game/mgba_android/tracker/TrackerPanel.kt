package hh.game.mgba_android.tracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.landscapist.glide.GlideImage
import hh.game.mgba_android.tracker.data.BagDetailInfo
import hh.game.mgba_android.tracker.data.GameStats
import hh.game.mgba_android.tracker.data.LearnsetInfo
import hh.game.mgba_android.tracker.models.*
import hh.game.mgba_android.tracker.tables.*
import hh.game.mgba_android.tracker.tables.MoveDescTable
import hh.game.mgba_android.tracker.tables.RouteEncounterSlots
import hh.game.mgba_android.tracker.tables.RouteNames
import hh.game.mgba_android.tracker.tables.SpeciesNames
import kotlinx.coroutines.launch

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
    10 to Color(0xFFF08030),  // Fire     (0x0A)
    11 to Color(0xFF6890F0),  // Water    (0x0B)
    12 to Color(0xFF78C850),  // Grass    (0x0C)
    13 to Color(0xFFF8D030),  // Electric (0x0D)
    14 to Color(0xFFF85888),  // Psychic  (0x0E) — confirmed via PokemonData.TypeIndexMap
    15 to Color(0xFF98D8D8),  // Ice      (0x0F)
    16 to Color(0xFF7038F8),  // Dragon   (0x10)
    17 to Color(0xFF705848),  // Dark     (0x11)
)

private fun typeColor(typeId: Int) = TYPE_COLORS[typeId] ?: Color(0xFF888888)

// ── Move category (Gen III: type-based, matching MoveData.lua lines 103–119) ─
// Gen III move category split: physical = types 0–8, special = types 10–17
// Source: PokemonData.TypeIndexMap in Lua tracker (0x0A=Fire is special, no type 9 in base stats)
private val PHYSICAL_TYPES = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
private val SPECIAL_TYPES  = setOf(10, 11, 12, 13, 14, 15, 16, 17)

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
// Stat markings key: Pair(speciesId, statKey) → state 0–3
// Matches Lua Constants.STAT_STATES: 0=blank, 1="+", 2="--", 3="="
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivePanel(state: TrackerState.Active, onQuickload: (() -> Unit)?) {
    val statMarkings: SnapshotStateMap<Pair<Int, String>, Int> = remember { mutableStateMapOf() }

    PanelHeader(state)

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

    // Three-tab pager: MY MON | ROUTE | OPPONENT
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = HeaderBg,
        contentColor = TextPrimary,
    ) {
        Tab(
            selected = pagerState.currentPage == 0,
            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
        ) {
            Text(
                "MY MON",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Tab(
            selected = pagerState.currentPage == 1,
            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
        ) {
            Text(
                "OPPONENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Tab(
            selected = pagerState.currentPage == 2,
            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
        ) {
            Text(
                "ROUTES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                state.leadPokemon?.let { lead ->
                    MainView(lead, state.battle, state.stats, state.bagDetail, state.playerLearnset)
                } ?: StatusText("Party empty")
            }
            1 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (state.battle.isActive) {
                    EnemyView(state.battle, statMarkings, state.enemyLearnset)
                } else {
                    StatusText("Not in battle")
                }
            }
            else -> RouteView(state)
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
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Run ${state.runAttempts + 1}",
            color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        )
    }
}

// ── Main view ─────────────────────────────────────────────────────────────────
@Composable
private fun MainView(pokemon: PokemonData, battle: BattleState, stats: GameStats? = null, bagDetail: BagDetailInfo? = null, learnset: LearnsetInfo? = null) {
    var showMoveSheet by remember { mutableStateOf<MoveData?>(null) }
    var showAbilitySheet by remember { mutableStateOf(false) }
    var showDefenseSheet by remember { mutableStateOf(false) }
    var showLearnsetSheet by remember { mutableStateOf(false) }
    var showIvSheet by remember { mutableStateOf(false) }
    var showBagSheet by remember { mutableStateOf(false) }

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
                modifier = Modifier.size(48.dp),
                failure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("#${pokemon.speciesId}", color = TextSecondary, fontSize = 10.sp)
                    }
                },
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append(pokemon.displayName)
                            when (pokemon.gender) {
                                Gender.MALE   -> append(" ♂")
                                Gender.FEMALE -> append(" ♀")
                                else          -> {}
                            }
                            if (pokemon.isShiny) append(" ✦")
                            if (pokemon.hasPokerus) append(" ✚")
                        },
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    StatusBadge(pokemon.statusCondition)
                    Spacer(Modifier.width(4.dp))
                    Text("Lv.${pokemon.level}", color = TextSecondary, fontSize = 13.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.clickable { showDefenseSheet = true },
                ) {
                    TypeChip(pokemon.type1, small = true)
                    if (pokemon.type2 != pokemon.type1) TypeChip(pokemon.type2, small = true)
                }
                Spacer(Modifier.height(2.dp))
                HpBar(pokemon)
                // Heals in bag — matches Lua TrackerScreen "Heals:" display (%.0f%% HP (N))
                if (bagDetail != null && bagDetail.hpHealCount > 0) {
                    Text(
                        text = "Heals: %.0f%% HP (%d)".format(bagDetail.hpHealPercent, bagDetail.hpHealCount),
                        color = TextSecondary, fontSize = 11.sp,
                        modifier = Modifier.clickable { showBagSheet = true },
                    )
                }
            }
        }

        // Steps / battles / centers — shown right below the sprite row
        stats?.let { gs ->
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("%,d steps".format(gs.steps), color = TextSecondary, fontSize = 11.sp)
                Text("${gs.totalBattles} battles", color = TextSecondary, fontSize = 11.sp)
                Text("${gs.pokemonCenterVisits} centers", color = TextSecondary, fontSize = 11.sp)
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

        // Learnset row + BST + evo level (Lua: "Moves X/Y (nextLevel)")
        LearnsetRow(learnset, pokemon.level, pokemon.bst, EvolutionLevel.get(pokemon.speciesId),
            onLearnsetTap = if (learnset != null) {{ showLearnsetSheet = true }} else null)

        Spacer(Modifier.height(4.dp))
        Divider(color = Color(0xFF303050), thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))

        // Player stat stages — shown when in battle and any stage differs from neutral (6)
        if (battle.isActive && battle.playerStatStages != null) {
            val stageLabels = listOf("Atk", "Def", "SpA", "SpD", "Spe", "Acc", "Eva")
            val anyChanged = battle.playerStatStages.any { it != 6 }
            if (anyChanged) {
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    battle.playerStatStages.forEachIndexed { i, stage ->
                        val delta = stage - 6
                        if (delta != 0) {
                            val (stageText, stageColor) = if (delta > 0)
                                "+$delta" to Color(0xFF4CAF50)
                            else
                                "$delta" to Color(0xFFFF6B6B)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stageLabels[i], color = TextSecondary, fontSize = 9.sp)
                                Text(stageText, color = stageColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.clickable { showIvSheet = true }) {
            StatsTable(pokemon)
        }

        Spacer(Modifier.height(4.dp))
        Divider(color = Color(0xFF303050), thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))

        MoveTable(pokemon.moves, battle, stabTypes = setOf(pokemon.type1, pokemon.type2), onClick = { showMoveSheet = it })
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
    if (showLearnsetSheet && learnset != null) {
        LearnsetSheet(learnset, pokemon.level, onDismiss = { showLearnsetSheet = false })
    }
    if (showIvSheet) {
        IVStatSheet(pokemon, onDismiss = { showIvSheet = false })
    }
    if (showBagSheet && bagDetail != null) {
        BagDetailSheet(bagDetail, onDismiss = { showBagSheet = false })
    }
}

// ── Route view (ROUTE tab) ────────────────────────────────────────────────────
// Shows all wild Pokemon encountered this run, organized by route.
// Matches Lua tracker Tracker.Data.encounterTable discovery mechanic.
@Composable
private fun RouteView(state: TrackerState.Active) {
    val isHoenn = state.game == GameVersion.RUBY || state.game == GameVersion.SAPPHIRE || state.game == GameVersion.EMERALD
    val currentMapId = state.currentRoute?.mapLayoutId

    // Pre-filter to routes that have at least one encounter — avoid early returns inside Compose lambdas
    val allMapIds = state.routeEncounters.keys
        .filter { state.routeEncounters[it]?.isNotEmpty() == true }
    val routesWithEncounters = buildList {
        // Current route first (if it has encounters)
        currentMapId?.let { cur ->
            state.routeEncounters[cur]?.takeIf { it.isNotEmpty() }?.let { add(cur to it) }
        }
        // Remaining routes sorted numerically
        allMapIds
            .filter { it != currentMapId }
            .sorted()
            .forEach { mapId -> add(mapId to state.routeEncounters[mapId]!!) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (routesWithEncounters.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "No encounters recorded yet.\nWild battles will appear here.",
                color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.padding(4.dp),
            )
        } else {
            routesWithEncounters.forEach { (mapId, encounters) ->
                val routeName = RouteNames.get(mapId, isHoenn)
                val isCurrent = mapId == currentMapId

                // Route header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = routeName,
                        color = if (isCurrent) AccentBlue else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCurrent) {
                        Text("◄", color = AccentBlue, fontSize = 10.sp)
                    }
                }

                // Species grid: pad to the route's defined encounter slot count (from
                // RouteData.lua), showing "?" for undiscovered slots like the Lua tracker.
                val definedSlots = RouteEncounterSlots.get(mapId, isHoenn)
                val totalSlots = maxOf(encounters.size, definedSlots)
                val slots = List(totalSlots) { i -> if (i < encounters.size) encounters[i] else -1 }
                slots.chunked(3).forEach { rowSlots ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowSlots.forEach { speciesId ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (speciesId > 0) {
                                    GlideImage(
                                        imageModel = { "file:///android_asset/sprites/$speciesId.gif" },
                                        modifier = Modifier.size(36.dp),
                                        failure = {
                                            Box(
                                                Modifier.size(36.dp).background(CardBg, RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center,
                                            ) { Text("#$speciesId", color = TextSecondary, fontSize = 8.sp) }
                                        },
                                    )
                                    Text(
                                        text = SpeciesNames.get(speciesId),
                                        color = TextPrimary, fontSize = 9.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                } else {
                                    // Undiscovered slot — "?" like Lua tracker
                                    Box(
                                        Modifier.size(36.dp).background(Color(0xFF1E2A3A), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("?", color = Color(0xFF445566), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                    Text("???", color = Color(0xFF445566), fontSize = 9.sp)
                                }
                            }
                        }
                        // Pad row to 3 if last row is short
                        val padding = 3 - rowSlots.size
                        if (padding > 0) { repeat(padding) { Spacer(Modifier.weight(1f)) } }
                    }
                }

                Divider(
                    color = Color(0xFF303050), thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Enemy view (OPPONENT tab) ─────────────────────────────────────────────────
// Stat marking states match Lua Constants.STAT_STATES:
//   0=blank, 1="+" (green), 2="--" (red), 3="=" (gray)
// Cycle: (state + 1) % 4  (matches Lua: statState = (statState + 1) % 4)
@Composable
private fun EnemyView(
    battle: BattleState,
    statMarkings: SnapshotStateMap<Pair<Int, String>, Int>,
    learnset: LearnsetInfo? = null,
) {
    val enemy = battle.enemy
    if (enemy == null) {
        StatusText("No enemy data")
        return
    }

    var showMoveSheet by remember { mutableStateOf<MoveData?>(null) }
    var showDefenseSheet by remember { mutableStateOf(false) }
    var showLearnsetSheet by remember { mutableStateOf(false) }

    // Stat keys matching Lua tracker (Constants.lua STAT_STATES)
    val statKeys = listOf("atk", "def", "spa", "spd", "spe", "acc", "eva")
    val statLabels = listOf("Atk", "Def", "SpA", "SpD", "Spe", "Acc", "Eva")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        // Header row: sprite + name/level/types/HP (mirrors MainView layout exactly)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            GlideImage(
                imageModel = { "file:///android_asset/sprites/${enemy.speciesId}.gif" },
                modifier = Modifier.size(48.dp),
                failure = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("#${enemy.speciesId}", color = TextSecondary, fontSize = 12.sp)
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = enemy.name,
                        color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text("Lv.${enemy.level}", color = TextSecondary, fontSize = 16.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.clickable { showDefenseSheet = true },
                ) {
                    TypeChip(enemy.type1)
                    if (enemy.type2 != enemy.type1) TypeChip(enemy.type2)
                }
                Spacer(Modifier.height(2.dp))
                HpBar(enemy.hpPercent, enemy.currentHp, enemy.maxHp)
            }
        }
        Spacer(Modifier.height(2.dp))
        // Learnset row + BST + evo level below header row (Lua: "Moves X/Y (nextLevel)")
        LearnsetRow(learnset, enemy.level, enemy.bst, EvolutionLevel.get(enemy.speciesId),
            onLearnsetTap = if (learnset != null) {{ showLearnsetSheet = true }} else null)

        Spacer(Modifier.height(4.dp))

        // Battle type + weather
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = if (battle.isWild) "WILD" else "TRAINER",
                    color = AccentRed, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
                if (!battle.isWild && battle.trainerOpponentId != 0) {
                    Text("Trainer #${battle.trainerOpponentId}", color = TextSecondary, fontSize = 11.sp)
                }
            }
            if (battle.weather != Weather.NONE) {
                Spacer(Modifier.width(6.dp))
                Text(battle.weather.displayName, color = AccentBlue, fontSize = 14.sp)
            }
        }

        // Side conditions
        if (battle.playerReflect > 0 || battle.playerLightScreen > 0 ||
            battle.enemySpikes > 0 || battle.playerSafeguard > 0) {
            Spacer(Modifier.height(2.dp))
            if (battle.playerReflect > 0)     Text("Reflect (${battle.playerReflect}t)", color = TextSecondary, fontSize = 12.sp)
            if (battle.playerLightScreen > 0) Text("Light Screen (${battle.playerLightScreen}t)", color = TextSecondary, fontSize = 12.sp)
            if (battle.enemySpikes > 0)       Text("Spikes ×${battle.enemySpikes}", color = TextSecondary, fontSize = 12.sp)
            if (battle.playerSafeguard > 0)   Text("Safeguard (${battle.playerSafeguard}t)", color = TextSecondary, fontSize = 12.sp)
        }

        // Revealed moves
        if (enemy.revealedMoveIds.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Divider(color = Color(0xFF303050), thickness = 0.5.dp)
            Spacer(Modifier.height(4.dp))
            Text("Revealed Moves:", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            enemy.revealedMoveIds.forEach { moveId ->
                val stats = MoveStatsTable.get(moveId)
                val moveTypeId = stats.type
                val movePower  = stats.power
                val moveAcc    = stats.accuracy
                val movePp     = stats.pp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMoveSheet = MoveData(
                                moveId   = moveId,
                                moveName = MoveNames.get(moveId),
                                pp       = movePp,
                                maxPp    = movePp,
                                power    = movePower,
                                accuracy = moveAcc,
                                moveType = moveTypeId,
                            )
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(typeColor(moveTypeId), CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    val ppText = enemy.ppByMoveId[moveId]?.let { " (${it}PP)" } ?: ""
                    Text(MoveNames.get(moveId) + ppText, color = TextPrimary, fontSize = 14.sp)
                }
            }
        }

        // ── Stat markings ─────────────────────────────────────────────────────
        // Matches Lua Constants.STAT_STATES; persists per species within session
        Spacer(Modifier.height(4.dp))
        Divider(color = Color(0xFF303050), thickness = 0.5.dp)
        Spacer(Modifier.height(4.dp))
        Text("Stat Markings:", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))

        // Row 1: Atk | Def | SpA  (3 entries)
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0..2) {
                val key = statKeys[i]
                val label = statLabels[i]
                val markKey = enemy.speciesId to key
                StatMarkingCell(
                    label = label,
                    state = statMarkings[markKey] ?: 0,
                    onTap = {
                        val cur = statMarkings[markKey] ?: 0
                        statMarkings[markKey] = (cur + 1) % 4
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // Row 2: SpD | Spe | Acc | Eva  (4 entries)
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 3..6) {
                val key = statKeys[i]
                val label = statLabels[i]
                val markKey = enemy.speciesId to key
                StatMarkingCell(
                    label = label,
                    state = statMarkings[markKey] ?: 0,
                    onTap = {
                        val cur = statMarkings[markKey] ?: 0
                        statMarkings[markKey] = (cur + 1) % 4
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    showMoveSheet?.let { move ->
        MoveDetailSheet(move, onDismiss = { showMoveSheet = null })
    }
    if (showDefenseSheet) {
        TypeDefenseSheet(enemy.type1, enemy.type2, onDismiss = { showDefenseSheet = false })
    }
    if (showLearnsetSheet && learnset != null) {
        LearnsetSheet(learnset, enemy.level, onDismiss = { showLearnsetSheet = false })
    }
}

// ── Stat marking cell ─────────────────────────────────────────────────────────
// 4 states matching Lua Constants.STAT_STATES:
//   0=blank (gray), 1="+" (green), 2="--" (red), 3="=" (gray)
@Composable
private fun StatMarkingCell(label: String, state: Int, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val (text, color) = when (state) {
        1    -> "+"  to Color(0xFF4CAF50)  // boosted — green
        2    -> "--" to Color(0xFFFF6B6B)  // dropped — red
        3    -> "="  to TextSecondary      // neutral — gray
        else -> " "  to TextSecondary      // blank
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Box(
            modifier = Modifier
                .background(Color(0xFF2A3550), RoundedCornerShape(4.dp))
                .clickable { onTap() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .defaultMinSize(minWidth = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Stats table (1 row × 6 cols) ─────────────────────────────────────────────
@Composable
private fun StatsTable(pokemon: PokemonData) {
    val nature = NatureTable.get(pokemon.nature)
    Row(modifier = Modifier.fillMaxWidth()) {
        StatCell("HP",  pokemon.maxHp,   nature, statIdx = -1, Modifier.weight(1f))
        StatCell("Atk", pokemon.attack,  nature, statIdx = 0,  Modifier.weight(1f))
        StatCell("Def", pokemon.defense, nature, statIdx = 1,  Modifier.weight(1f))
        StatCell("SpA", pokemon.spAtk,   nature, statIdx = 2,  Modifier.weight(1f))
        StatCell("SpD", pokemon.spDef,   nature, statIdx = 3,  Modifier.weight(1f))
        StatCell("Spe", pokemon.speed,   nature, statIdx = 4,  Modifier.weight(1f))
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

// ── Move category icon ────────────────────────────────────────────────────────
// Pixel art bitmaps from Lua tracker Constants.PixelImages.PHYSICAL / .SPECIAL (7×7 grids)
// Drawn via Canvas, each pixel rendered as a small square at the given color.
private val PHYSICAL_PIXELS = arrayOf(
    intArrayOf(1,0,0,1,0,0,1),
    intArrayOf(0,1,0,1,0,1,0),
    intArrayOf(0,0,1,1,1,0,0),
    intArrayOf(1,1,1,1,1,1,1),
    intArrayOf(0,0,1,1,1,0,0),
    intArrayOf(0,1,0,1,0,1,0),
    intArrayOf(1,0,0,1,0,0,1),
)
private val SPECIAL_PIXELS = arrayOf(
    intArrayOf(0,0,1,1,1,0,0),
    intArrayOf(0,1,0,0,0,1,0),
    intArrayOf(1,0,0,1,0,0,1),
    intArrayOf(1,0,1,0,1,0,1),
    intArrayOf(1,0,0,1,0,0,1),
    intArrayOf(0,1,0,0,0,1,0),
    intArrayOf(0,0,1,1,1,0,0),
)

@Composable
private fun MoveCategoryIcon(typeId: Int, power: Int, modifier: Modifier = Modifier) {
    val pixels: Array<IntArray>?
    val color: Color
    when {
        power == 0               -> { pixels = null; color = Color.Transparent }
        typeId in PHYSICAL_TYPES -> { pixels = PHYSICAL_PIXELS; color = Color(0xFFF08030) }
        typeId in SPECIAL_TYPES  -> { pixels = SPECIAL_PIXELS;  color = Color(0xFF6890F0) }
        else                     -> { pixels = null; color = Color.Transparent }
    }
    Canvas(modifier = modifier) {
        if (pixels == null) return@Canvas
        val cellW = size.width  / 7f
        val cellH = size.height / 7f
        for (row in 0..6) {
            for (col in 0..6) {
                if (pixels[row][col] == 1) {
                    drawRect(
                        color = color,
                        topLeft = Offset(col * cellW, row * cellH),
                        size = Size(cellW, cellH),
                    )
                }
            }
        }
    }
}

// ── Move table ────────────────────────────────────────────────────────────────
@Composable
private fun MoveTable(moves: List<MoveData>, battle: BattleState, stabTypes: Set<Int> = emptySet(), onClick: (MoveData) -> Unit) {
    // Column header
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(18.dp))  // category icon column
        Text("Move", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("Pwr", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
        Text("Eff", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
        Text("Acc", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
        Text("PP",  color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
    }
    Spacer(Modifier.height(2.dp))
    moves.forEach { move ->
        MoveTableRow(move, battle, isStab = move.moveType in stabTypes, onClick = { onClick(move) })
    }
}

@Composable
private fun MoveTableRow(move: MoveData, battle: BattleState, isStab: Boolean = false, onClick: () -> Unit) {
    val effectiveness = battle.enemy?.let { enemy ->
        TypeChart.effectiveness(move.moveType, enemy.type1, enemy.type2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category icon (physical starburst / special diamond, matching Lua Constants.PixelImages)
        MoveCategoryIcon(
            typeId = move.moveType,
            power  = move.power,
            modifier = Modifier.size(width = 14.dp, height = 14.dp),
        )
        Spacer(Modifier.width(4.dp))
        // Type dot + name
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(typeColor(move.moveType), CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = move.moveName,
                color = if (isStab) Color(0xFF4CAF50) else TextPrimary,
                fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // Power
        Text(
            text = if (move.power > 0) "${move.power}" else "—",
            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        // Effectiveness arrows
        val (effText, effColor) = when (effectiveness) {
            null  -> "" to TextSecondary
            0.0f  -> "✕"  to Color(0xFF888888)
            0.25f -> "⇊"  to Color(0xFFFF4444)
            0.5f  -> "↓"  to Color(0xFFFF8866)
            1.0f  -> ""   to TextSecondary
            2.0f  -> "↑"  to Color(0xFF66CC44)
            4.0f  -> "⇈"  to Color(0xFF00DD44)
            else  -> ""   to TextSecondary
        }
        Text(
            text = effText, color = effColor, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.width(22.dp),
        )
        // Accuracy
        Text(
            text = if (move.accuracy > 0) "${move.accuracy}" else "—",
            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        // PP
        Text(
            text = "${move.pp}", color = TextSecondary, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.width(24.dp),
        )
    }
}

// ── Learnset row (Lua: "Moves X/Y (nextLevel)") ──────────────────────────────
// Shows learned/total counts + next move level (tappable), evo level, BST right-aligned.
// Next move level turns yellow when 1 level away; evo level turns yellow when ≤2 away.
@Composable
private fun LearnsetRow(learnset: LearnsetInfo?, currentLevel: Int, bst: Int, evoLevel: Int = 0, onLearnsetTap: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (learnset != null) {
            Text(
                text = "Moves ${learnset.learnedCount}/${learnset.totalCount}",
                color = TextSecondary, fontSize = 12.sp,
            )
            if (!learnset.allLearned) {
                Spacer(Modifier.width(4.dp))
                val nextSoon = learnset.nextMoveLevel <= currentLevel + 1
                val levelColor = if (nextSoon) Color(0xFFFFEB3B) else TextSecondary
                val tapMod = if (onLearnsetTap != null)
                    Modifier.weight(1f).clickable { onLearnsetTap() }
                else
                    Modifier.weight(1f)
                Text(
                    text = "(Lv.${learnset.nextMoveLevel})",
                    color = levelColor, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = tapMod,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (evoLevel > 0) {
            val evoSoon = currentLevel >= evoLevel - 2
            val evoColor = if (evoSoon) Color(0xFFFFEB3B) else TextSecondary
            Text("Evo ", color = TextSecondary, fontSize = 12.sp)
            Text("Lv.$evoLevel", color = evoColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("  ", color = TextSecondary, fontSize = 12.sp)
        }
        Text("BST ", color = TextSecondary, fontSize = 12.sp)
        Text("$bst", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

// ── Learnset sheet ────────────────────────────────────────────────────────────
// Shows all level-up move levels. Past levels grayed, next level yellow, future white.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LearnsetSheet(learnset: LearnsetInfo, currentLevel: Int, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(16.dp)) {
            Text("Level-up Moves", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                learnset.allMoveLevels.forEach { lvl ->
                    val color = when {
                        lvl <= currentLevel -> TextSecondary
                        lvl == learnset.nextMoveLevel -> Color(0xFFFFEB3B)
                        else -> TextPrimary
                    }
                    Text(
                        text = "Lv.$lvl",
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = if (lvl == learnset.nextMoveLevel) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
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

// ── Status badge ──────────────────────────────────────────────────────────────
// status byte bit layout: bits 0-2=sleep turns, 3=PSN, 4=BRN, 5=FRZ, 6=PAR, 7=TOX
@Composable
private fun StatusBadge(status: Int) {
    if (status == 0) return
    val (label, color) = when {
        status and 0x07 != 0 -> "SLP ${status and 0x07}" to Color(0xFF888888)
        status and 0x08 != 0 -> "PSN"  to Color(0xFFA040A0)
        status and 0x10 != 0 -> "BRN"  to Color(0xFFE07030)
        status and 0x20 != 0 -> "FRZ"  to Color(0xFF60C8C8)
        status and 0x40 != 0 -> "PAR"  to Color(0xFFE0D000)
        status and 0x80 != 0 -> "TOX"  to Color(0xFF602080)
        else -> return
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ── IV/EV/Friendship/Hidden Power sheet ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IVStatSheet(pokemon: PokemonData, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(16.dp)) {
            Text("Stats Detail", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Stat", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("IV",  color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                Text("EV",  color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp))
            }
            Spacer(Modifier.height(4.dp))

            val statRows = listOf(
                Triple("HP",  pokemon.ivHp,  pokemon.evHp),
                Triple("Atk", pokemon.ivAtk, pokemon.evAtk),
                Triple("Def", pokemon.ivDef, pokemon.evDef),
                Triple("SpA", pokemon.ivSpA, pokemon.evSpA),
                Triple("SpD", pokemon.ivSpD, pokemon.evSpD),
                Triple("Spe", pokemon.ivSpe, pokemon.evSpe),
            )
            statRows.forEach { (label, iv, ev) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(label, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("$iv", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(36.dp))
                    Text("$ev", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(44.dp))
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Friendship:", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${pokemon.friendship} / 255", color = TextPrimary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hidden Power:", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TypeChip(pokemon.hiddenPowerType)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Bag detail sheet ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagDetailSheet(detail: BagDetailInfo, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Bag Detail", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            BagSection("HP Items", detail.hpItems)
            BagSection("PP Items", detail.ppItems)
            BagSection("Status Items", detail.statusItems)
            BagSection("Battle Items", detail.battleItems)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BagSection(title: String, items: List<hh.game.mgba_android.tracker.data.BagItemEntry>) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    Text(title, color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    items.forEach { entry ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Text(entry.name, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("×${entry.quantity}", color = TextSecondary, fontSize = 13.sp)
        }
    }
}
