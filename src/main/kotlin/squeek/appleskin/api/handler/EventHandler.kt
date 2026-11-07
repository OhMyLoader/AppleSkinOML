package squeek.appleskin.api.handler

/**
 * A listener of one of AppleSkin's extension events. The Fabric branch backed the `EVENT` fields
 * with fabric-api's `Event<T>`; on OML the same listener shape is backed by [AppleSkinEvent], so a
 * ported listener only swaps how it registers.
 */
fun interface EventHandler<IEvent> {
    fun interact(event: IEvent)
}

/** Array-backed listener list with the registration-order invocation fabric-api's factory provides. */
class AppleSkinEvent<E> {

    private val listeners = mutableListOf<EventHandler<E>>()

    @Volatile
    private var invoker: EventHandler<E> = EventHandler { }

    fun register(listener: EventHandler<E>) {
        synchronized(listeners) {
            listeners += listener
            val snapshot = listeners.toList()
            invoker = EventHandler { event -> snapshot.forEach { it.interact(event) } }
        }
    }

    fun invoker(): EventHandler<E> = invoker
}
