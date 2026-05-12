package hh.game.mgba_android.tracker.tables

/**
 * Gen III move names indexed by move ID.
 * IDs 1–354 cover all Gen III moves.
 *
 * Source: Bulbapedia / Gen III game data.
 * Used as the moveTable callback in PokemonDecoder.decode().
 */
object MoveNames {

    private val NAMES = arrayOf(
        "---",           // 0  — no move
        "Pound",         // 1
        "Karate Chop",   // 2
        "DoubleSlap",    // 3
        "Comet Punch",   // 4
        "Mega Punch",    // 5
        "Pay Day",       // 6
        "Fire Punch",    // 7
        "Ice Punch",     // 8
        "ThunderPunch",  // 9
        "Scratch",       // 10
        "ViceGrip",      // 11
        "Guillotine",    // 12
        "Razor Wind",    // 13
        "Swords Dance",  // 14
        "Cut",           // 15
        "Gust",          // 16
        "Wing Attack",   // 17
        "Whirlwind",     // 18
        "Fly",           // 19
        "Bind",          // 20
        "Slam",          // 21
        "Vine Whip",     // 22
        "Stomp",         // 23
        "Double Kick",   // 24
        "Mega Kick",     // 25
        "Jump Kick",     // 26
        "Rolling Kick",  // 27
        "Sand-Attack",   // 28
        "Headbutt",      // 29
        "Horn Attack",   // 30
        "Fury Attack",   // 31
        "Horn Drill",    // 32
        "Tackle",        // 33
        "Body Slam",     // 34
        "Wrap",          // 35
        "Take Down",     // 36
        "Thrash",        // 37
        "Double-Edge",   // 38
        "Tail Whip",     // 39
        "Poison Sting",  // 40
        "Twineedle",     // 41
        "Pin Missile",   // 42
        "Leer",          // 43
        "Bite",          // 44
        "Growl",         // 45
        "Roar",          // 46
        "Sing",          // 47
        "Supersonic",    // 48
        "SonicBoom",     // 49
        "Disable",       // 50
        "Acid",          // 51
        "Ember",         // 52
        "Flamethrower",  // 53
        "Mist",          // 54
        "Water Gun",     // 55
        "Hydro Pump",    // 56
        "Surf",          // 57
        "Ice Beam",      // 58
        "Blizzard",      // 59
        "Psybeam",       // 60
        "BubbleBeam",    // 61
        "Aurora Beam",   // 62
        "Hyper Beam",    // 63
        "Peck",          // 64
        "Drill Peck",    // 65
        "Submission",    // 66
        "Low Kick",      // 67
        "Counter",       // 68
        "Seismic Toss",  // 69
        "Strength",      // 70
        "Absorb",        // 71
        "Mega Drain",    // 72
        "Leech Seed",    // 73
        "Growth",        // 74
        "Razor Leaf",    // 75
        "SolarBeam",     // 76
        "PoisonPowder",  // 77
        "Stun Spore",    // 78
        "Sleep Powder",  // 79
        "Petal Dance",   // 80
        "String Shot",   // 81
        "Dragon Rage",   // 82
        "Fire Spin",     // 83
        "ThunderShock",  // 84
        "Thunderbolt",   // 85
        "Thunder Wave",  // 86
        "Thunder",       // 87
        "Rock Throw",    // 88
        "Earthquake",    // 89
        "Fissure",       // 90
        "Dig",           // 91
        "Toxic",         // 92
        "Confusion",     // 93
        "Psychic",       // 94
        "Hypnosis",      // 95
        "Meditate",      // 96
        "Agility",       // 97
        "Quick Attack",  // 98
        "Rage",          // 99
        "Teleport",      // 100
        "Night Shade",   // 101
        "Mimic",         // 102
        "Screech",       // 103
        "Double Team",   // 104
        "Recover",       // 105
        "Harden",        // 106
        "Minimize",      // 107
        "SmokeScreen",   // 108
        "Confuse Ray",   // 109
        "Withdraw",      // 110
        "Defense Curl",  // 111
        "Barrier",       // 112
        "Light Screen",  // 113
        "Haze",          // 114
        "Reflect",       // 115
        "Focus Energy",  // 116
        "Bide",          // 117
        "Metronome",     // 118
        "Mirror Move",   // 119
        "Selfdestruct",  // 120
        "Egg Bomb",      // 121
        "Lick",          // 122
        "Smog",          // 123
        "Sludge",        // 124
        "Bone Club",     // 125
        "Fire Blast",    // 126
        "Waterfall",     // 127
        "Clamp",         // 128
        "Swift",         // 129
        "Skull Bash",    // 130
        "Spike Cannon",  // 131
        "Constrict",     // 132
        "Amnesia",       // 133
        "Kinesis",       // 134
        "Softboiled",    // 135
        "Hi Jump Kick",  // 136
        "Glare",         // 137
        "Dream Eater",   // 138
        "Poison Gas",    // 139
        "Barrage",       // 140
        "Leech Life",    // 141
        "Lovely Kiss",   // 142
        "Sky Attack",    // 143
        "Transform",     // 144
        "Bubble",        // 145
        "Dizzy Punch",   // 146
        "Spore",         // 147
        "Flash",         // 148
        "Psywave",       // 149
        "Splash",        // 150
        "Acid Armor",    // 151
        "Crabhammer",    // 152
        "Explosion",     // 153
        "Fury Swipes",   // 154
        "Bonemerang",    // 155
        "Rest",          // 156
        "Rock Slide",    // 157
        "Hyper Fang",    // 158
        "Sharpen",       // 159
        "Conversion",    // 160
        "Tri Attack",    // 161
        "Super Fang",    // 162
        "Slash",         // 163
        "Substitute",    // 164
        "Struggle",      // 165
        "Sketch",        // 166
        "Triple Kick",   // 167
        "Thief",         // 168
        "Spider Web",    // 169
        "Mind Reader",   // 170
        "Nightmare",     // 171
        "Flame Wheel",   // 172
        "Snore",         // 173
        "Curse",         // 174
        "Flail",         // 175
        "Conversion 2",  // 176
        "Aeroblast",     // 177
        "Cotton Spore",  // 178
        "Reversal",      // 179
        "Spite",         // 180
        "Powder Snow",   // 181
        "Protect",       // 182
        "Mach Punch",    // 183
        "Scary Face",    // 184
        "Faint Attack",  // 185
        "Sweet Kiss",    // 186
        "Belly Drum",    // 187
        "Sludge Bomb",   // 188
        "Mud-Slap",      // 189
        "Octazooka",     // 190
        "Spikes",        // 191
        "Zap Cannon",    // 192
        "Foresight",     // 193
        "Destiny Bond",  // 194
        "Perish Song",   // 195
        "Icy Wind",      // 196
        "Detect",        // 197
        "Bone Rush",     // 198
        "Lock-On",       // 199
        "Outrage",       // 200
        "Sandstorm",     // 201
        "Giga Drain",    // 202
        "Endure",        // 203
        "Charm",         // 204
        "Rollout",       // 205
        "False Swipe",   // 206
        "Swagger",       // 207
        "Milk Drink",    // 208
        "Spark",         // 209
        "Fury Cutter",   // 210
        "Steel Wing",    // 211
        "Mean Look",     // 212
        "Attract",       // 213
        "Sleep Talk",    // 214
        "Heal Bell",     // 215
        "Return",        // 216
        "Present",       // 217
        "Frustration",   // 218
        "Safeguard",     // 219
        "Pain Split",    // 220
        "Sacred Fire",   // 221
        "Magnitude",     // 222
        "DynamicPunch",  // 223
        "Megahorn",      // 224
        "DragonBreath",  // 225
        "Baton Pass",    // 226
        "Encore",        // 227
        "Pursuit",       // 228
        "Rapid Spin",    // 229
        "Sweet Scent",   // 230
        "Iron Tail",     // 231
        "Metal Claw",    // 232
        "Vital Throw",   // 233
        "Morning Sun",   // 234
        "Synthesis",     // 235
        "Moonlight",     // 236
        "Hidden Power",  // 237
        "Cross Chop",    // 238
        "Twister",       // 239
        "Rain Dance",    // 240
        "Sunny Day",     // 241
        "Crunch",        // 242
        "Mirror Coat",   // 243
        "Psych Up",      // 244
        "ExtremeSpeed",  // 245
        "AncientPower",  // 246
        "Shadow Ball",   // 247
        "Future Sight",  // 248
        "Rock Smash",    // 249
        "Whirlpool",     // 250
        "Beat Up",       // 251
        "Fake Out",      // 252
        "Uproar",        // 253
        "Stockpile",     // 254
        "Spit Up",       // 255
        "Swallow",       // 256
        "Heat Wave",     // 257
        "Hail",          // 258
        "Torment",       // 259
        "Flatter",       // 260
        "Will-O-Wisp",   // 261
        "Memento",       // 262
        "Facade",        // 263
        "Focus Punch",   // 264
        "SmellingSalt",  // 265
        "Follow Me",     // 266
        "Nature Power",  // 267
        "Charge",        // 268
        "Taunt",         // 269
        "Helping Hand",  // 270
        "Trick",         // 271
        "Role Play",     // 272
        "Wish",          // 273
        "Assist",        // 274
        "Ingrain",       // 275
        "Superpower",    // 276
        "Magic Coat",    // 277
        "Recycle",       // 278
        "Revenge",       // 279
        "Brick Break",   // 280
        "Yawn",          // 281
        "Knock Off",     // 282
        "Endeavor",      // 283
        "Eruption",      // 284
        "Skill Swap",    // 285
        "Imprison",      // 286
        "Refresh",       // 287
        "Grudge",        // 288
        "Snatch",        // 289
        "Secret Power",  // 290
        "Dive",          // 291
        "Arm Thrust",    // 292
        "Camouflage",    // 293
        "Tail Glow",     // 294
        "Luster Purge",  // 295
        "Mist Ball",     // 296
        "FeatherDance",  // 297
        "Teeter Dance",  // 298
        "Blaze Kick",    // 299
        "Mud Sport",     // 300
        "Ice Ball",      // 301
        "Needle Arm",    // 302
        "Slack Off",     // 303
        "Hyper Voice",   // 304
        "Poison Fang",   // 305
        "Crush Claw",    // 306
        "Blast Burn",    // 307
        "Hydro Cannon",  // 308
        "Meteor Mash",   // 309
        "Astonish",      // 310
        "Weather Ball",  // 311
        "Aromatherapy",  // 312
        "Fake Tears",    // 313
        "Air Cutter",    // 314
        "Overheat",      // 315
        "Odor Sleuth",   // 316
        "Rock Tomb",     // 317
        "Silver Wind",   // 318
        "Metal Sound",   // 319
        "GrassWhistle",  // 320
        "Tickle",        // 321
        "Cosmic Power",  // 322
        "Water Spout",   // 323
        "Signal Beam",   // 324
        "Shadow Punch",  // 325
        "Extrasensory",  // 326
        "Sky Uppercut",  // 327
        "Sand Tomb",     // 328
        "Sheer Cold",    // 329
        "Muddy Water",   // 330
        "Bullet Seed",   // 331
        "Aerial Ace",    // 332
        "Icicle Spear",  // 333
        "Iron Defense",  // 334
        "Block",         // 335
        "Howl",          // 336
        "Dragon Claw",   // 337
        "Frenzy Plant",  // 338
        "Bulk Up",       // 339
        "Bounce",        // 340
        "Mud Shot",      // 341
        "Poison Tail",   // 342
        "Covet",         // 343
        "Volt Tackle",   // 344
        "Magical Leaf",  // 345
        "Water Sport",   // 346
        "Calm Mind",     // 347
        "Leaf Blade",    // 348
        "Dragon Dance",  // 349
        "Rock Blast",    // 350
        "Shock Wave",    // 351
        "Water Pulse",   // 352
        "Doom Desire",   // 353
        "Psycho Boost",  // 354
    )

