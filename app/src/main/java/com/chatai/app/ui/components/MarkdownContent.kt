package com.chatai.app.ui.components

import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.*
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jSyntaxHighlight
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import org.prism4j.Prism4j

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Int = Color.parseColor("#ECECF1"),
    codeBackgroundColor: Int = Color.parseColor("#1E1E2E"),
    linkColor: Int = Color.parseColor("#10A37F"),
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
    val prism4j = Prism4j(Prism4jGrammarLocator())

    return Markwon.builder(context)
        .usePlugin(CorePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(CoilImagesPlugin.create(context))
        .usePlugin(SyntaxHighlightPlugin.create(
            Prism4jSyntaxHighlight(prism4j, Prism4jThemeDark(codeBackgroundColor))
        ))
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .codeBlockMargin(16)
                    .codeBlockPadding(16)
                    .codeBlockBackgroundColor(codeBackgroundColor)
                    .codeBackgroundColor(codeBackgroundColor)
                    .codeTypeface(android.graphics.Typeface.MONOSPACE)
                    .headingBreakHeight(8)
                    .headingTextSizeMultipliers(floatArrayOf(1.6f, 1.45f, 1.3f, 1.15f, 1.0f, 0.9f))
                    .bulletWidth(8)
                    .linkColor(linkColor)
            }
        })
        .build()
}

class Prism4jGrammarLocator : Prism4j.GrammarLocator {
    override fun grammar(name: String?, visitor: Prism4j.GrammarVisitor) {
        // Register basic languages
        val languages = listOf(
            "clike", "c", "cpp", "java", "kotlin", "javascript", "js",
            "typescript", "ts", "python", "py", "ruby", "go", "rust",
            "swift", "sql", "bash", "shell", "json", "xml", "html",
            "css", "markdown", "yaml", "toml", "docker", "makefile",
            "groovy", "scala", "php"
        )
        languages.forEach { lang ->
            if (name.equals(lang, ignoreCase = true) || name == null) {
                visitor.visit(Prism4j.Grammar(name ?: lang))
            }
        }
    }

    override fun defaultGrammar(): Prism4j.Grammar {
        return Prism4j.Grammar("clike")
    }
}

class Prism4jThemeDark(private val backgroundColor: Int) : Prism4jTheme {
    override fun background(): Int = backgroundColor
    override fun textColor(): Int = Color.parseColor("#CDD6F4")
    override fun tokenColor(type: String?): Int {
        return when (type) {
            "keyword", "boolean", "constant", "symbol" -> Color.parseColor("#CBA6F7")
            "string", "char", "attr-value", "template-string" -> Color.parseColor("#A6E3A1")
            "comment", "prolog", "doctype", "cdata" -> Color.parseColor("#6C7086")
            "function", "class-name", "builtin" -> Color.parseColor("#89B4FA")
            "number", "operator", "variable", "punctuation" -> Color.parseColor("#FAB387")
            "property", "tag", "selector", "attr-name" -> Color.parseColor("#89DCEB")
            "regex", "important", "bold" -> Color.parseColor("#F9E2AF")
            else -> Color.parseColor("#CDD6F4")
        }
    }
}
