package de.devland.scanner.event

/**
 * @author David Kunzler (dk@devland.de)
 */
enum class FragmentType {
    SCAN, RESULT, UNKNOWN
}
data class FragmentSelectionEvent(val fragment: FragmentType)