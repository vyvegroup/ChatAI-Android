package com.chatai.app.ui.components

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import android.graphics.Color as AndroidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember { createMarkwon(context) }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(AndroidColor.parseColor("#ECECF1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                movementMethod = LinkMovementMethod.getInstance()
                setLinkTextColor(AndroidColor.parseColor("#10A37F"))
            }
        },
        modifier = modifier,
        update = { textView ->
            if (markdown.isNotEmpty()) {
                markwon.setMarkdown(textView, markdown)
            } else {
                textView.text = ""
            }
        }
    )
}

private fun createMarkwon(context: android.content.Context): Markwon {
    return Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(CoilImagesPlugin.create(context))
        .build()
}
