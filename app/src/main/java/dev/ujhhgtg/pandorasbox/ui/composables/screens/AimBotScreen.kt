package dev.ujhhgtg.pandorasbox.ui.composables.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AimBotScreen() {
//    val activity = LocalActivityContext.current
//    CameraPoseScreen(activity)
}

//@Composable
//fun CameraPoseScreen(activity: Activity) {
//    val lifecycleOwner = LocalLifecycleOwner.current
//    var poseResultState by remember { mutableStateOf<PoseResult?>(null) }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        AndroidView(factory = { ctx ->
//            PreviewView(ctx).apply {
//                scaleType = PreviewView.ScaleType.FILL_START
//                post {
//                    if (!PermissionManager.checkCamera(activity)) {
//                        Toast.makeText(
//                            ctx,
//                            ctx.getString(R.string.camera_perm_required),
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        ActivityCompat.requestPermissions(
//                            activity,
//                            arrayOf(Manifest.permission.CAMERA),
//                            0
//                        )
//                        return@post
//                    }
//
//                    startCamera(activity, lifecycleOwner, this) { pose, w, h ->
//                        poseResultState = PoseResult(pose, w, h)
//                    }
//                }
//            }
//        }, modifier = Modifier.fillMaxSize())
//
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            poseResultState?.let { result ->
//                val canvasWidth = size.width
//                val canvasHeight = size.height
//
//                val scale = canvasWidth / result.imageWidth.toFloat()
//
//                Log.d(
//                    "PB.PoseDetector",
//                    "dimensions: cW: $canvasWidth cH: $canvasHeight iW: ${result.imageWidth} iH: ${result.imageHeight} sc: $scale"
//                )
//
////                    result.pose.allPoseLandmarks.forEach { landmark ->
////                        drawCircle(
////                            color = Color.Green,
////                            radius = 6f,
////                            center = Offset(
////                                x = canvasWidth - landmark.position.x * scale,
////                                y = landmark.position.y * scale
////                            )
////                        )
////                    }
//                result.pose.forEach { landmark ->
//                    drawCircle(
//                        color = Color.Green,
//                        radius = 6f,
//                        center = Offset(
//                            x = canvasWidth - landmark.x * scale,
//                            y = landmark.y * scale
//                        )
//                    )
//                }
//            }
//        }
//    }
//}
//
//private fun startCamera(
//    ctx: Context,
//    lifecycleOwner: LifecycleOwner,
//    previewView: PreviewView,
//    onPoseDetected: (List<PoseLandmark>, imgWidth: Int, imgHeight: Int) -> Unit
//) {
//    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
//    cameraProviderFuture.addListener({
//        val cameraProvider = cameraProviderFuture.get()
//
//        val preview = Preview.Builder().build().also {
//            it.surfaceProvider = previewView.surfaceProvider
//        }
//
//        val model = Yolo11mPoseFloat32.newInstance(ctx)
//        val analyzer = ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
////                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//            .build()
//            .also { it.setAnalyzer(/*ContextCompat.getMainExecutor(ctx)*/Executors.newSingleThreadExecutor()) { imageProxy ->
//                processImageProxy(ctx.filesDir, imageProxy, /*poseDetector, */model, onPoseDetected)
//            } }
//
////            val poseDetector = PoseDetection.getClient(
////                AccuratePoseDetectorOptions.Builder()
////                    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
////                    .build()
////            )
//
//        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//        cameraProvider.unbindAll()
//        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
//    }, ContextCompat.getMainExecutor(ctx))
//}
//
//@androidx.annotation.OptIn(ExperimentalGetImage::class)
//private fun processImageProxy(
//    filesDir: File,
//    imageProxy: ImageProxy,
////        poseDetector: PoseDetector,
//    model: Yolo11mPoseFloat32,
//    onPoseDetected: (List<PoseLandmark>, imgWidth: Int, imgHeight: Int) -> Unit
//) {
////        var bitmap = createBitmap(imageProxy.width, imageProxy.height)
////        bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
////        imageProxy.close()
//    var bitmap = ImageUtils.convertImageToBitmap2(imageProxy.image ?: return imageProxy.close())
////        bitmap.scale(640, 640)
//    val matrix = Matrix().apply {
//        postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//    }
//    bitmap = Bitmap.createBitmap(
//        bitmap, 0, 0, bitmap.width, bitmap.height,
//        matrix, true
//    )
//
////        val mediaImage = imageProxy.image ?: return imageProxy.close()
////        val bitmap = ImageUtils.convertImageToBitmap(mediaImage)
//
//    val input = TensorBuffer.createFixedSize(intArrayOf(1, 640, 640, 3), DataType.FLOAT32)
//    input.loadBuffer(ImageUtils.convertBitmapToInputTensor(bitmap))
//
//    val outputs = model.process(input)
//    val output = outputs.outputFeature0AsTensorBuffer
//
//    Log.d("PB.PoseDetector", "output: ${output.floatArray.size}")
//    val path = filesDir.resolve("output.txt")
//    val writer = BufferedWriter(FileWriter(path))
//    writer.write("dataType: ${output.dataType}")
//    writer.newLine()
//    for (value in output.floatArray) {
//        writer.write(value.toString())
//        writer.newLine()
//    }
//    writer.newLine()
//    for (value in output.intArray) {
//        writer.write(value.toString())
//        writer.newLine()
//    }
//
//    writer.close()
//
//    val keypoints = parsePoseOutput(output.floatArray, imageProxy.width, imageProxy.height)
//
//    Log.d("PB.PoseDetector", "pose: $keypoints")
//
//    onPoseDetected(keypoints, imageProxy.width, imageProxy.height)
////        model.close()
//
////        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
////        poseDetector.process(inputImage)
////            .addOnSuccessListener { pose ->
////                onPoseDetected(pose, imageProxy.width, imageProxy.height)
////            }
////            .addOnFailureListener {
////                Log.e("PoseDetector", "detection error", it)
////            }
////            .addOnCompleteListener {
////                imageProxy.close()
////            }
//
////        imageProxy.close()
//}
//
//fun parsePoseOutput(
//    outputArray: FloatArray,
//    imageWidth: Int,
//    imageHeight: Int,
//    confidenceThreshold: Float = 0.5f
//): List<PoseLandmark> {
////        val keypoints = mutableListOf<PoseLandmark>()
////        val numKeypoints = outputArray.size / 3
////
////        for (i in 0 until numKeypoints) {
////            val x = outputArray[i * 3]
////            val y = outputArray[i * 3 + 1]
////            val confidence = outputArray[i * 3 + 2]
////            Log.d("PB.PoseDetector", "adding i: $i x: $x y: $y confidence: $confidence")
////            keypoints.add(PoseLandmark(i, x, y, confidence))
////        }
////
////        return keypoints
////        val outputArray = output.floatArray
//
////        val numDetections = outputArray.size / 56 // 56 = 4 + 17*3
////
////        for (i in 0 until numDetections) {
////            val offset = i * 56
////            val conf = outputArray[offset + 4] // usually object confidence
////
////            if (conf > 0.5f) {
////                val keypoints = mutableListOf<PoseLandmark>() // x, y, score
////
////                for (j in 0 until 17) {
////                    val x = outputArray[offset + 5 + j * 3]
////                    val y = outputArray[offset + 5 + j * 3 + 1]
////                    val score = outputArray[offset + 5 + j * 3 + 2]
////                    keypoints.add(PoseLandmark(x, y, score))
////                }
////            }
////        }
//
//    val results = mutableListOf<PoseLandmark>()
//
//    val entrySize = 56 // 4 b_box + 17 keypoints * 3 (x, y, score)
//    val numDetections = outputArray.size / entrySize
//
//    for (i in 0 until numDetections) {
//        val offset = i * entrySize
//        val objectConfidence = outputArray[offset + 4]
//
//        if (objectConfidence >= confidenceThreshold) {
//            for (j in 0 until 17) {
//                val x = outputArray[offset + 5 + j * 3]
//                val y = outputArray[offset + 5 + j * 3 + 1]
//                val score = outputArray[offset + 5 + j * 3 + 2]
//
//                // Convert normalized coordinates to image pixels
//                results.add(
//                    PoseLandmark(
//                        x = x * imageWidth,
//                        y = y * imageHeight,
//                        score = score
//                    )
//                )
//            }
//
//            // Optionally: break after first valid detection
//            break
//        }
//    }
//
//    return results
//}