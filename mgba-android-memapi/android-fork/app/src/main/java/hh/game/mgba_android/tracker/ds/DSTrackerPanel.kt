package hh.game.mgba_android.tracker.ds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hh.game.mgba_android.tracker.ds.models.DSPokemonData
import hh.game.mgba_android.tracker.ds.models.DSTrackerState
import hh.game.mgba_android.tracker.ds.tables.*
import kotlinx.coroutines.launch

// ── Color palette (matches GBA tracker palette) ───────────────────────────────
private val PanelBg       = Color(0xFF0F1621)
private val HeaderBg      = Color(0xFF16213E)
private val CardBg        = Color(0xFF1A2540)
private val TextPrimary   = Color(0xFFEEEEEE)
private val TextSecondary = Color(0xFFAAAAAA)
private val HpHigh        = Color(0xFF4CAF50)
private val HpMid         = Color(0xFFFFEB3B)
private val HpLow         = Color(0xFFF44336)
private val AccentBlue    = Color(0xFF4090FF)

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
    14 to Color(0xFFF85888),  // Psychic
    15 to Color(0xFF98D8D8),  // Ice
    16 to Color(0xFF7038F8),  // Dragon
    17 to Color(0xFF705848),  // Dark
)
private fun typeColor(id: Int) = TYPE_COLORS[id] ?: Color(0xFF888888)

// ── Root entry point ──────────────────────────────────────────────────────────

@Composable
fun DSTrackerPanel(state: DSTrackerState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
    ) {
        when (state) {
            is DSTrackerState.Disconnected  -> CenteredLabel("DS core not running")
            is DSTrackerState.NoGameLoaded  -> CenteredLabel("No DS game detected")
            is DSTrackerState.Active        -> ActivePanel(state)
        }
    }
}

// ── Active panel ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivePanel(state: DSTrackerState.Active) {
    val tabs = listOf("Main", "Stats")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBg)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = state.version.displayName,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Tab row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = HeaderBg,
            contentColor = AccentBlue,
            modifier = Modifier.fillMaxWidth().height(32.dp),
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = pagerState.currentPage == idx,
                    onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                ) {
                    Text(label, fontSize = 11.sp, color = if (pagerState.currentPage == idx) AccentBlue else TextSecondary)
                }
            }
        }

        // Pager content
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> MainTab(state.party)
                1 -> StatsTab(state.party)
            }
        }
    }
}

// ── Main tab: party list ──────────────────────────────────────────────────────

@Composable
private fun MainTab(party: List<DSPokemonData>) {
    if (party.isEmpty()) {
        CenteredLabel("Party is empty")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        party.forEach { mon -> PartyCard(mon) }
    }
}

@Composable
private fun PartyCard(mon: DSPokemonData) {
    val hpPct = if (mon.maxHp > 0) mon.currentHp / mon.maxHp.toFloat() else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Name + level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val displayName = if (mon.nickname.isNotBlank()) mon.nickname
                              else GenIVSpeciesNames.get(mon.speciesId)
            Text(displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Lv.${mon.level}", color = TextSecondary, fontSize = 11.sp)
        }

        // Types + ability
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DSTypeChip(mon.type1)
            if (mon.type2 != mon.type1) DSTypeChip(mon.type2)
            Spacer(Modifier.weight(1f))
            Text(
                text = GenIVAbilityTable.getName(mon.abilityId),
                color = TextSecondary, fontSize = 10.sp,
            )
        }

        // HP bar
        HpBar(hpPct, mon.currentHp, mon.maxHp)

        // Nature
        val nature = GenIVNatureTable.get(mon.nature)
        Text("${nature.name} nature", color = TextSecondary, fontSize = 10.sp)

        // Moves
        Spacer(Modifier.height(2.dp))
        MoveList(mon)
    }
}

@Composable
private fun MoveList(mon: DSPokemonData) {
    val moves = listOf(
        mon.move1Id to mon.move1Pp,
        mon.move2Id to mon.move2Pp,
        mon.move3Id to mon.move3Pp,
        mon.move4Id to mon.move4Pp,
    ).filter { (id, _) -> id > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        moves.forEach { (id, pp) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = GenIVMoveNames.get(id),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                Text("$pp PP", color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

// ── Stats tab ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsTab(party: List<DSPokemonData>) {
    if (party.isEmpty()) {
        CenteredLabel("Party is empty")
        return
    }
    val lead = party.first()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = GenIVSpeciesNames.get(lead.speciesId),
            color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        )
        StatRow("HP",    lead.maxHp,  lead.hpIv,  lead.hpEv,  boosted = false, hindered = false)
        StatRow("Atk",   lead.atk,    lead.atkIv, lead.atkEv, boosted = GenIVNatureTable.get(lead.nature).boosted == 0, hindered = GenIVNatureTable.get(lead.nature).hindered == 0)
        StatRow("Def",   lead.def,    lead.defIv, lead.defEv, boosted = GenIVNatureTable.get(lead.nature).boosted == 1, hindered = GenIVNatureTable.get(lead.nature).hindered == 1)
        StatRow("SpAtk", lead.spa,    lead.spaIv, lead.spaEv, boosted = GenIVNatureTable.get(lead.nature).boosted == 2, hindered = GenIVNatureTable.get(lead.nature).hindered == 2)
        StatRow("SpDef", lead.spd,    lead.spdIv, lead.spdEv, boosted = GenIVNatureTable.get(lead.nature).boosted == 3, hindered = GenIVNatureTable.get(lead.nature).hindered == 3)
        StatRow("Speed", lead.spe,    lead.speIv, lead.speEv, boosted = GenIVNatureTable.get(lead.nature).boosted == 4, hindered = GenIVNatureTable.get(lead.nature).hindered == 4)

        if (party.size > 1) {
            Spacer(Modifier.height(4.dp))
            Text("Other party members:", color = TextSecondary, fontSize = 11.sp)
            party.drop(1).forEach { mon ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(GenIVSpeciesNames.get(mon.speciesId), color = TextPrimary, fontSize = 12.sp, modifier = Modifier.width(80.dp))
                    Text("Lv.${mon.level}", color = TextSecondary, fontSize = 10.sp)
                    Text("${mon.currentHp}/${mon.maxHp} HP", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, iv: Int, ev: Int, boosted: Boolean, hindered: Boolean) {
    val labelColor = when {
        boosted  -> Color(0xFF4CAF50)   // green
        hindered -> Color(0xFFF44336)   // red
        else     -> TextSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = labelColor, fontSize = 11.sp, modifier = Modifier.width(42.dp))
        Text("$value", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
        Text("IV:$iv", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(40.dp))
        Text("EV:$ev", color = TextSecondary, fontSize = 10.sp)
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun HpBar(pct: Float, current: Int, max: Int) {
    val hpColor = when {
        pct > 0.5f -> HpHigh
        pct > 0.2f -> HpMid
        else        -> HpLow
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = pct.coerceIn(0f, 1f),
            color = hpColor,
            trackColor = Color(0xFF303050),
            modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(4.dp))
        Text("$current/$max", color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun DSTypeChip(typeId: Int) {
    val name = GenIVTypeChart.typeName(typeId)
    Box(
        modifier = Modifier
            .background(typeColor(typeId).copy(alpha = 0.85f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CenteredLabel(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}
