[fritz2](../index.md) / [io.fritz2.binding](index.md) / [routing](./routing.md)

# routing

`@FlowPreview @ExperimentalCoroutinesApi fun routing(default: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Router`](-router/index.md)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>`

Creates a new simple [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) based [Router](-router/index.md)

### Parameters

`default` - default route`@FlowPreview @ExperimentalCoroutinesApi fun routing(default: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): `[`Router`](-router/index.md)`<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>>`

Creates a new [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html) based [Router](-router/index.md)

### Parameters

`default` - default route`@FlowPreview @ExperimentalCoroutinesApi fun <T> routing(default: `[`Route`](-route/index.md)`<T>): `[`Router`](-router/index.md)`<T>`

Creates a new type based [Router](-router/index.md).
Therefore the given type must implement the [Route](-route/index.md) interface.

### Parameters

`default` - default route