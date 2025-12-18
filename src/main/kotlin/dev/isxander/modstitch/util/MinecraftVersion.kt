package dev.isxander.modstitch.util

/**
 * Represents a Minecraft version in various formats.
 *
 * Minecraft has used different versioning schemes over its history:
 * - Modern versions use a year.drop[.hotfix] format (e.g., 25.1, 25.1.2)
 * - Legacy versions use 1.minor.patch format (e.g., 1.12.2, 1.21.4)
 * - Snapshots exist in both modern (25.1-pre1) and legacy (24w10a) formats
 *
 * All implementations except [LegacySnapshot] can be compared across types via [Ordered].
 */
sealed interface MinecraftVersion {
    override fun toString(): String

    /**
     * Versions that can be compared across different version formats.
     *
     * Comparison ordering:
     * - [LegacyRelease] is always less than [Release] or [Snapshot]
     * - [Release] and [Snapshot] are compared by year, then drop
     * - For the same year/drop, [Snapshot] is less than [Release]
     *
     * [LegacySnapshot] is excluded as it cannot be meaningfully compared
     * to other version formats (the year/week scheme doesn't map to releases).
     */
    sealed interface Ordered : MinecraftVersion, Comparable<Ordered>

    sealed interface Unobfuscated : MinecraftVersion

    /**
     * A modern release version using the year.drop[.hotfix] format.
     *
     * Examples: 25.1, 25.1.2
     *
     * @property year The year component (e.g., 25 for 2025)
     * @property drop The drop/release number within the year
     * @property hotfix The hotfix number (0 if not specified)
     */
    data class Release(val year: Int, val drop: Int, val hotfix: Int) : Ordered, Unobfuscated {
        override fun toString(): String =
            "$year.$drop${if (hotfix > 0) ".$hotfix" else ""}"

        override fun compareTo(other: Ordered): Int = when (other) {
            is Release -> compareValuesBy(this, other, Release::year, Release::drop, Release::hotfix)
            is Snapshot -> compareByYearDrop(year, drop, other.year, other.drop, thisIsRelease = true)
            is LegacyRelease -> 1 // Modern release is always greater than legacy
        }

        companion object {
            private val PATTERN = Regex("^\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?")

            /**
             * Parses a modern release version string.
             *
             * @param value Version string like "25.1" or "25.1.2"
             * @return Parsed [Release], or null if the format doesn't match
             */
            fun parseOrNull(value: CharSequence): Release? =
                PATTERN.find(value)?.groupValues?.let { groups ->
                    Release(
                        year = groups[1].toIntOrNull() ?: return null,
                        drop = groups[2].toIntOrNull() ?: return null,
                        hotfix = groups[3].toIntOrNull() ?: 0
                    )
                }
        }
    }

    /**
     * Factory function to parse a [Release] version, throwing if parsing fails.
     */
    fun Release(string: CharSequence): Release =
        Release.parseOrNull(string) ?: throw IllegalArgumentException("Invalid release version: $string")

    /**
     * A modern snapshot/pre-release/release-candidate version.
     *
     * Examples: 25.1-snapshot-1, 25.1-pre-1, 25.1-rc-1
     *
     * @property year The year component
     * @property drop The drop number
     * @property type The snapshot type (snapshot, pre-release, or release candidate)
     * @property build The build number within the type
     */
    data class Snapshot(val year: Int, val drop: Int, val type: Type, val build: Int) : Ordered, Unobfuscated {
        override fun toString(): String =
            "$year.$drop-${type.id}-$build"

        override fun compareTo(other: Ordered): Int = when (other) {
            is Snapshot -> compareValuesBy(this, other, Snapshot::year, Snapshot::drop, Snapshot::type, Snapshot::build)
            is Release -> compareByYearDrop(year, drop, other.year, other.drop, thisIsRelease = false)
            is LegacyRelease -> 1 // Modern snapshot is always greater than legacy
        }

        /**
         * The type of snapshot, ordered from earliest to latest in the release cycle.
         *
         * Order: [Snapshot] < [PreRelease] < [ReleaseCandidate] (< Release)
         */
        enum class Type(val id: String) {
            /** Early development snapshot (e.g., 25.1-snapshot-1) */
            Snapshot("snapshot"),
            /** Pre-release version (e.g., 25.1-pre-1) */
            PreRelease("pre"),
            /** Release candidate (e.g., 25.1-rc-1) */
            ReleaseCandidate("rc");

            companion object {
                private val BY_ID = entries.associateBy { it.id }

                fun fromId(id: String): Type? = BY_ID[id]
            }
        }

        companion object {
            private val PATTERN = Regex("^\\s*(\\d+)\\.(\\d+)-(rc|pre|snapshot)-(\\d+)")

            /**
             * Parses a modern snapshot version string.
             *
             * @param value Version string like "25.1-pre-1" or "25.1-rc-2"
             * @return Parsed [Snapshot], or null if the format doesn't match
             */
            fun parseOrNull(value: CharSequence): Snapshot? =
                PATTERN.find(value)?.groupValues?.let { groups ->
                    Snapshot(
                        year = groups[1].toIntOrNull() ?: return null,
                        drop = groups[2].toIntOrNull() ?: return null,
                        type = Type.fromId(groups[3]) ?: return null,
                        build = groups[4].toIntOrNull() ?: return null
                    )
                }
        }
    }

