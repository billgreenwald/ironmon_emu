package hh.game.mgba_android.tracker.tables

/**
 * Static level-up evolution table matching Lua tracker PokemonData.lua.
 * Covers Gen III (IDs 1–411), MaxFR Gen IV/V (IDs 412–654), and NatDex (IDs 412–1235).
 * MaxFR and NatDex share the same ID space but map to different Pokémon at IDs 505+.
 */
object EvolutionLevel {

    // ── Gen III (IDs 1–411) ───────────────────────────────────────────────────

    private val TABLE: Map<Int, Int> = mapOf(
        // ── Gen I ────────────────────────────────────────────────────────────
        1   to 16,  2   to 32,  // Bulbasaur → Ivysaur → Venusaur
        4   to 16,  5   to 36,  // Charmander → Charmeleon → Charizard
        7   to 16,  8   to 36,  // Squirtle → Wartortle → Blastoise
        10  to 7,   11  to 10,  // Caterpie → Metapod → Butterfree
        13  to 7,   14  to 10,  // Weedle → Kakuna → Beedrill
        16  to 18,  17  to 36,  // Pidgey → Pidgeotto → Pidgeot
        19  to 20,              // Rattata → Raticate
        21  to 20,              // Spearow → Fearow
        23  to 22,              // Ekans → Arbok
        27  to 22,              // Sandshrew → Sandslash
        29  to 16,              // Nidoran♀ → Nidorina
        32  to 16,              // Nidoran♂ → Nidorino
        41  to 22,              // Zubat → Golbat
        43  to 21,              // Oddish → Gloom
        46  to 24,              // Paras → Parasect
        48  to 31,              // Venonat → Venomoth
        50  to 26,              // Diglett → Dugtrio
        52  to 28,              // Meowth → Persian
        54  to 33,              // Psyduck → Golduck
        56  to 28,              // Mankey → Primeape
        60  to 25,              // Poliwag → Poliwhirl
        63  to 16,              // Abra → Kadabra
        64  to 37,              // Kadabra → Alakazam (trade proxy)
        66  to 28,              // Machop → Machoke
        67  to 37,              // Machoke → Machamp (trade proxy)
        69  to 21,              // Bellsprout → Weepinbell
        72  to 30,              // Tentacool → Tentacruel
        74  to 25,              // Geodude → Graveler
        75  to 37,              // Graveler → Golem (trade proxy)
        77  to 40,              // Ponyta → Rapidash
        79  to 37,              // Slowpoke → Slowbro
        81  to 30,              // Magnemite → Magneton
        84  to 31,              // Doduo → Dodrio
        86  to 34,              // Seel → Dewgong
        88  to 38,              // Grimer → Muk
        92  to 25,              // Gastly → Haunter
        93  to 37,              // Haunter → Gengar (trade proxy)
        95  to 30,              // Onix → Steelix (trade proxy)
        96  to 26,              // Drowzee → Hypno
        98  to 28,              // Krabby → Kingler
        100 to 30,              // Voltorb → Electrode
        104 to 28,              // Cubone → Marowak
        109 to 35,              // Koffing → Weezing
        111 to 42,              // Rhyhorn → Rhydon
        116 to 32,              // Horsea → Seadra
        117 to 40,              // Seadra → Kingdra (trade proxy)
        118 to 33,              // Goldeen → Seaking
        123 to 30,              // Scyther → Scizor (trade proxy)
        129 to 20,              // Magikarp → Gyarados
        137 to 30,              // Porygon → Porygon2 (trade proxy)
        138 to 40,              // Omanyte → Omastar
        140 to 40,              // Kabuto → Kabutops
        147 to 30,  148 to 55,  // Dratini → Dragonair → Dragonite
        // ── Gen II ───────────────────────────────────────────────────────────
        152 to 16,  153 to 32,  // Chikorita → Bayleef → Meganium
        155 to 14,  156 to 36,  // Cyndaquil → Quilava → Typhlosion
        158 to 18,  159 to 30,  // Totodile → Croconaw → Feraligatr
        161 to 15,              // Sentret → Furret
        163 to 20,              // Hoothoot → Noctowl
        165 to 18,              // Ledyba → Ledian
        167 to 22,              // Spinarak → Ariados
        170 to 27,              // Chinchou → Lanturn
        177 to 25,              // Natu → Xatu
        179 to 15,  180 to 30,  // Mareep → Flaaffy → Ampharos
        183 to 18,              // Marill → Azumarill
        187 to 18,  188 to 27,  // Hoppip → Skiploom → Jumpluff
        194 to 20,              // Wooper → Quagsire
        204 to 31,              // Pineco → Forretress
        209 to 23,              // Snubbull → Granbull
        216 to 30,              // Teddiursa → Ursaring
        218 to 38,              // Slugma → Magcargo
        220 to 33,              // Swinub → Piloswine
        223 to 25,              // Remoraid → Octillery
        228 to 24,              // Houndour → Houndoom
        231 to 25,              // Phanpy → Donphan
        236 to 20,              // Tyrogue → (personality-based)
        238 to 30,              // Smoochum → Jynx
        239 to 30,              // Elekid → Electabuzz
        240 to 30,              // Magby → Magmar
        246 to 30,  247 to 55,  // Larvitar → Pupitar → Tyranitar
        // ── Gen III (keys = internal ROM species IDs, NOT national Dex) ──────
        277 to 16,  278 to 36,  // Treecko → Grovyle → Sceptile
        280 to 16,  281 to 36,  // Torchic → Combusken → Blaziken
        283 to 16,  284 to 36,  // Mudkip → Marshtomp → Swampert
        286 to 18,              // Poochyena → Mightyena
        288 to 20,              // Zigzagoon → Linoone
        290 to 7,               // Wurmple → Silcoon/Cascoon (personality)
        291 to 10,              // Silcoon → Beautifly
        293 to 10,              // Cascoon → Dustox
        295 to 14,              // Lotad → Lombre
        298 to 14,              // Seedot → Nuzleaf
        304 to 22,              // Taillow → Swellow
        309 to 25,              // Wingull → Pelipper
        392 to 16,  393 to 30,  // Ralts → Kirlia → Gardevoir
        311 to 22,              // Surskit → Masquerain
        306 to 23,              // Shroomish → Breloom
        364 to 18,  365 to 36,  // Slakoth → Vigoroth → Slaking
        301 to 20,              // Nincada → Ninjask
        370 to 20,  371 to 40,  // Whismur → Loudred → Exploud
        335 to 24,              // Makuhita → Hariyama
        382 to 32,  383 to 42,  // Aron → Lairon → Aggron
        356 to 37,              // Meditite → Medicham
        337 to 26,              // Electrike → Manectric
        367 to 26,              // Gulpin → Swalot
        330 to 30,              // Carvanha → Sharpedo
        313 to 40,              // Wailmer → Wailord
        339 to 33,              // Numel → Camerupt
        351 to 32,              // Spoink → Grumpig
        332 to 35,  333 to 45,  // Trapinch → Vibrava → Flygon
        344 to 32,              // Cacnea → Cacturne
        358 to 35,              // Swablu → Altaria
        323 to 30,              // Barboach → Whiscash
        326 to 30,              // Corphish → Crawdaunt
        318 to 36,              // Baltoy → Claydol
        388 to 40,              // Lileep → Cradily
        390 to 40,              // Anorith → Armaldo
        328 to 35,              // Feebas (beauty proxy)
        377 to 37,              // Shuppet → Banette
        361 to 37,              // Duskull → Dusclops
        360 to 15,              // Wynaut → Wobbuffet
        346 to 42,              // Snorunt → Glalie
        341 to 32,  342 to 44,  // Spheal → Sealeo → Walrein
        381 to 30,  325 to 50,  // Bagon → Shelgon → Salamence
        398 to 20,  399 to 45,  // Beldum → Metang → Metagross
    )

