package com.beboe.pioneerremote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.badgeable.secondaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        var btnAudioMenu = findViewById<Button>(R.id.btnAudioMenu)
        var btnVideoMenu = findViewById<Button>(R.id.btnVideoMenu)
        var btnHomeMenu = findViewById<Button>(R.id.btnHomeMenu)
        var btnReturn = findViewById<Button>(R.id.btnReturn)
        var btnOk = findViewById<ImageButton>(R.id.btnOk)
        var btnLeft = findViewById<ImageButton>(R.id.btnLeft)
        var btnRight = findViewById<ImageButton>(R.id.btnRight)
        var btnUp = findViewById<ImageButton>(R.id.btnUp)
        var btnDown = findViewById<ImageButton>(R.id.btnDown)
        var btnAdv = findViewById<Button>(R.id.btnAdv)
        var btnAuto = findViewById<Button>(R.id.btnAuto)
        var btnSurr = findViewById<Button>(R.id.btnSurr)
        var ip = intent.getStringExtra("EXTRA_IP")
        val port = intent.getIntExtra("EXTRA_PORT", 8102)

        fun connect() = GlobalScope.launch(Dispatchers.IO) {
            MainActivity.client = Socket()
            try {
                if (ip.isNullOrEmpty())
                    ip = "192.168.0.21"
                MainActivity.client.connect(InetSocketAddress(ip, port), 200)
                if (MainActivity.client.isConnected)
                    MainActivity.client.keepAlive = true
            } catch (e: IOException) {
                cancel("Could not connect")
            }
            launch(Dispatchers.Main) {
                Toast.makeText(this@Menu,"Connected",Toast.LENGTH_LONG).show()
            }
        }

        drawer {
            accountHeader() {
                profile("Pionner Remote") {
                    icon = R.drawable.logo
                }
            }
            primaryItem("Connection") {
                onClick { _ ->
                    Log.d("Drawer", "Click.")
                    connect().cancel()
                    val context = this@Menu
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    false
                }
            }
            divider {}
            primaryItem("Home Menu Controls") {
                onClick { _ ->
                    Log.d("Drawer", "Click.")
                    val context = this@Menu
                    val intent = Intent(context, Menu::class.java)
                    context.startActivity(intent)
                    false
                }
            }
        }

        btnAudioMenu.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                    if (MainActivity.client.isConnected) {
                        MainActivity.client.outputStream.write("apa\r".toByteArray())
                    }

            }
        }
        btnVideoMenu.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                    if (MainActivity.client.isConnected) {
                        MainActivity.client.outputStream.write("vpa\r".toByteArray())
                    }
            }
        }
        btnHomeMenu.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                    if (MainActivity.client.isConnected) {
                        MainActivity.client.outputStream.write("hm\r".toByteArray())
                    }
            }
        }
        btnReturn.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                    if (MainActivity.client.isConnected) {
                        MainActivity.client.outputStream.write("crt\r".toByteArray())
                    }
        }}
        btnOk.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                    if(MainActivity.client.isConnected){
                        MainActivity.client.outputStream.write("cen\r".toByteArray())
                    }
                }

        }
        btnUp.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                    if(MainActivity.client.isConnected){
                        MainActivity.client.outputStream.write("cup\r".toByteArray())
                    }

            }
        }
        btnRight.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                    if(MainActivity.client.isConnected){
                        MainActivity.client.outputStream.write("cri\r".toByteArray())
                    }
                }
            }
        btnDown.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                    if(MainActivity.client.isConnected){
                        MainActivity.client.outputStream.write("cdn\r".toByteArray())
                    }
                }

            }
        btnLeft.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                if(MainActivity.client.isConnected){
                    MainActivity.client.outputStream.write("cle\r".toByteArray())
                }
                }
            }
        btnAuto.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                if(MainActivity.client.isConnected){
                    MainActivity.client.outputStream.write("005sr\r".toByteArray())
                }
            }
        }
        btnSurr.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                if(MainActivity.client.isConnected){
                    MainActivity.client.outputStream.write("0010sr\r".toByteArray())
                }
            }
        }
        btnAdv.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                if(MainActivity.client.isConnected){
                    MainActivity.client.outputStream.write("0100sr\r".toByteArray())
                }
            }
        }

        try{
            connect().start()
        }
        catch(e:IOException){
            Toast.makeText(this,"Could not connect",Toast.LENGTH_LONG).show()
        }
        }
    }