    // NatDex ROM hack adds 6 Fairy-type moves (IDs 355–360)
    private val NATDEX_MOVES = mapOf(
        355 to "Disarming Voice",
        356 to "Draining Kiss",
        357 to "Play Rough",
        358 to "Fairy Wind",
        359 to "Moonblast",
        360 to "Dazzling Gleam",
    )

    // MaxFR/MaxEM ROM hack adds 146 Gen 4-5 moves (IDs 355–500 plus 559).
    // Names from maxData/moves.lua (provided by ROM hack creator).
    private val MAX_FR_MOVES = mapOf(
        355 to "Relic Song",
        356 to "Hone Claws",
        357 to "Miracle Eye",
        358 to "Wake-Up Slap",
        359 to "Hammer Arm",
        360 to "Gyro Ball",
        361 to "Healing Wish",
        362 to "Brine",
        363 to "Guard Split",
        364 to "Feint",
        365 to "Incinerate",
        366 to "Power Split",
        367 to "Acupressure",
        368 to "Metal Burst",
        369 to "U-turn",
        370 to "Close Combat",
        371 to "Payback",
        372 to "Assurance",
        373 to "Quiver Dance",
        374 to "Coil",
        375 to "Psycho Shift",
        376 to "Trump Card",
        377 to "Clear Smog",
        378 to "Wring Out",
        379 to "Power Trick",
        380 to "Gastro Acid",
        381 to "Shell Smash",
        382 to "Me First",
        383 to "Copycat",
        384 to "Power Swap",
        385 to "Guard Swap",
        386 to "Punishment",
        387 to "Last Resort",
        388 to "Worry Seed",
        389 to "Sucker Punch",
        390 to "Shift Gear",
        391 to "Heart Swap",
        392 to "Final Gambit",
        393 to "Work Up",
        394 to "Flare Blitz",
        395 to "Force Palm",
        396 to "Aura Sphere",
        397 to "Rock Polish",
        398 to "Poison Jab",
        399 to "Dark Pulse",
        400 to "Night Slash",
        401 to "Aqua Tail",
        402 to "Seed Bomb",
        403 to "Air Slash",
        404 to "X-Scissor",
        405 to "Bug Buzz",
        406 to "Dragon Pulse",
        407 to "Dragon Rush",
        408 to "Power Gem",
        409 to "Drain Punch",
        410 to "Vacuum Wave",
        411 to "Focus Blast",
        412 to "Energy Ball",
        413 to "Brave Bird",
        414 to "Earth Power",
        415 to "Switcheroo",
        416 to "Giga Impact",
        417 to "Nasty Plot",
        418 to "Bullet Punch",
        419 to "Avalanche",
        420 to "Ice Shard",
        421 to "Shadow Claw",
        422 to "Thunder Fang",
        423 to "Ice Fang",
        424 to "Fire Fang",
        425 to "Shadow Sneak",
        426 to "Mud Bomb",
        427 to "Psycho Cut",
        428 to "Zen Headbutt",
        429 to "Mirror Shot",
        430 to "Flash Cannon",
        431 to "Rock Climb",
        432 to "Defog",
        433 to "Fusion Flare",
        434 to "Draco Meteor",
        435 to "Discharge",
        436 to "Lava Plume",
        437 to "Leaf Storm",
        438 to "Power Whip",
        439 to "Rock Wrecker",
        440 to "Cross Poison",
        441 to "Gunk Shot",
        442 to "Iron Head",
        443 to "Magnet Bomb",
        444 to "Stone Edge",
        445 to "Captivate",
        447 to "Grass Knot",
        448 to "Chatter",
        449 to "Judgment",
        450 to "Struggle Bug",
        451 to "Charge Beam",
        452 to "Wood Hammer",
        453 to "Aqua Jet",
        454 to "Attack Order",
        455 to "Defend Order",
        456 to "Heal Order",
        457 to "Head Smash",
        458 to "Double Hit",
        459 to "Roar of Time",
        460 to "Spacial Rend",
        461 to "Lunar Dance",
        462 to "Crush Grip",
        463 to "Magma Storm",
        464 to "Dark Void",
        465 to "Seed Flare",
        466 to "Ominous Wind",
        467 to "Shadow Force",
        468 to "Venoshock",
        469 to "Sludge Wave",
        470 to "Flame Charge",
        471 to "Low Sweep",
        472 to "Acid Spray",
        473 to "Scald",
        474 to "Hex",
        475 to "Inferno",
        476 to "Volt Switch",
        477 to "Bulldoze",
        478 to "Electroweb",
        479 to "Wild Charge",
        480 to "Drill Run",
        481 to "Dual Chop",
        482 to "Heart Stamp",
        483 to "Horn Leech",
        484 to "Razor Shell",
        485 to "Leaf Tornado",
        486 to "Steamroller",
        487 to "Night Daze",
        488 to "Tail Slap",
        489 to "Hurricane",
        490 to "Head Charge",
        491 to "Gear Grind",
        492 to "Searing Shot",
        493 to "Glaciate",
        494 to "Bolt Strike",
        495 to "Blue Flare",
        496 to "Fiery Dance",
        497 to "Freeze Shock",
        498 to "Ice Burn",
        499 to "Snarl",
        500 to "Icicle Crash",
        559 to "Fusion Bolt",
    )

    fun get(id: Int, isMaxFr: Boolean = false): String = when {
        isMaxFr && id in MAX_FR_MOVES -> MAX_FR_MOVES[id]!!
        id in NAMES.indices            -> NAMES[id]
        id in NATDEX_MOVES             -> NATDEX_MOVES[id]!!
        else                           -> "Move#$id"
    }
}
