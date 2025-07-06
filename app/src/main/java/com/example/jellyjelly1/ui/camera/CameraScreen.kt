// NOTE: After adding the accompanist-permissions dependency, sync Gradle to resolve permission-related imports.
package com.example.jellyjelly1.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.jellyjelly1.data.VideoRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember

// Video quality settings
enum class VideoQuality(val label: String, val width: Int, val height: Int, val bitrate: Int, val fps: Int) {
    LOW("Low", 640, 480, 1000000, 24),      // 640x480, 1Mbps, 24fps
    MEDIUM("Medium", 1280, 720, 3000000, 30), // 1280x720, 3Mbps, 30fps
    HIGH("High", 1920, 1080, 10000000, 30)   // 1920x1080, 10Mbps, 30fps
}

data class CameraIdInfo(val back: String?, val front: String?)

fun getCameraIdInfo(context: Context): CameraIdInfo {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    var backId: String? = null
    var frontId: String? = null
    for (id in cameraManager.cameraIdList) {
        val characteristics = cameraManager.getCameraCharacteristics(id)
        when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_BACK -> backId = id
            CameraCharacteristics.LENS_FACING_FRONT -> frontId = id
        }
    }
    return CameraIdInfo(back = backId, front = frontId)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
        // Manifest.permission.WRITE_EXTERNAL_STORAGE // Not needed for API 29+
    )
    val permissionsState = rememberMultiplePermissionsState(cameraPermissions)
    var duration by remember { mutableStateOf(15) } // 15 or 60 seconds
    var isRecording by remember { mutableStateOf(false) }
    var showDualCamera by remember { mutableStateOf(false) }
    var dualCameraSupported by remember { mutableStateOf(false) }
    val cameraIdInfo = remember { mutableStateOf(CameraIdInfo(null, null)) }
    var currentCameraId by remember { mutableStateOf<String?>(null) }
    var lastVideoUri by remember { mutableStateOf<String?>(null) }
    var selectedQuality by remember { mutableStateOf(VideoQuality.MEDIUM) } // Default to medium
    var soundEnabled by remember { mutableStateOf(true) } // Default to sound enabled
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-detect optimal quality based on device capabilities
    LaunchedEffect(Unit) {
        selectedQuality = detectOptimalQuality(context)
    }

    // Get camera IDs on startup
    LaunchedEffect(Unit) {
        cameraIdInfo.value = getCameraIdInfo(context)
        currentCameraId = cameraIdInfo.value.back // Default to back camera
    }

    // Check for dual camera support (logical multi-camera)
    LaunchedEffect(Unit) {
        dualCameraSupported = supportsDualCamera(context)
        if (!dualCameraSupported && showDualCamera) {
            showDualCamera = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Dual camera preview is not supported on this device.")
            }
        }
    }

    Box(Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter))
        if (!permissionsState.allPermissionsGranted) {
            // Request permissions UI
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera and audio permissions are required.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            }
        } else if (cameraIdInfo.value.back == null && cameraIdInfo.value.front == null) {
            // No camera available
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No camera available", color = Color.White)
            }
        } else {
            // Camera UI
            AnimatedContent(targetState = showDualCamera) { dual ->
                if (dual && cameraIdInfo.value.front != null && cameraIdInfo.value.back != null) {
                    Column(Modifier.fillMaxSize()) {
                        Camera2Preview(
                            cameraId = cameraIdInfo.value.front!!, // Front camera
                            modifier = Modifier.weight(1f),
                            isRecording = isRecording,
                            quality = selectedQuality
                        )
                        Camera2Preview(
                            cameraId = cameraIdInfo.value.back!!, // Back camera
                            modifier = Modifier.weight(1f),
                            isRecording = isRecording,
                            quality = selectedQuality
                        )
                    }
                } else if (currentCameraId != null) {
                    // Single camera: just show the preview full screen
                    Camera2Preview(
                        cameraId = currentCameraId!!,
                        modifier = Modifier.fillMaxSize(),
                        isRecording = isRecording,
                        quality = selectedQuality
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No camera available", color = Color.White)
                    }
                }
            }

            // Top row overlay
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(onClick = { /* TODO: Link action */ }) {
                        Icon(Icons.Default.Link, contentDescription = "Link")
                    }
                    IconButton(onClick = { /* TODO: Add action */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                if (dualCameraSupported && cameraIdInfo.value.front != null && cameraIdInfo.value.back != null) {
                    IconButton(onClick = {
                        if (dualCameraSupported) {
                            showDualCamera = !showDualCamera
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Dual camera preview is not supported on this device.")
                            }
                        }
                    }) {
                        if (showDualCamera) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch to single camera")
                        } else {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch to dual camera")
                        }
                    }
                }
            }

            // Bottom controls overlay
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showDualCamera && (cameraIdInfo.value.front != null && cameraIdInfo.value.back != null)) {
                    // Show front/back camera toggle in single camera mode
                    Row{
                        DurationToggleButton(selected = duration == 60, text = "60s") { duration = 60 }
                        Spacer(Modifier.width(8.dp))
                        DurationToggleButton(selected = duration == 15, text = "15s") { duration = 15 }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                currentCameraId = if (currentCameraId == cameraIdInfo.value.back) cameraIdInfo.value.front else cameraIdInfo.value.back
                            },
                            enabled = cameraIdInfo.value.front != null && cameraIdInfo.value.back != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cached,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else if (!showDualCamera) {
                    // Only one camera available, show duration toggles only
                    Row {
                        DurationToggleButton(selected = duration == 60, text = "60s") { duration = 60 }
                        Spacer(Modifier.width(8.dp))
                        DurationToggleButton(selected = duration == 15, text = "15s") { duration = 15 }
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    // Show duration toggle in dual camera mode
                    Row {
                        DurationToggleButton(selected = duration == 60, text = "60s") { duration = 60 }
                        Spacer(Modifier.width(8.dp))
                        DurationToggleButton(selected = duration == 15, text = "15s") { duration = 15 }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                // Quality selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    QualityToggleButton(
                        selected = selectedQuality == VideoQuality.LOW,
                        text = "Low",
                        icon = Icons.Default.LowPriority,
                        onClick = { selectedQuality = VideoQuality.LOW }
                    )
                    QualityToggleButton(
                        selected = selectedQuality == VideoQuality.MEDIUM,
                        text = "Med",
                        icon = Icons.Default.HighQuality,
                        onClick = { selectedQuality = VideoQuality.MEDIUM }
                    )
                    QualityToggleButton(
                        selected = selectedQuality == VideoQuality.HIGH,
                        text = "High",
                        icon = Icons.Default.HighQuality,
                        onClick = { selectedQuality = VideoQuality.HIGH }
                    )
                }
                
                // Sound toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    SoundToggleButton(
                        enabled = soundEnabled,
                        onClick = { soundEnabled = !soundEnabled }
                    )
                }
                
                // Video recording logic
                RecordButton(isRecording = isRecording, onClick = {
                    if (!isRecording && currentCameraId != null) {
                        // Start recording
                        coroutineScope.launch {
                            startRecording(context, currentCameraId!!, duration, selectedQuality, soundEnabled) { uri ->
                                lastVideoUri = uri
                                if (uri != null && !VideoRepository.videoUris.contains(uri)) {
                                    VideoRepository.videoUris.add(uri)
                                }
                            }
                        }
                        isRecording = true
                    } else if (isRecording) {
                        // Stop recording
                        stopRecording()
                        isRecording = false
                    }
                })
                
                if (lastVideoUri != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Saved: $lastVideoUri", color = Color.White)
                }
            }
        }
    }
}

