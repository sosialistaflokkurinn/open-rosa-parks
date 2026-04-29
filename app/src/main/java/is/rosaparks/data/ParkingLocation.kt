package `is`.rosaparks.data

/**
 * Anything a user can park at: an on-street gjaldsvæði or a bílastæðahús.
 * The app's session machinery operates on ParkingLocation; screens that need
 * type-specific fields use a `when` to narrow.
 */
sealed interface ParkingLocation {
    val id: String
    val displayName: String
}
