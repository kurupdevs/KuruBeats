/*
 * KuruBeats (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.kurubeats.app.ui.player

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kurubeats.app.R
import com.kurubeats.app.models.MediaMetadata
import com.kurubeats.app.playback.PlayerConnection
import com.kurubeats.app.ui.component.BottomSheetState
import com.kurubeats.app.ui.utils.highRes
import com.kurubeats.app.utils.makeTimeString
import com.kurubeats.app.utils.rememberLowDataModeActive
import kotlin.random.Random

/** KuruBeats brand coral used as the V11 "Echo" accent. */
private val V11Accent = Color(0xFFED5564)
private val V11Text = Color.White
private val V11Subtext = Color.White.copy(alpha = 0.62f)
private val V11Unplayed = Color.White.copy(alpha = 0.22f)

@Composable
private fun V11PlayerBackdrop(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(72.dp)
                            } else {
                                Modifier
                            },
                        ).graphicsLayer {
                            scaleX = 1.3f
                            scaleY = 1.3f
                            alpha = 0.62f
                        },
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.86f),
                                ),
                        ),
                    ),
        )
    }
}

@Composable
private fun V11WaveformSlider(
    positionMs: Long,
    durationMs: Long,
    seed: Int,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barCount = 64
    val bars =
        remember(seed) {
            val rnd = Random(seed)
            List(barCount) { 0.18f + rnd.nextFloat() * 0.82f }
        }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val progressFraction =
        remember(positionMs, durationMs) {
            if (durationMs <= 0L) {
                0f
            } else {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            }
        }
    val displayFraction = dragFraction ?: progressFraction

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((f * durationMs).toLong())
                        onSeekFinished()
                    }
                }.pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val f = (offset.x / size.width).coerceIn(0f, 1f)
                            dragFraction = f
                            onSeek((f * durationMs).toLong())
                        },
                        onHorizontalDrag = { change, _ ->
                            val f = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = f
                            onSeek((f * durationMs).toLong())
                        },
                        onDragEnd = {
                            dragFraction = null
                            onSeekFinished()
                        },
                        onDragCancel = {
                            dragFraction = null
                        },
                    )
                },
    ) {
        val barWidthPx = 3.dp.toPx()
        val stridePx = 5.dp.toPx()
        val count = (size.width / stridePx).toInt().coerceAtLeast(1)
        val centerY = size.height / 2f
        for (i in 0 until count) {
            val h = bars[i % bars.size] * size.height * 0.94f
            val cx = i * stridePx + barWidthPx / 2f
            val played = (i + 1).toFloat() / count.toFloat() <= displayFraction
            drawLine(
                color = if (played) V11Accent else V11Unplayed,
                start = Offset(cx, centerY - h / 2f),
                end = Offset(cx, centerY + h / 2f),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round,
            )
        }
        val hx = (displayFraction * size.width).coerceIn(0f, size.width)
        drawCircle(
            color = V11Accent,
            radius = 5.dp.toPx(),
            center = Offset(hx, centerY),
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(hx, centerY),
        )
    }
}

