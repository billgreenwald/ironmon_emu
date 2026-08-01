package hh.game.mgba_android.tracker.persistence

import kotlinx.serialization.json.Json

/**
 * Shared JSON codec for all tracker persistence.
 *
 * - `ignoreUnknownKeys`: forward-compatibility — older builds tolerate fields added later.
 * - `encodeDefaults`: write every field (matches the previous Gson output) so the on-disk wire
 *   format stays identical and existing device saves round-trip unchanged.
 */
internal val trackerJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
