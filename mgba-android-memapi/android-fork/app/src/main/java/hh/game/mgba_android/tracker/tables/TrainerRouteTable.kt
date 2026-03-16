package hh.game.mgba_android.tracker.tables

import hh.game.mgba_android.tracker.models.GameVersion

/**
 * Maps mapLayoutId → list of trainer IDs for each game version.
 *
 * Ported directly from RouteData.lua (setupRouteInfoAsFRLG / setupRouteInfoAsRSE).
 * Each trainer has one defeat flag bit in SaveBlock1 (see TrainerFlagReader).
 *
 * Ruby/Sapphire use mapLayoutId = Emerald + 1 for entries above mapId 124 due to
 * LAYOUT_LILYCOVE_CITY_EMPTY_MAP being absent in Emerald (offset in RouteData.lua).
 */
object TrainerRouteTable {

    // ── FireRed / LeafGreen ───────────────────────────────────────────────────
    // Source: RouteData.lua setupRouteInfoAsFRLG()
    val FRLG: Map<Int, List<Int>> = mapOf(
        5   to listOf(326, 327, 328),                                                          // Oak's Lab
        12  to listOf(150, 234, 415),                                                          // Cerulean Gym
        15  to listOf(132, 133, 160, 265, 266, 267, 402, 417),                                 // Celadon Gym
        20  to listOf(294, 295, 288, 289, 292, 293, 418),                                      // Fuchsia Gym
        25  to listOf(141, 220, 423, 416),                                                     // Vermilion Gym
        27  to listOf(357),                                                                    // Game Corner
        28  to listOf(142, 414),                                                               // Pewter Gym
        34  to listOf(280, 281, 282, 283, 462, 463, 464, 420),                                 // Saffron Gym
        36  to listOf(177, 178, 179, 180, 213, 214, 215, 419),                                 // Cinnabar Gym
        37  to listOf(296, 297, 322, 323, 324, 392, 400, 401, 350),                            // Viridian Gym
        81  to listOf(332, 333, 334, 355),                                                     // Cerulean City
        91  to listOf(89, 90, 105, 106, 107, 116, 117, 118),                                   // Route 3
        92  to listOf(119),                                                                    // Mt. Moon
        94  to listOf(111, 112, 145, 146, 151, 152),                                           // Route 4
        96  to listOf(128, 129, 130, 131, 171, 172, 173, 262, 264, 484, 535, 536),             // Route 5
        97  to listOf(114, 115, 148, 149, 154, 155, 185, 186, 465),                            // Route 6
        98  to listOf(156, 157, 162, 163, 187, 188),                                           // Route 7
        99  to listOf(97, 98, 99, 100, 221, 222, 258, 259, 260, 261),                          // Route 8
        100 to listOf(225, 226, 227, 228, 233, 285, 477, 486),                                 // Route 9
        101 to listOf(195, 268, 269, 300, 301, 302, 466, 467, 468, 469),                       // Route 10
        102 to listOf(196, 207, 208, 209, 303, 304, 313, 314, 315, 316, 487),                  // Route 11
        103 to listOf(197, 198, 273, 274, 305, 306, 478, 479, 480, 481, 488),                  // Route 12
        104 to listOf(199, 201, 202, 249, 250, 251, 489),                                      // Route 13
        105 to listOf(203, 204, 205, 206, 252, 253, 254, 255, 256, 470),                       // Route 14
        106 to listOf(307, 308, 309),                                                          // Route 15
        107 to listOf(235, 236, 237, 238, 239, 240, 241, 276, 277, 278, 490),                  // Route 16
        108 to listOf(242, 243, 244, 270, 271, 272, 279, 310, 472, 473),                       // Route 17
        109 to listOf(229, 231, 245, 491),                                                     // Route 18
        110 to listOf(329, 330, 331, 435, 436, 437),                                           // Route 19
        112 to listOf(92, 110, 122, 123, 143, 144, 356),                                       // Route 24
        113 to listOf(93, 94, 95, 153, 125, 182, 183, 184, 471),                               // Route 25
        114 to listOf(181, 91, 120, 121, 169, 108, 109),                                       // S.S. Anne
        116 to listOf(170, 351, 352, 353, 354),                                                // Rock Tunnel
        117 to listOf(102, 103, 104, 531, 532),                                                // Pokemon Tower
        120 to listOf(426, 427, 428),                                                          // Silph Co.
        123 to listOf(134, 135),                                                               // Route 21
        125 to listOf(406, 396),                                                               // Route 22
        126 to listOf(167, 325, 287, 290, 298),                                                // Victory Road
        127 to listOf(393, 394, 403, 404, 485),                                                // Elite Four
        128 to listOf(358, 359, 360, 361, 362),                                                // Route 1
        129 to listOf(363),                                                                    // Route 2
        130 to listOf(364, 365),                                                               // Viridian Forest
        131 to listOf(348, 368, 366, 367),                                                     // Route 26
        133 to listOf(336, 337, 373, 374),                                                     // Route 27
        134 to listOf(338, 375),                                                               // Route 28
        135 to listOf(339, 376, 377),                                                          // Mt. Silver
        136 to listOf(340, 378, 379, 286),                                                     // Route 23
        137 to listOf(341, 380, 381),                                                          // Cerulean Cave
        138 to listOf(342, 383, 384, 385, 432, 433, 434),                                      // Rocket Hideout
        139 to listOf(343, 382, 386),                                                          // Pokemon Mansion
        140 to listOf(344, 387, 388),                                                          // Seafoam Islands
        141 to listOf(345, 389),                                                               // Power Plant
        142 to listOf(349, 390, 391),                                                          // Route 20
        143 to listOf(335, 534),                                                               // Route 21 (alt)
        144 to listOf(216),                                                                    // Trainer Tower
        145 to listOf(218, 346),                                                               // Lost Cave
        146 to listOf(219, 347),                                                               // Mt. Ember
        154 to listOf(192, 193, 194, 168, 476, 475, 474),                                      // Icefall Cave
        155 to listOf(158, 159, 189, 190, 191, 164, 165, 166),                                 // Route 10 N
        162 to listOf(429, 430, 431),                                                          // One Island
        163 to listOf(441, 442, 443),                                                          // Two Island
        164 to listOf(444, 445, 446),                                                          // Three Island
        165 to listOf(447, 448, 449, 450),                                                     // Four Island
        166 to listOf(451, 452, 453),                                                          // Five Island
        167 to listOf(369, 370, 371),                                                          // Six Island
        177 to listOf(483, 127, 223, 482, 422, 421, 126, 96),                                  // Water Path
        178 to listOf(138, 139, 224, 140, 136, 137),                                           // Green Path
        213 to listOf(410),                                                                    // Memorial Pillar
        214 to listOf(411),                                                                    // Outcast Island
        215 to listOf(412),                                                                    // Green Path (alt)
        216 to listOf(413),                                                                    // Water Path (alt)
        217 to listOf(438, 439, 440),                                                          // Tanoby Chambers
        219 to listOf(230, 232, 246, 247, 248),                                                // Kindle Road
        228 to listOf(321, 319, 320, 318, 317),                                                // Treasure Beach
        232 to listOf(527, 528, 529, 742),                                                     // Cape Brink
        237 to listOf(547, 548, 549, 550, 551, 518, 552, 553, 554, 555, 556, 557),             // Rocket Warehouse
        238 to listOf(546),                                                                    // Dotted Hole
        240 to listOf(523, 558, 519, 559, 561, 560),                                           // Resort Gorgeous
        246 to listOf(526, 562, 563, 525, 564, 565, 566),                                      // Water Labyrinth
        247 to listOf(520),                                                                    // Five Isle Meadow
        248 to listOf(567, 568, 569),                                                          // Memorial Pillar
        249 to listOf(570, 571, 572),                                                          // Outcast Island (alt)
        250 to listOf(573, 574, 575, 576, 540),                                                // Green Path (alt2)
        251 to listOf(517),                                                                    // Water Path (alt2)
        252 to listOf(577, 291, 578, 579, 580, 581),                                           // Seven Island
        253 to listOf(524, 582, 583, 584, 585),                                                // Sevault Canyon
        254 to listOf(586, 587),                                                               // Tanoby Ruins
        255 to listOf(588, 589, 590, 521, 522),                                                // Trainer Tower (alt)
        256 to listOf(591, 593, 596, 598, 599, 600, 601),                                      // Route alt
        280 to listOf(537, 538, 595, 597, 592),                                                // Mt. Moon (alt)
        292 to listOf(545, 541, 542, 544, 516, 543),                                           // Victory Road (alt)
        296 to listOf(539),                                                                    // Cerulean Cave (alt)
        317 to listOf(609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620),             // Elite Four (alt)
        321 to listOf(607),                                                                    // Steven room
        324 to listOf(608),                                                                    // Champion
        330 to listOf(606),                                                                    // Hall of Fame
    )

