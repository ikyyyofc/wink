package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream
import coil.compose.AsyncImage
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<RootFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasRoot by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var videoToPreview by remember { mutableStateOf<RootFile?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var isPreparingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(videoToPreview) {
        val fileToPreview = videoToPreview
        if (fileToPreview != null) {
            isPreparingPreview = true
            withContext(Dispatchers.IO) {
                val tmp = File(context.cacheDir, "preview.mp4")
                executeRootCmd("cp \"${fileToPreview.path}\" \"${tmp.absolutePath}\"")
                executeRootCmd("chmod 666 \"${tmp.absolutePath}\"")
                previewFile = tmp
            }
            isPreparingPreview = false
        }
    }

    fun refreshFiles() {
        coroutineScope.launch {
            isLoading = true
            hasRoot = hasRootAccess()
            if (hasRoot == true) {
                files = listWinkCacheRoot()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBackground)
                    .padding(end = 24.dp, start = 24.dp, bottom = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = PrimaryDark)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { refreshFiles() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                    }
                }
                Text("Wink Bypass", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TextPrimary, letterSpacing = (-0.5).sp)
                Text("Saves to: /sdcard/Download/hd", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Divider(color = OverlayLight, thickness = 1.dp)

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CACHE FOUND (${files.size} FILES)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(PrimaryLight, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("HD ENABLED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryDark)
                    }
                } else if (files.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(files, key = { it.path }) { file ->
                            FileItem(
                                file = file,
                                onClick = { videoToPreview = file }
                            ) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saving ${file.name}...")
                                    val success = saveFileToDownloadsRoot(context, file)
                                    if (success) {
                                        snackbarHostState.showSnackbar("Saved ${file.name} to Download/hd")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save ${file.name}")
                                    }
                                }
                            }
                        }
                        
                        // Removed Destination Footer

                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (hasRoot == false) "Root access denied or not available.\nThis application requires root on Android 16+." else "No files found in the folder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { refreshFiles() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                            shape = CircleShape,
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp), tint = Color.White)
                            Text("Retry Access", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
    
    if (videoToPreview != null) {
        if (isPreparingPreview) {
            Dialog(onDismissRequest = {}) {
                CircularProgressIndicator(color = PrimaryDark)
            }
        } else if (previewFile != null) {
            val isVideo = videoToPreview!!.name.endsWith(".mp4", ignoreCase = true) || videoToPreview!!.name.endsWith(".mov", ignoreCase = true)
            MediaPreviewDialog(
                previewFile = previewFile!!,
                isVideo = isVideo,
                onDismiss = {
                    videoToPreview = null
                    previewFile?.delete()
                    previewFile = null
                }
            )
        }
    }
}

