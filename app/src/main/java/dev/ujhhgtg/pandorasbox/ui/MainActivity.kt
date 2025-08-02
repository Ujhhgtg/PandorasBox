package dev.ujhhgtg.pandorasbox.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.utils.SettingsRepository
import dev.ujhhgtg.pandorasbox.ml.Yolo11mPoseFloat32
import dev.ujhhgtg.pandorasbox.models.BottomNavItem
import dev.ujhhgtg.pandorasbox.models.PoseLandmark
import dev.ujhhgtg.pandorasbox.models.PoseResult
import dev.ujhhgtg.pandorasbox.services.OverlayService
import dev.ujhhgtg.pandorasbox.ui.composables.NumberAdjuster
import dev.ujhhgtg.pandorasbox.ui.composables.OffsetAdjuster
import dev.ujhhgtg.pandorasbox.ui.composables.PackageChooserDialog
import dev.ujhhgtg.pandorasbox.ui.theme.AppTheme
import dev.ujhhgtg.pandorasbox.utils.ImageUtils
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                AppContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppContent() {
        val settings by remember { mutableStateOf(SettingsRepository(this)) }
        val navController = rememberNavController()

        Scaffold(
            topBar = { TopAppBar(title = { Text("Aim Assistant") }) },
            bottomBar = { BottomAppNavBar(navController) }
        ) { paddingValues ->
            NavHostContainer(navController, paddingValues, settings)
        }
    }


    @Composable
    fun NavHostContainer(
        navController: NavHostController,
        padding: PaddingValues,
        settings: SettingsRepository
    ) {
        NavHost(
            navController = navController,
            startDestination = "overlay",
            modifier = Modifier.padding(paddingValues = padding),
            enterTransition = {
                fadeIn(animationSpec = tween(340))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(340))
            },
            builder = {
                composable("overlay") {
                    OverlayScreen(navController, padding, settings)
                }
                composable("aim_bot") {
                    AimBotScreen(navController, padding, settings)
                }
                composable("settings") {
                    SettingsScreen(navController, padding, settings)
                }
            }
        )
    }

    @Composable
    fun OverlayScreen(
        navController: NavController,
        padding: PaddingValues,
        settings: SettingsRepository
    ) {
        var serviceStarted by remember { OverlayService.Companion.isRunning }
        var selectedPackage by remember { mutableStateOf("default") }
        var showPackageDialog by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()

        val hOffset by settings.loadSingleConfigFlow(
            floatPreferencesKey("${selectedPackage}_horizontal_offset"),
            0f
        ).collectAsState(0f)
        val vOffset by settings.loadSingleConfigFlow(
            floatPreferencesKey("${selectedPackage}_vertical_offset"),
            0f
        ).collectAsState(0f)
        val dotSize by settings.loadSingleConfigFlow(
            intPreferencesKey("${selectedPackage}_dot_size"),
            20
        ).collectAsState(20)
        val lineWidth by settings.loadSingleConfigFlow(
            intPreferencesKey("${selectedPackage}_line_width"),
            15
        ).collectAsState(15)
        var localHOffset by remember(hOffset) { mutableFloatStateOf(hOffset) }
        var localVOffset by remember(vOffset) { mutableFloatStateOf(vOffset) }
        var localDotSize by remember(dotSize) { mutableIntStateOf(dotSize) }
        var localLineWidth by remember(lineWidth) { mutableIntStateOf(lineWidth) }

        LaunchedEffect(localHOffset) {
            if (localHOffset != hOffset) {
                settings.saveSingleConfig(
                    floatPreferencesKey("${selectedPackage}_horizontal_offset"),
                    localHOffset
                )
            }
        }
        LaunchedEffect(localVOffset) {
            if (localVOffset != vOffset) {
                settings.saveSingleConfig(
                    floatPreferencesKey("${selectedPackage}_vertical_offset"),
                    localVOffset
                )
            }
        }
        LaunchedEffect(localDotSize) {
            if (localDotSize != dotSize) {
                settings.saveSingleConfig(
                    intPreferencesKey("${selectedPackage}_dot_size"),
                    localDotSize
                )
            }
        }
        LaunchedEffect(localLineWidth) {
            if (localLineWidth != lineWidth) {
                settings.saveSingleConfig(
                    intPreferencesKey("${selectedPackage}_line_width"),
                    localLineWidth
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Button(onClick = { showPackageDialog = true }) {
                Text("Package: $selectedPackage")
            }
            Spacer(Modifier.height(24.dp))
            OffsetAdjuster(
                label = stringResource(R.string.horizontal_offset),
                value = hOffset
            ) {
                localHOffset = it
            }
            Spacer(Modifier.height(24.dp))
            OffsetAdjuster(
                label = stringResource(R.string.vertical_offset),
                value = vOffset
            ) {
                localVOffset = it
            }
            Spacer(Modifier.height(24.dp))
            NumberAdjuster(
                label = "${stringResource(R.string.dot_size)}: %d",
                value = dotSize,
                defaultValue = 20,
                minValue = 0,
                maxValue = 30,
                valueStep = 1
            ) {
                localDotSize = it
            }
            Spacer(Modifier.height(24.dp))
            NumberAdjuster(
                label = "${stringResource(R.string.line_width)}: %d",
                value = lineWidth,
                defaultValue = 15,
                minValue = 0,
                maxValue = 20,
                valueStep = 1
            ) {
                localLineWidth = it
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { toggleOverlayService() }) {
                if (!serviceStarted) {
                    Icon(
                        painter = painterResource(R.drawable.play_arrow_24px),
                        contentDescription = "show overlay",
                    )
                    Spacer(modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.show_overlay))
                } else {
                    Icon(
                        painter = painterResource(R.drawable.pause_24px),
                        contentDescription = "hide overlay",
                    )
                    Spacer(modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.hide_overlay))
                }
            }
        }

        if (showPackageDialog) {
            PackageChooserDialog(
                onDismiss = { showPackageDialog = false },
                onSelect = {
                    selectedPackage = it
                    showPackageDialog = false
                }
            )
        }
    }

    @Composable
    fun AimBotScreen(
        navController: NavController,
        padding: PaddingValues,
        settings: SettingsRepository
    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.Companion.CenterHorizontally
//        ) {
//
//        }
        CameraPoseScreen()
    }

    @Composable
    fun CameraPoseScreen() {
        val lifecycleOwner = LocalLifecycleOwner.current
        var poseResultState by remember { mutableStateOf<PoseResult?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_START
                    post {
                        if (!PermissionManager.checkCamera(this@MainActivity)) {
                            Toast.makeText(ctx, ctx.getString(R.string.camera_perm_required), Toast.LENGTH_SHORT).show()
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.CAMERA),
                                0
                            )
                            return@post
                        }

                        startCamera(context, lifecycleOwner, this) { pose, w, h ->
                            poseResultState = PoseResult(pose, w, h)
                        }
                    }
                }
            }, modifier = Modifier.fillMaxSize())

            Canvas(modifier = Modifier.fillMaxSize()) {
                poseResultState?.let { result ->
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val scale = canvasWidth / result.imageWidth.toFloat()

                    Log.d("PoseDetector", "dimensions: cW: $canvasWidth cH: $canvasHeight iW: ${result.imageWidth} iH: ${result.imageHeight} sc: $scale")

//                    result.pose.allPoseLandmarks.forEach { landmark ->
//                        drawCircle(
//                            color = Color.Green,
//                            radius = 6f,
//                            center = Offset(
//                                x = canvasWidth - landmark.position.x * scale,
//                                y = landmark.position.y * scale
//                            )
//                        )
//                    }
                    result.pose.forEach { landmark ->
                        drawCircle(
                            color = Color.Green,
                            radius = 6f,
                            center = Offset(
                                x = canvasWidth - landmark.x * scale,
                                y = landmark.y * scale
                            )
                        )
                    }
                }
            }
        }
    }

    private fun startCamera(
        ctx: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onPoseDetected: (List<PoseLandmark>, imgWidth: Int, imgHeight: Int) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val model = Yolo11mPoseFloat32.newInstance(ctx)
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(/*ContextCompat.getMainExecutor(ctx)*/Executors.newSingleThreadExecutor()) { imageProxy ->
                    processImageProxy(imageProxy, /*poseDetector, */model, onPoseDetected)
                } }