    // ── Emerald ───────────────────────────────────────────────────────────────
    // Source: RouteData.lua setupRouteInfoAsRSE(), offset=0
    val EMERALD: Map<Int, List<Int>> = mapOf(
        3   to listOf(656),
        4   to listOf(592, 593, 599, 600, 768, 769),
        6   to listOf(661, 662, 663, 664, 665, 666),
        18  to listOf(318, 615, 333, 603),
        19  to listOf(520, 523, 526, 529, 532, 535, 36, 481, 293, 336, 703, 702, 736, 735),
        20  to listOf(319, 696, 114, 136, 604, 483, 337),
        21  to listOf(442, 152, 46, 441, 737, 738, 151),
        22  to listOf(340, 339, 153, 443),
        23  to listOf(444, 155, 692, 154, 445, 739),
        24  to listOf(447, 157, 446, 741, 740, 156),
        25  to listOf(490, 491, 697, 64, 57, 698, 58, 59, 158, 448, 345, 742, 680),
        26  to listOf(302, 699, 334, 512, 700, 232, 701, 521, 524, 527, 530, 533, 536, 243, 358, 352, 353, 359, 351),
        27  to listOf(704, 705, 706, 707, 51, 476, 218, 292, 299, 606, 312, 78, 94, 189, 469, 212, 211, 470, 44, 743, 744, 745),
        28  to listOf(213, 471, 627, 626, 746, 747),
        29  to listOf(326, 710, 420, 711, 434, 677, 419, 327, 708, 709),
        30  to listOf(342, 714, 713, 338, 472, 679, 214, 143, 206, 629, 712, 628),
        31  to listOf(183, 513, 752, 427, 749, 307, 748, 182, 751, 750),
        32  to listOf(617, 322, 280, 631, 754, 753, 695, 694, 273, 605),
        33  to listOf(364, 287, 538, 369, 227, 756, 755, 757, 545),
        34  to listOf(37, 715, 344, 196, 52, 343, 408, 398),
        35  to listOf(620, 224, 619, 225, 618, 223, 693, 559, 552, 761, 400, 416, 760, 399, 759, 415, 651, 522, 525, 528, 531, 534, 537),
        36  to listOf(435, 53, 406, 405, 762, 436, 653, 763, 95, 560, 553, 226, 652, 45),
        46  to listOf(165, 455, 168),
        47  to listOf(171, 385, 166, 457, 167, 456, 686),
        65  to listOf(426, 179, 425, 266),                                                     // Dewford Gym
        69  to listOf(202, 204, 201, 648, 203, 205, 650, 268),                                 // Lavaridge Gym
        70  to listOf(202, 204, 205, 501),                                                     // Lavaridge Gym 1F (Emerald variant)
        79  to listOf(71, 89, 72, 90, 73, 91, 74, 269),
        87  to listOf(20, 21),
        89  to listOf(649, 191, 323, 802, 194, 267),
        94  to listOf(320, 321, 265),                                                          // Rustboro Gym
        100 to listOf(402, 401, 655, 654, 404, 803, 270),
        108 to listOf(233, 246, 245, 235, 234, 244, 271),                                      // Mossdeep Gym
        109 to listOf(128, 613, 115, 131, 614, 301, 130, 118, 129, 272),                       // Sootopolis Gym
        110 to listOf(128, 613, 115, 502, 131, 614, 301, 130, 118, 129),                       // Sootopolis Gym 1F
        111 to listOf(261),
        112 to listOf(262),
        113 to listOf(263),
        114 to listOf(264),
        115 to listOf(335),
        126 to listOf(681, 392),
        129 to listOf(16, 635),
        135 to listOf(616, 10, 621),
        140 to listOf(109),
        141 to listOf(190),
        143 to listOf(2),
        145 to listOf(5, 27, 28, 30),
        190 to listOf(496),
        243 to listOf(65, 647, 493),
        247 to listOf(611, 612, 332),
        248 to listOf(274, 275, 281),
        249 to listOf(215, 473, 630),
        250 to listOf(188, 428, 429),
        252 to listOf(561, 554, 407),
        253 to listOf(237, 105, 248, 848, 850, 849),
        254 to listOf(93, 76, 77),
        272 to listOf(18, 19, 32),
        275 to listOf(586, 22, 587, 116),
        276 to listOf(588, 589, 590, 734, 514),
        278 to listOf(494, 495),
        279 to listOf(641, 138, 255, 294, 119, 256),
        335 to listOf(809, 806, 810, 805, 808, 807, 811),
        336 to listOf(717, 716),
        337 to listOf(718, 720, 719, 727),
        338 to listOf(721, 730, 722, 723),
        339 to listOf(724, 726, 729),
        340 to listOf(725),
        341 to listOf(728, 731, 732, 601),
        431 to listOf(804),
    )