    private val GEN3_METHODS: Map<Int, String> = mapOf(
        // Friendship evos
        42  to "FRIEND",  // Golbat → Crobat
        113 to "FRIEND",  // Chansey → Blissey
        172 to "FRIEND",  // Pichu → Pikachu
        173 to "FRIEND",  // Cleffa → Clefairy
        174 to "FRIEND",  // Igglybuff → Jigglypuff
        175 to "FRIEND",  // Togepi → Togetic
        350 to "FRIEND",  // Azurill → Marill
        // Stone evos — mirrors Lua PokemonData.lua (Evolutions.{FIRE,WATER,...})
        // Gen I (internal ID == National Dex for 1–251)
        25  to "THUNDER", // Pikachu → Raichu
        30  to "MOON",    // Nidorina → Nidoqueen
        33  to "MOON",    // Nidorino → Nidoking
        35  to "MOON",    // Clefairy → Clefable
        37  to "FIRE",    // Vulpix → Ninetales
        39  to "MOON",    // Jigglypuff → Wigglytuff
        44  to "LF/SN",   // Gloom → Vileplume (Leaf) / Bellossom (Sun)
        58  to "FIRE",    // Growlithe → Arcanine
        61  to "WTR/37",  // Poliwhirl → Poliwrath (Water) / Politoed (Lv.37 randomizer)
        70  to "LEAF",    // Weepinbell → Victreebel
        90  to "WATER",   // Shellder → Cloyster
        102 to "LEAF",    // Exeggcute → Exeggutor
        120 to "WATER",   // Staryu → Starmie
        133 to "STONE",   // Eevee → Vap/Jolt/Flare/Espeon/Umbreon (5 stones)
        // Gen II
        191 to "SUN",     // Sunkern → Sunflora
        // Gen III (internal ROM IDs, NOT National Dex)
        296 to "WATER",   // Lombre → Ludicolo
        299 to "LEAF",    // Nuzleaf → Shiftry
        315 to "MOON",    // Skitty → Delcatty
        373 to "30/WTR",  // Clamperl → Huntail/Gorebyss (Lv.30 or Water replaces trade)
    )

