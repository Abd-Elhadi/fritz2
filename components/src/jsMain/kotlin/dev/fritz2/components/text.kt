package dev.fritz2.components

import dev.fritz2.dom.html.HtmlElements
import dev.fritz2.dom.html.P
import dev.fritz2.styling.params.BasicParams
import dev.fritz2.styling.params.Style
import dev.fritz2.styling.params.use

fun HtmlElements.f2Text(build: Context<BasicComponentContext> = {}): Component<P> {
    val context = BasicComponentContext("f2Text").apply(build)

    return Component { init -> p(context.cssClass, content = init) }
}
