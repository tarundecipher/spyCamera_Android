package com.example.spycamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color

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
import androidx.core.text.isDigitsOnly
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.lang.Runnable
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import okhttp3.OkHttpClient
import java.util.regex.Pattern
import android.content.Intent





class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        submit_btn.setOnClickListener {
            attach_listener()
        }


    }

    fun attach_listener(){
        val interval = interval_input.text.toString()
        val email = email_input.text.toString()
        var isvalid = true
        if(!interval.isDigitsOnly()){
            Toast.makeText(this,
                "Enter Valid interval",
                Toast.LENGTH_SHORT).show();
            isvalid = false
        }
        if(!checkEmail(email)){
            Toast.makeText(this,
                "Enter Valid email",
                Toast.LENGTH_SHORT).show();
            isvalid = false
        }
        if(isvalid){
            val myIntent = Intent(this, CameraActivity::class.java)
            myIntent.putExtra("email", email)
            myIntent.putExtra("interval", interval)
            this.startActivity(myIntent)
        }
    }

    private fun checkEmail(email: String): Boolean {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches()
    }

    val EMAIL_ADDRESS_PATTERN: Pattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )


}