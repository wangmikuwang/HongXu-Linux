package android.os

/** Desktop stand-in for the two monotonic clocks the shared UI reads. */
object SystemClock {
    fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
}
