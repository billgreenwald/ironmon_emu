package hh.game.mgba_android.tracker

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.glide.GlideImageState
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
                    var currentPageScale by remember { mutableFloatStateOf(1f) }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        userScrollEnabled = currentPageScale <= 1.01f,
                    ) { page ->
                        var scale by remember(page) { mutableFloatStateOf(1f) }
                        var offset by remember(page) { mutableStateOf(Offset.Zero) }

                        LaunchedEffect(scale, pagerState.currentPage) {
                            if (page == pagerState.currentPage) currentPageScale = scale
                        }

                        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            if (scale > 1f) offset += panChange else offset = Offset.Zero
                        }

                        GlideImage(
                            imageModel = { "file:///android_asset/${images[page]}" },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .transformable(state = transformableState),
                            imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                            onImageStateChanged = { state ->
                                val path = images[page]
                                when (state) {
                                    is GlideImageState.Loading ->
                                        Log.d("Gallery", "loading: $path")
                                    is GlideImageState.Success -> {
                                        val sizeStr = when (val d = state.data) {
                                            is BitmapDrawable -> "${d.bitmap?.width}x${d.bitmap?.height}"
                                            is Drawable -> "${d.intrinsicWidth}x${d.intrinsicHeight}"
                                            else -> d?.javaClass?.simpleName ?: "null"
                                        }
                                        Log.d("Gallery", "success: $path size=$sizeStr src=${state.dataSource}")
                                    }
                                    is GlideImageState.Failure ->
                                        Log.e("Gallery", "failure: $path reason=${state.reason}")
                                    else -> {}
                                }
                            },
                            loading = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = TextSecGallery)
                                }
                            },
                            failure = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Image unavailable", color = TextSecGallery, fontSize = 13.sp)
                                }
                            },
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

                // ── Attribution footer ──────────────────────────────────────
                Text(
                    text = "Images provided by Fellshadow",
                    color = TextSecGallery,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GalleryBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