//            val poseDetector = PoseDetection.getClient(
//                AccuratePoseDetectorOptions.Builder()
//                    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
//                    .build()
//            )

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
        }, ContextCompat.getMainExecutor(ctx))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        imageProxy: ImageProxy,
//        poseDetector: PoseDetector,
        model: Yolo11mPoseFloat32,
        onPoseDetected: (List<PoseLandmark>, imgWidth: Int, imgHeight: Int) -> Unit
    ) {
//        var bitmap = createBitmap(imageProxy.width, imageProxy.height)
//        bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
//        imageProxy.close()
        var bitmap = ImageUtils.convertImageToBitmap2(imageProxy.image ?: return imageProxy.close())
//        bitmap.scale(640, 640)
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        bitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            matrix, true
        )

//        val mediaImage = imageProxy.image ?: return imageProxy.close()
//        val bitmap = ImageUtils.convertImageToBitmap(mediaImage)

        val input = TensorBuffer.createFixedSize(intArrayOf(1, 640, 640, 3), DataType.FLOAT32)
        input.loadBuffer(ImageUtils.convertBitmapToInputTensor(bitmap))

        val outputs = model.process(input)
        val output = outputs.outputFeature0AsTensorBuffer

        Log.d("PoseDetector", "output: ${output.floatArray.size}")
        val path = this.filesDir.resolve("output.txt")
        val writer = BufferedWriter(FileWriter(path))
        writer.write("dataType: ${output.dataType}")
        writer.newLine()
        for (value in output.floatArray) {
            writer.write(value.toString())
            writer.newLine()
        }
        writer.newLine()
        for (value in output.intArray) {
            writer.write(value.toString())
            writer.newLine()
        }

        writer.close()

        val keypoints = parsePoseOutput(output.floatArray, imageProxy.width, imageProxy.height)

        Log.d("PoseDetector", "pose: $keypoints")

        onPoseDetected(keypoints, imageProxy.width, imageProxy.height)
