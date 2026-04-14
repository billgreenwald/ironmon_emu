package hh.game.mgba_android.tracker.ds.tables

object GenIVSpeciesNames {
    // Index = National Dex ID (1-493). Index 0 = "???".
    private val NAMES = arrayOf(
        "???",          // 0
        "Bulbasaur", "Ivysaur", "Venusaur", "Charmander", "Charmeleon",        // 1-5
        "Charizard", "Squirtle", "Wartortle", "Blastoise", "Caterpie",          // 6-10
        "Metapod", "Butterfree", "Weedle", "Kakuna", "Beedrill",                // 11-15
        "Pidgey", "Pidgeotto", "Pidgeot", "Rattata", "Raticate",                // 16-20
        "Spearow", "Fearow", "Ekans", "Arbok", "Pikachu",                       // 21-25
        "Raichu", "Sandshrew", "Sandslash", "Nidoran F", "Nidorina",            // 26-30
        "Nidoqueen", "Nidoran M", "Nidorino", "Nidoking", "Clefairy",           // 31-35
        "Clefable", "Vulpix", "Ninetales", "Jigglypuff", "Wigglytuff",          // 36-40
        "Zubat", "Golbat", "Oddish", "Gloom", "Vileplume",                      // 41-45
        "Paras", "Parasect", "Venonat", "Venomoth", "Diglett",                  // 46-50
        "Dugtrio", "Meowth", "Persian", "Psyduck", "Golduck",                   // 51-55
        "Mankey", "Primeape", "Growlithe", "Arcanine", "Poliwag",               // 56-60
        "Poliwhirl", "Poliwrath", "Abra", "Kadabra", "Alakazam",                // 61-65
        "Machop", "Machoke", "Machamp", "Bellsprout", "Weepinbell",             // 66-70
        "Victreebel", "Tentacool", "Tentacruel", "Geodude", "Graveler",         // 71-75
        "Golem", "Ponyta", "Rapidash", "Slowpoke", "Slowbro",                   // 76-80
        "Magnemite", "Magneton", "Farfetch'd", "Doduo", "Dodrio",               // 81-85
        "Seel", "Dewgong", "Grimer", "Muk", "Shellder",                         // 86-90
        "Cloyster", "Gastly", "Haunter", "Gengar", "Onix",                      // 91-95
        "Drowzee", "Hypno", "Krabby", "Kingler", "Voltorb",                     // 96-100
        "Electrode", "Exeggcute", "Exeggutor", "Cubone", "Marowak",             // 101-105
        "Hitmonlee", "Hitmonchan", "Lickitung", "Koffing", "Weezing",           // 106-110
        "Rhyhorn", "Rhydon", "Chansey", "Tangela", "Kangaskhan",                // 111-115
        "Horsea", "Seadra", "Goldeen", "Seaking", "Staryu",                     // 116-120
        "Starmie", "Mr. Mime", "Scyther", "Jynx", "Electabuzz",                // 121-125
        "Magmar", "Pinsir", "Tauros", "Magikarp", "Gyarados",                   // 126-130
        "Lapras", "Ditto", "Eevee", "Vaporeon", "Jolteon",                      // 131-135
        "Flareon", "Porygon", "Omanyte", "Omastar", "Kabuto",                   // 136-140
        "Kabutops", "Aerodactyl", "Snorlax", "Articuno", "Zapdos",              // 141-145
        "Moltres", "Dratini", "Dragonair", "Dragonite", "Mewtwo",               // 146-150
        "Mew", "Chikorita", "Bayleef", "Meganium", "Cyndaquil",                 // 151-155
        "Quilava", "Typhlosion", "Totodile", "Croconaw", "Feraligatr",          // 156-160
        "Sentret", "Furret", "Hoothoot", "Noctowl", "Ledyba",                   // 161-165
        "Ledian", "Spinarak", "Ariados", "Crobat", "Chinchou",                  // 166-170
        "Lanturn", "Pichu", "Cleffa", "Igglybuff", "Togepi",                    // 171-175
        "Togetic", "Natu", "Xatu", "Mareep", "Flaaffy",                         // 176-180
        "Ampharos", "Bellossom", "Marill", "Azumarill", "Sudowoodo",            // 181-185
        "Politoed", "Hoppip", "Skiploom", "Jumpluff", "Aipom",                  // 186-190
        "Sunkern", "Sunflora", "Yanma", "Wooper", "Quagsire",                   // 191-195
        "Espeon", "Umbreon", "Murkrow", "Slowking", "Misdreavus",               // 196-200
        "Unown", "Wobbuffet", "Girafarig", "Pineco", "Forretress",              // 201-205
        "Dunsparce", "Gligar", "Steelix", "Snubbull", "Granbull",               // 206-210
        "Qwilfish", "Scizor", "Shuckle", "Heracross", "Sneasel",                // 211-215
        "Teddiursa", "Ursaring", "Slugma", "Magcargo", "Swinub",                // 216-220
        "Piloswine", "Corsola", "Remoraid", "Octillery", "Delibird",            // 221-225
        "Mantine", "Skarmory", "Houndour", "Houndoom", "Kingdra",               // 226-230
        "Phanpy", "Donphan", "Porygon2", "Stantler", "Smeargle",                // 231-235
        "Tyrogue", "Hitmontop", "Smoochum", "Elekid", "Magby",                  // 236-240
        "Miltank", "Blissey", "Raikou", "Entei", "Suicune",                     // 241-245
        "Larvitar", "Pupitar", "Tyranitar", "Lugia", "Ho-Oh",                   // 246-250
        "Celebi", "Treecko", "Grovyle", "Sceptile", "Torchic",                  // 251-255
        "Combusken", "Blaziken", "Mudkip", "Marshtomp", "Swampert",             // 256-260
        "Poochyena", "Mightyena", "Zigzagoon", "Linoone", "Wurmple",            // 261-265
        "Silcoon", "Beautifly", "Cascoon", "Dustox", "Lotad",                   // 266-270
        "Lombre", "Ludicolo", "Seedot", "Nuzleaf", "Shiftry",                   // 271-275
        "Taillow", "Swellow", "Wingull", "Pelipper", "Ralts",                   // 276-280
        "Kirlia", "Gardevoir", "Surskit", "Masquerain", "Shroomish",            // 281-285
        "Breloom", "Slakoth", "Vigoroth", "Slaking", "Nincada",                 // 286-290
        "Ninjask", "Shedinja", "Whismur", "Loudred", "Exploud",                 // 291-295
        "Makuhita", "Hariyama", "Azurill", "Nosepass", "Skitty",                // 296-300
        "Delcatty", "Sableye", "Mawile", "Aron", "Lairon",                      // 301-305
        "Aggron", "Meditite", "Medicham", "Electrike", "Manectric",             // 306-310
        "Plusle", "Minun", "Volbeat", "Illumise", "Roselia",                    // 311-315
        "Gulpin", "Swalot", "Carvanha", "Sharpedo", "Wailmer",                  // 316-320
        "Wailord", "Numel", "Camerupt", "Torkoal", "Spoink",                    // 321-325
        "Grumpig", "Spinda", "Trapinch", "Vibrava", "Flygon",                   // 326-330
        "Cacnea", "Cacturne", "Swablu", "Altaria", "Zangoose",                  // 331-335
        "Seviper", "Lunatone", "Solrock", "Barboach", "Whiscash",               // 336-340
        "Corphish", "Crawdaunt", "Baltoy", "Claydol", "Lileep",                 // 341-345
        "Cradily", "Anorith", "Armaldo", "Feebas", "Milotic",                   // 346-350
        "Castform", "Kecleon", "Shuppet", "Banette", "Duskull",                 // 351-355
        "Dusclops", "Tropius", "Chimecho", "Absol", "Wynaut",                   // 356-360
        "Snorunt", "Glalie", "Spheal", "Sealeo", "Walrein",                     // 361-365
        "Clamperl", "Huntail", "Gorebyss", "Relicanth", "Luvdisc",             // 366-370
        "Bagon", "Shelgon", "Salamence", "Beldum", "Metang",                    // 371-375
        "Metagross", "Regirock", "Regice", "Registeel", "Latias",               // 376-380
        "Latios", "Kyogre", "Groudon", "Rayquaza", "Jirachi",                   // 381-385
        "Deoxys", "Turtwig", "Grotle", "Torterra", "Chimchar",                  // 386-390
        "Monferno", "Infernape", "Piplup", "Prinplup", "Empoleon",              // 391-395
        "Starly", "Staravia", "Staraptor", "Bidoof", "Bibarel",                 // 396-400
        "Kricketot", "Kricketune", "Shinx", "Luxio", "Luxray",                  // 401-405
        "Budew", "Roserade", "Cranidos", "Rampardos", "Shieldon",               // 406-410
        "Bastiodon", "Burmy", "Wormadam", "Mothim", "Combee",                   // 411-415
        "Vespiquen", "Pachirisu", "Buizel", "Floatzel", "Cherubi",              // 416-420
        "Cherrim", "Shellos", "Gastrodon", "Ambipom", "Drifloon",               // 421-425
        "Drifblim", "Buneary", "Lopunny", "Mismagius", "Honchkrow",             // 426-430
        "Glameow", "Purugly", "Chingling", "Stunky", "Skuntank",                // 431-435
        "Bronzor", "Bronzong", "Bonsly", "Mime Jr.", "Happiny",                 // 436-440
        "Chatot", "Spiritomb", "Gible", "Gabite", "Garchomp",                   // 441-445
        "Munchlax", "Riolu", "Lucario", "Hippopotas", "Hippowdon",              // 446-450
        "Skorupi", "Drapion", "Croagunk", "Toxicroak", "Carnivine",             // 451-455
        "Finneon", "Lumineon", "Mantyke", "Snover", "Abomasnow",                // 456-460
        "Weavile", "Magnezone", "Lickilicky", "Rhyperior", "Tangrowth",         // 461-465
        "Electivire", "Magmortar", "Togekiss", "Yanmega", "Leafeon",            // 466-470
        "Glaceon", "Gliscor", "Mamoswine", "Porygon-Z", "Gallade",              // 471-475
        "Probopass", "Dusknoir", "Froslass", "Rotom", "Uxie",                   // 476-480
        "Mesprit", "Azelf", "Dialga", "Palkia", "Heatran",                      // 481-485
        "Regigigas", "Giratina", "Cresselia", "Phione", "Manaphy",              // 486-490
        "Darkrai", "Shaymin", "Arceus",                                          // 491-493
    )

    fun get(id: Int): String = if (id in 1..493) NAMES[id] else "???"
}
