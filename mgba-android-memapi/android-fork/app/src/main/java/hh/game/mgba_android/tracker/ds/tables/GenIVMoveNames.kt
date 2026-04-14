package hh.game.mgba_android.tracker.ds.tables

object GenIVMoveNames {
    // Index = move ID (1-467). Index 0 = "--".
    private val NAMES = arrayOf(
        "--",                // 0
        "Pound", "Karate Chop", "Double Slap", "Comet Punch", "Mega Punch",             // 1-5
        "Pay Day", "Fire Punch", "Ice Punch", "Thunder Punch", "Scratch",                // 6-10
        "Vise Grip", "Guillotine", "Razor Wind", "Swords Dance", "Cut",                 // 11-15
        "Gust", "Wing Attack", "Whirlwind", "Fly", "Bind",                              // 16-20
        "Slam", "Vine Whip", "Stomp", "Double Kick", "Mega Kick",                       // 21-25
        "Jump Kick", "Rolling Kick", "Sand Attack", "Headbutt", "Horn Attack",          // 26-30
        "Fury Attack", "Horn Drill", "Tackle", "Body Slam", "Wrap",                     // 31-35
        "Take Down", "Thrash", "Double-Edge", "Tail Whip", "Poison Sting",              // 36-40
        "Twineedle", "Pin Missile", "Leer", "Bite", "Growl",                            // 41-45
        "Roar", "Sing", "Supersonic", "Sonic Boom", "Disable",                          // 46-50
        "Acid", "Ember", "Flamethrower", "Mist", "Water Gun",                           // 51-55
        "Hydro Pump", "Surf", "Ice Beam", "Blizzard", "Psybeam",                        // 56-60
        "Bubble Beam", "Aurora Beam", "Hyper Beam", "Peck", "Drill Peck",               // 61-65
        "Submission", "Low Kick", "Counter", "Seismic Toss", "Strength",                // 66-70
        "Absorb", "Mega Drain", "Leech Seed", "Growth", "Razor Leaf",                   // 71-75
        "Solar Beam", "Poison Powder", "Stun Spore", "Sleep Powder", "Petal Dance",     // 76-80
        "String Shot", "Dragon Rage", "Fire Spin", "Thunder Shock", "Thunderbolt",      // 81-85
        "Thunder Wave", "Thunder", "Rock Throw", "Earthquake", "Fissure",               // 86-90
        "Dig", "Toxic", "Confusion", "Psychic", "Hypnosis",                             // 91-95
        "Meditate", "Agility", "Quick Attack", "Rage", "Teleport",                      // 96-100
        "Night Shade", "Mimic", "Screech", "Double Team", "Recover",                    // 101-105
        "Harden", "Minimize", "Smokescreen", "Confuse Ray", "Withdraw",                 // 106-110
        "Defense Curl", "Barrier", "Light Screen", "Haze", "Reflect",                   // 111-115
        "Focus Energy", "Bide", "Metronome", "Mirror Move", "Self-Destruct",            // 116-120
        "Egg Bomb", "Lick", "Smog", "Sludge", "Bone Club",                              // 121-125
        "Fire Blast", "Waterfall", "Clamp", "Swift", "Skull Bash",                      // 126-130
        "Spike Cannon", "Constrict", "Amnesia", "Kinesis", "Soft-Boiled",               // 131-135
        "High Jump Kick", "Glare", "Dream Eater", "Poison Gas", "Barrage",              // 136-140
        "Leech Life", "Lovely Kiss", "Sky Attack", "Transform", "Bubble",               // 141-145
        "Dizzy Punch", "Spore", "Flash", "Psywave", "Splash",                           // 146-150
        "Acid Armor", "Crabhammer", "Explosion", "Fury Swipes", "Bonemerang",           // 151-155
        "Rest", "Rock Slide", "Hyper Fang", "Sharpen", "Conversion",                    // 156-160
        "Tri Attack", "Super Fang", "Slash", "Substitute", "Struggle",                  // 161-165
        "Sketch", "Triple Kick", "Thief", "Spider Web", "Mind Reader",                  // 166-170
        "Nightmare", "Flame Wheel", "Snore", "Curse", "Flail",                          // 171-175
        "Conversion 2", "Aeroblast", "Cotton Spore", "Reversal", "Spite",               // 176-180
        "Powder Snow", "Protect", "Mach Punch", "Scary Face", "Feint Attack",           // 181-185
        "Sweet Kiss", "Belly Drum", "Sludge Bomb", "Mud-Slap", "Octazooka",             // 186-190
        "Spikes", "Zap Cannon", "Foresight", "Destiny Bond", "Perish Song",             // 191-195
        "Icy Wind", "Detect", "Bone Rush", "Lock-On", "Outrage",                        // 196-200
        "Sandstorm", "Giga Drain", "Endure", "Charm", "Rollout",                        // 201-205
        "False Swipe", "Swagger", "Milk Drink", "Spark", "Fury Cutter",                 // 206-210
        "Steel Wing", "Mean Look", "Attract", "Sleep Talk", "Heal Bell",                // 211-215
        "Return", "Present", "Frustration", "Safeguard", "Pain Split",                  // 216-220
        "Sacred Fire", "Magnitude", "Dynamic Punch", "Megahorn", "Dragon Breath",       // 221-225
        "Baton Pass", "Encore", "Pursuit", "Rapid Spin", "Sweet Scent",                 // 226-230
        "Iron Tail", "Metal Claw", "Vital Throw", "Morning Sun", "Synthesis",           // 231-235
        "Moonlight", "Hidden Power", "Cross Chop", "Twister", "Rain Dance",             // 236-240
        "Sunny Day", "Crunch", "Mirror Coat", "Psych Up", "Extreme Speed",              // 241-245
        "Ancient Power", "Shadow Ball", "Future Sight", "Rock Smash", "Whirlpool",      // 246-250
        "Beat Up", "Fake Out", "Uproar", "Stockpile", "Spit Up",                        // 251-255
        "Swallow", "Heat Wave", "Hail", "Torment", "Flatter",                           // 256-260
        "Will-O-Wisp", "Memento", "Facade", "Focus Punch", "Smelling Salts",            // 261-265
        "Follow Me", "Nature Power", "Charge", "Taunt", "Helping Hand",                 // 266-270
        "Trick", "Role Play", "Wish", "Assist", "Ingrain",                              // 271-275
        "Superpower", "Magic Coat", "Recycle", "Revenge", "Brick Break",                // 276-280
        "Yawn", "Knock Off", "Endeavor", "Eruption", "Skill Swap",                      // 281-285
        "Imprison", "Refresh", "Grudge", "Snatch", "Secret Power",                      // 286-290
        "Dive", "Arm Thrust", "Camouflage", "Tail Glow", "Luster Purge",               // 291-295
        "Mist Ball", "Feather Dance", "Teeter Dance", "Blaze Kick", "Mud Sport",        // 296-300
        "Ice Ball", "Needle Arm", "Slack Off", "Hyper Voice", "Poison Fang",            // 301-305
        "Crush Claw", "Blast Burn", "Hydro Cannon", "Meteor Mash", "Astonish",          // 306-310
        "Weather Ball", "Aromatherapy", "Fake Tears", "Air Cutter", "Overheat",         // 311-315
        "Odor Sleuth", "Rock Tomb", "Silver Wind", "Metal Sound", "Grass Whistle",      // 316-320
        "Tickle", "Cosmic Power", "Water Spout", "Signal Beam", "Shadow Punch",         // 321-325
        "Extrasensory", "Sky Uppercut", "Sand Tomb", "Sheer Cold", "Muddy Water",       // 326-330
        "Bullet Seed", "Aerial Ace", "Icicle Spear", "Iron Defense", "Block",           // 331-335
        "Howl", "Dragon Claw", "Frenzy Plant", "Bulk Up", "Bounce",                     // 336-340
        "Mud Shot", "Poison Tail", "Covet", "Volt Tackle", "Magical Leaf",              // 341-345
        "Water Sport", "Calm Mind", "Leaf Blade", "Dragon Dance", "Rock Blast",         // 346-350
        "Shock Wave", "Water Pulse", "Doom Desire", "Psycho Boost", "Roost",            // 351-355
        "Gravity", "Miracle Eye", "Wake-Up Slap", "Hammer Arm", "Gyro Ball",           // 356-360
        "Healing Wish", "Brine", "Natural Gift", "Feint", "Pluck",                      // 361-365
        "Tailwind", "Acupressure", "Metal Burst", "U-turn", "Close Combat",             // 366-370
        "Payback", "Assurance", "Embargo", "Fling", "Psycho Shift",                     // 371-375
        "Trump Card", "Heal Block", "Wring Out", "Power Trick", "Gastro Acid",          // 376-380
        "Lucky Chant", "Me First", "Copycat", "Power Swap", "Guard Swap",               // 381-385
        "Punishment", "Last Resort", "Worry Seed", "Sucker Punch", "Toxic Spikes",      // 386-390
        "Heart Swap", "Aqua Ring", "Magnet Rise", "Flare Blitz", "Force Palm",          // 391-395
        "Aura Sphere", "Rock Polish", "Poison Jab", "Dark Pulse", "Night Slash",        // 396-400
        "Aqua Tail", "Seed Bomb", "Air Slash", "X-Scissor", "Bug Buzz",                 // 401-405
        "Dragon Pulse", "Dragon Rush", "Power Gem", "Drain Punch", "Vacuum Wave",       // 406-410
        "Focus Blast", "Energy Ball", "Brave Bird", "Earth Power", "Switcheroo",        // 411-415
        "Giga Impact", "Nasty Plot", "Bullet Punch", "Avalanche", "Ice Shard",          // 416-420
        "Shadow Claw", "Thunder Fang", "Ice Fang", "Fire Fang", "Shadow Sneak",         // 421-425
        "Mud Bomb", "Psycho Cut", "Zen Headbutt", "Mirror Shot", "Flash Cannon",        // 426-430
        "Rock Climb", "Defog", "Trick Room", "Draco Meteor", "Discharge",               // 431-435
        "Lava Plume", "Leaf Storm", "Power Whip", "Rock Wrecker", "Cross Poison",       // 436-440
        "Gunk Shot", "Iron Head", "Magnet Bomb", "Stone Edge", "Captivate",             // 441-445
        "Stealth Rock", "Grass Knot", "Chatter", "Judgment", "Bug Bite",                // 446-450
        "Charge Beam", "Wood Hammer", "Aqua Jet", "Attack Order", "Defend Order",       // 451-455
        "Heal Order", "Head Smash", "Double Hit", "Roar of Time", "Spacial Rend",       // 456-460
        "Lunar Dance", "Crush Grip", "Magma Storm", "Dark Void", "Seed Flare",          // 461-465
        "Ominous Wind", "Shadow Force",                                                  // 466-467
    )

    fun get(id: Int): String = if (id in 1..467) NAMES[id] else "--"
}
