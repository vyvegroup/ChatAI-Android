package com.chatai.app.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.CorePlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Int = android.graphics.Color.parseColor("#ECECF1"),
    codeBackgroundColor: Int = android.graphics.Color.parseColor("#1E1E2E"),
    linkColor: Int = android.graphics.Color.parseColor("#10A37F"),
    onImageClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val markwon = remember {
        createMarkwon(context, textColor, codeBackgroundColor, linkColor)
    }

    AndroidView(
        factory = { ctx ->
            android.widget.TextView(ctx).apply {
                setTextColor(textColor)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                movementMethod = LinkMovementMethod.getInstance()
                setLinkTextColor(linkColor)
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

private fun createMarkwon(
    context: Context,
    textColor: Int,
    codeBackgroundColor: Int,
    linkColor: Int
): Markwon {
    return Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(CoilImagesPlugin.create(context))
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .codeBlockMargin(16)
                    .codeBlockPadding(16)
                    .codeBlockBackgroundColor(codeBackgroundColor)
                    .codeBackgroundColor(codeBackgroundColor)
                    .codeTypeface(Typeface.MONOSPACE)
                    .headingBreakHeight(8)
                    .headingTextSizeMultipliers(floatArrayOf(1.6f, 1.45f, 1.3f, 1.15f, 1.0f, 0.9f))
                    .bulletWidth(8)
                    .linkColor(linkColor)
            }
        })
        .build()
}
