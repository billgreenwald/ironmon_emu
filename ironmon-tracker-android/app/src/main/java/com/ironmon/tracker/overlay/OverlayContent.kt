package com.ironmon.tracker.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironmon.tracker.data.models.GameVersion
import com.ironmon.tracker.data.models.MoveData
import com.ironmon.tracker.data.models.PokemonData
import com.ironmon.tracker.data.models.TrackerState

// ── Color palette ────────────────────────────────────────────────────────────

private val BackgroundColor  = Color(0xCC1A1A2E)   // semi-transparent dark navy
private val HeaderColor      = Color(0xFF16213E)
private val AccentColor      = Color(0xFFE94560)
private val TextPrimary      = Color(0xFFEEEEEE)
private val TextSecondary    = Color(0xFFAAAAAA)
private val HpColorHigh      = Color(0xFF4CAF50)
private val HpColorMid       = Color(0xFFFFEB3B)
private val HpColorLow       = Color(0xFFF44336)

/**
 * Root overlay composable. Switches between Disconnected / NoGame / Active states.
 */
@Composable
fun OverlayContent(state: TrackerState) {
    when (state) {
        is TrackerState.Disconnected -> DisconnectedOverlay()
        is TrackerState.NoGameLoaded -> NoGameOverlay()
        is TrackerState.Active       -> ActiveOverlay(state)
    }
}

@Composable
private fun DisconnectedOverlay() {
    OverlayCard {
        Text(
            text      = "Waiting for mGBA…",
            color     = TextSecondary,
            fontSize  = 11.sp,
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun NoGameOverlay() {
    OverlayCard {
        Text(
            text      = "No supported game loaded",
            color     = TextSecondary,
            fontSize  = 11.sp,
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ActiveOverlay(state: TrackerState.Active) {
    OverlayCard {
        Column {
            // Header: game name
            OverlayHeader(state.game)

            // Lead Pokémon (always shown)
            state.leadPokemon?.let { lead ->
                PokemonRow(pokemon = lead, isLead = true)
            }

            // Remaining party (if any)
            state.party.drop(1).forEachIndexed { _, pokemon ->
                Spacer(modifier = Modifier.height(2.dp))
                PokemonRow(pokemon = pokemon, isLead = false)
            }
        }
    }
}

@Composable
private fun OverlayHeader(game: GameVersion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text      = "IRONMON",
            color     = AccentColor,
            fontSize  = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text      = game.displayName.removePrefix("Pokémon "),
            color     = TextSecondary,
            fontSize  = 9.sp,
        )
    }
}

@Composable
private fun PokemonRow(pokemon: PokemonData, isLead: Boolean) {
    Column(
        modifier = Modifier
            .widthIn(min = 180.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        // Name + level
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = pokemon.speciesName,
                color      = if (isLead) TextPrimary else TextSecondary,
                fontSize   = if (isLead) 13.sp else 11.sp,
                fontWeight = if (isLead) FontWeight.Bold else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text    = "Lv.${pokemon.level}",
                color   = TextSecondary,
                fontSize = 10.sp,
            )
        }

        // HP bar
        val hpColor = when {
            pokemon.hpPercent > 0.5f -> HpColorHigh
            pokemon.hpPercent > 0.2f -> HpColorMid
            else                     -> HpColorLow
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinearProgressIndicator(
                progress  = { pokemon.hpPercent },
                color     = hpColor,
                trackColor = Color(0xFF333355),
                modifier  = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text    = "${pokemon.currentHp}/${pokemon.maxHp}",
                color   = TextSecondary,
                fontSize = 9.sp,
            )
        }

        // Moves (lead only — condensed for others)
        if (isLead && pokemon.moves.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            MovesRow(pokemon.moves)
        }
    }
}

@Composable
private fun MovesRow(moves: List<MoveData>) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        moves.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { move ->
                    Text(
                        text     = "${move.moveName} (${move.pp})",
                        color    = TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverlayCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundColor),
    ) {
        content()
    }
}
