package us.jmresearch.abcflashcards.data

fun encodeProgress(map: Map<String, ItemProgress>): String =
    map.entries.joinToString(";") { (id, p) -> "$id:${p.correctCount}:${p.lastSeenEpochDay}" }

fun decodeProgress(raw: String): Map<String, ItemProgress> =
    raw.split(";").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 3) return@mapNotNull null
        val count = parts[1].toIntOrNull() ?: return@mapNotNull null
        val day = parts[2].toLongOrNull() ?: return@mapNotNull null
        parts[0] to ItemProgress(count, day)
    }.toMap()
