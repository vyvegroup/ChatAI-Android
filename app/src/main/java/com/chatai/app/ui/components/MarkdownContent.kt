package com.chatai.app.ui.components

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import com.chatai.app.ui.theme.ChatColors

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
                setTextColor(android.graphics.Color.parseColor("#ECECF1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setLineSpacing(4f, 1f)
                movementMethod = LinkMovementMethod.getInstance()
                setLinkTextColor(android.graphics.Color.parseColor("#10A37F"))
                setOnClickListener { /* handle clicks */ }
            }
        },
        modifier = modifier.fillMaxWidth(),
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

@Composable
fun GeneratedImageCard(
    imageUrl: String,
    prompt: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = prompt,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        )
        Surface(
            color = ChatColors.SurfaceVariant,
            tonalElevation = 0.dp
        ) {
            Text(
                text = prompt,
                color = ChatColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
fun ImageGeneratingPlaceholder(
    prompt: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(ChatColors.SurfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = ChatColors.Accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Generating image...",
                    color = ChatColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        Text(
            text = prompt,
            color = ChatColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp),
            maxLines = 2
        )
    }
}
