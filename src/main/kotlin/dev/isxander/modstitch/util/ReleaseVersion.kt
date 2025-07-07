package dev.isxander.modstitch.util

/**
 * Represents a semantic version without pre-release or build metadata components.
 *
 * @property major The major version component.
 * @property minor The minor version component.
 * @property patch The patch version component.
 */
internal data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    /**
     * Compares this version to another [ReleaseVersion].
     *
     * @param other The version to compare against.
     * @return A negative integer if this version is lower than [other], zero if equal, or a positive integer if greater.
     */
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)

    /**
     * Returns a string representation of this version in the `major.minor.patch` format.
     */
    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Attempts to parse a dot-separated version string into a [ReleaseVersion].
         *
         * Accepts up to three numeric components (e.g., "1", "1.2", "1.2.3").
         * Missing components default to zero.
         *
         * @param value The input string to parse.
         * @return A [ReleaseVersion] instance, or `null` if parsing fails.
         */
        fun parseOrNull(value: CharSequence): ReleaseVersion? {
            val parts = value.splitToSequence('.').take(3).map { it.toIntOrNull() }.toList()
            if (parts.isEmpty() || parts.any { it == null }) {
                return null
            }
            return ReleaseVersion(parts[0] ?: 0, parts.elementAtOrNull(1) ?: 0, parts.elementAtOrNull(2) ?: 0)
        }
    }
}
