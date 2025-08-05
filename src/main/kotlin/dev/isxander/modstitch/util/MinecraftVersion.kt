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
         * A pattern used to match release version strings.
         */
        private val PATTERN = Regex("^\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?")

        /**
         * Attempts to parse a dot-separated version string into a [ReleaseVersion].
         *
         * Accepts two or three numeric components (e.g., "1.2" or "1.2.3").
         * Missing patch component defaults to zero.
         *
         * @param value The input string to parse.
         * @return A [ReleaseVersion] instance, or `null` if parsing fails.
         */
        fun parseOrNull(value: CharSequence): ReleaseVersion? =
            PATTERN.find(value)?.groupValues?.map { it.toIntOrNull() ?: 0 }?.let { ReleaseVersion(it[1], it[2], it[3]) }
    }
}

internal data class SnapshotVersion(val year: Int, val week: Int, val patch: Int) : Comparable<SnapshotVersion> {
    constructor(year: Int, week: Int, patch: Char) : this(year, week, charToPatch(patch))

    override fun compareTo(other: SnapshotVersion): Int =
        compareValuesBy(this, other, SnapshotVersion::year, SnapshotVersion::week, SnapshotVersion::patch)

    override fun toString(): String = "${year}w$week${patchToChar(patch)}"

    companion object {
        private val PATTERN = Regex("^(\\d{2})w(\\d{2})([a-z])")

        private fun charToPatch(c: Char): Int = c - 'a'
        private fun patchToChar(patch: Int): Char = 'a' + patch

        fun parseOrNull(value: CharSequence): SnapshotVersion? =
            PATTERN.find(value)?.groupValues
                ?.let { SnapshotVersion(it[1].toIntOrNull() ?: 0, it[2].toIntOrNull() ?: 0, charToPatch(it[3][0])) }
    }
}