@Composable
private fun V11ControlButton(
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String?,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun V11PlayerContent(
    mediaMetadata: MediaMetadata,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    onCollapseClick: () -> Unit,
    onQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
) {
    val artworkUrl = mediaMetadata.thumbnailUrl?.highRes()
    val thumbnailSwapState =
        rememberThumbnailSwapState(
            videoId = mediaMetadata.id,
            ytmUrl = artworkUrl,
            lowDataMode = rememberLowDataModeActive(),
            isMusicVideo = mediaMetadata.isMusicVideo,
        )
    val displayArtworkUrl = thumbnailSwapState.displayUrl
    val titleActions = rememberPlayerTitleActions(mediaMetadata, navController, state)

    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val liked = currentSong?.song?.liked == true

    val displayPositionMs = sliderPosition ?: position
    val artistLine = remember(mediaMetadata.artists) {
        mediaMetadata.artists.joinToString(", ") { it.name }
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "v11ArtScale",
    )
    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "v11PlayScale",
    )

    val onPlayPauseClick = {
        if (playbackState == Player.STATE_ENDED) {
            playerConnection.player.seekTo(0, 0)
            playerConnection.player.playWhenReady = true
        } else {
            playerConnection.player.togglePlayPause()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        V11PlayerBackdrop(thumbnailUrl = displayArtworkUrl)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
        ) {
            // ===== Top bar =====
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                V11ControlButton(
                    onClick = onCollapseClick,
                    iconRes = R.drawable.expand_more,
                    contentDescription = "Collapse",
                    tint = V11Text,
                    iconSize = 30.dp,
                )
                Text(
                    text = artistLine,
                    color = V11Subtext,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable {
                                val first = mediaMetadata.artists.firstOrNull()
                                if (first != null) titleActions.onArtistClick(first.id)
                            },
                )
                V11ControlButton(
                    onClick = onLyricsClick,
                    iconRes = R.drawable.lyrics,
                    contentDescription = "Lyrics",
                    tint = V11Text,
                    iconSize = 24.dp,
                )
                V11ControlButton(
                    onClick = onMenuClick,
                    iconRes = R.drawable.more_vert,
                    contentDescription = "More",
                    tint = V11Text,
                    iconSize = 24.dp,
                )
            }

            if (landscape) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        V11Artwork(
                            url = displayArtworkUrl,
                            scale = artScale,
                            modifier = Modifier.fillMaxWidth(0.86f),
                        )
                    }
                    Spacer(modifier = Modifier.width(28.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        V11TitleBlock(
                            title = mediaMetadata.title,
                            artistLine = artistLine,
                            onTitleClick = titleActions.onTitleClick,
                            onArtistClick = {
                                val first = mediaMetadata.artists.firstOrNull()
                                if (first != null) titleActions.onArtistClick(first.id)
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        V11WaveformSlider(
                            positionMs = displayPositionMs,
                            durationMs = duration,
                            seed = mediaMetadata.id.hashCode(),
                            onSeek = onSliderValueChange,
                            onSeekFinished = onSliderValueChangeFinished,
                        )
                        V11TimeRow(positionMs = displayPositionMs, durationMs = duration)
                        Spacer(modifier = Modifier.height(8.dp))
                        V11ControlsRow(
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            canSkipPrevious = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            shuffleModeEnabled = shuffleModeEnabled,
                            repeatMode = repeatMode,
                            playScale = playScale,
                            onPlayPauseClick = onPlayPauseClick,
                            playerConnection = playerConnection,
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(0.55f))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    V11Artwork(
                        url = displayArtworkUrl,
                        scale = artScale,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
                V11TitleBlock(
                    title = mediaMetadata.title,
                    artistLine = artistLine,
                    onTitleClick = titleActions.onTitleClick,
                    onArtistClick = {
                        val first = mediaMetadata.artists.firstOrNull()
                        if (first != null) titleActions.onArtistClick(first.id)
                    },
                    centered = true,
                )
                Spacer(modifier = Modifier.height(22.dp))
                V11WaveformSlider(
                    positionMs = displayPositionMs,
                    durationMs = duration,
                    seed = mediaMetadata.id.hashCode(),
                    onSeek = onSliderValueChange,
                    onSeekFinished = onSliderValueChangeFinished,
                )
                V11TimeRow(positionMs = displayPositionMs, durationMs = duration)
                Spacer(modifier = Modifier.height(10.dp))
                V11ControlsRow(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    playScale = playScale,
                    onPlayPauseClick = onPlayPauseClick,
                    playerConnection = playerConnection,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    V11ControlButton(
                        onClick = { playerConnection.toggleLike() },
                        iconRes = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                        contentDescription = "Like",
                        tint = if (liked) V11Accent else V11Text,
                        iconSize = 26.dp,
                    )
                    Text(
                        text = "swipe for lyrics",
                        color = V11Text.copy(alpha = 0.38f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    V11ControlButton(
                        onClick = onQueueClick,
                        iconRes = R.drawable.queue_music,
                        contentDescription = "Queue",
                        tint = V11Text,
                        iconSize = 26.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun V11Artwork(
    url: String?,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = V11Accent.copy(alpha = 0.35f),
                    spotColor = V11Accent.copy(alpha = 0.35f),
                ).clip(RoundedCornerShape(28.dp)),
    )
}

@Composable
private fun V11TitleBlock(
    title: String,
    artistLine: String,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit,
    centered: Boolean = false,
) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            color = V11Text,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTitleClick),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = artistLine,
            color = V11Subtext,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onArtistClick),
        )
    }
}

@Composable
private fun V11TimeRow(
    positionMs: Long,
    durationMs: Long,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = makeTimeString(positionMs),
            color = V11Subtext,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = makeTimeString(durationMs),
            color = V11Subtext,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun V11ControlsRow(
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    playScale: Float,
    onPlayPauseClick: () -> Unit,
    playerConnection: PlayerConnection,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        V11ControlButton(
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
            iconRes = if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
            contentDescription = "Shuffle",
            tint = if (shuffleModeEnabled) V11Accent else V11Text,
            iconSize = 26.dp,
        )
        V11ControlButton(
            onClick = { playerConnection.player.seekToPrevious() },
            iconRes = R.drawable.skip_previous,
            contentDescription = "Previous",
            tint = V11Text,
            iconSize = 38.dp,
            enabled = canSkipPrevious,
        )
        Box(
            modifier =
                Modifier
                    .size(78.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }.clip(CircleShape)
                    .background(V11Accent)
                    .clickable(onClick = onPlayPauseClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        V11ControlButton(
            onClick = { playerConnection.player.seekToNext() },
            iconRes = R.drawable.skip_next,
            contentDescription = "Next",
            tint = V11Text,
            iconSize = 38.dp,
            enabled = canSkipNext,
        )
        V11ControlButton(
            onClick = { playerConnection.player.toggleRepeatMode() },
            iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            contentDescription = "Repeat",
            tint = if (repeatMode != Player.REPEAT_MODE_OFF) V11Accent else V11Text,
            iconSize = 26.dp,
        )
    }
}
