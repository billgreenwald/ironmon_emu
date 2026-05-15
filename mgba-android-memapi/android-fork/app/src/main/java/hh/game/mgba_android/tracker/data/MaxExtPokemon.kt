package hh.game.mgba_android.tracker.data

data class MaxExtPokemon(val name: String, val bst: Int, val spriteId: Int)
data class MaxExtAbility(val name: String, val description: String)
data class MaxExtMove(val name: String, val description: String)

object MaxExtPokemonParser {

    private val NAME_RE  = Regex("""name\s*=\s*"([^"]+)"""")
    private val BST_RE   = Regex("""bst\s*=\s*(\d+)""")

    /**
     * Parses a gen4.lua / gen5.lua style file and returns speciesId → MaxExtPokemon.
     * Only the first top-level block is consumed (main-game Pokemon).
     */
    fun parseFirstBlock(lua: String, startId: Int, spriteStart: Int): Map<Int, MaxExtPokemon> {
        val firstBlock = lua.substringBefore("\n},{")
        val names = NAME_RE.findAll(firstBlock).map { it.groupValues[1] }.toList()
        val bsts  = BST_RE.findAll(firstBlock).map { it.groupValues[1].toInt() }.toList()
        return buildMap {
            for (i in 0 until minOf(names.size, bsts.size)) {
                put(startId + i, MaxExtPokemon(names[i], bsts[i], spriteStart + i))
            }
        }
    }
}

object MaxExtAbilityParser {

    private val ID_RE   = Regex("""id\s*=\s*(\d+)""")
    private val NAME_RE = Regex("""name\s*=\s*"([^"]+)"""")

    fun parse(lua: String): Map<Int, MaxExtAbility> {
        // Split into per-entry blocks on "},\n    {" or similar
        val entries = splitEntries(lua)
        return buildMap {
            for (entry in entries) {
                val id   = ID_RE.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val name = NAME_RE.find(entry)?.groupValues?.get(1) ?: continue
                val desc = extractDescription(entry)
                put(id, MaxExtAbility(name, desc))
            }
        }
    }
}

object MaxExtMoveParser {

    private val ID_RE   = Regex("""id\s*=\s*"?(\d+)"?""")
    private val NAME_RE = Regex("""name\s*=\s*"([^"]+)"""")

    fun parse(lua: String): Map<Int, MaxExtMove> {
        val entries = splitEntries(lua)
        return buildMap {
            for (entry in entries) {
                val id   = ID_RE.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val name = NAME_RE.find(entry)?.groupValues?.get(1) ?: continue
                val desc = extractDescription(entry)
                put(id, MaxExtMove(name, desc))
            }
        }
    }
}

// Split a Lua table body into individual entry strings.
// Entries are separated by "},\n{" or "},\n    {" patterns.
private fun splitEntries(lua: String): List<String> {
    // Find the outer braces of the return table, then split on entry boundaries
    val inner = lua.substringAfter("return {").substringBeforeLast("}")
    return inner.split(Regex("""\},\s*\n\s*\{"""))
}

// Extract and clean the description string from a Lua entry block.
// Handles multi-line concatenation and Constants.getC("X") calls.
private val CONSTANTS_CHAR_RE = Regex("""Constants\.getC\("(.)"\)""")
private val STRING_LITERAL_RE = Regex(""""((?:[^"\\]|\\.)*)"""")

private fun extractDescription(entry: String): String {
    val descIdx = entry.indexOf("description")
    if (descIdx < 0) return ""
    // Resolve Constants.getC("X") → X
    val resolved = CONSTANTS_CHAR_RE.replace(entry.substring(descIdx)) { it.groupValues[1] }
    // Skip past "description" and "="
    val eqIdx = resolved.indexOf("=")
    if (eqIdx < 0) return ""
    // Collect all quoted string literals after the "="
    return STRING_LITERAL_RE.findAll(resolved, eqIdx + 1)
        .joinToString("") { it.groupValues[1] }
}