    // ── MaxFR Gen IV/V (IDs 412–654) ─────────────────────────────────────────

    private val MAXFR_LEVELS: Map<Int, Int> = mapOf(
        // Gen IV (412–504) — first block of maxData/gen4.lua (Turtwig→Rotom)
        412 to 18,  413 to 32,  415 to 14,  416 to 36,  418 to 16,  419 to 36,
        421 to 14,  422 to 34,  424 to 15,  426 to 10,  428 to 15,  429 to 30,
        433 to 30,  435 to 30,  437 to 20,  440 to 21,  443 to 26,  445 to 25,
        447 to 30,  450 to 28,  456 to 38,  459 to 34,  461 to 33,  468 to 24,
        469 to 48,  474 to 34,  476 to 40,  478 to 37,  481 to 31,  484 to 40,
        // Gen V (505–654) — maxData/gen5.lua
        505 to 17,  506 to 36,  508 to 17,  509 to 36,  511 to 17,  512 to 36,
        514 to 20,  516 to 16,  517 to 32,  519 to 20,  529 to 21,  530 to 32,
        532 to 27,  534 to 25,  535 to 37,  539 to 31,  542 to 25,  543 to 37,
        545 to 25,  546 to 36,  550 to 20,  553 to 22,  554 to 30,  561 to 29,
        562 to 40,  564 to 35,  567 to 34,  569 to 39,  572 to 34,  574 to 37,
        576 to 37,  578 to 36,  580 to 30,  584 to 32,  585 to 41,  587 to 32,
        588 to 41,  590 to 35,  592 to 35,  593 to 47,  595 to 34,  600 to 39,
        602 to 40,  605 to 36,  607 to 40,  609 to 38,  610 to 49,  612 to 39,
        615 to 42,  617 to 41,  620 to 38,  621 to 48,  623 to 37,  629 to 50,
        632 to 43,  634 to 52,  637 to 54,  639 to 54,  643 to 50,  644 to 64,
        646 to 59,
    )

    private val MAXFR_METHODS: Map<Int, String> = mapOf(
        // Gen IV friendship evos
        431 to "FRIEND",  // Budew
        452 to "FRIEND",  // Buneary
        458 to "FRIEND",  // Chingling
        463 to "FRIEND",  // Bonsly
        464 to "FRIEND",  // Mime Jr.
        465 to "FRIEND",  // Happiny
        471 to "FRIEND",  // Munchlax
        472 to "FRIEND",  // Riolu
        483 to "FRIEND",  // Mantyke
        // Gen V stone / friendship evos
        521 to "LEAF",    // Pansage
        523 to "FIRE",    // Pansear
        525 to "WATER",   // Panpour
        527 to "MOON",    // Munna
        537 to "FRIEND",  // Woobat
        551 to "FRIEND",  // Swadloon
        556 to "SUN",     // Cottonee
        558 to "SUN",     // Petilil
        582 to "SUN",     // Minccino
        598 to "FRIEND",  // Karrablast
        613 to "THUNDER", // Eelektrik
        618 to "MOON",    // Lampent
        626 to "FRIEND",  // Shelmet
    )

