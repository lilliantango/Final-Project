package com.example.lilliantango.finalproject

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.io.IOException
import java.io.InputStream
import java.util.*
import com.android.volley.toolbox.*
import com.android.volley.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    val handler = Handler()

    var mmSocket: BluetoothSocket? = null
    var mmDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!mBluetoothAdapter.isEnabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
        }

        val pairedDevices = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                if (device.name == "little_pi") { //Note, you will need to change this to match the name of your device{
                    mmDevice = device
                    break
                }
            }
        }

        val button = findViewById<View>(R.id.button) as Button
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Perform action on temp button click
                Thread(WorkerThread("Work")).start()

            }
        })

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "http://api.openweathermap.org/data/2.5/weather?q=Austin&appid=4dbdb406e8b2361ca486918bfbbeaa1c"

// Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    // Display the first 500 characters of the response string.
                    val humidval = findViewById<View>(R.id.humidval) as TextView
                    val pressureval = findViewById<View>(R.id.pressureval) as TextView
                    val windval = findViewById<View>(R.id.windval) as TextView

//                    Gson g = new Gson()
//                    Weather p = g.fromJson(jsonString,Weather.class)

                    val jsonObj = JSONObject("${response}")
                    println(jsonObj.getJSONObject("main").getInt("humidity"))
                    println(jsonObj.getJSONObject("wind").getInt("speed"))
                    println(jsonObj.getJSONObject("main").getInt("pressure"))

                    humidval.text = jsonObj.getJSONObject("main").getInt("humidity").toString()
//                    windval.text = jsonObj.getJSONObject("wind").getInt("speed").toString()
 //                   pressureval.text = jsonObj.getJSONObject("main").getInt("pressure").toString()

                    println("${response}")
                },
                Response.ErrorListener { }
            )


// Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun sendBtMsg(msg: String) {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") //Standard SerialPortService ID
        try {
            mmSocket = mmDevice?.createRfcommSocketToServiceRecord(uuid)
            if (!mmSocket!!.isConnected()) {
                mmSocket!!.connect()
            }
            val mmOutputStream = mmSocket!!.getOutputStream()
            mmOutputStream.write(msg.toByteArray())

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    internal inner class WorkerThread(private val btMsg: String) : Runnable {
        override fun run() {
            val delimiter: Byte = 33
            var readBufferPosition = 0

            sendBtMsg(btMsg)

            while (!Thread.currentThread().isInterrupted) {
                val bytesAvailable: Int
                var workDone = false
                try {
                    val mmInputStream: InputStream
                    mmInputStream = mmSocket!!.inputStream
                    bytesAvailable = mmInputStream.available()

                    if (bytesAvailable > 0) {
                        val readBuffer = ByteArray(1024)
                        val packetBytes = ByteArray(bytesAvailable)
                        mmInputStream.read(packetBytes)

                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.size)
                                val data = String(encodedBytes,charset("US-ASCII"))

                                //The variable data now contains our full response (up to "!")
                                handler.post {
                                    val tempF = findViewById<View>(R.id.tempF) as TextView
                                    val tempC = findViewById<View>(R.id.tempC) as TextView
                                    tempF.text = data;
                                    tempC.text = data;
                                }

                                readBufferPosition = 0
                                workDone = true
                                break
                            } else {
                                readBuffer[readBufferPosition] = b
                                readBufferPosition++
                            }
                        }
                        if (workDone) {
                            mmSocket!!.close()
                            break
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }
}