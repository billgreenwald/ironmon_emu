package hh.game.mgba_android.tracker.tables

object ImageAssetMap {
    data class RouteImages(
        val routeMaps: List<String> = emptyList(),    // paths relative to assets/
        val hiddenItems: List<String> = emptyList(),  // paths relative to assets/
    )

    // Keys must match exact strings from RouteNames.kt.
    val MAP: Map<String, RouteImages> = mapOf(

        // ── Cities ──────────────────────────────────────────────────────────
        "Pewter City" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/pewter_city.png"),
        ),
        "Cerulean City" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/cerulean_city.png"),
        ),
        "Vermilion City" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/vermilion_city.png"),
        ),
        "Celadon City" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/celadon_city.png"),
        ),
        "Fuchsia City" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/fuschia_city.png"),
        ),
        "Saffron City" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/saffron_city.png"),
        ),

        // ── Routes ──────────────────────────────────────────────────────────
        "Viridian Forest" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/viridian_forest_1.png",
                "maps/hidden_items/viridian_forest_2.png",
            ),
        ),
        "Route 3" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_3.png"),
        ),
        "Route 4" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_4.png",
                "maps/hidden_items/route_4_1.png",
                "maps/hidden_items/route_4_2.png",
                "maps/hidden_items/route_4_3.png",
            ),
        ),
        "Route 6" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_6_1.png",
                "maps/hidden_items/route_6_2.png",
            ),
        ),
        "Route 7" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_7.png"),
        ),
        "Route 8" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_8_1.png",
                "maps/hidden_items/route_8_2.png",
                "maps/hidden_items/route_8_3.png",
            ),
        ),
        "Route 9" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_9_1.png",
                "maps/hidden_items/route_9_2.png",
                "maps/hidden_items/route_9_3.png",
            ),
        ),
        "Route 10" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_10_1.png",
                "maps/hidden_items/route_10_2.png",
                "maps/hidden_items/route_10_3.png",
                "maps/hidden_items/route_10_4.png",
                "maps/hidden_items/route_10_5.png",
            ),
        ),
        "Route 11" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_11.png"),
        ),
        "Route 12" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_12_1.png",
                "maps/hidden_items/route_12_2.png",
            ),
        ),
        "Route 13" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_13.png"),
        ),
        "Route 14" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_14_1.png",
                "maps/hidden_items/route_14_2.png",
            ),
        ),
        "Route 17" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/cycling_roadroute_17_1.png",
                "maps/hidden_items/cycling_roadroute_17_2.png",
                "maps/hidden_items/cycling_roadroute_17_3.png",
                "maps/hidden_items/cycling_roadroute_17_4.png",
                "maps/hidden_items/cycling_roadroute_17_5.png",
            ),
        ),
        "Route 20" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_20.png"),
        ),
        "Route 21 North" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_21.png"),
        ),
        "Route 23" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_23_1.png",
                "maps/hidden_items/route_23_2.png",
                "maps/hidden_items/route_23_3.png",
                "maps/hidden_items/route_23_4.png",
                "maps/hidden_items/route_23_5.png",
                "maps/hidden_items/route_23_6.png",
                "maps/hidden_items/route_23_7.png",
                "maps/hidden_items/route_23_8.png",
            ),
        ),
        "Route 24" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/route_24.png"),
        ),
        "Route 25" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/route_25_1.png",
                "maps/hidden_items/route_25_2.png",
                "maps/hidden_items/route_25_3.png",
                "maps/hidden_items/route_25_4.png",
            ),
        ),

        // ── Dungeons / Buildings ─────────────────────────────────────────────
        "Mt. Moon 1F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/mt_moon.png"),
            hiddenItems = (1..8).map { "maps/hidden_items/mt_moon_$it.png" },
        ),
        "Mt. Moon B1F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/mt_moon.png"),
            hiddenItems = (1..8).map { "maps/hidden_items/mt_moon_$it.png" },
        ),
        "Mt. Moon B2F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/mt_moon.png"),
            hiddenItems = (1..8).map { "maps/hidden_items/mt_moon_$it.png" },
        ),
        "Rock Tunnel 1F" to RouteImages(
            routeMaps = listOf("maps/route_maps/rock_tunnel.png"),
        ),
        "Power Plant" to RouteImages(
            routeMaps   = listOf("maps/route_maps/power_plant.png"),
            hiddenItems = listOf(
                "maps/hidden_items/power_plant_1.png",
                "maps/hidden_items/power_plant_2.png",
            ),
        ),
        "Safari Zone Center" to RouteImages(
            routeMaps   = listOf("maps/route_maps/safari_zone.png"),
            hiddenItems = listOf(
                "maps/hidden_items/safari_zone_1.png",
                "maps/hidden_items/safari_zone_2.png",
            ),
        ),
        "Safari Zone East" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/safari_zone_1.png",
                "maps/hidden_items/safari_zone_2.png",
            ),
        ),
        "Pokemon Tower 5F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/pokemon_tower.png"),
        ),
        "Rocket Hideout B1F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/rocket_hideout.png"),
            hiddenItems = listOf("maps/hidden_items/rocket_hideout_b1.png"),
        ),
        "Rocket Hideout B3F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/rocket_hideout_b3.png"),
        ),
        "Rocket Hideout B4F" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/rocket_hideout_b4.png",
                "maps/hidden_items/rocket_hideout.png",
            ),
        ),
        "Silph Co. 1F" to RouteImages(
            routeMaps = listOf("maps/route_maps/silph_co.png"),
        ),
        "Silph Co. 2F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/silph_co_2f.png"),
        ),
        "Silph Co. 3F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/siilph_co_3f.png"),
        ),
        "Silph Co. 4F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/silph_co_4f.png"),
        ),
        "Silph Co. 5F" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/silph_co_5f_1.png",
                "maps/hidden_items/silph_co_5f_2.png",
            ),
        ),
        "Silph Co. 6F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/silph_co_6f.png"),
        ),
        "Silph Co. 7F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/silph_co_7f.png"),
        ),
        "Silph Co. 8F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/silph_co_8f.png"),
        ),
        "Silph Co. 9F" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/silph_co_9f_1.png",
                "maps/hidden_items/silph_co_9f_2.png",
            ),
        ),
        "Silph Co. 10F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/silph_co_10f.png"),
        ),
        "Silph Co. 11F" to RouteImages(
            hiddenItems = listOf("maps/hidden_items/11f.png"),
        ),
        "Saffron Gym" to RouteImages(
            routeMaps = listOf("maps/route_maps/saffron_gym.png"),
        ),
        "Seafoam Islands 1F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/seafoam_islands.png"),
            hiddenItems = listOf(
                "maps/hidden_items/seafoam_islands_1.png",
                "maps/hidden_items/seafoam_islands_2.png",
            ),
        ),
        "Victory Road 1F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/victory_road.png"),
            hiddenItems = listOf("maps/hidden_items/victory_road.png"),
        ),
        "Poke Mansion 1F" to RouteImages(
            routeMaps   = listOf("maps/route_maps/cinnabar_mansion.png"),
            hiddenItems = listOf(
                "maps/hidden_items/pokemon_mansion_1.png",
                "maps/hidden_items/pokemon_mansion_2.png",
                "maps/hidden_items/pokemon_mansion_3.png",
            ),
        ),
        "Poke Mansion 2F" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/pokemon_mansion_1.png",
                "maps/hidden_items/pokemon_mansion_2.png",
                "maps/hidden_items/pokemon_mansion_3.png",
            ),
        ),
        "Poke Mansion 3F" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/pokemon_mansion_1.png",
                "maps/hidden_items/pokemon_mansion_2.png",
                "maps/hidden_items/pokemon_mansion_3.png",
            ),
        ),
        "Poke Mansion B1F" to RouteImages(
            hiddenItems = listOf(
                "maps/hidden_items/pokemon_mansion_1.png",
                "maps/hidden_items/pokemon_mansion_2.png",
                "maps/hidden_items/pokemon_mansion_3.png",
            ),
        ),
    )
}