// Auto-detect optimal quality based on device capabilities
private fun detectOptimalQuality(context: Context): VideoQuality {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memoryInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
    val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
    
    return when {
        totalMemoryMB < 2048 || availableMemoryMB < 512 -> {
            // Low-end device: < 2GB RAM or < 512MB available
            VideoQuality.LOW
        }
        totalMemoryMB < 4096 || availableMemoryMB < 1024 -> {
            // Mid-range device: < 4GB RAM or < 1GB available
            VideoQuality.MEDIUM
        }
        else -> {
            // High-end device: >= 4GB RAM and >= 1GB available
            VideoQuality.HIGH
        }
    }
}

// Global variables for Camera2 recording
private var cameraDevice: CameraDevice? = null
private var cameraCaptureSession: CameraCaptureSession? = null
private var mediaRecorder: MediaRecorder? = null
private var backgroundHandler: Handler? = null
private var backgroundThread: HandlerThread? = null
private var recordingSurface: Surface? = null

private fun startBackgroundThread() {
    backgroundThread = HandlerThread("CameraBackground").apply { start() }
    backgroundHandler = Handler(backgroundThread!!.looper)
}

private fun stopBackgroundThread() {
    backgroundThread?.quitSafely()
    try {
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    } catch (e: InterruptedException) {
        Log.e("Camera2", "Background thread interrupted", e)
    }
}

private suspend fun startRecording(context: Context, cameraId: String, duration: Int, quality: VideoQuality, soundEnabled: Boolean, onComplete: (String?) -> Unit) {
    startBackgroundThread()
    
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            setupMediaRecorder(context, duration, quality, soundEnabled) { uri ->
                onComplete(uri)
            }
        }
        
        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }
        
        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
            Log.e("Camera2", "Camera open error: $error")
        }
    }, backgroundHandler)
}

