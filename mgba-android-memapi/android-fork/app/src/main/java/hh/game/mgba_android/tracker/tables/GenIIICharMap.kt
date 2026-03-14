package hh.game.mgba_android.tracker.tables

// Gen III character encoding → Unicode character mapping.
// Source: GameSettings.lua GameCharMap (lines 125–282), which references:
//   https://github.com/pret/pokefirered/blob/master/charmap.txt
// 0xFF is the string terminator (not mapped here — callers stop on it).
object GenIIICharMap {

    private val MAP: Map<Int, Char> = buildMap {
        put(0x00, ' ')
        put(0x01, 'À'); put(0x02, 'Á'); put(0x03, 'Â'); put(0x04, 'Ç')
        put(0x05, 'È'); put(0x06, 'É'); put(0x07, 'Ê'); put(0x08, 'Ë')
        put(0x09, 'Ì'); put(0x0B, 'Î'); put(0x0C, 'Ï'); put(0x0D, 'Ò')
        put(0x0E, 'Ó'); put(0x0F, 'Ô'); put(0x10, 'Œ'); put(0x11, 'Ù')
        put(0x12, 'Ú'); put(0x13, 'Û'); put(0x14, 'Ñ'); put(0x15, 'ß')
        put(0x16, 'à'); put(0x17, 'á'); put(0x19, 'ç'); put(0x1A, 'è')
        put(0x1B, 'é'); put(0x1C, 'ê'); put(0x1D, 'ë'); put(0x1E, 'ì')
        put(0x20, 'î'); put(0x21, 'ï'); put(0x22, 'ò'); put(0x23, 'ó')
        put(0x24, 'ô'); put(0x25, 'œ'); put(0x26, 'ù'); put(0x27, 'ú')
        put(0x28, 'û'); put(0x29, 'ñ'); put(0x2A, 'º'); put(0x2B, 'ª')
        put(0x2D, '&'); put(0x2E, '+')
        put(0x35, '='); put(0x36, ';')
        put(0x51, '¿'); put(0x52, '¡')
        put(0x5A, 'Í'); put(0x5B, '%'); put(0x5C, '('); put(0x5D, ')')
        put(0x68, 'â'); put(0x6F, 'í')
        put(0x85, '<'); put(0x86, '>')
        put(0xA1, '0'); put(0xA2, '1'); put(0xA3, '2'); put(0xA4, '3')
        put(0xA5, '4'); put(0xA6, '5'); put(0xA7, '6'); put(0xA8, '7')
        put(0xA9, '8'); put(0xAA, '9')
        put(0xAB, '!'); put(0xAC, '?'); put(0xAD, '.'); put(0xAE, '-')
        put(0xB0, '\u2026'); put(0xB1, '\u201C'); put(0xB2, '\u201D'); put(0xB3, '\u2018')
        put(0xB4, '\''); put(0xB5, '\u2642'); put(0xB6, '\u2640'); put(0xB7, '\u00A5')
        put(0xB8, ','); put(0xB9, '\u00D7'); put(0xBA, '/')
        put(0xBB, 'A'); put(0xBC, 'B'); put(0xBD, 'C'); put(0xBE, 'D')
        put(0xBF, 'E'); put(0xC0, 'F'); put(0xC1, 'G'); put(0xC2, 'H')
        put(0xC3, 'I'); put(0xC4, 'J'); put(0xC5, 'K'); put(0xC6, 'L')
        put(0xC7, 'M'); put(0xC8, 'N'); put(0xC9, 'O'); put(0xCA, 'P')
        put(0xCB, 'Q'); put(0xCC, 'R'); put(0xCD, 'S'); put(0xCE, 'T')
        put(0xCF, 'U'); put(0xD0, 'V'); put(0xD1, 'W'); put(0xD2, 'X')
        put(0xD3, 'Y'); put(0xD4, 'Z')
        put(0xD5, 'a'); put(0xD6, 'b'); put(0xD7, 'c'); put(0xD8, 'd')
        put(0xD9, 'e'); put(0xDA, 'f'); put(0xDB, 'g'); put(0xDC, 'h')
        put(0xDD, 'i'); put(0xDE, 'j'); put(0xDF, 'k'); put(0xE0, 'l')
        put(0xE1, 'm'); put(0xE2, 'n'); put(0xE3, 'o'); put(0xE4, 'p')
        put(0xE5, 'q'); put(0xE6, 'r'); put(0xE7, 's'); put(0xE8, 't')
        put(0xE9, 'u'); put(0xEA, 'v'); put(0xEB, 'w'); put(0xEC, 'x')
        put(0xED, 'y'); put(0xEE, 'z')
        put(0xEF, '?'); put(0xF0, ':')
        put(0xF1, 'Ä'); put(0xF2, 'Ö'); put(0xF3, 'Ü')
        put(0xF4, 'ä'); put(0xF5, 'ö'); put(0xF6, 'ü')
    }

    fun get(byte: Int): Char = MAP[byte] ?: '?'
}