    // ── NatDex (IDs 412–1235) ─────────────────────────────────────────────────

    private val NATDEX_LEVELS: Map<Int, Int> = mapOf(
        // Gen IV (412–504)
        412 to 18,  413 to 32,  415 to 14,  416 to 36,  418 to 16,  419 to 36,
        421 to 14,  422 to 34,  424 to 15,  426 to 10,  428 to 15,  429 to 30,
        433 to 30,  435 to 30,  437 to 20,  440 to 21,  443 to 26,  445 to 25,
        447 to 30,  450 to 28,  456 to 38,  459 to 34,  461 to 33,  463 to 15,
        464 to 15,  468 to 24,  469 to 48,  474 to 34,  476 to 40,  478 to 37,
        481 to 31,  483 to 35,  484 to 40,
        // Gen V (505–654)
        520 to 17,  521 to 36,  523 to 17,  524 to 36,  526 to 17,  527 to 36,
        529 to 20,  531 to 16,  532 to 32,  534 to 20,  544 to 21,  545 to 32,
        547 to 27,  549 to 25,  554 to 31,  557 to 25,  560 to 25,  561 to 36,
        565 to 20,  568 to 22,  569 to 30,  576 to 29,  577 to 40,  579 to 35,
        582 to 34,  584 to 39,  587 to 34,  589 to 37,  591 to 37,  593 to 36,
        595 to 30,  599 to 32,  600 to 41,  602 to 32,  603 to 41,  605 to 35,
        607 to 35,  608 to 47,  610 to 34,  615 to 39,  617 to 40,  620 to 36,
        622 to 40,  624 to 38,  625 to 49,  627 to 39,  630 to 42,  632 to 41,
        635 to 38,  636 to 48,  638 to 37,  644 to 50,  647 to 43,  649 to 52,
        650 to 62,  652 to 54,  654 to 54,  658 to 50,  659 to 64,  661 to 59,
        // Gen VI (655–721)
        675 to 16,  676 to 36,  678 to 16,  679 to 36,  681 to 16,  682 to 36,
        684 to 20,  686 to 17,  687 to 35,  689 to 9,   690 to 12,  692 to 35,
        694 to 19,  697 to 32,  699 to 32,  702 to 25,  704 to 35,  711 to 30,
        713 to 39,  715 to 48,  717 to 37,  721 to 39,  723 to 39,
        // Gen VII (722–809)
        729 to 40,  730 to 50,  737 to 37,  739 to 48,  747 to 17,  748 to 34,
        750 to 17,  751 to 34,  753 to 17,  754 to 34,  756 to 14,  757 to 28,
        759 to 20,  761 to 20,  767 to 25,  769 to 25,  772 to 38,  774 to 30,
        776 to 22,  778 to 34,  780 to 24,  782 to 33,  784 to 27,  786 to 18,
        787 to 29,  792 to 30,  794 to 42,  807 to 35,  808 to 45,
        // Gen VIII (810–905)
        814 to 43,  815 to 53,  828 to 50,  835 to 16,  836 to 35,  838 to 16,
        839 to 35,  841 to 16,  842 to 35,  844 to 24,  846 to 18,  847 to 38,
        849 to 10,  850 to 30,  852 to 18,  854 to 20,  856 to 24,  858 to 22,
        860 to 25,  862 to 18,  863 to 34,  868 to 36,  871 to 26,  873 to 30,
        875 to 28,  877 to 35,  881 to 32,  882 to 42,  884 to 32,  885 to 42,
        903 to 34,  910 to 50,  911 to 60,
        // Gen IX (906–1025)
        931 to 16,  932 to 36,  934 to 16,  935 to 36,  937 to 16,  938 to 36,
        940 to 18,  942 to 15,  944 to 24,  946 to 18,  949 to 25,  951 to 26,
        953 to 25,  954 to 35,  957 to 24,  958 to 38,  965 to 25,  967 to 30,
        969 to 28,  973 to 30,  980 to 35,  982 to 24,  983 to 38,  985 to 26,
        988 to 38,  990 to 40,  994 to 35,  996 to 30,  1021 to 35, 1022 to 54,
        1024 to 50,
        // Gen V (Scarlet/Violet era, IDs 1101–1235)
        1101 to 20, 1108 to 26, 1112 to 25, 1115 to 38, 1119 to 28, 1120 to 40,
        1124 to 34, 1126 to 42, 1131 to 38, 1132 to 20, 1133 to 35, 1136 to 34,
        1143 to 28, 1147 to 30, 1150 to 50, 1155 to 20, 1168 to 20, 1169 to 20,
        1183 to 35, 1230 to 50,
    )

