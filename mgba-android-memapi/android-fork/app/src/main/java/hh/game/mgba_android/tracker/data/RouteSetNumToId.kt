package hh.game.mgba_android.tracker.data

/**
 * Maps a FireRed/LeafGreen randomizer-log "Set #N" wild-encounter number to the
 * Gen III internal map layout id (same scheme as RouteNames / TrainerRouteTable).
 *
 * Ported verbatim from RandomizerLog.lua `setupFRLGRouteMappings()`.
 * RSE/Emerald Set# tables are not yet ported (route wild data is FRLG-only for now).
 */
internal val FRLG_ROUTE_SET_NUM_TO_ID: Map<Int, Int> = mapOf(
    1 to 335, 2 to 336, 3 to 337, 4 to 338, 5 to 339, 6 to 362, 7 to 363, 8 to 117,
    9 to 114, 10 to 115, 11 to 116, 12 to 118, 13 to 118, 14 to 124, 15 to 125,
    16 to 126, 17 to 127, 18 to 143, 19 to 144, 20 to 145, 21 to 146, 22 to 147,
    23 to 147, 24 to 147, 25 to 148, 26 to 148, 27 to 148, 28 to 149, 29 to 149,
    30 to 149, 31 to 150, 32 to 150, 33 to 150, 34 to 151, 35 to 151, 36 to 151,
    37 to 151, 38 to 152, 39 to 152, 40 to 153, 41 to 153, 42 to 153, 43 to 153,
    44 to 154, 45 to 155, 46 to 155, 47 to 156, 48 to 157, 49 to 158, 50 to 159,
    51 to 159, 52 to 159, 53 to 160, 54 to 160, 55 to 160, 56 to 163, 57 to 164,
    58 to 165, 59 to 166, 60 to 167, 61 to 168, 62 to 280, 63 to 280, 64 to 282,
    65 to 283, 66 to 283, 67 to 284, 68 to 285, 69 to 285, 70 to 286, 71 to 286,
    72 to 287, 73 to 287, 74 to 288, 75 to 288, 76 to 289, 77 to 289, 78 to 290,
    79 to 290, 80 to 270, 81 to 270, 82 to 270, 83 to 293, 84 to 293, 85 to 293,
    86 to 294, 87 to 295, 88 to 296, 89 to 296, 90 to 296, 91 to 317, 92 to 321,
    93 to 322, 94 to 323, 95 to 324, 96 to 325, 97 to 326, 98 to 327, 99 to 328,
    100 to 329, 101 to 330, 102 to 331, 103 to 332, 104 to 333, 105 to 334,
    106 to 237, 107 to 237, 108 to 237, 109 to 237, 110 to 238, 111 to 238,
    112 to 238, 113 to 239, 114 to 239, 115 to 239, 116 to 240, 117 to 240,
    118 to 240, 119 to 241, 120 to 246, 121 to 246, 122 to 247, 123 to 247,
    124 to 248, 125 to 248, 126 to 248, 127 to 249, 128 to 249, 129 to 249,
    130 to 250, 131 to 250, 132 to 251, 133 to 251, 134 to 252, 135 to 252,
    136 to 252, 137 to 253, 138 to 253, 139 to 253, 140 to 254, 141 to 254,
    142 to 255, 143 to 256, 144 to 256, 145 to 257, 146 to 257, 147 to 89,
    148 to 90, 149 to 91, 150 to 92, 151 to 92, 152 to 92, 153 to 93, 154 to 94,
    155 to 94, 156 to 94, 157 to 95, 158 to 96, 159 to 97, 160 to 98, 161 to 98,
    162 to 98, 163 to 99, 164 to 99, 165 to 99, 166 to 100, 167 to 100, 168 to 100,
    169 to 101, 170 to 101, 171 to 101, 172 to 102, 173 to 103, 174 to 104,
    175 to 105, 176 to 106, 177 to 107, 178 to 107, 179 to 108, 180 to 108,
    181 to 109, 182 to 109, 183 to 109, 184 to 219, 185 to 219, 186 to 219,
    187 to 110, 188 to 110, 189 to 110, 190 to 111, 191 to 111, 192 to 111,
    193 to 112, 194 to 112, 195 to 112, 196 to 113, 197 to 113, 198 to 113,
    199 to 78, 200 to 78, 201 to 79, 202 to 79, 203 to 81, 204 to 81, 205 to 83,
    206 to 83, 207 to 84, 208 to 84, 209 to 85, 210 to 85, 211 to 86, 212 to 86,
    213 to 230, 214 to 230, 215 to 233, 216 to 233, 217 to 234, 218 to 234,
    219 to 340, 220 to 340, 221 to 340, 222 to 340, 223 to 340, 224 to 340,
    225 to 340, 226 to 340, 227 to 340,
)
