package me.jeremy.timestamps

import me.jeremy.timestamps.Timestamps.Companion.onlyWhenItChangesTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class TimestampsTest {
    @Test
    fun `initial timestamps`() {
        // given
        val start = Instant.now()

        // when
        val e = SomeEntity.new()

        // then common timestamps are initialised
        assertThat(e.timestamps.createdAt).isBetween(start, Instant.now())
        assertThat(e.timestamps.updatedAt).isEqualTo(e.timestamps.createdAt)

        // and field-specific timestamps are not initialised
        assertThat(e.timestamps["field1"]).isNull()
        assertThat(e.whenThingsGotInteresting).isNull()
    }

    @Test
    fun `basic field setting`() {
        // given a new entity instance
        val start = Instant.now()
        val e = SomeEntity.new("field1")

        // when a field changes
        e.field1 = "field1.1"

        // then updatedAt is updated
        assertThat(e.timestamps.updatedAt).isBetween(start, Instant.now())
        assertThat(e.timestamps.updatedAt).isAfter(e.timestamps.createdAt)
        // and the timestamp for the changed field is updated
        assertThat(e.timestamps["field1"]).isEqualTo(e.timestamps.updatedAt)
        // and the timestamps for other fields are not updated
        assertThat(e.whenThingsGotInteresting).isNull()
    }

    @Test
    fun `flexible tracking`() {
        // given
        val start = Instant.now()
        val e = SomeEntity.new()

        // when the field changes to something interesting
        e.field2 = SomeEnum.INTERESTING
        // then the field is changed
        assertThat(e.field2).isEqualTo(SomeEnum.INTERESTING)
        // and the timestamp for the field is set
        assertThat(e.whenThingsGotInteresting).isBetween(start, Instant.now())
        val whenField2FirstBecameInteresting = e.whenThingsGotInteresting

        // and when the field is set again but doesn't change
        e.field2 = SomeEnum.INTERESTING
        // then the field is as expected
        assertThat(e.field2).isEqualTo(SomeEnum.INTERESTING)
        // and the timestamp is not updated
        assertThat(e.whenThingsGotInteresting).isEqualTo(whenField2FirstBecameInteresting)
        assertThat(e.timestamps.updatedAt).isEqualTo(whenField2FirstBecameInteresting)

        // and when the field is set to something uninteresting
        e.field2 = SomeEnum.UNINTERESTING
        // then the field is changed
        assertThat(e.field2).isEqualTo(SomeEnum.UNINTERESTING)
        // but the timestamp is not updated
        assertThat(e.whenThingsGotInteresting).isEqualTo(whenField2FirstBecameInteresting)

        // but it's not clear if this is right: even though the entity didn't
        // become interesting, it was still updated 🤔
        assertThat(e.timestamps.updatedAt).isEqualTo(whenField2FirstBecameInteresting)
    }

    @Test
    fun rehydration() {
        // given an original copy of an entity
        val e = SomeEntity.new()
        e.field1 = "field1.1"
        e.field2 = SomeEnum.INTERESTING

        // that gets persisted
        val storedTimestamps: Map<String, Instant> = HashMap(e.timestamps)
        val storedCreatedAt = e.timestamps.createdAt
        val storedUpdatedAt = e.timestamps.updatedAt
        val storedField1 = e.field1
        val storedField2 = e.field2

        // when a new copy is rehydrated
        val re = SomeEntity.rehydrate(
            timestamps = Timestamps.rehydrate(
                fields = storedTimestamps,
                createdAt = storedCreatedAt,
                updatedAt = storedUpdatedAt,
            ),
            field1 = storedField1,
            field2 = storedField2,
        )

        // then the timestamps are the same
        assertThat(re.timestamps.createdAt).isEqualTo(e.timestamps.createdAt)
        assertThat(re.timestamps.updatedAt).isEqualTo(e.timestamps.updatedAt)
        assertThat(re.timestamps["field1"]).isEqualTo(e.timestamps["field1"])
        assertThat(re.whenThingsGotInteresting).isEqualTo(e.whenThingsGotInteresting)
    }
}

class SomeEntity private constructor(
    val timestamps: Timestamps = Timestamps.new(),
    field1: String,
    field2: SomeEnum,
) {
    companion object {
        fun new(
            field1: String = "field1",
            field2: SomeEnum = SomeEnum.UNINTERESTING,
        ) = SomeEntity(
            field1 = field1,
            field2 = field2,
        )

        fun rehydrate(
            timestamps: Timestamps,
            field1: String,
            field2: SomeEnum,
        ) = SomeEntity(
            timestamps = timestamps,
            field1 = field1,
            field2 = field2,
        )
    }

    var field1 by timestamps.track(field1)

    var field2 by timestamps.track(
        field2,
        "becameInterestingAt",
        onlyWhenItChangesTo(SomeEnum.INTERESTING),
    )

    val whenThingsGotInteresting: Instant?
        get() = timestamps["becameInterestingAt"]
}

enum class SomeEnum {
    UNINTERESTING, INTERESTING
}
