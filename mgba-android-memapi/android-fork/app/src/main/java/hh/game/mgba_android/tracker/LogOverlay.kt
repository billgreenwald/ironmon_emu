package hh.game.mgba_android.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skydoves.landscapist.glide.GlideImage
import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.models.LogData
import hh.game.mgba_android.tracker.models.LogPokemon
import hh.game.mgba_android.tracker.models.LogRoute
import hh.game.mgba_android.tracker.models.LogTrainer
import hh.game.mgba_android.tracker.models.TrackerState
import hh.game.mgba_android.tracker.persistence.LogRepository
import hh.game.mgba_android.tracker.tables.TrainerGroups
import hh.game.mgba_android.tracker.tables.TypeChart

// ── Palette (kept local, mirrors TrackerPanel) ────────────────────────────────
private val LogBg       = Color(0xFF0F1621)
private val LogHeader    = Color(0xFF16213E)
private val LogCard      = Color(0xFF1A2540)
private val LogAccent    = Color(0xFF4090FF)
private val LogText      = Color(0xFFEEEEEE)
private val LogTextSec   = Color(0xFFAAAAAA)
private val StabHi       = Color(0xFF4CAF50)

private val LOG_TYPE_COLORS = mapOf(
    0 to Color(0xFFA8A878), 1 to Color(0xFFC03028), 2 to Color(0xFF8EB8E0), 3 to Color(0xFFA040A0),
    4 to Color(0xFFE0C068), 5 to Color(0xFFB8A038), 6 to Color(0xFFA8B820), 7 to Color(0xFF705898),
    8 to Color(0xFFB8B8D0), 10 to Color(0xFFF08030), 11 to Color(0xFF6890F0), 12 to Color(0xFF78C850),
    13 to Color(0xFFF8D030), 14 to Color(0xFFF85888), 15 to Color(0xFF98D8D8), 16 to Color(0xFF7038F8),
    17 to Color(0xFF705848), 18 to Color(0xFFEE99AC),
)
private fun logTypeColor(t: Int) = LOG_TYPE_COLORS[t] ?: Color(0xFF888888)

private fun spriteUrlLog(natDexId: Int) =
    if (natDexId >= 412) "file:///android_asset/sprites/$natDexId.png"
    else "file:///android_asset/sprites/$natDexId.gif"

private fun String.titlecaseWords(): String =
    split(" ").joinToString(" ") { w -> w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }

// Navigation within the overlay
private sealed interface LogNav {
    data class Pokemon(val id: Int) : LogNav
    data class Trainer(val num: Int) : LogNav
    data class Route(val mapId: Int) : LogNav
}

/**
 * Full-screen randomizer-log viewer, opened from the "Review Logs" banner on game over.
 * Mirrors the Lua LogOverlay: 5 tabs (Pokémon / Trainers / Routes / TMs / Misc) with
 * tappable detail sub-views. Parses the log lazily off the main thread.
 */
