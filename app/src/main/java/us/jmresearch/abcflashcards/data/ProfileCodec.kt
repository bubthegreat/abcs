package us.jmresearch.abcflashcards.data

data class Profile(val id: String, val name: String)

fun encodeProfiles(profiles: List<Profile>): String =
    profiles.joinToString(";") { "${it.id}|${it.name}" }

fun decodeProfiles(raw: String): List<Profile> =
    raw.split(";").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return@mapNotNull null
        Profile(parts[0], parts[1])
    }

fun sanitizeProfileName(name: String): String =
    name.replace(Regex("[;|:]"), "").trim()

fun nextProfileId(profiles: List<Profile>): String {
    val maxNum = profiles.mapNotNull { it.id.removePrefix("p").toIntOrNull() }.maxOrNull() ?: 0
    return "p${maxNum + 1}"
}
