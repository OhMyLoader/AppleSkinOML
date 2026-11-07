package squeek.appleskin.network

/**
 * Client-side sync state: whether the server's `naturalHealthRegeneration` rule is on. The HUD and
 * tooltip estimates read this, because a client cannot read server game rules.
 */
object ClientSyncHandler {
    @Volatile
    var naturalRegeneration = true
}
