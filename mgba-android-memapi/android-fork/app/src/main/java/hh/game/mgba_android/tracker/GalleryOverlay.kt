package hh.game.mgba_android.tracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import hh.game.mgba_android.tracker.tables.ImageAssetMap
import kotlinx.coroutines.launch

private val GalleryBg      = Color(0xFF0A0A0A)
private val GalleryHeader  = Color(0xFF1A1A2E)
private val TextSecGallery = Color(0xFFAAAAAA)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryOverlay(
    routeName: String,
    routeImages: ImageAssetMap.RouteImages,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(GalleryBg)) {
            Column(Modifier.fillMaxSize()) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GalleryHeader)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = routeName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("✕", color = Color.White, fontSize = 18.sp)
                    }
                }

                // ── Tabs ────────────────────────────────────────────────────
                var selectedTab by remember { mutableIntStateOf(
                    if (routeImages.routeMaps.isNotEmpty()) 0 else 1
                ) }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GalleryHeader,
                    contentColor = Color.White,
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Maps", modifier = Modifier.padding(vertical = 8.dp), fontSize = 12.sp)
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Hidden Items", modifier = Modifier.padding(vertical = 8.dp), fontSize = 12.sp)
                    }
                }

                // ── Image pager ─────────────────────────────────────────────
                val images = if (selectedTab == 0) routeImages.routeMaps else routeImages.hiddenItems

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No images available", color = TextSecGallery, fontSize = 13.sp)
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { images.size })
                    val scope = rememberCoroutineScope()

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) { page ->
                        GlideImage(
                            imageModel = { "file:///android_asset/${images[page]}" },
                            modifier = Modifier.fillMaxSize(),
                            imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                        )
                    }

                    // ── Page navigation footer ──────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GalleryHeader)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Text("◀", color = if (pagerState.currentPage > 0) Color.White else TextSecGallery, fontSize = 16.sp)
                        }
                        Text(
                            "${pagerState.currentPage + 1} / ${images.size}",
                            color = TextSecGallery,
                            fontSize = 12.sp,
                        )
                        IconButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                            enabled = pagerState.currentPage < images.size - 1,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Text("▶", color = if (pagerState.currentPage < images.size - 1) Color.White else TextSecGallery, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
