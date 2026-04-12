package hh.game.mgba_android.tracker.tables

/**
 * Gen III Pokémon species names, indexed by National Dex ID.
 * Index 0 = "???" (empty/egg), index 1 = Bulbasaur, ..., index 386 = Deoxys.
 *
 * Used as the nameTable callback in PokemonDecoder.decode().
 * Source: Gen III game data / Bulbapedia.
 */
object SpeciesNames {

    private val NAMES = arrayOf(
        "???",           // 0 — empty slot
        "Bulbasaur",     // 1
        "Ivysaur",       // 2
        "Venusaur",      // 3
        "Charmander",    // 4
        "Charmeleon",    // 5
        "Charizard",     // 6
        "Squirtle",      // 7
        "Wartortle",     // 8
        "Blastoise",     // 9
        "Caterpie",      // 10
        "Metapod",       // 11
        "Butterfree",    // 12
        "Weedle",        // 13
        "Kakuna",        // 14
        "Beedrill",      // 15
        "Pidgey",        // 16
        "Pidgeotto",     // 17
        "Pidgeot",       // 18
        "Rattata",       // 19
        "Raticate",      // 20
        "Spearow",       // 21
        "Fearow",        // 22
        "Ekans",         // 23
        "Arbok",         // 24
        "Pikachu",       // 25
        "Raichu",        // 26
        "Sandshrew",     // 27
        "Sandslash",     // 28
        "Nidoran♀",      // 29
        "Nidorina",      // 30
        "Nidoqueen",     // 31
        "Nidoran♂",      // 32
        "Nidorino",      // 33
        "Nidoking",      // 34
        "Clefairy",      // 35
        "Clefable",      // 36
        "Vulpix",        // 37
        "Ninetales",     // 38
        "Jigglypuff",    // 39
        "Wigglytuff",    // 40
        "Zubat",         // 41
        "Golbat",        // 42
        "Oddish",        // 43
        "Gloom",         // 44
        "Vileplume",     // 45
        "Paras",         // 46
        "Parasect",      // 47
        "Venonat",       // 48
        "Venomoth",      // 49
        "Diglett",       // 50
        "Dugtrio",       // 51
        "Meowth",        // 52
        "Persian",       // 53
        "Psyduck",       // 54
        "Golduck",       // 55
        "Mankey",        // 56
        "Primeape",      // 57
        "Growlithe",     // 58
        "Arcanine",      // 59
        "Poliwag",       // 60
        "Poliwhirl",     // 61
        "Poliwrath",     // 62
        "Abra",          // 63
        "Kadabra",       // 64
        "Alakazam",      // 65
        "Machop",        // 66
        "Machoke",       // 67
        "Machamp",       // 68
        "Bellsprout",    // 69
        "Weepinbell",    // 70
        "Victreebel",    // 71
        "Tentacool",     // 72
        "Tentacruel",    // 73
        "Geodude",       // 74
        "Graveler",      // 75
        "Golem",         // 76
        "Ponyta",        // 77
        "Rapidash",      // 78
        "Slowpoke",      // 79
        "Slowbro",       // 80
        "Magnemite",     // 81
        "Magneton",      // 82
        "Farfetch'd",    // 83
        "Doduo",         // 84
        "Dodrio",        // 85
        "Seel",          // 86
        "Dewgong",       // 87
        "Grimer",        // 88
        "Muk",           // 89
        "Shellder",      // 90
        "Cloyster",      // 91
        "Gastly",        // 92
        "Haunter",       // 93
        "Gengar",        // 94
        "Onix",          // 95
        "Drowzee",       // 96
        "Hypno",         // 97
        "Krabby",        // 98
        "Kingler",       // 99
        "Voltorb",       // 100
        "Electrode",     // 101
        "Exeggcute",     // 102
        "Exeggutor",     // 103
        "Cubone",        // 104
        "Marowak",       // 105
        "Hitmonlee",     // 106
        "Hitmonchan",    // 107
        "Lickitung",     // 108
        "Koffing",       // 109
        "Weezing",       // 110
        "Rhyhorn",       // 111
        "Rhydon",        // 112
        "Chansey",       // 113
        "Tangela",       // 114
        "Kangaskhan",    // 115
        "Horsea",        // 116
        "Seadra",        // 117
        "Goldeen",       // 118
        "Seaking",       // 119
        "Staryu",        // 120
        "Starmie",       // 121
        "Mr. Mime",      // 122
        "Scyther",       // 123
        "Jynx",          // 124
        "Electabuzz",    // 125
        "Magmar",        // 126
        "Pinsir",        // 127
        "Tauros",        // 128
        "Magikarp",      // 129
        "Gyarados",      // 130
        "Lapras",        // 131
        "Ditto",         // 132
        "Eevee",         // 133
        "Vaporeon",      // 134
        "Jolteon",       // 135
        "Flareon",       // 136
        "Porygon",       // 137
        "Omanyte",       // 138
        "Omastar",       // 139
        "Kabuto",        // 140
        "Kabutops",      // 141
        "Aerodactyl",    // 142
        "Snorlax",       // 143
        "Articuno",      // 144
        "Zapdos",        // 145
        "Moltres",       // 146
        "Dratini",       // 147
        "Dragonair",     // 148
        "Dragonite",     // 149
        "Mewtwo",        // 150
        "Mew",           // 151
        "Chikorita",     // 152
        "Bayleef",       // 153
        "Meganium",      // 154
        "Cyndaquil",     // 155
        "Quilava",       // 156
        "Typhlosion",    // 157
        "Totodile",      // 158
        "Croconaw",      // 159
        "Feraligatr",    // 160
        "Sentret",       // 161
        "Furret",        // 162
        "Hoothoot",      // 163
        "Noctowl",       // 164
        "Ledyba",        // 165
        "Ledian",        // 166
        "Spinarak",      // 167
        "Ariados",       // 168
        "Crobat",        // 169
        "Chinchou",      // 170
        "Lanturn",       // 171
        "Pichu",         // 172
        "Cleffa",        // 173
        "Igglybuff",     // 174
        "Togepi",        // 175
        "Togetic",       // 176
        "Natu",          // 177
        "Xatu",          // 178
        "Mareep",        // 179
        "Flaaffy",       // 180
        "Ampharos",      // 181
        "Bellossom",     // 182
        "Marill",        // 183
        "Azumarill",     // 184
        "Sudowoodo",     // 185
        "Politoed",      // 186
        "Hoppip",        // 187
        "Skiploom",      // 188
        "Jumpluff",      // 189
        "Aipom",         // 190
        "Sunkern",       // 191
        "Sunflora",      // 192
        "Yanma",         // 193
        "Wooper",        // 194
        "Quagsire",      // 195
        "Espeon",        // 196
        "Umbreon",       // 197
        "Murkrow",       // 198
        "Slowking",      // 199
        "Misdreavus",    // 200
        "Unown",         // 201
        "Wobbuffet",     // 202
        "Girafarig",     // 203
        "Pineco",        // 204
        "Forretress",    // 205
        "Dunsparce",     // 206
        "Gligar",        // 207
        "Steelix",       // 208
        "Snubbull",      // 209
        "Granbull",      // 210
        "Qwilfish",      // 211
        "Scizor",        // 212
        "Shuckle",       // 213
        "Heracross",     // 214
        "Sneasel",       // 215
        "Teddiursa",     // 216
        "Ursaring",      // 217
        "Slugma",        // 218
        "Magcargo",      // 219
        "Swinub",        // 220
        "Piloswine",     // 221
        "Corsola",       // 222
        "Remoraid",      // 223
        "Octillery",     // 224
        "Delibird",      // 225
        "Mantine",       // 226
        "Skarmory",      // 227
        "Houndour",      // 228
        "Houndoom",      // 229
        "Kingdra",       // 230
        "Phanpy",        // 231
        "Donphan",       // 232
        "Porygon2",      // 233
        "Stantler",      // 234
        "Smeargle",      // 235
        "Tyrogue",       // 236
        "Hitmontop",     // 237
        "Smoochum",      // 238
        "Elekid",        // 239
        "Magby",         // 240
        "Miltank",       // 241
        "Blissey",       // 242
        "Raikou",        // 243
        "Entei",         // 244
        "Suicune",       // 245
        "Larvitar",      // 246
        "Pupitar",       // 247
        "Tyranitar",     // 248
        "Lugia",         // 249
        "Ho-Oh",         // 250
        "Celebi",        // 251
        // 252–276: Gen III Pokémon (Hoenn starters etc.)
        "Treecko",       // 252
        "Grovyle",       // 253
        "Sceptile",      // 254
        "Torchic",       // 255
        "Combusken",     // 256
        "Blaziken",      // 257
        "Mudkip",        // 258
        "Marshtomp",     // 259
        "Swampert",      // 260
        "Poochyena",     // 261
        "Mightyena",     // 262
        "Zigzagoon",     // 263
        "Linoone",       // 264
        "Wurmple",       // 265
        "Silcoon",       // 266
        "Beautifly",     // 267
        "Cascoon",       // 268
        "Dustox",        // 269
        "Lotad",         // 270
        "Lombre",        // 271
        "Ludicolo",      // 272
        "Seedot",        // 273
        "Nuzleaf",       // 274
        "Shiftry",       // 275
        "Taillow",       // 276
        "Swellow",       // 277
        "Wingull",       // 278
        "Pelipper",      // 279
        "Ralts",         // 280
        "Kirlia",        // 281
        "Gardevoir",     // 282
        "Surskit",       // 283
        "Masquerain",    // 284
        "Shroomish",     // 285
        "Breloom",       // 286
        "Slakoth",       // 287
        "Vigoroth",      // 288
        "Slaking",       // 289
        "Nincada",       // 290
        "Ninjask",       // 291
        "Shedinja",      // 292
        "Whismur",       // 293
        "Loudred",       // 294
        "Exploud",       // 295
        "Makuhita",      // 296
        "Hariyama",      // 297
        "Azurill",       // 298
        "Nosepass",      // 299
        "Skitty",        // 300
        "Delcatty",      // 301
        "Sableye",       // 302
        "Mawile",        // 303
        "Aron",          // 304
        "Lairon",        // 305
        "Aggron",        // 306
        "Meditite",      // 307
        "Medicham",      // 308
        "Electrike",     // 309
        "Manectric",     // 310
        "Plusle",        // 311
        "Minun",         // 312
        "Volbeat",       // 313
        "Illumise",      // 314
        "Roselia",       // 315
        "Gulpin",        // 316
        "Swalot",        // 317
        "Carvanha",      // 318
        "Sharpedo",      // 319
        "Wailmer",       // 320
        "Wailord",       // 321
        "Numel",         // 322
        "Camerupt",      // 323
        "Torkoal",       // 324
        "Spoink",        // 325
        "Grumpig",       // 326
        "Spinda",        // 327
        "Trapinch",      // 328
        "Vibrava",       // 329
        "Flygon",        // 330
        "Cacnea",        // 331
        "Cacturne",      // 332
        "Swablu",        // 333
        "Altaria",       // 334
        "Zangoose",      // 335
        "Seviper",       // 336
        "Lunatone",      // 337
        "Solrock",       // 338
        "Barboach",      // 339
        "Whiscash",      // 340
        "Corphish",      // 341
        "Crawdaunt",     // 342
        "Baltoy",        // 343
        "Claydol",       // 344
        "Lileep",        // 345
        "Cradily",       // 346
        "Anorith",       // 347
        "Armaldo",       // 348
        "Feebas",        // 349
        "Milotic",       // 350
        "Castform",      // 351
        "Kecleon",       // 352
        "Shuppet",       // 353
        "Banette",       // 354
        "Duskull",       // 355
        "Dusclops",      // 356
        "Tropius",       // 357
        "Chimecho",      // 358
        "Absol",         // 359
        "Wynaut",        // 360
        "Snorunt",       // 361
        "Glalie",        // 362
        "Spheal",        // 363
        "Sealeo",        // 364
        "Walrein",       // 365
        "Clamperl",      // 366
        "Huntail",       // 367
        "Gorebyss",      // 368
        "Relicanth",     // 369
        "Luvdisc",       // 370
        "Bagon",         // 371
        "Shelgon",       // 372
        "Salamence",     // 373
        "Beldum",        // 374
        "Metang",        // 375
        "Metagross",     // 376
        "Regirock",      // 377
        "Regice",        // 378
        "Registeel",     // 379
        "Latias",        // 380
        "Latios",        // 381
        "Kyogre",        // 382
        "Groudon",       // 383
        "Rayquaza",      // 384
        "Jirachi",       // 385
        "Deoxys",        // 386
    )