private fun setupMediaRecorder(context: Context, duration: Int, quality: VideoQuality, soundEnabled: Boolean, onComplete: (String?) -> Unit) {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val soundSuffix = if (soundEnabled) "AUDIO" else "MUTED"
    val videoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VID_${quality.name}_${soundSuffix}_$name.mp4")
    
    mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }.apply {
        // Set audio source only if sound is enabled
        if (soundEnabled) {
            setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(videoFile.absolutePath)
        setVideoEncodingBitRate(quality.bitrate)
        setVideoFrameRate(quality.fps)
        setVideoSize(quality.width, quality.height)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        // Set audio encoder only if sound is enabled
        if (soundEnabled) {
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }
        setOrientationHint(90)
        prepare()
    }
    
    recordingSurface = mediaRecorder?.surface
    
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
    captureRequestBuilder?.addTarget(recordingSurface!!)
    
    cameraDevice?.createCaptureSession(
        listOf(recordingSurface!!),
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                session.setRepeatingRequest(
                    captureRequestBuilder?.build()!!,
                    null,
                    backgroundHandler
                )
                
                // Start recording
                mediaRecorder?.start()
                
                // Stop after duration
                backgroundHandler?.postDelayed({
                    stopRecording()
                    onComplete(videoFile.absolutePath)
                }, duration * 1000L)
            }
            
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("Camera2", "Failed to configure camera session")
            }
        },
        backgroundHandler
    )
}

private fun stopRecording() {
    try {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
        
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        
        recordingSurface?.release()
        recordingSurface = null
        
        cameraDevice?.close()
        cameraDevice = null
        
        stopBackgroundThread()
    } catch (e: Exception) {
        Log.e("Camera2", "Error stopping recording", e)
    }
}

/**
 * Camera2Preview composable using TextureView inside Compose.
 */
@Composable
fun Camera2Preview(cameraId: String, modifier: Modifier = Modifier, isRecording: Boolean = false, quality: VideoQuality = VideoQuality.MEDIUM) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            val textureView = TextureView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    // Only open camera here, and catch errors
                    try {
                        setupCamera2Preview(ctx, cameraId, surface, quality)
                    } catch (e: Exception) {
                        Log.e("Camera2", "Error in onSurfaceTextureAvailable", e)
                    }
                }
                
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
            
            textureView
        },
        modifier = modifier
    )
}

private fun setupCamera2Preview(context: Context, cameraId: String, surfaceTexture: SurfaceTexture, quality: VideoQuality) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    // Set preview size based on quality
    surfaceTexture.setDefaultBufferSize(quality.width, quality.height)
    val surface = Surface(surfaceTexture)
    try {
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                try {
                    val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequestBuilder.addTarget(surface)
                    camera.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    session.setRepeatingRequest(
                                        captureRequestBuilder.build(),
                                        null,
                                        null
                                    )
                                } catch (e: Exception) {
                                    Log.e("Camera2", "Error starting preview request", e)
                                }
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.e("Camera2", "Failed to configure preview session")
                            }
                        },
                        null
                    )
                } catch (e: Exception) {
                    Log.e("Camera2", "Error configuring camera preview", e)
                }
            }
            override fun onDisconnected(camera: CameraDevice) {
                try { camera.close() } catch (_: Exception) {}
            }
            override fun onError(camera: CameraDevice, error: Int) {
                try { camera.close() } catch (_: Exception) {}
                Log.e("Camera2", "Camera preview error: $error")
            }
        }, null)
    } catch (e: SecurityException) {
        Log.e("Camera2", "SecurityException opening camera. Permissions missing?", e)
    } catch (e: Exception) {
        Log.e("Camera2", "Exception opening camera", e)
    }
}

@Composable
fun DurationToggleButton(selected: Boolean, text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) Color.White else Color(0xAA222222),
        shadowElevation = if (selected) 4.dp else 0.dp,
        onClick = onClick
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun QualityToggleButton(selected: Boolean, text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) Color.White else Color(0xAA222222),
        shadowElevation = if (selected) 4.dp else 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                color = if (selected) Color.Black else Color.White
            )
        }
    }
}

@Composable
fun SoundToggleButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) Color.White else Color(0xAA222222),
        shadowElevation = if (enabled) 4.dp else 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = if (enabled) "Sound On" else "Sound Off",
                tint = if (enabled) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (enabled) "Sound" else "Muted",
                color = if (enabled) Color.Black else Color.White
            )
        }
    }
}

@Composable
fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "record-ring")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ), label = "record-scale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .background(
                color = if (isRecording) Color.Red else Color(0xFFFF4444),
                shape = CircleShape
            )
            .padding(8.dp)
            .background(Color.White, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Animated pulsing ring
            Box(
                modifier = Modifier
                    .size(64.dp * ringScale)
                    .background(Color.Red.copy(alpha = 0.3f), shape = CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isRecording) Color.Red else Color(0xFFFF4444),
                    shape = CircleShape
                )
        )
    }
}

// Utility function to check for logical multi-camera support
private fun supportsDualCamera(context: Context): Boolean {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return cameraManager.cameraIdList.any { id ->
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val logicalMultiCamera = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        logicalMultiCamera?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
    }
} 