package com.example.spycamera

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class spyService : Service(), LifecycleOwner {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override fun onBind(intent: Intent): IBinder? = null

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("fuck","yep started service")
        return START_NOT_STICKY
    }



    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}