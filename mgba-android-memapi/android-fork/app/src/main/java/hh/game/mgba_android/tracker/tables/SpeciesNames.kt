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
        val nationalId = INTERNAL_TO_NATIONAL[internalId] ?: internalId
        return if (nationalId in NAMES.indices) NAMES[nationalId] else "#$internalId"
    }

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
