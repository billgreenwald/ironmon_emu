package hh.game.mgba_android.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NameEntryButtons(
    onTrainerName: () -> Unit,
    onRivalName: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val btnColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xB3111111),
        contentColor = Color.White,
    )
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onTrainerName,
            colors = btnColors,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(36.dp),
        ) { Text("tName", fontSize = 12.sp) }
        Button(
            onClick = onRivalName,
            colors = btnColors,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(36.dp),
        ) { Text("rName", fontSize = 12.sp) }
    }
}
