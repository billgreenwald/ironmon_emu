package hh.game.mgba_android.tracker.tables

object MoveDescTable {
    private val descs = arrayOf(
        "",                                                                          // 0
        "A regular physical attack.",                                                // 1 Pound
        "A critical-hit-prone chop.",                                               // 2 Karate Chop
        "Slaps 2–5 times in a row.",                                                // 3 Double Slap
        "Punches 2–5 times in a row.",                                              // 4 Comet Punch
        "A powerful punch.",                                                         // 5 Mega Punch
        "Scatters coins; doubles prize money.",                                      // 6 Pay Day
        "A fiery punch. May burn.",                                                  // 7 Fire Punch
        "An icy punch. May freeze.",                                                 // 8 Ice Punch
        "An electric punch. May paralyze.",                                          // 9 ThunderPunch
        "Scratches with sharp claws.",                                               // 10 Scratch
        "Grips foe with pincers for 2–5 turns.",                                    // 11 Vice Grip
        "One-hit KO if it connects.",                                               // 12 Guillotine
        "Charges 1 turn, then strikes both foes.",                                   // 13 Razor Wind
        "Sharply raises the user's Attack.",                                         // 14 Swords Dance
        "Weak cut; can remove field obstacles.",                                     // 15 Cut
        "Whips up strong wind. Hits Fly users.",                                    // 16 Gust
        "Strikes with wings.",                                                       // 17 Wing Attack
        "Blows foe back to its Poké Ball.",                                         // 18 Whirlwind
        "User flies up, then dives next turn.",                                     // 19 Fly
        "Constricts foe for 2–5 turns.",                                            // 20 Bind
        "Throws foe with great force.",                                              // 21 Slam
        "Lashes out with a long vine.",                                              // 22 Vine Whip
        "Heavy stomp. May cause flinching.",                                         // 23 Stomp
        "Kicks foe twice in a row.",                                                 // 24 Double Kick
        "A powerful full-force kick.",                                               // 25 Mega Kick
        "Leaps high, then kicks. May crash.",                                        // 26 Jump Kick
        "A rolling kick. May cause flinching.",                                      // 27 Rolling Kick
        "Reduces the foe's accuracy.",                                               // 28 Sand Attack
        "A headbutt. May cause flinching.",                                          // 29 Headbutt
        "Stabs foe with a sharp horn.",                                             // 30 Horn Attack
        "Jabs 2–5 times with a horn.",                                              // 31 Fury Attack
        "One-hit KO if it connects.",                                               // 32 Horn Drill
        "Charges into the foe with full force.",                                     // 33 Tackle
        "Full-body slam. May paralyze.",                                             // 34 Body Slam
        "Wraps around foe for 2–5 turns.",                                          // 35 Wrap
        "A reckless tackle; user also hurt.",                                        // 36 Take Down
        "Thrashes 2–3 turns; user confused after.",                                  // 37 Thrash
        "Crashes into foe with reckless force.",                                     // 38 Double-Edge
        "Lowers the foe's Defense.",                                                 // 39 Tail Whip
        "Jabs with a barbed stinger. May poison.",                                  // 40 Poison Sting
        "Stings twice. May poison.",                                                 // 41 Twineedle
        "Fires pins 2–5 times in a row.",                                           // 42 Pin Missile
        "Intimidates the foe to lower Defense.",                                    // 43 Leer
        "Bites with dark energy. May flinch.",                                       // 44 Bite
        "Weakens the foe's Attack.",                                                 // 45 Growl
        "Roars the foe away; forces switch.",                                        // 46 Roar
        "A soothing song that causes sleep.",                                        // 47 Sing
        "Emits strange waves. May confuse.",                                         // 48 Supersonic
        "Always deals 20 HP damage.",                                                // 49 Sonic Boom
        "Disables one of the foe's moves for 2–5 turns.",                           // 50 Disable
        "Sprays acid. May lower Sp. Def.",                                           // 51 Acid
        "Weak flame. May burn.",                                                     // 52 Ember
        "Powerful fire. May burn.",                                                  // 53 Flamethrower
        "Shields team from stat reductions for 5 turns.",                            // 54 Mist
        "A standard water attack.",                                                  // 55 Water Gun
        "Blasts water with enormous pressure.",                                      // 56 Hydro Pump
        "A wave that hits all adjacent Pokémon.",                                   // 57 Surf
        "A freezing beam. May freeze.",                                              // 58 Ice Beam
        "A biting blizzard. May freeze.",                                            // 59 Blizzard
        "A multicolored beam. May confuse.",                                         // 60 Psybeam
        "Bubbles that may lower Speed.",                                             // 61 Bubble Beam
        "An icy beam. May lower Attack.",                                            // 62 Aurora Beam
        "Blasts a huge beam; must rest next turn.",                                  // 63 Hyper Beam
        "Jabs foe with a sharp beak.",                                              // 64 Peck
        "A fast, powerful drilling beak.",                                           // 65 Drill Peck
        "Throws body at foe; user also hurt.",                                       // 66 Submission
        "Heavier foes take more damage.",                                            // 67 Low Kick
        "Mirrors physical damage taken.",                                            // 68 Counter
        "Flings foe using its own weight.",                                          // 69 Seismic Toss
        "Powerful, straightforward attack.",                                         // 70 Strength
        "Drains a small amount of HP.",                                              // 71 Absorb
        "Drains HP from the foe to heal.",                                           // 72 Mega Drain
        "Seeds foe; drains HP every turn.",                                          // 73 Leech Seed
        "Raises the user's Special Attack.",                                         // 74 Growth
        "Sharp leaves. High critical hit rate.",                                     // 75 Razor Leaf
        "Charges 1 turn; powerful in sunlight.",                                     // 76 Solar Beam
        "A powder that poisons on contact.",                                         // 77 PoisonPowder
        "A powder that paralyzes.",                                                  // 78 Stun Spore
        "A fine powder that induces sleep.",                                         // 79 Sleep Powder
        "Attacks 2–3 turns; user confused after.",                                   // 80 Petal Dance
        "Sprays sticky thread; lowers Speed.",                                       // 81 String Shot
        "Always deals 40 HP damage.",                                                // 82 Dragon Rage
        "Traps foe in spinning fire for 2–5 turns.",                                // 83 Fire Spin
        "Weak electric jolt. May paralyze.",                                         // 84 Thunder Shock
        "Strong lightning bolt. May paralyze.",                                      // 85 Thunderbolt
        "Paralyzes foe with electricity.",                                           // 86 Thunder Wave
        "A massive lightning strike. May paralyze.",                                 // 87 Thunder
        "Hurls a rock at the foe.",                                                  // 88 Rock Throw
        "Ground shakes; hits all non-Flying types.",                                 // 89 Earthquake
        "One-hit KO if it connects.",                                               // 90 Fissure
        "Burrows underground; strikes next turn.",                                   // 91 Dig
        "Badly poisons the foe.",                                                    // 92 Toxic
        "A psychic attack. May confuse.",                                            // 93 Confusion
        "Powerful psychic. May lower Sp. Def.",                                     // 94 Psychic
        "Puts the foe to sleep with psychic power.",                                // 95 Hypnosis
        "Raises the user's Attack.",                                                 // 96 Meditate
        "Sharply raises the user's Speed.",                                          // 97 Agility
        "Strikes before almost any move.",                                           // 98 Quick Attack
        "Stores rage; damage grows when hit.",                                       // 99 Rage
        "Teleports out of battle or away.",                                          // 100 Teleport
        "Deals damage equal to user's level.",                                       // 101 Night Shade
        "Copies one of the foe's moves.",                                            // 102 Mimic
        "Sharply lowers the foe's Defense.",                                         // 103 Screech
        "Creates illusory copies to raise evasion.",                                // 104 Double Team
        "Restores up to half max HP.",                                               // 105 Recover
        "Stiffens body to raise Defense.",                                           // 106 Harden
        "Shrinks to raise evasion.",                                                 // 107 Minimize
        "Lowers the foe's accuracy.",                                                // 108 Smokescreen
        "Causes confusion with ghostly light.",                                      // 109 Confuse Ray
        "Withdraws into shell; raises Defense.",                                     // 110 Withdraw
        "Curls up tightly; raises Defense.",                                         // 111 Defense Curl
        "Sharply raises the user's Defense.",                                        // 112 Barrier
        "Halves Sp. damage for team for 5 turns.",                                  // 113 Light Screen
        "Resets all stat changes in battle.",                                        // 114 Haze
        "Halves physical damage for team for 5 turns.",                             // 115 Reflect
        "Raises critical hit ratio.",                                                // 116 Focus Energy
        "Stores energy; releases it after taking 2 hits.",                          // 117 Bide
        "Randomly selects and uses any move.",                                       // 118 Metronome
        "Mimics the move the foe just used.",                                        // 119 Mirror Move
        "User explodes; hits all adjacent foes.",                                    // 120 Self-Destruct
        "Hurls an egg-shaped bomb.",                                                 // 121 Egg Bomb
        "Licks with a long tongue. May paralyze.",                                  // 122 Lick
        "Foggy gas. May poison.",                                                    // 123 Smog
        "Sludge attack. May poison.",                                                // 124 Sludge
        "Strikes foe with a thick bone.",                                            // 125 Bone Club
        "An intense fire blast. May burn.",                                          // 126 Fire Blast
        "Charges up a waterfall at the foe.",                                        // 127 Waterfall
        "Clamps foe in pincers for 2–5 turns.",                                     // 128 Clamp
        "Hits all foes; never misses.",                                             // 129 Swift
        "Charges 1 turn, then rams; raises Defense.",                               // 130 Skull Bash
        "Fires sharp spikes 2–5 times.",                                             // 131 Spike Cannon
        "Squeezes foe. May lower Speed.",                                            // 132 Constrict
        "Raises Sp. Atk sharply twice.",                                             // 133 Amnesia
        "Bends spoons at foe to lower accuracy.",                                   // 134 Kinesis
        "Restores up to half max HP.",                                               // 135 Soft-Boiled
        "Leaps and kicks powerfully. May crash.",                                    // 136 High Jump Kick
        "Paralyzes foe with a glare.",                                               // 137 Glare
        "Feasts on sleeping foe's dreams; restores HP.",                            // 138 Dream Eater
        "Envelops foe in poison gas.",                                               // 139 Poison Gas
        "Hurls multiple objects 2–5 times.",                                         // 140 Barrage
        "Drains a small amount of HP from foe.",                                    // 141 Leech Life
        "A pretty kiss that causes confusion.",                                      // 142 Lovely Kiss
        "Charges 1 turn; then a devastating attack.",                               // 143 Sky Attack
        "Transforms into an exact copy of the foe.",                                // 144 Transform
        "Fires bubbles. May lower Speed.",                                           // 145 Bubble
        "A dizzying punch. May confuse.",                                            // 146 Dizzy Punch
        "A spore that causes sleep; never misses.",                                  // 147 Spore
        "Flashes a blinding light to lower accuracy.",                              // 148 Flash
        "Fires psychic waves with variable power.",                                  // 149 Psywave
        "Completely useless. Does nothing.",                                         // 150 Splash
        "Dissolves into liquid to sharply raise Defense.",                          // 151 Acid Armor
        "Slams with a large claw. High critical rate.",                             // 152 Crabhammer
        "User explodes, hitting all adjacent foes.",                                 // 153 Explosion
        "Slashes with sharp claws 2–5 times.",                                      // 154 Fury Swipes
        "Hurls a bone at the foe twice.",                                            // 155 Bonemerang
        "User sleeps to fully restore HP and cure status.",                         // 156 Rest
        "Rock boulders crash down. May flinch.",                                    // 157 Rock Slide
        "Bites with sharp fang. May flinch.",                                       // 158 Hyper Fang
        "Raises the user's Attack.",                                                 // 159 Sharpen
        "Changes user's type to match one of its moves.",                           // 160 Conversion
        "Fires a three-colored beam. May cause status.",                            // 161 Tri Attack
        "Always cuts the foe's HP in half.",                                        // 162 Super Fang
        "Slashes with high critical hit rate.",                                     // 163 Slash
        "Creates a decoy from HP to protect user.",                                 // 164 Substitute
        "Used only when all other moves run out of PP.",                            // 165 Struggle
        "Permanently copies the foe's last move.",                                  // 166 Sketch
        "Kicks 1–3 times; damage grows each hit.",                                  // 167 Triple Kick
        "Steals the foe's held item.",                                               // 168 Thief
        "Prevents the foe from fleeing or switching.",                              // 169 Spider Web
        "Ensures the next move hits perfectly.",                                     // 170 Mind Reader
        "Causes a sleeping foe to take nightmare damage.",                          // 171 Nightmare
        "Rolls in flames. May burn.",                                                // 172 Flame Wheel
        "A loud snore. May flinch. Usable only while asleep.",                     // 173 Snore
        "Non-Ghost: lowers Speed & raises Defense. Ghost: subtracts HP/raises all stats.", // 174 Curse
        "Power grows as user's HP decreases.",                                       // 175 Flail
        "User's type changes to resist the foe's last move.",                       // 176 Conversion 2
        "Aerodynamic attack; high critical hit rate.",                              // 177 Aeroblast
        "Cotton spores that sharply lower Speed.",                                   // 178 Cotton Spore
        "Power grows as user's HP decreases.",                                       // 179 Reversal
        "Cuts PP of the foe's last used move.",                                     // 180 Spite
        "A chilling wind. May lower Speed.",                                         // 181 Powder Snow
        "Protects from all attacks for one turn.",                                   // 182 Protect
        "A quick punch that always moves first.",                                    // 183 Mach Punch
        "Frightens foe to sharply lower Speed.",                                    // 184 Scary Face
        "Sneaks up and strikes. Never misses.",                                      // 185 Faint Attack
        "A sweet kiss that causes confusion.",                                       // 186 Sweet Kiss
        "Maximizes Attack; costs half of max HP.",                                  // 187 Belly Drum
        "Sludge bomb. May poison.",                                                  // 188 Sludge Bomb
        "Hurls mud at foe; lowers accuracy.",                                        // 189 Mud-Slap
        "Blasts water. May lower accuracy.",                                         // 190 Octazooka
        "Scatters sharp spikes; damages incoming foes.",                            // 191 Spikes
        "An electric ball; always paralyzes.",                                       // 192 Zap Cannon
        "Normal and Fighting moves can now hit Ghost types.",                       // 193 Foresight
        "If user faints, so does the foe.",                                         // 194 Destiny Bond
        "All Pokémon hearing this faint after 3 turns.",                           // 195 Perish Song
        "A chilling wind. Lowers Speed.",                                            // 196 Icy Wind
        "Protects from all attacks for one turn.",                                   // 197 Detect
        "Strikes 2–5 times with a bone.",                                           // 198 Bone Rush
        "Ensures the next move lands.",                                              // 199 Lock-On
        "Attacks 2–3 turns; user confused after.",                                  // 200 Outrage
        "Summons a sandstorm that damages non-Rock/Steel/Ground.",                  // 201 Sandstorm
        "Drains a large amount of HP from the foe.",                                // 202 Giga Drain
        "Survives any hit with at least 1 HP.",                                     // 203 Endure
        "Sharply lowers the foe's Attack.",                                          // 204 Charm
        "Rolls into foe; power grows each turn. Boosted by Defense Curl.",         // 205 Rollout
        "Attacks foe but won't knock it out.",                                       // 206 False Swipe
        "Confuses foe but sharply raises its Attack.",                              // 207 Swagger
        "Restores HP with a soothing drink of milk.",                              // 208 Milk Drink
        "An electric jolt. May paralyze.",                                           // 209 Spark
        "Slashes repeatedly; power doubles each use.",                              // 210 Fury Cutter
        "Slashes with steel wings. May raise Defense.",                             // 211 Steel Wing
        "Locks foe in place; prevents fleeing or switching.",                       // 212 Mean Look
        "Infatuates foe of opposite gender; may not attack.",                       // 213 Attract
        "Randomly uses a move the user knows. Only usable while asleep.",          // 214 Sleep Talk
        "Cures the team's status conditions with a bell.",                          // 215 Heal Bell
        "Power rises with the user's friendship.",                                   // 216 Return
        "Gives a present; may deal damage or restore HP.",                          // 217 Present
        "Power rises as friendship decreases.",                                      // 218 Frustration
        "Protects team from status conditions for 5 turns.",                        // 219 Safeguard
        "Averages HP between user and foe.",                                         // 220 Pain Split
        "A sacred, intensely hot flame. May burn.",                                 // 221 Sacred Fire
        "Variable power based on random magnitude.",                                 // 222 Magnitude
        "A punch that always confuses if it hits.",                                 // 223 DynamicPunch
        "A powerful strike with a large horn.",                                     // 224 Megahorn
        "A breath attack. May paralyze.",                                            // 225 DragonBreath
        "Switches out while passing on stat changes to ally.",                      // 226 Baton Pass
        "Forces foe to repeat its last move for 2–6 turns.",                       // 227 Encore
        "Chases a foe that is switching out; deals double damage.",                 // 228 Pursuit
        "Spins to clear entry hazards; removes Leech Seed.",                        // 229 Rapid Spin
        "A sweet scent that lowers foe's evasion.",                                 // 230 Sweet Scent
        "Slams with a heavy iron tail. May lower Defense.",                         // 231 Iron Tail
        "Slashes with metallic claws. May raise Attack.",                           // 232 Metal Claw
        "Always moves last; compensates with power.",                               // 233 Vital Throw
        "Restores HP; amount varies with weather.",                                  // 234 Morning Sun
        "Restores HP; amount varies with weather.",                                  // 235 Synthesis
        "Restores HP; amount varies with weather.",                                  // 236 Moonlight
        "Type and power depend on the user's IVs.",                                 // 237 Hidden Power
        "A two-fisted chop. High critical hit rate.",                               // 238 Cross Chop
        "A cyclone that may flinch. Hits Fly users.",                               // 239 Twister
        "Summons rain for 5 turns; boosts Water moves.",                            // 240 Rain Dance
        "Summons harsh sun for 5 turns; boosts Fire moves.",                        // 241 Sunny Day
        "Bites with dark fangs. May lower Sp. Def.",                                // 242 Crunch
        "Returns the full amount of Sp. damage received.",                          // 243 Mirror Coat
        "Copies the foe's stat changes.",                                            // 244 Psych Up
        "Extremely fast; moves before almost anything.",                            // 245 Extreme Speed
        "A prehistoric power. May raise all stats.",                                 // 246 AncientPower
        "Hurls a shadowy ball. May lower Sp. Def.",                                 // 247 Shadow Ball
        "A psychic attack that strikes 2 turns later.",                             // 248 Future Sight
        "Breaks rocks in the field. May lower Defense.",                            // 249 Rock Smash
        "Traps foe in a whirlpool for 2–5 turns.",                                 // 250 Whirlpool
        "All Pokémon in the party attack the foe.",                                // 251 Beat Up
        "Strikes first; causes flinching. Only works on turn 1.",                  // 252 Fake Out
        "User shouts for 2–5 turns; confuses all after.",                           // 253 Uproar
        "Stores power; can use up to 3 times.",                                     // 254 Stockpile
        "Fires stored Stockpile energy as an attack.",                              // 255 Spit Up
        "Heals HP using stored Stockpile energy.",                                  // 256 Swallow
        "A blast of scorching heat. May burn.",                                     // 257 Heat Wave
        "Summons hail for 5 turns; chips non-Ice types.",                           // 258 Hail
        "Prevents foe from using the same move twice.",                             // 259 Torment
        "Confuses foe but raises its Sp. Atk.",                                     // 260 Flatter
        "Burns foe with an eerie fire.",                                             // 261 Will-O-Wisp
        "User faints; sharply lowers foe's Attack and Sp. Atk.",                   // 262 Memento
        "Power doubles if the user has a status condition.",                        // 263 Facade
        "Charges 1 turn; then delivers a full-power punch.",                        // 264 Focus Punch
        "Doubles power on a paralyzed foe.",                                        // 265 SmellingSalt
        "Draws all attacks to the user for one turn.",                              // 266 Follow Me
        "Uses an attack suited to the current terrain.",                            // 267 Nature Power
        "Charges electricity; boosts next Electric move.",                          // 268 Charge
        "Prevents foe from using status moves for 3 turns.",                       // 269 Taunt
        "Boosts the power of an ally's next move.",                                 // 270 Helping Hand
        "Swaps the user's held item with the foe's.",                              // 271 Trick
        "Copies the foe's Ability.",                                                 // 272 Role Play
        "Restores HP the following turn.",                                           // 273 Wish
        "Uses a random move known by a party member.",                              // 274 Assist
        "Roots user; restores HP each turn; can't switch.",                        // 275 Ingrain
        "A powerful attack that lowers user's stats.",                              // 276 Superpower
        "Bounces status moves back at the user.",                                   // 277 Magic Coat
        "Picks up an item that was recently consumed.",                             // 278 Recycle
        "Stronger if the user moved after the foe.",                               // 279 Revenge
        "A punch that shatters Reflect and Light Screen.",                         // 280 Brick Break
        "Makes foe drowsy; it falls asleep next turn.",                             // 281 Yawn
        "Knocks off and destroys the foe's held item.",                            // 282 Knock Off
        "Reduces the foe's HP to match the user's.",                               // 283 Endeavor
        "Power decreases as user's HP decreases.",                                  // 284 Eruption
        "Swaps the Abilities of the user and target.",                              // 285 Skill Swap
        "Seals moves the foe also knows.",                                          // 286 Imprison
        "Cures the user's own status condition.",                                   // 287 Refresh
        "If user is KO'd, the move that did it loses all PP.",                     // 288 Grudge
        "Grabs and uses any beneficial status move used this turn.",                // 289 Snatch
        "Varies in effect based on the current location.",                          // 290 Secret Power
        "Dives underwater; strikes next turn.",                                     // 291 Dive
        "Repeatedly pummels foe; hits 2–5 times.",                                  // 292 Arm Thrust
        "Changes user's type to match the terrain.",                               // 293 Camouflage
        "Sharply raises the user's Sp. Atk.",                                      // 294 Tail Glow
        "A Psychic attack. May lower Sp. Def.",                                    // 295 Luster Purge
        "A Psychic attack. May lower Sp. Atk.",                                    // 296 Mist Ball
        "Coats foe in feathers; sharply lowers Attack.",                            // 297 FeatherDance
        "A swirling dance that confuses all nearby.",                               // 298 Teeter Dance
        "A fiery kick. High critical hit rate. May burn.",                          // 299 Blaze Kick
        "Stirs up mud; grounds the foe so Electric moves work.",                   // 300 Mud Sport
        "Hits consecutively; gains power with Defense Curl.",                       // 301 Ice Ball
        "Stabs with a cactus needle. May flinch.",                                  // 302 Needle Arm
        "Lays back to restore up to half max HP.",                                  // 303 Slack Off
        "A deafening sound wave that hits all foes.",                               // 304 Hyper Voice
        "A venomous bite. May badly poison.",                                        // 305 Poison Fang
        "Slashes with a sharp claw. May lower Defense.",                            // 306 Crush Claw
        "The ultimate Fire move; user must rest afterward.",                         // 307 Blast Burn
        "The ultimate Water move; user must rest afterward.",                        // 308 Hydro Cannon
        "An iron punch. May raise the user's Attack.",                              // 309 Meteor Mash
        "Surprises foe with a ghostly touch. May flinch.",                          // 310 Astonish
        "Type and power change based on the current weather.",                      // 311 Weather Ball
        "Heals all status conditions in the party.",                                 // 312 Aromatherapy
        "A pitiful cry that sharply lowers Sp. Def.",                               // 313 Fake Tears
        "A slicing gust. High critical hit rate.",                                  // 314 Air Cutter
        "Extremely powerful fire; user's Sp. Atk drops sharply.",                  // 315 Overheat
        "Allows Normal and Fighting moves to hit Ghost types.",                     // 316 Odor Sleuth
        "Drops a rock on foe. Lowers Speed.",                                       // 317 Rock Tomb
        "A silvery wind. May raise all stats.",                                     // 318 Silver Wind
        "Emits a harsh metallic sound; sharply lowers Sp. Def.",                   // 319 Metal Sound
        "A shrill whistle. May cause sleep.",                                        // 320 GrassWhistle
        "Tickles foe to lower Attack and Defense.",                                  // 321 Tickle
        "Raises the user's Defense and Sp. Def.",                                   // 322 Cosmic Power
        "Power decreases as user's HP decreases.",                                  // 323 Water Spout
        "A signal beam. May confuse.",                                               // 324 Signal Beam
        "A ghost punch that never misses.",                                          // 325 Shadow Punch
        "A supernatural attack. May cause flinching.",                              // 326 Extrasensory
        "An uppercut that hits airborne foes.",                                     // 327 Sky Uppercut
        "Traps foe in a sand vortex for 2–5 turns.",                               // 328 Sand Tomb
        "One-hit KO if the foe is slower.",                                         // 329 Sheer Cold
        "Muddy water. May lower accuracy.",                                          // 330 Muddy Water
        "Fires seeds 2–5 times in rapid succession.",                               // 331 Bullet Seed
        "A swift slash that never misses.",                                          // 332 Aerial Ace
        "Fires icicles 2–5 times in a row.",                                        // 333 Icicle Spear
        "Hardens the body; sharply raises Defense.",                                 // 334 Iron Defense
        "Blocks the foe from leaving battle.",                                       // 335 Block
        "Raises the user's Attack.",                                                 // 336 Howl
        "Slashes with sharp dragon claws.",                                          // 337 Dragon Claw
        "The ultimate Grass move; user must rest afterward.",                        // 338 Frenzy Plant
        "Raises the user's Attack and Defense.",                                     // 339 Bulk Up
        "Leaps high; strikes next turn. May paralyze.",                             // 340 Bounce
        "A muddy shot that lowers Speed.",                                           // 341 Mud Shot
        "A poison tail slash. High critical rate. May poison.",                     // 342 Poison Tail
        "Steals foe's held item; user keeps it after battle.",                      // 343 Covet
        "A full-power electric tackle; user takes recoil.",                         // 344 Volt Tackle
        "Fires magical leaves that never miss.",                                     // 345 Magical Leaf
        "Grounds foe so Electric moves work; weakens Electric for 5 turns.",       // 346 Water Sport
        "Raises the user's Sp. Atk and Sp. Def.",                                  // 347 Calm Mind
        "Slashes with a razor-sharp leaf. High critical rate.",                     // 348 Leaf Blade
        "Raises the user's Attack and Speed.",                                       // 349 Dragon Dance
        "Fires boulders 2–5 times.",                                                 // 350 Rock Blast
        "An electric attack that never misses.",                                     // 351 Shock Wave
        "A water pulse. May confuse.",                                               // 352 Water Pulse
        "A steel projectile that strikes 2 turns later.",                           // 353 Doom Desire
        "A powerful psychic attack; user's stats drop sharply.",                    // 354 Psycho Boost
    )

    fun get(moveId: Int): String =
        if (moveId in descs.indices) descs[moveId] else ""
}
