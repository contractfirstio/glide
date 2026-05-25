package glide.data

import glide.data.persistence.GlideDataRepository

internal fun persistAppData() {
    GlideDataRepository.onStoresMutated()
}
