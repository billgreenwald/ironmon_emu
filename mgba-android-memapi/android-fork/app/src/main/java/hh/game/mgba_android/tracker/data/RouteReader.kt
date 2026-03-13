package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.MemoryBridge
import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.tables.RouteNames

data class RouteInfo(val mapLayoutId: Int, val name: String)

object RouteReader {

    fun read(game: GameVersion, addresses: GameAddresses): RouteInfo? {
        // Read mapLayoutId: u16 at gMapHeader + 0x12
        val mapLayoutId = MemoryBridge.readU16(
            addresses.gMapHeader + DataHelper.MAP_HEADER_LAYOUT_ID_OFFSET
        ) ?: return null
        if (mapLayoutId == 0) return null
        val isEmerald = game == GameVersion.EMERALD
        val name = RouteNames.get(mapLayoutId, isEmerald)
        return RouteInfo(mapLayoutId, name)
    }
}
