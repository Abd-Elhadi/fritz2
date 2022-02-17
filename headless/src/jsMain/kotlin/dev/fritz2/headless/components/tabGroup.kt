package dev.fritz2.headless.components

import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.dom.Tag
import dev.fritz2.dom.Keys
import dev.fritz2.dom.RenderContext
import dev.fritz2.core.ScopeContext
import dev.fritz2.dom.shortcutOf
import dev.fritz2.headless.foundation.*
import dev.fritz2.identification.Id
import kotlinx.browser.document
import kotlinx.coroutines.flow.*
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement


class TabGroup<C : HTMLElement>(tag: Tag<C>, id: String?) : Tag<C> by tag {

    private class DisabledTabStore(initial: List<Boolean>) : RootStore<List<Boolean>>(initial) {
        val addTab = handle { state -> state + false }

        fun disabledHandler(index: Int) = handle<Boolean> { state, disabled ->
            state.withIndex().map { if (it.index == index) disabled else it.value }
        }
    }

    private val disabledTabs = DisabledTabStore(emptyList())
    val value by lazy { DatabindingProperty<Int>() }

    /**
     * This is a "gate-keeper" of the external data flow for all internal usage!
     *
     * "External" data from `TabDatabindingHook` is *dirty*, which means there is no control, whether the index
     * is valid (enabled and in bounds!) or not. So we have to check and correct the index for internal usage.
     *
     * In order to return the corrected value to the outside, see the `render` method and the handling also by
     * `selectDefaultTab`.
     *
     * Not optimal solution, but without a buffering internal store I cannot imagine a solution with only one central
     * location for correcting the stream.
     */
    val selected by lazy {
        // set a databinding if user has not provided one
        if (!value.isSet) value(storeOf(0))

        value.data.combine(disabledTabs.data) { index, disabledStates ->
            selectDefaultTab(0, index, disabledStates)
        }
    }

    private val state by lazy { selected.combine(disabledTabs.data, ::Pair) }

    private infix fun <T> Flow<T>.handledBy(handler: (Int, T, List<Boolean>) -> Int): Unit? =
        value.handler?.invoke(
            state.flatMapLatest { (currentIndex, disabledTabs) ->
                this.map { nextIndex ->
                    if (disabledTabs.all { it }) -1 else handler(currentIndex, nextIndex, disabledTabs)
                }
            })

    private fun nextByClick(currentIndex: Int, nextIndex: Int, disabledTabs: List<Boolean>) =
        if (disabledTabs[nextIndex]) currentIndex
        else nextIndex

    private fun nextByKeys(currentIndex: Int, direction: Direction, disabledTabs: List<Boolean>) =
        generateSequence {
            disabledTabs.withIndex().let {
                if (direction == Direction.Next) it else it.reversed()
            }
        }.flatten()
            .drop(
                when (direction) {
                    Direction.Next -> currentIndex + 1
                    Direction.Previous -> disabledTabs.size - currentIndex
                }
            ).take(disabledTabs.size + 1)
            .firstOrNull { !it.value }?.index ?: -1

    private fun firstByKey(currentIndex: Int, payload: Unit, disabledTabs: List<Boolean>) =
        disabledTabs.indexOf(false)

    private fun lastByKey(currentIndex: Int, payload: Unit, disabledTabs: List<Boolean>) =
        disabledTabs.lastIndexOf(false)

    private fun selectDefaultTab(currentIndex: Int, desiredIndex: Int, disabledTabs: List<Boolean>) =
        disabledTabs.take(minOf(desiredIndex, disabledTabs.size - 1) + 1).lastIndexOf(false)

    val componentId: String by lazy { id ?: value.id ?: Id.next() }

    private fun tabId(index: Int) = "$componentId-tab-list-tab-$index"
    private fun panelId(index: Int) = "$componentId-tab-panels-panel-$index"

    var orientation = Orientation.Horizontal

    fun render() {
        attr("id", componentId)
        // We need to emit all internal changes to the outside for realising two-way-data-binding!
        // This includes the automatic correction by `selectDefaultTab` of `selected` setup.
        selected handledBy ::selectDefaultTab
    }

