package me.jeremy.timestamps

import java.time.Instant
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Timestamps manages change timestamps for entity classes.
 * It automatically tracks creation and update times, and
 * provides a property delegate that can flexibly track update
 * times for fields of the entity.
 *
 * Usage suggestion:
 * ```
 * class MyEntity {
 *   val timestamps = Timestamps.empty()
 *   var field1 by timestamps.track("inialValue")
 *   var field2 by timestamps.track(123)
 * }
 * val e = MyEntity()
 * e.field1 = "updated"
 * println(e.timestamps.createdAt)
 * println(e.timestamps.updatedAt)
 * println(e.timestamps["field1"])
 * ```
 */
class Timestamps private constructor(
    private val fields: MutableMap<String, Instant>,
    val createdAt: Instant = Instant.now(),
    updatedAt: Instant = createdAt,
) : Map<String, Instant> by fields {
    companion object {
        /**
         * Construct an empty timestamp tracker with
         * creation and update times set to now and all
         * other values unset.
         */
        fun new() = Timestamps(HashMap())

        /**
         * Use this to rehydrate the timestamps from data
         * eg, from a database.
         */
        fun rehydrate(
            fields: Map<String, Instant>,
            createdAt: Instant,
            updatedAt: Instant,
        ) = Timestamps(HashMap(fields), createdAt, updatedAt)

        /**
         * An implementation of [track]'s `trackChange` parameter that only
         * records a new timestamp when a field changes to a specific value.
         */
        fun <T> onlyWhenItChangesTo(interestingValue: T) = { oldValue: T, newValue: T ->
            interestingValue == newValue && newValue != oldValue
        }
    }

    /**
     * The most recent time at which any field was updated.
     */
    var updatedAt: Instant = updatedAt
        private set

    /**
     * Create a new delegate to track timestamps of changes to some
     * field.
     *
     * @param initialValue the initial value of the field
     * @param timestampName a name for the timestamp;
     *   if null, the name of the field will be used instead.
     * @param trackChange a callback that, if it returns false, causes
     *   an update to the field not to be tracked; by default, changes
     *   are always tracked.
     */
    fun <T> track(
        initialValue: T,
        timestampName: String? = null,
        trackChange: (oldValue: T, newValue: T) -> Boolean = { _, _ -> true }
    ): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            private var value = initialValue

            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return value
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
                if (!trackChange(value, newValue)) {
                    return
                }
                value = newValue
                updatedAt = Instant.now()
                fields[timestampName ?: property.name] = updatedAt
            }
        }
    }
}
