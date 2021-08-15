# Entity field timestamp tracking with delegates in Kotlin

Given an entity like this:
```kotlin
class SomeEntity {
    var field1: String
    var field2: SomeEnum
}
```

It's common to want to track timestamps of various types
of changes, eg:
- when the entity was created
- when it was last updated
- perhaps when certain changes to certain fields happened

It can be a bit repetitive to implement this, so this repo
is an experiment to see if kotlin delegates can help.  Now
you can do this:
```kotlin
class SomeEntity {
    val timestamps = Timestamps.new()
    var field1 by timestamps.track("value1")
    var field2 by timestamps.track(SomeEnum.SOME_VALUE)
}
val e = SomeEntity()
e.field1 = "value1.2"
println(e.field1)
println(e.timestamps.createdAt)
println(e.timestamps.updatedAt)
println(e.timestamps["field1"])
```

See [the tests](./src/test/kotlin/me/jeremy/timestamps/TimestampsTest.kt)
for more detailed usage examples.