    /**
     * Factory function to parse a [Snapshot] version, throwing if parsing fails.
     */
    fun Snapshot(string: CharSequence): Snapshot =
        Snapshot.parseOrNull(string) ?: throw IllegalArgumentException("Invalid snapshot version: $string")

    /**
     * A legacy release version using the 1.minor.patch format.
     *
     * Examples: 1.12.2, 1.16.5, 1.21.4
     *
     * @property minor The minor version
     * @property patch The patch number (0 if not specified)
     */
    data class LegacyRelease(val minor: Int, val patch: Int) : Ordered {
        override fun toString(): String =
            "1.$minor${if (patch > 0) ".$patch" else ""}"

        override fun compareTo(other: Ordered): Int = when (other) {
            is LegacyRelease -> compareValuesBy(this, other, LegacyRelease::minor, LegacyRelease::patch)
            is Release, is Snapshot -> -1 // Legacy is always less than modern
        }

        companion object {
            private val PATTERN = Regex("^\\s*1\\.(\\d+)(?:\\.(\\d+))?")

            /**
             * Parses a legacy release version string.
             *
             * @param value Version string like "1.12.2" or "1.16"
             * @return Parsed [LegacyRelease], or null if the format doesn't match
             */
            fun parseOrNull(value: CharSequence): LegacyRelease? =
                PATTERN.find(value)?.groupValues?.let { groups ->
                    LegacyRelease(
                        minor = groups[1].toIntOrNull() ?: return null,
                        patch = groups[2].toIntOrNull() ?: 0
                    )
                }
        }
    }

    /**
     * Factory function to parse a [LegacyRelease] version, throwing if parsing fails.
     */
    fun LegacyRelease(string: CharSequence): LegacyRelease =
        LegacyRelease.parseOrNull(string) ?: throw IllegalArgumentException("Invalid legacy release version: $string")

    /**
     * A legacy snapshot version using the year-week-letter format.
     *
     * Examples: 24w10a, 23w45b, 19w14a
     *
     * This format cannot be meaningfully compared to other version types
     * since the year/week scheme doesn't directly correspond to release versions.
     *
     * @property year Two-digit year (e.g., 24 for 2024)
     * @property week Week number within the year
     * @property revision Revision letter as an index (a=0, b=1, etc.)
     */
    data class LegacySnapshot(val year: Int, val week: Int, val revision: Int) : MinecraftVersion, Comparable<LegacySnapshot> {
        override fun toString(): String =
            "${year}w$week${revisionToChar(revision)}"

        override fun compareTo(other: LegacySnapshot): Int =
            compareValuesBy(this, other, LegacySnapshot::year, LegacySnapshot::week, LegacySnapshot::revision)

        companion object {
            private val PATTERN = Regex("^(\\d{2})w(\\d{2})([a-z])")

            private fun charToRevision(c: Char): Int = c - 'a'
            private fun revisionToChar(revision: Int): Char = 'a' + revision

            /**
             * Parses a legacy snapshot version string.
             *
             * @param value Version string like "24w10a" or "23w45b"
             * @return Parsed [LegacySnapshot], or null if the format doesn't match
             */
            fun parseOrNull(value: CharSequence): LegacySnapshot? =
                PATTERN.find(value)?.groupValues?.let { groups ->
                    LegacySnapshot(
                        year = groups[1].toIntOrNull() ?: return null,
                        week = groups[2].toIntOrNull() ?: return null,
                        revision = charToRevision(groups[3][0])
                    )
                }
        }
    }

    /**
     * Factory function to parse a [LegacySnapshot] version, throwing if parsing fails.
     */
    fun LegacySnapshot(string: CharSequence): LegacySnapshot =
        LegacySnapshot.parseOrNull(string) ?: throw IllegalArgumentException("Invalid legacy snapshot version: $string")

    companion object {
        fun parseOrderableOrNull(value: CharSequence): Ordered? {
            return LegacyRelease.parseOrNull(value)
                ?: Snapshot.parseOrNull(value)
                ?: Release.parseOrNull(value)
        }

        fun parseLegacySnapshotOrNull(value: CharSequence): LegacySnapshot? {
            return LegacySnapshot.parseOrNull(value)
        }

        /**
         * Compares two versions by year and drop, with tie-breaking for release vs snapshot.
         *
         * When year and drop are equal, releases are considered greater than snapshots
         * (since snapshots precede the release in the development cycle).
         *
         * @param thisIsRelease True if the calling version is a Release, false if Snapshot
         */
        private fun compareByYearDrop(
            thisYear: Int, thisDrop: Int,
            otherYear: Int, otherDrop: Int,
            thisIsRelease: Boolean
        ): Int {
            val yearCmp = thisYear.compareTo(otherYear)
            if (yearCmp != 0) return yearCmp

            val dropCmp = thisDrop.compareTo(otherDrop)
            if (dropCmp != 0) return dropCmp

            // Same year and drop: release > snapshot
            return if (thisIsRelease) 1 else -1
        }
    }
}

fun minecraftVersion(string: CharSequence): MinecraftVersion.Ordered =
    MinecraftVersion.parseOrderableOrNull(string)
        ?: throw IllegalArgumentException("Invalid Minecraft version: $string")

fun minecraftLegacySnapshot(string: CharSequence): MinecraftVersion.LegacySnapshot =
    MinecraftVersion.parseLegacySnapshotOrNull(string)
        ?: throw IllegalArgumentException("Invalid Minecraft legacy snapshot version: $string")