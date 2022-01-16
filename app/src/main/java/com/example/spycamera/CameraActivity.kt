package com.example.spycamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.lang.Runnable
import java.net.URISyntaxException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    public var recording:Boolean = false
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var scope = CoroutineScope(Dispatchers.IO)
    private var socket_scope = CoroutineScope(Dispatchers.IO)
    public var take_interval = true;
    public var interval = 5;
    public var email:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        email =   intent.getStringExtra("email").toString()
        interval = intent.getStringExtra("interval")!!.toInt()

        Log.d("fuck",email!!)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, CameraActivity.REQUIRED_PERMISSIONS, CameraActivity.REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener {
            take_interval = !take_interval
            toggleRecording()
        }
        connectSocket()

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
        Log.d("fuck",email!!)

    }

    private fun connectSocket(){
        val listener = EchoWebSocketListener(this)
            listener.connect()

    }

    public fun toggleRecording(){
        if(recording==false) {
            Intent(this,spyService::class.java).also{
                startService(it)
            }
            recording = true
            camera_capture_button.setBackgroundColor(ContextCompat.getColor(this,R.color.red))
            takePhoto()
        }
        else{
            Intent(this,spyService::class.java).also{
                stopService(it)
            }
            recording = false
            camera_capture_button.setBackgroundColor(ContextCompat.getColor(this,R.color.purple_500))
            scope.cancel()
            scope = CoroutineScope(Dispatchers.IO)
        }
    }

    private fun takePhoto() { // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            "snap.jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    scope.launch{
                        uploadtoServer()
                    }

                }
            })
    }

    private suspend fun uploadtoServer() {
        var file: File? = null

        try {
            file = File(outputDirectory,"snap.jpg")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        val mOkHttpClent = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "imageUpload", file?.name,
                RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file!!)
            )   // Upload files
            .build()

        val request = Request.Builder()
            .url("http://192.168.1.7:4000/upload")
            .post(requestBody)
            .build()
        val call = mOkHttpClent.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("fuck","upload failed")
                Log.d("fuck",e.message.toString())
                if(take_interval) {
                    scope.launch {
                        repeat()
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("fuck","upload successfull")
                if(take_interval) {
                    scope.launch {
                        repeat()
                    }
                }
            }

        })

    }

    private suspend fun repeat(){
        delay((interval*1000).toLong())
        takePhoto()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}