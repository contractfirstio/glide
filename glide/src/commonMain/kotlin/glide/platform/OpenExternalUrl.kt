package glide.platform

/** Opens [url] in the system browser. Returns false if unsupported or the open failed. */
expect fun openExternalUrl(url: String): Boolean