    inner class TabList<CL : HTMLElement>(tag: Tag<CL>) : Tag<CL> by tag {

        private var nextIndex = 0

        private val backwardsKey by lazy {
            if (orientation == Orientation.Horizontal) Keys.ArrowLeft else Keys.ArrowDown
        }
        private val forwardKey by lazy {
            if (orientation == Orientation.Horizontal) Keys.ArrowRight else Keys.ArrowUp
        }

        fun render() {
            attr("role", Aria.Role.tablist)
            attr(Aria.orientation, orientation.toString().lowercase())

            keydowns.mapNotNull { event ->
                when (shortcutOf(event)) {
                    backwardsKey -> Direction.Previous
                    forwardKey -> Direction.Next
                    else -> null
                }.also {
                    if (it != null) {
                        event.stopImmediatePropagation()
                        event.preventDefault()
                    }
                }
            } handledBy ::nextByKeys

            keydowns.filter { setOf(Keys.Home, Keys.PageUp).contains(shortcutOf(it)) }.map {
                it.stopImmediatePropagation()
                it.preventDefault()
            } handledBy ::firstByKey

            keydowns.filter { setOf(Keys.End, Keys.PageDown).contains(shortcutOf(it)) }.map {
                it.stopImmediatePropagation()
                it.preventDefault()
            } handledBy ::lastByKey
        }

        inner class Tab<CT : HTMLElement>(
            tag: Tag<CT>,
            val index: Int
        ) : Tag<CT> by tag {

            val disabled by lazy { disabledTabs.data.map { it[index] } }

            val disable by lazy { disabledTabs.disabledHandler(index) }

            fun render() {
                attr("tabindex", selected.map { if (it == index) "0" else "-1" })
                attr(Aria.selected, selected.map { it == index }.asString())
                attr(Aria.controls, selected.map { if (it == index) panelId(index) else null })
                clicks.map { index } handledBy ::nextByClick
                selected handledBy {
                    if (it == index && domNode != document.activeElement) {
                        domNode.focus()
                    }
                }
            }
        }

        fun <CT : HTMLElement> RenderContext.tab(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            tag: TagFactory<Tag<CT>>,
            initialize: Tab<CT>.() -> Unit
        ) = tag(this, classes, tabId(nextIndex), scope) {
            disabledTabs.addTab()
            Tab(this, nextIndex++).run {
                initialize()
                render()
            }
        }

        fun RenderContext.tab(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            initialize: Tab<HTMLButtonElement>.() -> Unit
        ) = tab(classes, scope, RenderContext::button, initialize)
    }

    fun <CL : HTMLElement> RenderContext.tabList(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        tag: TagFactory<Tag<CL>>,
        initialize: TabList<CL>.() -> Unit
    ): Tag<CL> = tag(this, classes, "$componentId-tab-list", scope) {
        TabList(this).run {
            initialize()
            render()
        }
    }

    fun RenderContext.tabList(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        initialize: TabList<HTMLDivElement>.() -> Unit
    ): Tag<HTMLDivElement> = tabList(classes, scope, RenderContext::div, initialize)


    inner class TabPanels<CP : HTMLElement>(
        tag: Tag<CP>
    ) : Tag<CP> by tag {

        private var panels = mutableListOf<RenderContext.() -> Tag<HTMLElement>>()

        private var nextIndex = 0

        fun render() {
            selected.render { index ->
                if (index != -1) {
                    // the index is always in bounds, so no further check is needed! See `selected` for details.
                    panels[index]()
                }
            }
        }

        fun <CT : HTMLElement> RenderContext.panel(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            tag: TagFactory<Tag<CT>>,
            content: Tag<CT>.() -> Unit
        ) {
            val currentIndex = nextIndex
            panels.add {
                tag(this, classes, panelId(currentIndex), scope) {
                    content()
                    attr("tabindex", "0")
                    attr("role", Aria.Role.tabpanel)
                    attr(Aria.labelledby, tabId(currentIndex))
                }
            }
            nextIndex += 1
        }

        fun RenderContext.panel(
            classes: String? = null,
            scope: (ScopeContext.() -> Unit) = {},
            content: Tag<HTMLDivElement>.() -> Unit
        ) = panel(classes, scope, RenderContext::div, content)
    }

    fun <CP : HTMLElement> RenderContext.tabPanels(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        tag: TagFactory<Tag<CP>>,
        initialize: TabPanels<CP>.() -> Unit
    ): Tag<CP> = tag(this, classes, "$componentId-tab-panels", scope) {
        TabPanels(this).run {
            initialize()
            render()
        }
    }

    fun RenderContext.tabPanels(
        classes: String? = null,
        scope: (ScopeContext.() -> Unit) = {},
        initialize: TabPanels<HTMLDivElement>.() -> Unit
    ): Tag<HTMLDivElement> = tabPanels(classes, scope, RenderContext::div, initialize)
}

fun <C : HTMLElement> RenderContext.tabGroup(
    classes: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    tag: TagFactory<Tag<C>>,
    initialize: TabGroup<C>.() -> Unit
): Tag<C> = tag(this, classes, id, scope) {
    TabGroup(this, id).run {
        initialize()
        render()
    }
}

fun RenderContext.tabGroup(
    classes: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: TabGroup<HTMLDivElement>.() -> Unit
): Tag<HTMLDivElement> = tabGroup(classes, id, scope, RenderContext::div, initialize)