@Composable
fun MediaPreviewDialog(previewFile: File, isVideo: Boolean, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (isVideo) {
                CustomVideoPlayer(videoFile = previewFile)
            } else {
                val bitmap = remember { android.graphics.BitmapFactory.decodeFile(previewFile.absolutePath) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),    
                        contentDescription = "Image Preview",
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).align(Alignment.Center),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            IconButton(
                onClick = onDismiss, 
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun CustomVideoPlayer(videoFile: File) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val videoView = remember { 
        android.widget.VideoView(context).apply {
            setVideoPath(videoFile.absolutePath)
            setOnPreparedListener { mp ->
                mp.start()
                isPlaying = true
            }
            setOnCompletionListener {
                isPlaying = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
        AndroidView(factory = { videoView }, modifier = Modifier.align(Alignment.Center).fillMaxSize())
        
        // Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                ))
                .padding(bottom = 16.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { videoView.seekTo(maxOf(0, videoView.currentPosition - 5000)) }) {
                    Icon(androidx.compose.material.icons.Icons.Default.FastRewind, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                IconButton(onClick = { 
                    if (isPlaying) {
                        videoView.pause()
                    } else {
                        videoView.start()
                    }
                    isPlaying = !isPlaying 
                }) {
                    Icon(
                        imageVector = if (isPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow, 
                        contentDescription = "Play/Pause", 
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                IconButton(onClick = { videoView.seekTo(videoView.currentPosition + 5000) }) {
                    Icon(androidx.compose.material.icons.Icons.Default.FastForward, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

val thumbSemaphore = Semaphore(2)

suspend fun loadThumbnailFile(context: Context, file: RootFile): File? = withContext(Dispatchers.IO) {
    val thumbCache = File(context.cacheDir, "thumbs")
    if (!thumbCache.exists()) thumbCache.mkdirs()
    val thumbFile = File(thumbCache, "${file.name}.jpg")
    
    if (thumbFile.exists()) {
        return@withContext thumbFile
    }

    val isVideo = file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mov", ignoreCase = true)

    thumbSemaphore.withPermit {
        if (thumbFile.exists()) return@withContext thumbFile

        if (isVideo) {
            val tmpMedia = File(context.cacheDir, "tmp_${file.name.replace(" ", "_")}")
            executeRootCmd("cp \"${file.path}\" \"${tmpMedia.absolutePath}\"")
            executeRootCmd("chmod 666 \"${tmpMedia.absolutePath}\"")
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(tmpMedia.absolutePath)
                val bitmap = retriever.getFrameAtTime(0)
                bitmap?.let {
                    val out = java.io.FileOutputStream(thumbFile)
                    it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                    out.close()
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                tmpMedia.delete()
            }
        } else {
            // For images, caching the file directly works perfectly
            executeRootCmd("cp \"${file.path}\" \"${thumbFile.absolutePath}\"")
            executeRootCmd("chmod 666 \"${thumbFile.absolutePath}\"")
        }
    }
    
    return@withContext if (thumbFile.exists()) thumbFile else null
}

@Composable
fun FileItem(file: RootFile, onClick: () -> Unit, onDownload: () -> Unit) {
    val context = LocalContext.current
    var thumbnailFile by remember { mutableStateOf<File?>(null) }
    
    LaunchedEffect(file) {
        thumbnailFile = loadThumbnailFile(context, file)
    }

    val isVideo = file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mov", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(28.dp))
            .shadow(0.dp, RoundedCornerShape(28.dp)) // Very subtle shadow, handled by Card elevation essentially, keeping 0 to not render grey box behind translucent background
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Avoid solid shadow behind transparency
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailFile != null) {
                    AsyncImage(
                        model = thumbnailFile,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0x3360A5FA), Color.Transparent)
                                )
                            )
                    )
                    Icon(if (isVideo) Icons.Default.Movie else Icons.Default.Image, contentDescription = null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeText = if (file.sizeBytes >= 1024 * 1024) "${file.sizeBytes / (1024 * 1024)} MB" else "${file.sizeBytes / 1024} KB"
                Text(
                    text = "$sizeText • ${if (isVideo) "Video" else "Image"}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryLight, CircleShape)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Save file", tint = PrimaryDark, modifier = Modifier.size(20.dp))
            }
        }
    }
}

data class RootFile(val name: String, val path: String, val sizeBytes: Long, val time: Long)

suspend fun hasRootAccess(): Boolean = withContext(Dispatchers.IO) {
    try {
        val process = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(process.outputStream)
        os.writeBytes("id\n")
        os.writeBytes("exit\n")
        os.flush()
        process.waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

suspend fun executeRootCmd(cmd: String): List<String> = withContext(Dispatchers.IO) {
    val output = mutableListOf<String>()
    var process: Process? = null
    try {
        process = Runtime.getRuntime().exec("su")
        val os = DataOutputStream(process.outputStream)
        os.writeBytes("$cmd\n")
        os.writeBytes("exit\n")
        os.flush()
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (!isActive) break
            output.add(line!!)
        }
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        process?.destroy()
    }
    return@withContext output
}

suspend fun listWinkCacheRoot(): List<RootFile> = withContext(Dispatchers.IO) {
    val dirPath = "/sdcard/Android/data/com.meitu.wink/cache/video_edit/video_repair"
    val output = executeRootCmd("for f in \"$dirPath\"/*; do if [ -f \"\$f\" ]; then stat -c \"%n|%s|%Y\" \"\$f\"; fi; done")
    val list = mutableListOf<RootFile>()
    for (line in output) {
        val parts = line.split("|")
        if (parts.size >= 3) {
            val path = parts[0]
            val name = path.substringAfterLast("/")
            val size = parts[1].toLongOrNull() ?: 0L
            val time = parts[2].toLongOrNull() ?: 0L
            list.add(RootFile(name, path, size, time))
        }
    }
    list.sortedByDescending { it.time }
}

suspend fun saveFileToDownloadsRoot(context: Context, file: RootFile): Boolean = withContext(Dispatchers.IO) {
    val destDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "hd")
    if (!destDir.exists()) destDir.mkdirs()
    val destFile = File(destDir, file.name)
    
    val tmpFile = File(context.cacheDir, "export_${file.name.replace(" ", "_")}")
    val cpRes = executeRootCmd("cp -f \"${file.path}\" \"${tmpFile.absolutePath}\" && chmod 666 \"${tmpFile.absolutePath}\" && echo success")
    
    var success = false
    if (cpRes.any { it.contains("success") }) {
        try {
            tmpFile.inputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            // Verify and declare success
            success = destFile.exists() && destFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tmpFile.delete()
        }
    }
    
    if (success) {
        android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
    }
    return@withContext success
}