@Composable
fun LogViewerOverlay(state: TrackerState.Active, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val romFileName = remember { TrackerPoller.getRomFileName() }

    val result by produceState<LogRepository.Result?>(initialValue = null, romFileName) {
        value = LogRepository.getLog(context, romFileName, state.game, state.isMaxFr, state.isNatDex)
    }

    var tab by remember { mutableIntStateOf(0) }
    var nav by remember { mutableStateOf<LogNav?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(LogBg)) {
            Column(Modifier.fillMaxSize()) {
                // ── Header ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().background(LogHeader).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val data = (result as? LogRepository.Result.Success)?.data
                    val title = when {
                        nav != null -> "◀ Back"
                        else -> "Run Log" + (data?.settings?.randomSeed?.let { "  ·  Seed $it" } ?: "")
                    }
                    Text(
                        text = title,
                        color = LogText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).let {
                            if (nav != null) it.clickable { nav = null } else it
                        },
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("✕", color = LogText, fontSize = 18.sp)
                    }
                }

                when (val r = result) {
                    null -> LoadingBox("Reading log…")
                    is LogRepository.Result.NotFound -> LoadingBox("No log file found next to this ROM.")
                    is LogRepository.Result.ParseFailed -> LoadingBox("Could not parse the log file.")
                    is LogRepository.Result.Success -> {
                        val data = r.data
                        val currentNav = nav
                        if (currentNav != null) {
                            when (currentNav) {
                                is LogNav.Pokemon -> PokemonDetail(data, currentNav.id, state) { nav = it }
                                is LogNav.Trainer -> TrainerDetail(data, currentNav.num, state) { nav = it }
                                is LogNav.Route -> RouteDetail(data, currentNav.mapId, state) { nav = it }
                            }
                        } else {
                            // ── Tabs ────────────────────────────────────
                            val tabs = listOf("Pokémon", "Trainers", "Routes", "Gym TMs", "Misc")
                            ScrollableTabRow(
                                selectedTabIndex = tab,
                                containerColor = LogHeader,
                                contentColor = LogText,
                                edgePadding = 0.dp,
                            ) {
                                tabs.forEachIndexed { i, t ->
                                    Tab(selected = tab == i, onClick = { tab = i }) {
                                        Text(t, Modifier.padding(vertical = 8.dp, horizontal = 4.dp), fontSize = 12.sp)
                                    }
                                }
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                when (tab) {
                                    0 -> PokemonTab(data, state) { nav = LogNav.Pokemon(it) }
                                    1 -> TrainersTab(data, state.game) { nav = LogNav.Trainer(it) }
                                    2 -> RoutesTab(data) { nav = LogNav.Route(it) }
                                    3 -> TmsTab(data, state.game) { nav = LogNav.Trainer(it) }
                                    else -> MiscTab(data)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBox(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (msg.endsWith("…")) CircularProgressIndicator(color = LogTextSec)
            Spacer(Modifier.height(12.dp))
            Text(msg, color = LogTextSec, fontSize = 13.sp)
        }
    }
}

// ── Shared bits ───────────────────────────────────────────────────────────────
@Composable
private fun TypeChip(typeId: Int) {
    Box(
        Modifier.background(logTypeColor(typeId), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(TypeChart.typeName(typeId), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MonSprite(natDexId: Int, size: Int) {
    GlideImage(imageModel = { spriteUrlLog(natDexId) }, modifier = Modifier.size(size.dp))
}

// ═══════════════════════════════════ POKÉMON ═════════════════════════════════
private enum class PokeSort(val label: String) { DEX("Dex #"), NAME("Name"), BST("BST") }

@Composable
private fun PokemonTab(data: LogData, state: TrackerState.Active, onOpen: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(PokeSort.DEX) }

    val mons = remember(query, sort) {
        data.pokemon.values
            .filter { it.name.isNotEmpty() && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
            .sortedWith(
                when (sort) {
                    PokeSort.DEX -> compareBy { it.id }
                    PokeSort.NAME -> compareBy { it.name.lowercase() }
                    PokeSort.BST -> compareByDescending { it.bst }
                }
            )
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(query, { query = it }, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            SortDropdown(PokeSort.values().toList(), sort, { sort = it }) { it.label }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(mons, key = { it.id }) { mon ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(mon.id) }.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonSprite(state.natDexId(mon.id), 32)
                    Spacer(Modifier.width(6.dp))
                    Text(mon.name, color = LogText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    mon.type1?.let { TypeChip(it) }
                    mon.type2?.let { Spacer(Modifier.width(3.dp)); TypeChip(it) }
                    Spacer(Modifier.width(8.dp))
                    Text("${mon.bst}", color = LogTextSec, fontSize = 12.sp)
                }
                Divider(color = Color(0xFF223), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun PokemonDetail(data: LogData, id: Int, state: TrackerState.Active, onNav: (LogNav?) -> Unit) {
    val mon = data.pokemon[id] ?: return
    var moveTab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonSprite(state.natDexId(mon.id), 56)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(mon.name, color = LogText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row {
                    mon.type1?.let { TypeChip(it) }
                    mon.type2?.let { Spacer(Modifier.width(4.dp)); TypeChip(it) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Base stats
        SectionHeader("Base Stats  (BST ${mon.bst})")
        StatBar("HP", mon.baseStats.hp); StatBar("Attack", mon.baseStats.atk); StatBar("Defense", mon.baseStats.def)
        StatBar("Sp. Atk", mon.baseStats.spa); StatBar("Sp. Def", mon.baseStats.spd); StatBar("Speed", mon.baseStats.spe)

        // Abilities
        Spacer(Modifier.height(8.dp))
        SectionHeader("Abilities")
        Text(abilityName(mon.ability1, state), color = LogText, fontSize = 13.sp)
        mon.ability2?.takeIf { it > 0 }?.let { Text(abilityName(it, state), color = LogText, fontSize = 13.sp) }

        mon.heldItems?.let {
            Spacer(Modifier.height(8.dp)); SectionHeader("Held Items")
            Text(it.titlecaseWords(), color = LogText, fontSize = 13.sp)
        }

        // Evolutions / pre-evolutions
        if (mon.evolutions.isNotEmpty() || mon.preEvolutions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); SectionHeader("Evolutions")
            EvoRow("Evolves into", mon.evolutions, data, state, onNav)
            EvoRow("Evolves from", mon.preEvolutions, data, state, onNav)
        }

        // Moves
        Spacer(Modifier.height(10.dp))
        TabRow(selectedTabIndex = moveTab, containerColor = LogHeader, contentColor = LogText) {
            Tab(selected = moveTab == 0, onClick = { moveTab = 0 }) { Text("Level-up", Modifier.padding(8.dp), fontSize = 12.sp) }
            Tab(selected = moveTab == 1, onClick = { moveTab = 1 }) { Text("TM Moves", Modifier.padding(8.dp), fontSize = 12.sp) }
        }
        Spacer(Modifier.height(6.dp))
        if (moveTab == 0) {
            if (mon.moveSet.isEmpty()) Text("No level-up moves listed.", color = LogTextSec, fontSize = 12.sp)
            mon.moveSet.forEach { mv ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(if (mv.level == 0) "Evo" else "Lv ${mv.level}", color = LogTextSec, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                    Text(mv.name.titlecaseWords(), color = moveColor(mv.moveId, data, mon), fontSize = 13.sp)
                }
            }
        } else {
            if (mon.tmMoves.isEmpty()) Text("Learns no TMs.", color = LogTextSec, fontSize = 12.sp)
            mon.tmMoves.sorted().forEach { tmNum ->
                val tm = data.tms[tmNum]
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("TM%02d".format(tmNum), color = LogTextSec, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                    Text(tm?.name?.titlecaseWords() ?: "?", color = LogText, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun EvoRow(label: String, ids: List<Int>, data: LogData, state: TrackerState.Active, onNav: (LogNav?) -> Unit) {
    if (ids.isEmpty()) return
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = LogTextSec, fontSize = 12.sp)
        ids.forEach { eid ->
            val name = data.pokemon[eid]?.name ?: state.speciesName(eid)
            Row(
                Modifier.clickable { onNav(LogNav.Pokemon(eid)) }.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonSprite(state.natDexId(eid), 24)
                Text(name, color = LogAccent, fontSize = 12.sp)
            }
        }
    }
}

private fun moveColor(moveId: Int, data: LogData, mon: LogPokemon): Color {
    val mtype = data.moves[moveId]?.type ?: return LogText
    return if (mtype == mon.type1 || mtype == mon.type2) StabHi else LogText
}

private fun abilityName(id: Int, state: TrackerState.Active): String =
    hh.game.mgba_android.tracker.tables.AbilityTable.get(id, state.isMaxFr).name

@Composable
private fun StatBar(label: String, value: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LogTextSec, fontSize = 11.sp, modifier = Modifier.width(56.dp))
        Text("$value", color = LogText, fontSize = 11.sp, modifier = Modifier.width(32.dp))
        Box(Modifier.weight(1f).height(8.dp).background(Color(0xFF20304F), RoundedCornerShape(4.dp))) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth((value / 200f).coerceIn(0.02f, 1f))
                    .background(statColor(value), RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun statColor(v: Int) = when {
    v >= 120 -> Color(0xFF4CAF50)
    v >= 90 -> Color(0xFF8BC34A)
    v >= 60 -> Color(0xFFFFC107)
    else -> Color(0xFFFF7043)
}

// ═══════════════════════════════════ TRAINERS ════════════════════════════════
private enum class TrainerFilter(val label: String) { ALL("All"), RIVAL("Rival"), GYM("Gym"), ELITE("Elite 4"), BOSS("Boss") }

private fun trainerMatchesFilter(t: LogTrainer, f: TrainerFilter, game: GameVersion): Boolean {
    if (f == TrainerFilter.ALL) return true
    return when (TrainerGroups.groupOf(game, t.num)) {
        TrainerGroups.Group.RIVAL -> f == TrainerFilter.RIVAL
        TrainerGroups.Group.GYM -> f == TrainerFilter.GYM
        TrainerGroups.Group.ELITE4 -> f == TrainerFilter.ELITE
        TrainerGroups.Group.BOSS -> f == TrainerFilter.BOSS
        null -> false
    }
}

@Composable
private fun TrainersTab(data: LogData, game: GameVersion, onOpen: (Int) -> Unit) {
    var filter by remember { mutableStateOf(TrainerFilter.ALL) }
    val trainers = remember(filter) {
        data.trainers.values.filter { it.party.isNotEmpty() && trainerMatchesFilter(it, filter, game) }.sortedBy { it.num }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
            TrainerFilter.values().forEach { f ->
                FilterChip(f.label, filter == f) { filter = f }
                Spacer(Modifier.width(6.dp))
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(trainers, key = { it.num }) { t ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(t.num) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name.ifBlank { t.trainerClass }.titlecaseWords(), color = LogText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(t.trainerClass.titlecaseWords(), color = LogTextSec, fontSize = 11.sp)
                    }
                    Text("${t.party.size} ●", color = LogTextSec, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Lv ${t.maxLevel ?: 0}", color = LogAccent, fontSize = 12.sp)
                }
                Divider(color = Color(0xFF223), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun TrainerDetail(data: LogData, num: Int, state: TrackerState.Active, onNav: (LogNav?) -> Unit) {
    val t = data.trainers[num] ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text(t.name.ifBlank { t.trainerClass }.titlecaseWords(), color = LogText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(t.trainerClass.titlecaseWords(), color = LogTextSec, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        t.party.forEach { member ->
            val mon = data.pokemon[member.pokemonId]
            Row(
                Modifier.fillMaxWidth().background(LogCard, RoundedCornerShape(6.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.clickable { onNav(LogNav.Pokemon(member.pokemonId)) }.width(76.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MonSprite(state.natDexId(member.pokemonId), 40)
                    Text(mon?.name ?: state.speciesName(member.pokemonId), color = LogAccent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Lv ${member.level}", color = LogTextSec, fontSize = 11.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    member.heldItem?.let { Text("@ ${it.titlecaseWords()}", color = Color(0xFFFFD54F), fontSize = 11.sp) }
                    val moves = member.moveIds.filter { it != 0 }
                    if (moves.isEmpty()) Text("(moves unknown)", color = LogTextSec, fontSize = 11.sp)
                    moves.forEach { mid ->
                        val nm = data.moves[mid]?.name ?: hh.game.mgba_android.tracker.tables.MoveNames.get(mid, state.isMaxFr)
                        val stab = data.moves[mid]?.type?.let { it == mon?.type1 || it == mon?.type2 } ?: false
                        Text("• ${nm.titlecaseWords()}", color = if (stab) StabHi else LogText, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ═══════════════════════════════════ ROUTES ══════════════════════════════════
@Composable
private fun RoutesTab(data: LogData, onOpen: (Int) -> Unit) {
    val routes = remember {
        data.routes.values
            .filter { it.numWilds > 0 || it.numTrainers > 0 }
            .sortedBy { it.name }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(routes, key = { it.mapId }) { r ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(r.mapId) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(r.name, color = LogText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val bits = buildList {
                        if (r.numWilds > 0) add("${r.numWilds} wild (Lv ${r.minWildLv}-${r.maxWildLv})")
                        if (r.numTrainers > 0) add("${r.numTrainers} trainers (Lv ${r.minTrainerLv}-${r.maxTrainerLv})")
                    }
                    Text(bits.joinToString("  ·  "), color = LogTextSec, fontSize = 11.sp)
                }
            }
            Divider(color = Color(0xFF223), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun RouteDetail(data: LogData, mapId: Int, state: TrackerState.Active, onNav: (LogNav?) -> Unit) {
    val route = data.routes[mapId] ?: return
    // Ordered encounter areas
    val areaOrder = listOf("Trainers", "GrassCave", "Surfing", "RockSmash", "OldRod", "GoodRod", "SuperRod")
    val areas = areaOrder.mapNotNull { key -> route.encounterAreas[key]?.let { key to it } }
    var sel by remember(mapId) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text(route.name, color = LogText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
        if (areas.isEmpty()) { Text("No encounter data.", color = LogTextSec, fontSize = 13.sp, modifier = Modifier.padding(12.dp)); return }
        ScrollableTabRow(selectedTabIndex = sel.coerceIn(0, areas.size - 1), containerColor = LogHeader, contentColor = LogText, edgePadding = 0.dp) {
            areas.forEachIndexed { i, (key, _) ->
                Tab(selected = sel == i, onClick = { sel = i }) { Text(areaLabel(key), Modifier.padding(8.dp), fontSize = 11.sp) }
            }
        }
        val (key, area) = areas[sel.coerceIn(0, areas.size - 1)]
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            if (key == "Trainers") {
                items(area.trainers, key = { it }) { tid ->
                    val t = data.trainers[tid] ?: return@items
                    Row(Modifier.fillMaxWidth().clickable { onNav(LogNav.Trainer(tid)) }.padding(vertical = 6.dp)) {
                        Text(t.name.ifBlank { t.trainerClass }.titlecaseWords(), color = LogAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("Lv ${t.maxLevel ?: 0}", color = LogTextSec, fontSize = 12.sp)
                    }
                    Divider(color = Color(0xFF223), thickness = 0.5.dp)
                }
            } else {
                val sorted = area.pokemon.entries.sortedByDescending { it.value.rate }
                items(sorted, key = { it.key }) { (pid, enc) ->
                    val name = data.pokemon[pid]?.name ?: state.speciesName(pid)
                    Row(Modifier.fillMaxWidth().clickable { onNav(LogNav.Pokemon(pid)) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        MonSprite(state.natDexId(pid), 28)
                        Spacer(Modifier.width(6.dp))
                        Text(name, color = LogAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("Lv ${enc.levelMin}-${enc.levelMax}", color = LogTextSec, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("${(enc.rate * 100).toInt()}%", color = LogText, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    }
                    Divider(color = Color(0xFF223), thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun areaLabel(key: String) = when (key) {
    "GrassCave" -> "Grass/Cave"; "OldRod" -> "Old Rod"; "GoodRod" -> "Good Rod"; "SuperRod" -> "Super Rod"
    "RockSmash" -> "Rock Smash"; else -> key
}

// ═══════════════════════════════════ GYM TMs ═════════════════════════════════
@Composable
private fun TmsTab(data: LogData, game: GameVersion, onOpenTrainer: (Int) -> Unit) {
    val gymTMs = remember(game) { TrainerGroups.gymTMs(game) }
    if (gymTMs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No gym TM data for this game.", color = LogTextSec, fontSize = 13.sp)
        }
        return
    }
    // Which TM each gym leader awards on defeat
    LazyColumn(Modifier.fillMaxSize()) {
        items(gymTMs, key = { it.badge }) { g ->
            val tm = data.tms[g.tmNumber]
            val exists = data.trainers.containsKey(g.leaderTrainerId)
            Row(
                Modifier.fillMaxWidth()
                    .then(if (exists) Modifier.clickable { onOpenTrainer(g.leaderTrainerId) } else Modifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Badge ${g.badge}", color = LogTextSec, fontSize = 11.sp, modifier = Modifier.width(64.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.leader, color = if (exists) LogAccent else LogText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "TM%02d  ·  %s".format(g.tmNumber, tm?.name?.titlecaseWords() ?: "?"),
                        color = LogText, fontSize = 12.sp,
                    )
                }
            }
            Divider(color = Color(0xFF223), thickness = 0.5.dp)
        }
    }
}

// ═══════════════════════════════════ MISC ════════════════════════════════════
@Composable
private fun MiscTab(data: LogData) {
    val s = data.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MiscRow("Game", s.game ?: "—")
        MiscRow("Randomizer Version", s.version ?: "—")
        MiscRow("Random Seed", s.randomSeed ?: "—")
        Spacer(Modifier.height(8.dp))
        SectionHeader("Settings String")
        Text(s.settingsString ?: "—", color = LogText, fontSize = 12.sp)
    }
}

@Composable
private fun MiscRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ", color = LogTextSec, fontSize = 13.sp)
        Text(value, color = LogText, fontSize = 13.sp)
    }
}

// ── small reusable widgets ───────────────────────────────────────────────────
@Composable
private fun SectionHeader(text: String) {
    Text(text, color = LogAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 3.dp))
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text("Search", color = LogTextSec, fontSize = 12.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LogAccent,
            unfocusedBorderColor = Color(0xFF303050),
            focusedTextColor = LogText,
            unfocusedTextColor = LogText,
        ),
    )
}

@Composable
private fun <T> SortDropdown(options: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.background(LogCard, RoundedCornerShape(4.dp)).clickable { open = true }.padding(horizontal = 10.dp, vertical = 8.dp),
        ) { Text(label(selected) + " ▾", color = LogText, fontSize = 12.sp) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(label(opt)) }, onClick = { onSelect(opt); open = false })
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) LogAccent else LogCard, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(label, color = if (selected) Color.White else LogTextSec, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}
