package com.chatai.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatai.app.ui.theme.ChatColors

@Composable
fun ImageGenerationSheet(
    onDismiss: () -> Unit,
    onGenerate: (prompt: String, width: Int, height: Int) -> Unit,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    var prompt by remember { mutableStateOf("") }
    var selectedWidth by remember { mutableIntStateOf(1024) }
    var selectedHeight by remember { mutableIntStateOf(1024) }

    data class SizeOption(val label: String, val width: Int, val height: Int)
    val sizeOptions = listOf(
        SizeOption("1024 x 1024", 1024, 1024),
        SizeOption("1024 x 1792", 1024, 1792),
        SizeOption("1792 x 1024", 1792, 1024)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = ChatColors.Accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Generate Image",
                color = ChatColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = "Describe the image you want to generate...",
                    color = ChatColors.TextTertiary,
                    fontSize = 14.sp
                )
            },
            textStyle = LocalTextStyle.current.copy(
                color = ChatColors.TextPrimary,
                fontSize = 14.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChatColors.Accent,
                unfocusedBorderColor = ChatColors.Border,
                cursorColor = ChatColors.TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5,
            enabled = !isGenerating
        )

        // Size picker
        Text(
            text = "Image Size",
            color = ChatColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sizeOptions.forEach { option ->
                val isSelected = selectedWidth == option.width && selectedHeight == option.height
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedWidth = option.width
                        selectedHeight = option.height
                    },
                    label = {
                        Text(
                            text = option.label,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChatColors.Accent.copy(alpha = 0.15f),
                        selectedLabelColor = ChatColors.TextPrimary
                    ),
                    enabled = !isGenerating
                )
            }
        }

        // Generate button
        Button(
            onClick = {
                if (prompt.isNotBlank()) {
                    onGenerate(prompt, selectedWidth, selectedHeight)
                    prompt = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = prompt.isNotBlank() && !isGenerating,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChatColors.Accent,
                contentColor = ChatColors.TextOnAccent,
                disabledContainerColor = ChatColors.Border,
                disabledContentColor = ChatColors.TextTertiary
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ChatColors.TextOnAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating...", fontSize = 14.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
