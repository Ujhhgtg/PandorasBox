package dev.ujhhgtg.pandorasbox.ui.composables.screens

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.ui.activities.LocalNavController
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG: String = "PB.GalleryOrganizer"

private val IMAGE_FILE_EXTENSIONS: List<String> =
    listOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp", "tiff", "dng")

@Parcelize
data class MediaItem(
    val file: File,
    val name: String,
) : Parcelable {
    private companion object : Parceler<MediaItem> {
        override fun MediaItem.write(parcel: Parcel, flags: Int) {
            parcel.writeString(file.absolutePath)
        }

        override fun create(parcel: Parcel): MediaItem {
            val file = File(parcel.readString()!!)
            return MediaItem(file, file.name)
        }
    }
}

private fun findImageFolders(): List<File> {
    val rootDir = Environment.getExternalStorageDirectory()
    val basePaths = listOf(
        File(rootDir, "DCIM"),
        File(rootDir, "Pictures")
    )

    val folders = mutableSetOf<File>()

    for (basePath in basePaths) {
        if (basePath.exists() && basePath.isDirectory) {
            folders.add(basePath)

            basePath.listFiles { file -> file.isDirectory }?.forEach { subDir ->
                folders.add(subDir)
            }
        }
    }

    return folders.filter { folder ->
        folder.listFiles { file ->
            file.isFile && file.extension.lowercase() in IMAGE_FILE_EXTENSIONS
        }?.isNotEmpty() == true
    }.toList().sortedBy { it.name }
}

private fun loadImagesFromPath(path: String): List<MediaItem> {
    val folder = File(path)
    if (!folder.exists() || !folder.isDirectory) return emptyList()

    val imageFiles = folder.listFiles { file ->
        file.isFile && file.extension.lowercase() in IMAGE_FILE_EXTENSIONS
    }?.sortedByDescending { it.lastModified() } ?: emptyList()

    return imageFiles.map { file ->
        MediaItem(file, file.name)
    }
}

private suspend fun requestDelete(
    imageFile: File,
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            imageFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed direct file deletion: ${e.message}")
            false
        }
    }
}