    // ── Ruby / Sapphire ───────────────────────────────────────────────────────
    // Source: RouteData.lua setupRouteInfoAsRSE(), offset=1
    // Entries with mapId >= 125 are shifted by +1 vs Emerald.
    val RUBY_SAPPHIRE: Map<Int, List<Int>> = mapOf(
        3   to listOf(656),
        4   to listOf(592, 593, 599, 600, 768, 769),
        6   to listOf(661, 662, 663, 664, 665, 666),
        18  to listOf(318, 615, 333, 603),
        19  to listOf(520, 523, 526, 529, 532, 535, 36, 481, 293, 336, 703, 702, 736, 735),
        20  to listOf(319, 696, 114, 136, 604, 483, 337),
        21  to listOf(442, 152, 46, 441, 737, 738, 151),
        22  to listOf(340, 339, 153, 443),
        23  to listOf(444, 155, 692, 154, 445, 739),
        24  to listOf(447, 157, 446, 741, 740, 156),
        25  to listOf(490, 491, 697, 64, 57, 698, 58, 59, 158, 448, 345, 742, 680),
        26  to listOf(302, 699, 334, 512, 700, 232, 701, 521, 524, 527, 530, 533, 536, 243, 358, 352, 353, 359, 351),
        27  to listOf(704, 705, 706, 707, 51, 476, 218, 292, 299, 606, 312, 78, 94, 189, 469, 212, 211, 470, 44, 743, 744, 745),
        28  to listOf(213, 471, 627, 626, 746, 747),
        29  to listOf(326, 710, 420, 711, 434, 677, 419, 327, 708, 709),
        30  to listOf(342, 714, 713, 338, 472, 679, 214, 143, 206, 629, 712, 628),
        31  to listOf(183, 513, 752, 427, 749, 307, 748, 182, 751, 750),
        32  to listOf(617, 322, 280, 631, 754, 753, 695, 694, 273, 605),
        33  to listOf(364, 287, 538, 369, 227, 756, 755, 757, 545),
        34  to listOf(37, 715, 344, 196, 52, 343, 408, 398),
        35  to listOf(620, 224, 619, 225, 618, 223, 693, 559, 552, 761, 400, 416, 760, 399, 759, 415, 651, 522, 525, 528, 531, 534, 537),
        36  to listOf(435, 53, 406, 405, 762, 436, 653, 763, 95, 560, 553, 226, 652, 45),
        46  to listOf(165, 455, 168),
        47  to listOf(171, 385, 166, 457, 167, 456, 686),
        65  to listOf(426, 179, 425, 266),                                                     // Dewford Gym
        69  to listOf(202, 204, 201, 648, 203, 205, 650, 268),                                 // Lavaridge Gym
        70  to listOf(202, 204, 205, 501),
        79  to listOf(71, 89, 72, 90, 73, 91, 74, 269),
        87  to listOf(20, 21),
        89  to listOf(649, 191, 323, 802, 194, 267),
        94  to listOf(320, 321, 265),                                                          // Rustboro Gym
        100 to listOf(402, 401, 655, 654, 404, 803, 270),
        108 to listOf(233, 246, 245, 235, 234, 244, 271),                                      // Mossdeep Gym
        109 to listOf(128, 613, 115, 131, 614, 301, 130, 118, 129, 272),                       // Sootopolis Gym
        110 to listOf(128, 613, 115, 502, 131, 614, 301, 130, 118, 129),
        111 to listOf(261),
        112 to listOf(262),
        113 to listOf(263),
        114 to listOf(264),
        115 to listOf(335),
        // shifted by +1 below this line (mapId >= 125 in Emerald → mapId >= 126 in RS)
        127 to listOf(681, 392),
        130 to listOf(16, 635),
        136 to listOf(616, 10, 621),
        141 to listOf(109),
        142 to listOf(190),
        144 to listOf(2),
        146 to listOf(5, 27, 28, 30),
        191 to listOf(496),
        244 to listOf(65, 647, 493),
        248 to listOf(611, 612, 332),
        249 to listOf(274, 275, 281),
        250 to listOf(215, 473, 630),
        251 to listOf(188, 428, 429),
        253 to listOf(561, 554, 407),
        254 to listOf(237, 105, 248, 848, 850, 849),
        255 to listOf(93, 76, 77),
        273 to listOf(18, 19, 32),
        276 to listOf(586, 22, 587, 116),
        277 to listOf(588, 589, 590, 734, 514),
        279 to listOf(494, 495),
        280 to listOf(641, 138, 255, 294, 119, 256),
        335 to listOf(809, 806, 810, 805, 808, 807, 811),
        336 to listOf(717, 716),
        337 to listOf(718, 720, 719, 727),
        338 to listOf(721, 730, 722, 723),
        339 to listOf(724, 726, 729),
        340 to listOf(725),
        341 to listOf(728, 731, 732, 601),
        431 to listOf(804),
    )

    fun get(version: GameVersion): Map<Int, List<Int>> = when (version) {
        GameVersion.FIRE_RED, GameVersion.LEAF_GREEN -> FRLG
        GameVersion.EMERALD                          -> EMERALD
        GameVersion.RUBY, GameVersion.SAPPHIRE       -> RUBY_SAPPHIRE
        else                                         -> emptyMap()
    }
}
