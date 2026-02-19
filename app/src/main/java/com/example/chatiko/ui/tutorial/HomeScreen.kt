package com.example.chatiko.ui.tutorial

import android.widget.Button
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.chatiko.R

data class ScreenContent(val imageRes: Int, val text: String, val subText: String)

// List of content for each page
val screens = listOf(
    ScreenContent(R.drawable.homescreen_image1, "Share your mood anonymously", "Express how you feel without revealing your identity."),
    ScreenContent(R.drawable.homescreen_image2, "See who feels like you nearby", "Discover what others around you are experiencing."),
    ScreenContent(R.drawable.homescreen_image3, "Chat only if both accept", "Connect and chat only when there's mutual interest.")
)

@Composable
fun HomeScreen(navController: NavController) {
    // State for HorizontalPager (keeps track of current page)
    val pagerState = rememberPagerState(0){3}

    // Column to hold HorizontalPager and dots indicator
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // HorizontalPager to swipe between pages
        HorizontalPager(
            state = pagerState, // Use pagerState to control the current page
            modifier = Modifier.weight(1f) // Pager takes all available space
        ) { pageIndex ->
            val screen = screens[pageIndex]
            // Page content (image and text) for the current page
            PagerContent(screen, pagerState, pageIndex)
        }

        // Spacer to add space between the content and pager indicator
        Spacer(modifier = Modifier.height(24.dp))

        // Dots indicator placed outside the HorizontalPager, below the text content
        PagerIndicator(pagerState)

        // Spacer to push button towards the bottom
        Spacer(modifier = Modifier.height(24.dp))
        // Button at the bottom with gradient background
        Button(
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
            onClick = {
                navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)

                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00C6FF), Color(0xFF6A82FB))
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = if (pagerState.currentPage == screens.size - 1) "Get Started" else "Next",
                color = Color.White
            )
        }

    }
}


@Composable
fun PagerContent(screen: ScreenContent, pagerState: PagerState, pageIndex: Int) {
    // Add a background gradient to the pager content
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Use Coil for image loading with scaling
            val painter = rememberImagePainter(
                data = screen.imageRes,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background) // optional placeholder
                    error(R.drawable.ic_launcher_foreground) // optional error image
                }
            )

            // Image with shadow and rounded corners
            Image(
                painter = painter,
                contentDescription = "Page Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)  // You can adjust the height based on your design
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .shadow(10.dp, shape = MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop // Ensures that the image is cropped appropriately
            )

            Spacer(modifier = Modifier.height(16.dp)) // Add space between image and text

            // Heading Text with some animation for scale effect
            val scale by animateFloatAsState(
                targetValue = if (pagerState.currentPage == pageIndex) 1.1f else 1f
            )
            Text(
                text = screen.text,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer(scaleY = scale, scaleX = scale),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp)) // Add space between heading and subheading

            // Subheading Text with lighter opacity
            Text(
                text = screen.subText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black.copy(alpha = 0.50f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )


        }
    }
}

@Composable
fun PagerIndicator(pagerState: PagerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // Create custom dots based on the current page in the pager
        repeat(pagerState.pageCount) { index ->
            val isSelected = index == pagerState.currentPage
            IndicatorDot(isSelected)
            if (index < pagerState.pageCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun IndicatorDot(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = if (isSelected) Color(0xFF6A82FB) else Color.Gray,
                shape = CircleShape
            )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
//    HomeScreen()
}