@Composable
fun GalleryOrganizerScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current

    val readPermission = Manifest.permission.MANAGE_EXTERNAL_STORAGE

    var folders by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionDenied = false
            folders = findImageFolders()
            isLoading = false
        } else {
            permissionDenied = true
            isLoading = false
            Toast.makeText(context, "Storage access required to list folders.", Toast.LENGTH_LONG)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        if (PermissionManager.checkExternalStorage()) {
            folders = findImageFolders()
            isLoading = false
        } else {
            permissionLauncher.launch(readPermission)
        }
    }


    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (permissionDenied) {
        PermissionDeniedContent(permissionLauncher, readPermission)
    } else if (folders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No folders containing images found.",
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Folders Scanned: ${Environment.getExternalStorageDirectory().path}/DCIM & /Pictures",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(folders) { folder ->
                ListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        .clickable {
                            navController.navigate("gallery_organizing/${Uri.encode(folder.absolutePath)}")
                        },
                    leadingContent = {
                        Icon(
                            painterResource(R.drawable.folder_open_24px),
                            contentDescription = "Folder",
                        )
                    },
                    headlineContent = {
                        Text(
                            folder.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    supportingContent = {
                        Text(
                            folder.absolutePath.removePrefix(Environment.getExternalStorageDirectory().path),
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun GalleryOrganizingScreen(folderPath: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var allImages by rememberSaveable { mutableStateOf<List<MediaItem>>(emptyList()) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var isOverviewMode by rememberSaveable { mutableStateOf(false) }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(folderPath) {
        isLoading = true
        allImages = withContext(Dispatchers.IO) {
            loadImagesFromPath(folderPath)
        }
        isLoading = false
        currentIndex = 0
    }

    if (isOverviewMode) {
        FolderOverviewGrid(
            allImages = allImages,
            onImageClick = { index ->
                currentIndex = index
                isOverviewMode = false
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (allImages.isEmpty()) {
                Text(
                    "No images found in folder: ${File(folderPath).name}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            } else {
                val currentImage = allImages.getOrNull(currentIndex)

                if (currentImage != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .zIndex(3f)
                    ) {
                        Text(
                            "${currentIndex + 1} / ${allImages.size}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            currentImage.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (allImages.size > 1) {
                        val nextImageIndex = (currentIndex + 1) % allImages.size
                        val nextImage = allImages[nextImageIndex]
                        ImageCard(
                            mediaItem = nextImage,
                            modifier = Modifier
                                .zIndex(1f)
                                .graphicsLayer(alpha = 0.6f)
                        )
                    }

                    ImageCard(
                        mediaItem = currentImage,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    offsetX.value.roundToInt(),
                                    offsetY.value.roundToInt()
                                )
                            }
                            .zIndex(2f)
                            .fillMaxWidth(0.85f)
                            .fillMaxHeight(0.85f)
                            .clickable { isOverviewMode = true }
                            .pointerInput(currentImage) {
                                coroutineScope.launch {
                                    offsetX.snapTo(0f)
                                    offsetY.snapTo(0f)
                                }
                                detectDragGestures(
                                    onDragEnd = {
                                        val swipeXThreshold = size.width * 0.25f
                                        val swipeYThreshold = size.height * 0.15f

                                        if (abs(offsetY.value) > swipeYThreshold) {
                                            if (offsetY.value < 0) {
                                                coroutineScope.launch {
                                                    offsetY.animateTo(-size.height.toFloat() * 1.5f)

                                                    val success = requestDelete(currentImage.file)

                                                    if (success) {
                                                        Toast.makeText(
                                                            context,
                                                            "Image deleted: ${currentImage.name}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        allImages = allImages.toMutableList()
                                                            .apply { removeAt(currentIndex) }
                                                        currentIndex =
                                                            if (allImages.isEmpty()) 0 else (currentIndex).coerceAtMost(
                                                                allImages.lastIndex
                                                            )

                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to delete: ${currentImage.name}. Check MANAGE_EXTERNAL_STORAGE permission.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }

                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    offsetY.animateTo(size.height.toFloat() * 1.5f)
                                                    currentIndex =
                                                        (currentIndex + 1) % allImages.size
                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            }
                                        } else if (abs(offsetX.value) > swipeXThreshold) {
                                            coroutineScope.launch {
                                                if (offsetX.value > 0) {
                                                    offsetX.animateTo(size.width.toFloat() * 1.5f)
                                                    currentIndex =
                                                        (currentIndex - 1 + allImages.size) % allImages.size
                                                } else {
                                                    offsetX.animateTo(-size.width.toFloat() * 1.5f)
                                                    currentIndex =
                                                        (currentIndex + 1) % allImages.size
                                                }
                                                offsetY.snapTo(0f)
                                                offsetX.snapTo(0f)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                offsetX.animateTo(0f)
                                                offsetY.animateTo(0f)
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            offsetX.snapTo(offsetX.value + dragAmount.x)
                                            offsetY.snapTo(offsetY.value + dragAmount.y)
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageCard(mediaItem: MediaItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground)
    ) {
        ImageCardContent(mediaItem)
    }
}

@Composable
fun PermissionDeniedContent(
    permissionLauncher: ActivityResultLauncher<String>,
    permission: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            "Storage Access Required",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Due to your request for direct file access, the app requires the $permission permission to scan and manage files.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { permissionLauncher.launch(permission) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun FolderOverviewGrid(
    allImages: List<MediaItem>,
    onImageClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    ) {
        itemsIndexed(allImages, key = { _, item -> item.file.absolutePath }) { index, mediaItem ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onImageClick(index) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                ImageCardContent(mediaItem, targetSizeDp = 75)
            }
        }
    }
}

@Composable
fun ImageCardContent(mediaItem: MediaItem, targetSizeDp: Int? = null) {
    var painter: AsyncImagePainter?

    if (targetSizeDp != null) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val targetSizePx = with(density) { targetSizeDp.dp.roundToPx() }
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(mediaItem.file)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey(mediaItem.name)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(mediaItem.name)
                .size(targetSizePx)
                .build()
        )
    } else {
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(mediaItem.file)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey(mediaItem.name)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(mediaItem.name)
                .build()
        )
    }

    Image(
        painter = painter,
        contentDescription = mediaItem.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