//        model.close()

//        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

//        poseDetector.process(inputImage)
//            .addOnSuccessListener { pose ->
//                onPoseDetected(pose, imageProxy.width, imageProxy.height)
//            }
//            .addOnFailureListener {
//                Log.e("PoseDetector", "detection error", it)
//            }
//            .addOnCompleteListener {
//                imageProxy.close()
//            }

//        imageProxy.close()
    }

    fun parsePoseOutput(
        outputArray: FloatArray,
        imageWidth: Int,
        imageHeight: Int,
        confidenceThreshold: Float = 0.5f
    ): List<PoseLandmark> {
//        val keypoints = mutableListOf<PoseLandmark>()
//        val numKeypoints = outputArray.size / 3
//
//        for (i in 0 until numKeypoints) {
//            val x = outputArray[i * 3]
//            val y = outputArray[i * 3 + 1]
//            val confidence = outputArray[i * 3 + 2]
//            Log.d("PoseDetector", "adding i: $i x: $x y: $y confidence: $confidence")
//            keypoints.add(PoseLandmark(i, x, y, confidence))
//        }
//
//        return keypoints
//        val outputArray = output.floatArray

//        val numDetections = outputArray.size / 56 // 56 = 4 + 17*3
//
//        for (i in 0 until numDetections) {
//            val offset = i * 56
//            val conf = outputArray[offset + 4] // usually object confidence
//
//            if (conf > 0.5f) {
//                val keypoints = mutableListOf<PoseLandmark>() // x, y, score
//
//                for (j in 0 until 17) {
//                    val x = outputArray[offset + 5 + j * 3]
//                    val y = outputArray[offset + 5 + j * 3 + 1]
//                    val score = outputArray[offset + 5 + j * 3 + 2]
//                    keypoints.add(PoseLandmark(x, y, score))
//                }
//            }
//        }

        val results = mutableListOf<PoseLandmark>()

        val entrySize = 56 // 4 bbox + 17 keypoints * 3 (x, y, score)
        val numDetections = outputArray.size / entrySize

        for (i in 0 until numDetections) {
            val offset = i * entrySize
            val objectConfidence = outputArray[offset + 4]

            if (objectConfidence >= confidenceThreshold) {
                for (j in 0 until 17) {
                    val x = outputArray[offset + 5 + j * 3]
                    val y = outputArray[offset + 5 + j * 3 + 1]
                    val score = outputArray[offset + 5 + j * 3 + 2]

                    // Convert normalized coordinates to image pixels
                    results.add(
                        PoseLandmark(
                            x = x * imageWidth,
                            y = y * imageHeight,
                            score = score
                        )
                    )
                }

                // Optionally: break after first valid detection
                break
            }
        }

        return results
    }

    @Composable
    fun SettingsScreen(
        navController: NavController,
        padding: PaddingValues,
        settings: SettingsRepository
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {

        }
    }

    @Composable
    fun BottomAppNavBar(navController: NavHostController) {
        val currentRoute = getCurrentRouteAsState(navController)

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            bottomNavItems.forEach { navItem ->
                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors().copy(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    selected = currentRoute == navItem.route,
                    onClick = {
                        navController.navigate(navItem.route)
                    },
                    icon = {
//                        AnimatedContent(
//                            targetState = currentRoute,
//                            transitionSpec = { fadeIn() togetherWith fadeOut() }
//                        ) {
//                            if (it == navItem.route) {
                            if (currentRoute == navItem.route) {
                                navItem.selectedIcon()
                            } else {
                                navItem.unselectedIcon()
                            }
//                        }
                    },
                    label = {
                        Text(text = stringResource(navItem.label))
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem(
            label = R.string.overlay,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.picture_in_picture_24px),
                    contentDescription = "overlay"
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.picture_in_picture_filled_24px),
                    contentDescription = "overlay"
                )
            },
            route = "overlay"
        ),
        BottomNavItem(
            label = R.string.aim_bot,
            unselectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.center_focus_strong_24px),
                    contentDescription = "aim bot"
                )
            },
            selectedIcon = {
                Icon(
                    painter = painterResource(R.drawable.center_focus_strong_filled_24px),
                    contentDescription = "aim bot"
                )
            },
            route = "aim_bot"
        ),
        BottomNavItem(
            label = R.string.settings,
            unselectedIcon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "settings"
                )
            },
            selectedIcon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "settings"
                )
            },
            route = "settings"
        )
    )

    @Composable
    private fun getCurrentRouteAsState(navController: NavHostController): String? {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        return currentRoute
    }

    private fun toggleOverlayService() {
        if (!PermissionManager.checkAndRequestNotifications(this))
            return

        if (!PermissionManager.checkAndRequestOverlay(this))
            return

        if (!PermissionManager.checkAndRequestUsageStats(this))
            return

        if (!OverlayService.Companion.isRunning.value) {
            startService(Intent(this, OverlayService::class.java))
        }
        else {
            stopService(Intent(this, OverlayService::class.java))
        }
    }
}