    /**
     * Returns the display name for a Gen III internal species ID.
     *
     * IDs 1–251 (Kanto/Johto): internal ID == National Dex number — direct lookup.
     * IDs 277–411 (Hoenn): internal ordering differs from National Dex.
     *   Remapping mirrors PokemonData.lua `idInternalToNat` in the Lua tracker.
     * IDs 252–276: unused slots in ROM — returned as "#id".
     */
    fun get(internalId: Int): String {
        if (internalId > 411) return NATDEX_NAMES[internalId] ?: "#$internalId"
        val nationalId = INTERNAL_TO_NATIONAL[internalId] ?: internalId
        return if (nationalId in NAMES.indices) NAMES[nationalId] else "#$internalId"
    }

    // NatDex ROM hack species names (IDs 412–1235), from CyanSMP64/NatDexExtension pokeNameList.
    // IDs are the hack's internal species IDs, not National Dex numbers.
    private val NATDEX_NAMES = mapOf(
        412 to "Turtwig",
        413 to "Grotle",
        414 to "Torterra",
        415 to "Chimchar",
        416 to "Monferno",
        417 to "Infernape",
        418 to "Piplup",
        419 to "Prinplup",
        420 to "Empoleon",
        421 to "Starly",
        422 to "Staravia",
        423 to "Staraptor",
        424 to "Bidoof",
        425 to "Bibarel",
        426 to "Kricketot",
        427 to "Kricketune",
        428 to "Shinx",
        429 to "Luxio",
        430 to "Luxray",
        431 to "Budew",
        432 to "Roserade",
        433 to "Cranidos",
        434 to "Rampardos",
        435 to "Shieldon",
        436 to "Bastiodon",
        437 to "Burmy",
        438 to "Wormadam",
        439 to "Mothim",
        440 to "Combee",
        441 to "Vespiquen",
        442 to "Pachirisu",
        443 to "Buizel",
        444 to "Floatzel",
        445 to "Cherubi",
        446 to "Cherrim",
        447 to "Shellos",
        448 to "Gastrodon",
        449 to "Ambipom",
        450 to "Drifloon",
        451 to "Drifblim",
        452 to "Buneary",
        453 to "Lopunny",
        454 to "Mismagius",
        455 to "Honchkrow",
        456 to "Glameow",
        457 to "Purugly",
        458 to "Chingling",
        459 to "Stunky",
        460 to "Skuntank",
        461 to "Bronzor",
        462 to "Bronzong",
        463 to "Bonsly",
        464 to "Mime Jr.",
        465 to "Happiny",
        466 to "Chatot",
        467 to "Spiritomb",
        468 to "Gible",
        469 to "Gabite",
        470 to "Garchomp",
        471 to "Munchlax",
        472 to "Riolu",
        473 to "Lucario",
        474 to "Hippopotas",
        475 to "Hippowdon",
        476 to "Skorupi",
        477 to "Drapion",
        478 to "Croagunk",
        479 to "Toxicroak",
        480 to "Carnivine",
        481 to "Finneon",
        482 to "Lumineon",
        483 to "Mantyke",
        484 to "Snover",
        485 to "Abomasnow",
        486 to "Weavile",
        487 to "Magnezone",
        488 to "Lickilicky",
        489 to "Rhyperior",
        490 to "Tangrowth",
        491 to "Electivire",
        492 to "Magmortar",
        493 to "Togekiss",
        494 to "Yanmega",
        495 to "Leafeon",
        496 to "Glaceon",
        497 to "Gliscor",
        498 to "Mamoswine",
        499 to "Porygon-Z",
        500 to "Gallade",
        501 to "Probopass",
        502 to "Dusknoir",
        503 to "Froslass",
        504 to "Rotom",
        505 to "Uxie",
        506 to "Mesprit",
        507 to "Azelf",
        508 to "Dialga",
        509 to "Palkia",
        510 to "Heatran",
        511 to "Regigigas",
        512 to "Giratina",
        513 to "Cresselia",
        514 to "Phione",
        515 to "Manaphy",
        516 to "Darkrai",
        517 to "Shaymin",
        518 to "Arceus",
        519 to "Victini",
        520 to "Snivy",
        521 to "Servine",
        522 to "Serperior",
        523 to "Tepig",
        524 to "Pignite",
        525 to "Emboar",
        526 to "Oshawott",
        527 to "Dewott",
        528 to "Samurott",
        529 to "Patrat",
        530 to "Watchog",
        531 to "Lillipup",
        532 to "Herdier",
        533 to "Stoutland",
        534 to "Purrloin",
        535 to "Liepard",
        536 to "Pansage",
        537 to "Simisage",
        538 to "Pansear",
        539 to "Simisear",
        540 to "Panpour",
        541 to "Simipour",
        542 to "Munna",
        543 to "Musharna",
        544 to "Pidove",
        545 to "Tranquill",
        546 to "Unfezant",
        547 to "Blitzle",
        548 to "Zebstrika",
        549 to "Roggenrola",
        550 to "Boldore",
        551 to "Gigalith",
        552 to "Woobat",
        553 to "Swoobat",
        554 to "Drilbur",
        555 to "Excadrill",
        556 to "Audino",
        557 to "Timburr",
        558 to "Gurdurr",
        559 to "Conkeldurr",
        560 to "Tympole",
        561 to "Palpitoad",
        562 to "Seismitoad",
        563 to "Throh",
        564 to "Sawk",
        565 to "Sewaddle",
        566 to "Swadloon",
        567 to "Leavanny",
        568 to "Venipede",
        569 to "Whirlipede",
        570 to "Scolipede",
        571 to "Cottonee",
        572 to "Whimsicott",
        573 to "Petilil",
        574 to "Lilligant",
        575 to "Basculin",
        576 to "Sandile",
        577 to "Krokorok",
        578 to "Krookodile",
        579 to "Darumaka",
        580 to "Darmanitan",
        581 to "Maractus",
        582 to "Dwebble",
        583 to "Crustle",
        584 to "Scraggy",
        585 to "Scrafty",
        586 to "Sigilyph",
        587 to "Yamask",
        588 to "Cofagrigus",
        589 to "Tirtouga",
        590 to "Carracosta",
        591 to "Archen",
        592 to "Archeops",
        593 to "Trubbish",
        594 to "Garbodor",
        595 to "Zorua",
        596 to "Zoroark",
        597 to "Minccino",
        598 to "Cinccino",
        599 to "Gothita",
        600 to "Gothorita",
        601 to "Gothitelle",
        602 to "Solosis",
        603 to "Duosion",
        604 to "Reuniclus",
        605 to "Ducklett",
        606 to "Swanna",
        607 to "Vanillite",
        608 to "Vanillish",
        609 to "Vanilluxe",
        610 to "Deerling",
        611 to "Sawsbuck",
        612 to "Emolga",
        613 to "Karrablast",
        614 to "Escavalier",
        615 to "Foongus",
        616 to "Amoonguss",
        617 to "Frillish",
        618 to "Jellicent",
        619 to "Alomomola",
        620 to "Joltik",
        621 to "Galvantula",
        622 to "Ferroseed",
        623 to "Ferrothorn",
        624 to "Klink",
        625 to "Klang",
        626 to "Klinklang",
        627 to "Tynamo",
        628 to "Eelektrik",
        629 to "Eelektross",
        630 to "Elgyem",
        631 to "Beheeyem",
        632 to "Litwick",
        633 to "Lampent",
        634 to "Chandelure",
        635 to "Axew",
        636 to "Fraxure",
        637 to "Haxorus",
        638 to "Cubchoo",
        639 to "Beartic",
        640 to "Cryogonal",
        641 to "Shelmet",
        642 to "Accelgor",
        643 to "Stunfisk",
        644 to "Mienfoo",
        645 to "Mienshao",
        646 to "Druddigon",
        647 to "Golett",
        648 to "Golurk",
        649 to "Pawniard",
        650 to "Bisharp",
        651 to "Bouffalant",
        652 to "Rufflet",
        653 to "Braviary",
        654 to "Vullaby",
        655 to "Mandibuzz",
        656 to "Heatmor",
        657 to "Durant",
        658 to "Deino",
        659 to "Zweilous",
        660 to "Hydreigon",
        661 to "Larvesta",
        662 to "Volcarona",
        663 to "Cobalion",
        664 to "Terrakion",
        665 to "Virizion",
        666 to "Tornadus",
        667 to "Thundurus",
        668 to "Reshiram",
        669 to "Zekrom",
        670 to "Landorus",
        671 to "Kyurem",
        672 to "Keldeo",
        673 to "Meloetta",
        674 to "Genesect",
        675 to "Chespin",
        676 to "Quilladin",
        677 to "Chesnaught",
        678 to "Fennekin",
        679 to "Braixen",
        680 to "Delphox",
        681 to "Froakie",
        682 to "Frogadier",
        683 to "Greninja",
        684 to "Bunnelby",
        685 to "Diggersby",
        686 to "Fletchling",
        687 to "Fletchinder",
        688 to "Talonflame",
        689 to "Scatterbug",
        690 to "Spewpa",
        691 to "Vivillon",
        692 to "Litleo",
        693 to "Pyroar",
        694 to "Flabébé",
        695 to "Floette",
        696 to "Florges",
        697 to "Skiddo",
        698 to "Gogoat",
        699 to "Pancham",
        700 to "Pangoro",
        701 to "Furfrou",
        702 to "Espurr",
        703 to "Meowstic",
        704 to "Honedge",
        705 to "Doublade",
        706 to "Aegislash",
        707 to "Spritzee",
        708 to "Aromatisse",
        709 to "Swirlix",
        710 to "Slurpuff",
        711 to "Inkay",
        712 to "Malamar",
        713 to "Binacle",
        714 to "Barbaracle",
        715 to "Skrelp",
        716 to "Dragalge",
        717 to "Clauncher",
        718 to "Clawitzer",
        719 to "Helioptile",
        720 to "Heliolisk",
        721 to "Tyrunt",
        722 to "Tyrantrum",
        723 to "Amaura",
        724 to "Aurorus",
        725 to "Sylveon",
        726 to "Hawlucha",
        727 to "Dedenne",
        728 to "Carbink",
        729 to "Goomy",
        730 to "Sliggoo",
        731 to "Goodra",
        732 to "Klefki",
        733 to "Phantump",
        734 to "Trevenant",
        735 to "Pumpkaboo",
        736 to "Gourgeist",
        737 to "Bergmite",
        738 to "Avalugg",
        739 to "Noibat",
        740 to "Noivern",
        741 to "Xerneas",
        742 to "Yveltal",
        743 to "Zygarde",
        744 to "Diancie",
        745 to "Hoopa",
        746 to "Volcanion",
        747 to "Rowlet",
        748 to "Dartrix",
        749 to "Decidueye",
        750 to "Litten",
        751 to "Torracat",
        752 to "Incineroar",
        753 to "Popplio",
        754 to "Brionne",
        755 to "Primarina",
        756 to "Pikipek",
        757 to "Trumbeak",
        758 to "Toucannon",
        759 to "Yungoos",
        760 to "Gumshoos",
        761 to "Grubbin",
        762 to "Charjabug",
        763 to "Vikavolt",
        764 to "Crabrawler",
        765 to "Crabominable",
        766 to "Oricorio",
        767 to "Cutiefly",
        768 to "Ribombee",
        769 to "Rockruff",
        770 to "Lycanroc",
        771 to "Wishiwashi",
        772 to "Mareanie",
        773 to "Toxapex",
        774 to "Mudbray",
        775 to "Mudsdale",
        776 to "Dewpider",
        777 to "Araquanid",
        778 to "Fomantis",
        779 to "Lurantis",
        780 to "Morelull",
        781 to "Shiinotic",
        782 to "Salandit",
        783 to "Salazzle",
        784 to "Stufful",
        785 to "Bewear",
        786 to "Bounsweet",
        787 to "Steenee",
        788 to "Tsareena",
        789 to "Comfey",
        790 to "Oranguru",
        791 to "Passimian",
        792 to "Wimpod",
        793 to "Golisopod",
        794 to "Sandygast",
        795 to "Palossand",
        796 to "Pyukumuku",
        797 to "Type: Null",
        798 to "Silvally",
        799 to "Minior",
        800 to "Komala",
        801 to "Turtonator",
        802 to "Togedemaru",
        803 to "Mimikyu",
        804 to "Bruxish",
        805 to "Drampa",
        806 to "Dhelmise",
        807 to "Jangmo-o",
        808 to "Hakamo-o",
        809 to "Kommo-o",
        810 to "Tapu Koko",
        811 to "Tapu Lele",
        812 to "Tapu Bulu",
        813 to "Tapu Fini",
        814 to "Cosmog",
        815 to "Cosmoem",
        816 to "Solgaleo",
        817 to "Lunala",
        818 to "Nihilego",
        819 to "Buzzwole",
        820 to "Pheromosa",
        821 to "Xurkitree",
        822 to "Celesteela",
        823 to "Kartana",
        824 to "Guzzlord",
        825 to "Necrozma",
        826 to "Magearna",
        827 to "Marshadow",
        828 to "Poipole",
        829 to "Naganadel",
        830 to "Stakataka",
        831 to "Blacephalon",
        832 to "Zeraora",
        833 to "Meltan",
        834 to "Melmetal",
        835 to "Grookey",
        836 to "Thwackey",
        837 to "Rillaboom",
        838 to "Scorbunny",
        839 to "Raboot",
        840 to "Cinderace",
        841 to "Sobble",
        842 to "Drizzile",
        843 to "Inteleon",
        844 to "Skwovet",
        845 to "Greedent",
        846 to "Rookidee",
        847 to "Corvisquire",
        848 to "Corviknight",
        849 to "Blipbug",
        850 to "Dottler",
        851 to "Orbeetle",
        852 to "Nickit",
        853 to "Thievul",
        854 to "Gossifleur",
        855 to "Eldegoss",
        856 to "Wooloo",
        857 to "Dubwool",
        858 to "Chewtle",
        859 to "Drednaw",
        860 to "Yamper",
        861 to "Boltund",
        862 to "Rolycoly",
        863 to "Carkol",
        864 to "Coalossal",
        865 to "Applin",
        866 to "Flapple",
        867 to "Appletun",
        868 to "Silicobra",
        869 to "Sandaconda",
        870 to "Cramorant",
        871 to "Arrokuda",
        872 to "Barraskewda",
        873 to "Toxel",
        874 to "Toxtricity",
        875 to "Sizzlipede",
        876 to "Centiskorch",
        877 to "Clobbopus",
        878 to "Grapploct",
        879 to "Sinistea",
        880 to "Polteageist",
        881 to "Hatenna",
        882 to "Hattrem",
        883 to "Hatterene",
        884 to "Impidimp",
        885 to "Morgrem",
        886 to "Grimmsnarl",
        887 to "Obstagoon",
        888 to "Perrserker",
        889 to "Cursola",
        890 to "Sirfetch'd",
        891 to "Mr. Rime",
        892 to "Runerigus",
        893 to "Milcery",
        894 to "Alcremie",
        895 to "Falinks",
        896 to "Pincurchin",
        897 to "Snom",
        898 to "Frosmoth",
        899 to "Stonjourner",
        900 to "Eiscue",
        901 to "Indeedee",
        902 to "Morpeko",
        903 to "Cufant",
        904 to "Copperajah",
        905 to "Dracozolt",
        906 to "Arctozolt",
        907 to "Dracovish",
        908 to "Arctovish",
        909 to "Duraludon",
        910 to "Dreepy",
        911 to "Drakloak",
        912 to "Dragapult",
        913 to "Zacian",
        914 to "Zamazenta",
        915 to "Eternatus",
        916 to "Kubfu",
        917 to "Urshifu",
        918 to "Zarude",
        919 to "Regieleki",
        920 to "Regidrago",
        921 to "Glastrier",
        922 to "Spectrier",
        923 to "Calyrex",
        924 to "Wyrdeer",
        925 to "Kleavor",
        926 to "Ursaluna",
        927 to "Basculegion",
        928 to "Sneasler",
        929 to "Overqwil",
        930 to "Enamorus",
        931 to "Sprigatito",
        932 to "Floragato",
        933 to "Meowscarada",
        934 to "Fuecoco",
        935 to "Crocalor",
        936 to "Skeledirge",
        937 to "Quaxly",
        938 to "Quaxwell",
        939 to "Quaquaval",
        940 to "Lechonk",
        941 to "Oinkologne",
        942 to "Tarountula",
        943 to "Spidops",
        944 to "Nymble",
        945 to "Lokix",
        946 to "Pawmi",
        947 to "Pawmo",
        948 to "Pawmot",
        949 to "Tandemaus",
        950 to "Maushold",
        951 to "Fidough",
        952 to "Dachsbun",
        953 to "Smoliv",
        954 to "Dolliv",
        955 to "Arboliva",
        956 to "Squawkabilly",
        957 to "Nacli",
        958 to "Naclstack",
        959 to "Garganacl",
        960 to "Charcadet",
        961 to "Armarouge",
        962 to "Ceruledge",
        963 to "Tadbulb",
        964 to "Bellibolt",
        965 to "Wattrel",
        966 to "Kilowattrel",
        967 to "Maschiff",
        968 to "Mabosstiff",
        969 to "Shroodle",
        970 to "Grafaiai",
        971 to "Bramblin",
        972 to "Brambleghast",
        973 to "Toedscool",
        974 to "Toedscruel",
        975 to "Klawf",
        976 to "Capsakid",
        977 to "Scovillain",
        978 to "Rellor",
        979 to "Rabsca",
        980 to "Flittle",
        981 to "Espathra",
        982 to "Tinkatink",
        983 to "Tinkatuff",
        984 to "Tinkaton",
        985 to "Wiglett",
        986 to "Wugtrio",
        987 to "Bombirdier",
        988 to "Finizen",
        989 to "Palafin",
        990 to "Varoom",
        991 to "Revavroom",
        992 to "Cyclizar",
        993 to "Orthworm",
        994 to "Glimmet",
        995 to "Glimmora",
        996 to "Greavard",
        997 to "Houndstone",
        998 to "Flamigo",
        999 to "Cetoddle",
        1000 to "Cetitan",
        1001 to "Veluza",
        1002 to "Dondozo",
        1003 to "Tatsugiri",
        1004 to "Annihilape",
        1005 to "Clodsire",
        1006 to "Farigiraf",
        1007 to "Dudunsparce",
        1008 to "Kingambit",
        1009 to "Great Tusk",
        1010 to "Scream Tail",
        1011 to "Brute Bonnet",
        1012 to "Flutter Mane",
        1013 to "Slither Wing",
        1014 to "Sandy Shocks",
        1015 to "Iron Treads",
        1016 to "Iron Bundle",
        1017 to "Iron Hands",
        1018 to "Iron Jugulis",
        1019 to "Iron Moth",
        1020 to "Iron Thorns",
        1021 to "Frigibax",
        1022 to "Arctibax",
        1023 to "Baxcalibur",
        1024 to "Gimmighoul",
        1025 to "Gholdengo",
        1026 to "Wo-Chien",
        1027 to "Chien-Pao",
        1028 to "Ting-Lu",
        1029 to "Chi-Yu",
        1030 to "Roaring Moon",
        1031 to "Iron Valiant",
        1032 to "Koraidon",
        1033 to "Miraidon",
        1034 to "Walking Wake",
        1035 to "Iron Leaves",
        1036 to "Dipplin",
        1037 to "Poltchageist",
        1038 to "Sinistcha",
        1039 to "Okidogi",
        1040 to "Munkidori",
        1041 to "Fezandipiti",
        1042 to "Ogerpon",
        1043 to "Archaludon",
        1044 to "Hydrapple",
        1045 to "Gouging Fire",
        1046 to "Raging Bolt",
        1047 to "Iron Boulder",
        1048 to "Iron Crown",
        1049 to "Terapagos",
        1050 to "Pecharunt",
        1051 to "Venusaur M",
        1052 to "Charizard X",
        1053 to "Charizard Y",
        1054 to "Blastoise M",
        1055 to "Beedrill M",
        1056 to "Pidgeot M",
        1057 to "Alakazam M",
        1058 to "Slowbro M",
        1059 to "Gengar M",
        1060 to "Kangaskhan M",
        1061 to "Pinsir M",
        1062 to "Gyarados M",
        1063 to "Aerodactyl M",
        1064 to "Mewtwo X",
        1065 to "Mewtwo Y",
        1066 to "Ampharos M",
        1067 to "Steelix M",
        1068 to "Scizor M",
        1069 to "Heracross M",
        1070 to "Houndoom M",
        1071 to "Tyranitar M",
        1072 to "Sceptile M",
        1073 to "Blaziken M",
        1074 to "Swampert M",
        1075 to "Gardevoir M",
        1076 to "Sableye M",
        1077 to "Mawile M",
        1078 to "Aggron M",
        1079 to "Medicham M",
        1080 to "Manectric M",
        1081 to "Sharpedo M",
        1082 to "Camerupt M",
        1083 to "Altaria M",
        1084 to "Banette M",
        1085 to "Absol M",
        1086 to "Glalie M",
        1087 to "Salamence M",
        1088 to "Metagross M",
        1089 to "Latias M",
        1090 to "Latios M",
        1091 to "Lopunny M",
        1092 to "Garchomp M",
        1093 to "Lucario M",
        1094 to "Abomasnow M",
        1095 to "Gallade M",
        1096 to "Audino M",
        1097 to "Diancie M",
        1098 to "Rayquaza M",
        1099 to "Kyogre P",
        1100 to "Groudon P",
        1101 to "Rattata A",
        1102 to "Raticate A",
        1103 to "Raichu A",
        1104 to "Sandshrew A",
        1105 to "Sandslash A",
        1106 to "Vulpix A",
        1107 to "Ninetales A",
        1108 to "Diglett A",
        1109 to "Dugtrio A",
        1110 to "Meowth A",
        1111 to "Persian A",
        1112 to "Geodude A",
        1113 to "Graveler A",
        1114 to "Golem A",
        1115 to "Grimer A",
        1116 to "Muk A",
        1117 to "Exeggutor A",
        1118 to "Marowak A",
        1119 to "Meowth G",
        1120 to "Ponyta G",
        1121 to "Rapidash G",
        1122 to "Slowpoke G",
        1123 to "Slowbro G",
        1124 to "Farfetch'd G",
        1125 to "Weezing G",
        1126 to "Mr. Mime G",
        1127 to "Articuno G",
        1128 to "Zapdos G",
        1129 to "Moltres G",
        1130 to "Slowking G",
        1131 to "Corsola G",
        1132 to "Zigzagoon G",
        1133 to "Linoone G",
        1134 to "Darumaka G",
        1135 to "Darmanitan G",
        1136 to "Yamask G",
        1137 to "Stunfisk G",
        1138 to "Growlithe H",
        1139 to "Arcanine H",
        1140 to "Voltorb H",
        1141 to "Electrode H",
        1142 to "Typhlosion H",
        1143 to "Qwilfish H",
        1144 to "Sneasel H",
        1145 to "Samurott H",
        1146 to "Lilligant H",
        1147 to "Zorua H",
        1148 to "Zoroark H",
        1149 to "Braviary H",
        1150 to "Sliggoo H",
        1151 to "Goodra H",
        1152 to "Avalugg H",
        1153 to "Decidueye H",
        1154 to "Tauros P",
        1155 to "Wooper P",
        1156 to "Pikachu C",
        1157 to "Pikachu P",
        1158 to "Tauros P F",
        1159 to "Tauros P W",
        1160 to "Eevee P",
        1161 to "Pichu S",
        1162 to "Castform F",
        1163 to "Castform W",
        1164 to "Castform I",
        1165 to "Deoxys Atk",
        1166 to "Deoxys Def",
        1167 to "Deoxys Spe",
        1168 to "Burmy S",
        1169 to "Burmy T",
        1170 to "Wormadam S",
        1171 to "Wormadam T",
        1172 to "Cherrim S",
        1173 to "Rotom Heat",
        1174 to "Rotom Wash",
        1175 to "Rotom Frost",
        1176 to "Rotom Fan",
        1177 to "Rotom Mow",
        1178 to "Dialga O",
        1179 to "Palkia O",
        1180 to "Giratina O",
        1181 to "Shaymin S",
        1182 to "Basculin B",
        1183 to "Basculin W",
        1184 to "Darmanitan Z",
        1185 to "Darmanitan Z G",
        1186 to "Tornadus T",
        1187 to "Thundurus T",
        1188 to "Landorus T",
        1189 to "Kyurem W",
        1190 to "Kyurem B",
        1191 to "Meloetta P",
        1192 to "Greninja A",
        1193 to "Floette E",
        1194 to "Meowstic F",
        1195 to "Aegislash B",
        1196 to "Pumpkaboo S",
        1197 to "Pumpkaboo L",
        1198 to "Pumpkaboo X",
        1199 to "Gourgeist S",
        1200 to "Gourgeist L",
        1201 to "Gourgeist X",
        1202 to "Zygarde 10",
        1203 to "Zygarde C",
        1204 to "Hoopa U",
        1205 to "Oricorio E",
        1206 to "Oricorio P",
        1207 to "Oricorio G",
        1208 to "Lycanroc M",
        1209 to "Lycanroc D",
        1210 to "Wishiwashi S",
        1211 to "Minior C",
        1212 to "Necrozma DM",
        1213 to "Necrozma DW",
        1214 to "Necrozma U",
        1215 to "Toxtricity L",
        1216 to "Eiscue N",
        1217 to "Indeedee F",
        1218 to "Morpeko H",
        1219 to "Zacian C",
        1220 to "Zamazenta C",
        1221 to "Eternatus E",
        1222 to "Urshifu R",
        1223 to "Calyrex I",
        1224 to "Calyrex S",
        1225 to "Ursaluna B",
        1226 to "Basculegion F",
        1227 to "Enamorus T",
        1228 to "Oinkologne F",
        1229 to "Palafin H",
        1230 to "Gimmighoul R",
        1231 to "Ogerpon W",
        1232 to "Ogerpon F",
        1233 to "Ogerpon R",
        1234 to "Terapagos T",
        1235 to "Terapagos S",
    )



