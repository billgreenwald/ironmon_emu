package hh.game.mgba_android.tracker.tables

/**
 * Move ratings ported from GachaMonRatingSystem.json "Moves" section.
 * Index = move ID (1–360). Source of truth: Ironmon-Tracker lua scripts.
 */
object MoveRatingTable {
    // Index 0 unused; indices 1–360 match move IDs
    private val RATINGS = floatArrayOf(
         0.00f,  // 0  (unused)
         5.05f,  // 1   Pound
         6.28f,  // 2   Karate Chop
         4.38f,  // 3   Double Slap
         5.44f,  // 4   Comet Punch
         7.85f,  // 5   Mega Punch
         5.35f,  // 6   Pay Day
         8.54f,  // 7   Fire Punch
         8.67f,  // 8   Ice Punch
         8.68f,  // 9   Thunder Punch
         5.05f,  // 10  Scratch
         6.55f,  // 11  Vise Grip
         6.00f,  // 12  Guillotine
         4.95f,  // 13  Razor Wind
        10.00f,  // 14  Swords Dance
         5.50f,  // 15  Cut
         5.57f,  // 16  Gust
         7.27f,  // 17  Wing Attack
         2.00f,  // 18  Whirlwind
         7.33f,  // 19  Fly
         2.42f,  // 20  Bind
         6.85f,  // 21  Slam
         5.14f,  // 22  Vine Whip
         7.85f,  // 23  Stomp
         7.18f,  // 24  Double Kick
         8.55f,  // 25  Mega Kick
         7.51f,  // 26  Jump Kick
         6.38f,  // 27  Rolling Kick
         5.00f,  // 28  Sand Attack
         8.15f,  // 29  Headbutt
         7.55f,  // 30  Horn Attack
         4.88f,  // 31  Fury Attack
         6.00f,  // 32  Horn Drill
         5.05f,  // 33  Tackle
         9.65f,  // 34  Body Slam
         2.30f,  // 35  Wrap
         4.88f,  // 36  Take Down
         5.55f,  // 37  Thrash
         6.85f,  // 38  Double-Edge
         3.00f,  // 39  Tail Whip
         3.09f,  // 40  Poison Sting
         6.58f,  // 41  Twineedle
         5.97f,  // 42  Pin Missile
         3.00f,  // 43  Leer
         7.51f,  // 44  Bite
         3.00f,  // 45  Growl
         2.00f,  // 46  Roar
         5.00f,  // 47  Sing
         4.50f,  // 48  Supersonic
         0.00f,  // 49  Sonic Boom
         5.00f,  // 50  Disable
         5.39f,  // 51  Acid
         5.54f,  // 52  Ember
        10.84f,  // 53  Flamethrower
         3.00f,  // 54  Mist
         5.44f,  // 55  Water Gun
         9.54f,  // 56  Hydro Pump
         0.00f,  // 57  Surf
        10.67f,  // 58  Ice Beam
         8.57f,  // 59  Blizzard
         8.01f,  // 60  Psybeam
         8.04f,  // 61  Bubble Beam
         8.17f,  // 62  Aurora Beam
         6.60f,  // 63  Hyper Beam
         4.77f,  // 64  Peck
         9.27f,  // 65  Drill Peck
         4.38f,  // 66  Submission
         8.18f,  // 67  Low Kick
         3.00f,  // 68  Counter
         6.50f,  // 69  Seismic Toss
         0.00f,  // 70  Strength
         5.41f,  // 71  Absorb
         6.94f,  // 72  Mega Drain
         7.00f,  // 73  Leech Seed
         7.00f,  // 74  Growth
         6.77f,  // 75  Razor Leaf
         6.94f,  // 76  Solar Beam
         4.50f,  // 77  Poison Powder
         5.00f,  // 78  Stun Spore
         5.00f,  // 79  Sleep Powder
         4.64f,  // 80  Petal Dance
         3.00f,  // 81  String Shot
         9.00f,  // 82  Dragon Rage
         4.32f,  // 83  Fire Spin
         5.68f,  // 84  Thunder Shock
        10.98f,  // 85  Thunderbolt
         5.00f,  // 86  Thunder Wave
         9.78f,  // 87  Thunder
         6.05f,  // 88  Rock Throw
        11.02f,  // 89  Earthquake
         6.00f,  // 90  Fissure
         6.17f,  // 91  Dig
         4.50f,  // 92  Toxic
         6.51f,  // 93  Confusion
        10.01f,  // 94  Psychic
         5.00f,  // 95  Hypnosis
         7.00f,  // 96  Meditate
         6.00f,  // 97  Agility
         6.05f,  // 98  Quick Attack
         4.05f,  // 99  Rage
         1.00f,  // 100 Teleport
         6.50f,  // 101 Night Shade
         6.00f,  // 102 Mimic
         3.00f,  // 103 Screech
         5.00f,  // 104 Double Team
        10.00f,  // 105 Recover
         5.00f,  // 106 Harden
         5.00f,  // 107 Minimize
         5.00f,  // 108 Smokescreen
         6.00f,  // 109 Confuse Ray
         5.00f,  // 110 Withdraw
         5.00f,  // 111 Defense Curl
         6.00f,  // 112 Barrier
         4.00f,  // 113 Light Screen
         3.00f,  // 114 Haze
         4.00f,  // 115 Reflect
         5.00f,  // 116 Focus Energy
         3.00f,  // 117 Bide
         5.00f,  // 118 Metronome
         6.50f,  // 119 Mirror Move
         0.00f,  // 120 Selfdestruct
         8.35f,  // 121 Egg Bomb
         4.39f,  // 122 Lick
         3.79f,  // 123 Smog
         8.09f,  // 124 Sludge
         7.15f,  // 125 Bone Club
        10.24f,  // 126 Fire Blast
         0.00f,  // 127 Waterfall
         3.72f,  // 128 Clamp
         7.35f,  // 129 Swift
         6.85f,  // 130 Skull Bash
         7.15f,  // 131 Spike Cannon
         2.15f,  // 132 Constrict
         6.00f,  // 133 Amnesia
         3.00f,  // 134 Kinesis
         9.00f,  // 135 Soft-Boiled
         8.13f,  // 136 High Jump Kick
         5.00f,  // 137 Glare
         5.97f,  // 138 Dream Eater
         4.00f,  // 139 Poison Gas
         5.18f,  // 140 Barrage
         4.88f,  // 141 Leech Life
         6.00f,  // 142 Lovely Kiss
         7.57f,  // 143 Sky Attack
         5.00f,  // 144 Transform
         3.64f,  // 145 Bubble
         7.75f,  // 146 Dizzy Punch
         0.00f,  // 147 Spore
         0.00f,  // 148 Flash
         5.00f,  // 149 Psywave
         0.00f,  // 150 Splash
         6.00f,  // 151 Acid Armor
         8.39f,  // 152 Crabhammer
         0.00f,  // 153 Explosion
         5.17f,  // 154 Fury Swipes
        10.02f,  // 155 Bonemerang
         7.00f,  // 156 Rest
         8.30f,  // 157 Rock Slide
         8.15f,  // 158 Hyper Fang
         7.00f,  // 159 Sharpen
         4.00f,  // 160 Conversion
         9.05f,  // 161 Tri Attack
         0.00f,  // 162 Super Fang
         7.95f,  // 163 Slash
         3.00f,  // 164 Substitute
         0.00f,  // 165 Struggle
        12.00f,  // 166 Sketch
         3.38f,  // 167 Triple Kick
         5.71f,  // 168 Thief
         0.00f,  // 169 Spider Web
         2.00f,  // 170 Mind Reader
         0.00f,  // 171 Nightmare
         7.24f,  // 172 Flame Wheel
         3.65f,  // 173 Snore
         6.00f,  // 174 Curse
         4.00f,  // 175 Flail
         4.00f,  // 176 Conversion 2
         9.67f,  // 177 Aeroblast
         3.00f,  // 178 Cotton Spore
         5.00f,  // 179 Reversal
         3.00f,  // 180 Spite
         5.67f,  // 181 Powder Snow
         4.50f,  // 182 Protect
         6.18f,  // 183 Mach Punch
         3.00f,  // 184 Scary Face
         7.51f,  // 185 Feint Attack
         5.00f,  // 186 Sweet Kiss
         0.00f,  // 187 Belly Drum
        10.09f,  // 188 Sludge Bomb
         4.02f,  // 189 Mud-Slap
         6.97f,  // 190 Octazooka
         3.00f,  // 191 Spikes
         6.58f,  // 192 Zap Cannon
         3.00f,  // 193 Foresight
         0.00f,  // 194 Destiny Bond
         0.00f,  // 195 Perish Song
         7.55f,  // 196 Icy Wind
         4.00f,  // 197 Detect
         7.02f,  // 198 Bone Rush
         2.00f,  // 199 Lock-On
         5.49f,  // 200 Outrage
         0.00f,  // 201 Sandstorm
         7.94f,  // 202 Giga Drain
         1.00f,  // 203 Endure
         3.00f,  // 204 Charm
         4.25f,  // 205 Rollout
         2.85f,  // 206 False Swipe
         5.50f,  // 207 Swagger
         9.00f,  // 208 Milk Drink
         8.08f,  // 209 Spark
         2.13f,  // 210 Fury Cutter
         7.48f,  // 211 Steel Wing
         0.00f,  // 212 Mean Look
         5.00f,  // 213 Attract
         1.00f,  // 214 Sleep Talk
         0.00f,  // 215 Heal Bell
        11.25f,  // 216 Return
         5.00f,  // 217 Present
         0.00f,  // 218 Frustration
         3.00f,  // 219 Safeguard
         7.00f,  // 220 Pain Split
         9.94f,  // 221 Sacred Fire
         8.52f,  // 222 Magnitude
         5.68f,  // 223 Dynamic Punch
        10.78f,  // 224 Megahorn
         7.79f,  // 225 Dragon Breath
         0.00f,  // 226 Baton Pass
         6.00f,  // 227 Encore
         5.31f,  // 228 Pursuit
         3.15f,  // 229 Rapid Spin
         0.00f,  // 230 Sweet Scent
         8.58f,  // 231 Iron Tail
         5.83f,  // 232 Metal Claw
         4.18f,  // 233 Vital Throw
         8.00f,  // 234 Morning Sun
         8.00f,  // 235 Synthesis
         8.00f,  // 236 Moonlight
         6.30f,  // 237 Hidden Power
         7.78f,  // 238 Cross Chop
         5.69f,  // 239 Twister
         0.00f,  // 240 Rain Dance
         0.00f,  // 241 Sunny Day
         9.21f,  // 242 Crunch
         0.00f,  // 243 Mirror Coat
         0.00f,  // 244 Psych Up
         8.55f,  // 245 Extreme Speed
         6.55f,  // 246 Ancient Power
         9.29f,  // 247 Shadow Ball
         3.70f,  // 248 Future Sight
         0.00f,  // 249 Rock Smash
         4.32f,  // 250 Whirlpool
         0.00f,  // 251 Beat Up
         6.15f,  // 252 Fake Out
         3.35f,  // 253 Uproar
         0.00f,  // 254 Stockpile
         0.00f,  // 255 Spit Up
         0.00f,  // 256 Swallow
        10.04f,  // 257 Heat Wave
         1.00f,  // 258 Hail
         3.00f,  // 259 Torment
         5.00f,  // 260 Flatter
         5.00f,  // 261 Will-O-Wisp
         0.00f,  // 262 Memento
         8.15f,  // 263 Facade
         0.00f,  // 264 Focus Punch
         7.55f,  // 265 Smelling Salts
         0.00f,  // 266 Follow Me
         6.00f,  // 267 Nature Power
         0.00f,  // 268 Charge
         3.00f,  // 269 Taunt
         0.00f,  // 270 Helping Hand
         5.00f,  // 271 Trick
         3.00f,  // 272 Role Play
         8.00f,  // 273 Wish
         0.00f,  // 274 Assist
         0.00f,  // 275 Ingrain
         5.68f,  // 276 Superpower
         0.00f,  // 277 Magic Coat
         5.00f,  // 278 Recycle
         6.78f,  // 279 Revenge
         8.58f,  // 280 Brick Break
         5.00f,  // 281 Yawn
         4.21f,  // 282 Knock Off
         3.00f,  // 283 Endeavor
         9.94f,  // 284 Eruption
         0.00f,  // 285 Skill Swap
         0.00f,  // 286 Imprison
         8.00f,  // 287 Refresh
         0.00f,  // 288 Grudge
         0.00f,  // 289 Snatch
         8.65f,  // 290 Secret Power
         0.00f,  // 291 Dive
         5.68f,  // 292 Arm Thrust
         0.00f,  // 293 Camouflage
        10.00f,  // 294 Tail Glow
         7.41f,  // 295 Luster Purge
         7.41f,  // 296 Mist Ball
         3.00f,  // 297 Feather Dance
         6.00f,  // 298 Teeter Dance
         8.49f,  // 299 Blaze Kick
         0.00f,  // 300 Mud Sport
         4.07f,  // 301 Ice Ball
         7.24f,  // 302 Needle Arm
         9.00f,  // 303 Slack Off
         9.85f,  // 304 Hyper Voice
         6.09f,  // 305 Poison Fang
         8.18f,  // 306 Crush Claw
         6.69f,  // 307 Blast Burn
         6.69f,  // 308 Hydro Cannon
         9.18f,  // 309 Meteor Mash
         4.19f,  // 310 Astonish
         5.85f,  // 311 Weather Ball
         0.00f,  // 312 Aromatherapy
         3.00f,  // 313 Fake Tears
         6.90f,  // 314 Air Cutter
         6.94f,  // 315 Overheat
         3.00f,  // 316 Odor Sleuth
         6.25f,  // 317 Rock Tomb
         6.48f,  // 318 Silver Wind
         3.00f,  // 319 Metal Sound
         5.00f,  // 320 Grass Whistle
         5.00f,  // 321 Tickle
         6.00f,  // 322 Cosmic Power
         9.94f,  // 323 Water Spout
         8.78f,  // 324 Signal Beam
         7.09f,  // 325 Shadow Punch
         9.51f,  // 326 Extrasensory
         8.63f,  // 327 Sky Uppercut
         4.39f,  // 328 Sand Tomb
         6.50f,  // 329 Sheer Cold
         9.32f,  // 330 Muddy Water
         4.44f,  // 331 Bullet Seed
         7.27f,  // 332 Aerial Ace
         4.37f,  // 333 Icicle Spear
         6.00f,  // 334 Iron Defense
         0.00f,  // 335 Block
         7.00f,  // 336 Howl
         8.99f,  // 337 Dragon Claw
         6.69f,  // 338 Frenzy Plant
         8.50f,  // 339 Bulk Up
         3.68f,  // 340 Bounce
         7.55f,  // 341 Mud Shot
         6.19f,  // 342 Poison Tail
         6.35f,  // 343 Covet
         7.08f,  // 344 Volt Tackle
         7.44f,  // 345 Magical Leaf
         0.00f,  // 346 Water Sport
         8.50f,  // 347 Calm Mind
         8.04f,  // 348 Leaf Blade
         9.00f,  // 349 Dragon Dance
         7.25f,  // 350 Rock Blast
         7.58f,  // 351 Shock Wave
         7.64f,  // 352 Water Pulse
         3.40f,  // 353 Doom Desire
         6.21f,  // 354 Psycho Boost
         5.75f,  // 355 Roost
        10.20f,  // 356 Recover (TM)
         8.50f,  // 357 Wring Out
         5.50f,  // 358 Power Trick
        11.00f,  // 359 Gastro Acid
         9.25f,  // 360 Lucky Chant
    )

    fun get(moveId: Int): Float =
        if (moveId in RATINGS.indices) RATINGS[moveId] else 0f
}