    private val NATDEX_METHODS: Map<Int, String> = mapOf(
        // Gen IV
        431 to "FRIEND",   // Budew
        440 to "FEMALE",   // Combee (FEMALE21)
        452 to "FRIEND",   // Buneary
        458 to "FRIEND",   // Chingling
        465 to "SHINY",    // Happiny
        471 to "FRIEND",   // Munchlax
        472 to "FRIEND",   // Riolu
        // Gen V
        536 to "LEAF",     // Pansage
        538 to "FIRE",     // Pansear
        540 to "WATER",    // Panpour
        542 to "MOON",     // Munna
        550 to "LINK",     // Boldore
        552 to "FRIEND",   // Swadloon
        558 to "LINK",     // Gurdurr
        566 to "FRIEND",   // Karrablast
        571 to "SUN",      // Cottonee
        573 to "SUN",      // Petilil
        597 to "SHINY",    // Minccino
        613 to "LINK",     // Klang
        628 to "THUNDER",  // Eelektrik
        633 to "DUSK",     // Lampent
        641 to "LINK",     // Shelmet
        // Gen VI
        695 to "SHINY",    // Floette (Eternal)
        705 to "DUSK",     // Doublade
        707 to "LINK",     // Phantump
        709 to "LINK",     // Pumpkaboo
        719 to "SUN",      // Helioptile
        733 to "LINK",     // Spritzee
        735 to "LINK",     // Swirlix
        762 to "THUNDER",  // Charjabug
        764 to "ICE",      // Crabrawler
        // Gen VII
        782 to "FEMALE",   // Salandit (FEMALE33)
        797 to "FRIEND",   // Type: Null
        865 to "SUN",      // Applin (SUN_LEAF_DAWN)
        879 to "MOON",     // Milcery
        893 to "SHINY",    // Sinistea
        897 to "FRIEND",   // Kubfu
        // Gen VIII
        909 to "LINK",     // Charcadet (METAL_COAT)
        916 to "WATER",    // Toedscool (WATER_DUSK)
        947 to "FRIEND",   // Frigibax
        960 to "MOON",     // Varoom (MOON_SUN)
        963 to "THUNDER",  // Tadbulb
        971 to "FRIEND",   // Pawmi
        976 to "FIRE",     // Dipplin
        978 to "FRIEND",   // Poltchageist
        999 to "ICE",      // Cetoddle
        1036 to "FRIEND",  // Greavard
        1037 to "MOON",    // Flittle
        // Gen IX (IDs 1101+)
        1104 to "ICE",
        1106 to "ICE",
        1110 to "FRIEND",
        1113 to "LINK",
        1122 to "WATER",   // WATER_ROCK
        1134 to "ICE",
        1138 to "FIRE",
        1140 to "LEAF",
        1144 to "LINK",    // RAZOR_CLAW
        1196 to "LINK",
        1197 to "LINK",
        1198 to "LINK",
    )

    // ─────────────────────────────────────────────────────────────────────────

    /** Level at which [speciesId] evolves, or 0 if no level-based evolution. */
    fun get(speciesId: Int, isMaxFr: Boolean = false): Int = when {
        speciesId <= 411 -> TABLE[speciesId] ?: 0
        isMaxFr          -> MAXFR_LEVELS[speciesId] ?: 0
        else             -> NATDEX_LEVELS[speciesId] ?: 0
    }

    /** Evolution method label (e.g. "FRIEND", "FIRE", "LINK") or null if level-based / none. */
    fun getMethod(speciesId: Int, isMaxFr: Boolean = false): String? = when {
        speciesId <= 411 -> GEN3_METHODS[speciesId]
        isMaxFr          -> MAXFR_METHODS[speciesId]
        else             -> NATDEX_METHODS[speciesId]
    }
}