    // Internal Gen III ROM species ID → National Dex number
    // Source: PokemonData.lua idInternalToNat (Ironmon Tracker Lua, lines 365–379)
    // IDs 1–251 are identity (not listed); IDs 252–276 unused in ROM.
    private val INTERNAL_TO_NATIONAL = mapOf(
        277 to 252, 278 to 253, 279 to 254, 280 to 255, 281 to 256, 282 to 257, 283 to 258, 284 to 259,
        285 to 260, 286 to 261, 287 to 262, 288 to 263, 289 to 264, 290 to 265, 291 to 266, 292 to 267,
        293 to 268, 294 to 269, 295 to 270, 296 to 271, 297 to 272, 298 to 273, 299 to 274, 300 to 275,
        304 to 276, 305 to 277, 309 to 278, 310 to 279, 392 to 280, 393 to 281, 394 to 282, 311 to 283,
        312 to 284, 306 to 285, 307 to 286, 364 to 287, 365 to 288, 366 to 289, 301 to 290, 302 to 291,
        303 to 292, 370 to 293, 371 to 294, 372 to 295, 335 to 296, 336 to 297, 350 to 298, 320 to 299,
        315 to 300, 316 to 301, 322 to 302, 355 to 303, 382 to 304, 383 to 305, 384 to 306, 356 to 307,
        357 to 308, 337 to 309, 338 to 310, 353 to 311, 354 to 312, 386 to 313, 387 to 314, 363 to 315,
        367 to 316, 368 to 317, 330 to 318, 331 to 319, 313 to 320, 314 to 321, 339 to 322, 340 to 323,
        321 to 324, 351 to 325, 352 to 326, 308 to 327, 332 to 328, 333 to 329, 334 to 330, 344 to 331,
        345 to 332, 358 to 333, 359 to 334, 380 to 335, 379 to 336, 348 to 337, 349 to 338, 323 to 339,
        324 to 340, 326 to 341, 327 to 342, 318 to 343, 319 to 344, 388 to 345, 389 to 346, 390 to 347,
        391 to 348, 328 to 349, 329 to 350, 385 to 351, 317 to 352, 377 to 353, 378 to 354, 361 to 355,
        362 to 356, 369 to 357, 411 to 358, 376 to 359, 360 to 360, 346 to 361, 347 to 362, 341 to 363,
        342 to 364, 343 to 365, 373 to 366, 374 to 367, 375 to 368, 381 to 369, 325 to 370, 395 to 371,
        396 to 372, 397 to 373, 398 to 374, 399 to 375, 400 to 376, 401 to 377, 402 to 378, 403 to 379,
        407 to 380, 408 to 381, 404 to 382, 405 to 383, 406 to 384, 409 to 385, 410 to 386,
    )
}
