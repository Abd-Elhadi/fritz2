package io.fritz2.binding

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class StoreTests {

    @FlowPreview
    @Test
    fun testSingleMountPoint(): Promise<Boolean> {

        val store = RootStore("")

        val mp = checkFlow(store.data, 5) { count, value, _ ->
            //console.log("CHECK $count: $value from $last\n")
            val expected = (0 until count).fold("",{ s,i ->
                "$s-$i"
            })
            assertEquals(expected, value, "set wrong value in SingleMountPoint\n")
        }

        return GlobalScope.promise {
            for (i in 0..4) {
                //console.log("enqueue: -$i\n")
                store.enqueue { "$it-$i" }
            }
            mp.await()
        }
    }

    @FlowPreview
    @Test
    fun testMultiMountPointAppendingAtEnd(): Promise<Boolean> {

        val store = RootStore<List<Int>>(emptyList())
        store.data.launchIn(GlobalScope)

        val mp = checkFlow(store.data.each().data, 5) { count, patch ->
            val expected = Patch(count, listOf(count), 0)

            assertEquals(expected, patch, "set wrong value in MultiMountPoint\n")
        }

        return GlobalScope.promise {
            delay(1) //needs a point to suspend
            for (i in 0..4) {
                store.enqueue { it + i }
            }
            delay(1)

            mp.await()

            true
        }
    }

    @FlowPreview
    @Test
    fun testMultiMountPointAppendingAtBeginning(): Promise<Boolean> {

        val store = RootStore(listOf(0))
        store.data.launchIn(GlobalScope)

        val mp = checkFlow(store.data.each().data, 3) { count, patch ->
            val expected = when (count) {
                0 -> Patch(0, listOf(0), 0)
                1 -> Patch(1, listOf(0), 0)
                2 -> Patch(0, listOf(1), 1)
                else -> throw AssertionError("set wrong value in MultiMountPoint\n")
            }
            assertEquals(expected, patch, "set wrong value in MultiMountPoint\n")
        }

        return GlobalScope.promise {
            delay(1) //needs a point to suspend
            store.enqueue { listOf(1) + it }
            delay(1)

            mp.await()

            true
        }
    }

    @FlowPreview
    @Test
    fun testMultiMountPointAppendingAtMiddle(): Promise<Boolean> {

        val store = RootStore(listOf(0, 2))
        store.data.launchIn(GlobalScope)

        val mp = checkFlow(store.data.each().data, 3) { count, patch ->
            val expected = when (count) {
                0 -> Patch(0, listOf(0, 2), 0)
                1 -> Patch(2, listOf(2), 0)
                2 -> Patch(1, listOf(1), 1)
                else -> throw AssertionError("set wrong value in MultiMountPoint\n")
            }
            assertEquals(expected, patch, "set wrong value in MultiMountPoint\n")
        }

        return GlobalScope.promise {
            delay(1) //needs a point to suspend
            store.enqueue { listOf(0, 1, 2) }
            delay(1)

            mp.await()

            true
        }
    }

    @FlowPreview
    @Test
    fun testMultiMountPointRemovingAtEnd(): Promise<Boolean> {

        val store = RootStore(listOf(0, 1, 2))
        store.data.launchIn(GlobalScope)

        val mp = checkFlow(store.data.each().data, 2) { count, patch ->
            val expected = when (count) {
                0 -> Patch(0, listOf(0, 1, 2), 0)
                1 -> Patch(2, emptyList(), 1)
                else -> throw AssertionError("set wrong value in MultiMountPoint\n")
            }
            assertEquals(expected, patch, "set wrong value in MultiMountPoint\n")
        }

        return GlobalScope.promise {
            delay(1) //needs a point to suspend
            store.enqueue { listOf(0, 1) }
            delay(1)

            mp.await()

            true
        }
    }

    @FlowPreview
    @Test
    fun testMultiMountPointRemovingAtBeginning(): Promise<Boolean> {

        val store = RootStore(listOf(0, 1, 2))
        store.data.launchIn(GlobalScope)

        val mp = checkFlow(store.data.each().data, 4) { count, patch ->
            val expected = when (count) {
                0 -> Patch(0, listOf(0, 1, 2), 0)
                1 -> Patch(2, emptyList(), 1)
                2 -> Patch(0, listOf(1), 1)
                3 -> Patch(1, listOf(2), 1)
                else -> throw AssertionError("set wrong value in MultiMountPoint\n")
            }
            assertEquals(expected, patch, "set wrong value in MultiMountPoint\n")
        }

        return GlobalScope.promise {
            delay(1) //needs a point to suspend
            store.enqueue { listOf(1, 2) }
            delay(1)

            mp.await()

            true
        }
    }

    @FlowPreview
    @Test
    fun testMultiMountPointRemovingAtMiddle(): Promise<Boolean> {

        val store = RootStore(listOf(0, 1, 2))
        store.data.launchIn(GlobalScope)

        val mp = checkFlow(store.data.each().data, 3) { count, patch ->
            val expected = when (count) {
                0 -> Patch(0, listOf(0, 1, 2), 0)
                1 -> Patch(2, emptyList(), 1)
                2 -> Patch(1, listOf(2), 1)
                else -> throw AssertionError("set wrong value in MultiMountPoint\n")
            }
            assertEquals(expected, patch, "set wrong value in MultiMountPoint\n")
        }

        return GlobalScope.promise {
            delay(1) //needs a point to suspend
            store.enqueue { listOf(0, 2) }
            delay(1)

            mp.await()

            true
        }
    }
}