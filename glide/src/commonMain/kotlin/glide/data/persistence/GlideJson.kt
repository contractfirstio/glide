package glide.data.persistence

import kotlinx.serialization.json.Json

internal val glideJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
