package com.example.chatiko.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit
) {

    var selectedCategory by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {

        // CATEGORY BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {

            emojiCategoryIcons.forEachIndexed { index, icon ->

                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .clickable {
                            selectedCategory = index
                        }
                )
            }
        }

        Divider()

        val emojis = emojiCategories[selectedCategory]

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {

            items(emojis.size) { index ->

                val emoji = emojis[index]

                Text(
                    text = emoji,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {

                            onEmojiSelected(emoji)
                        }
                )
            }
        }
    }
}