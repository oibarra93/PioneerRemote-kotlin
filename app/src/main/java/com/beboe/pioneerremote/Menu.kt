package com.beboe.pioneerremote

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
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
    companion object{

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        var btnMenuConnect = findViewById<Button>(R.id.btnMenuConnect)
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
        var address :MutableList<String> = mutableListOf()
        if (ip != null) {
            address.add(ip)
        }
        address.add(port.toString())

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
                if (MainActivity.client.isConnected)
                    Toast.makeText(this@Menu, "Connected", Toast.LENGTH_SHORT).show()
            }
        }
        /*do {
            try {
                Toast.makeText(this@Menu, "Connecting...", Toast.LENGTH_SHORT).show()
                connect().start()
            }
            catch (e: IOException) {
                Toast.makeText(this, "Could not connect", Toast.LENGTH_LONG).show()
            }
        }while(MainActivity.client.isConnected)*/

        drawer {
            accountHeader() {
                profile("Pionner Remote") {
                    icon = R.drawable.logo
                }
            }
            primaryItem("Connection") {
                onClick { _ ->
                    Log.d("Drawer", "Click.")
                    //connect().cancel()
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
            MainActivity.sendCommand("apa")
        }
        btnVideoMenu.setOnClickListener {
            MainActivity.sendCommand("vpa")
        }
        btnHomeMenu.setOnClickListener {
            MainActivity.sendCommand("hm")
        }
        btnReturn.setOnClickListener {
            MainActivity.sendCommand("crt")
        }
        btnOk.setOnClickListener {
            MainActivity.sendCommand("cen")
        }
        btnUp.setOnClickListener {
            MainActivity.sendCommand("cup")
        }
        btnRight.setOnClickListener {
            MainActivity.sendCommand("cri")
        }
        btnDown.setOnClickListener {
            MainActivity.sendCommand("cdn")
        }
        btnLeft.setOnClickListener {
            MainActivity.sendCommand("cle")
        }
        btnAuto.setOnClickListener {
            MainActivity.sendCommand("0005sr")
        }
        btnSurr.setOnClickListener {
            MainActivity.sendCommand("0010sr")
        }
        btnAdv.setOnClickListener {
            MainActivity.sendCommand("0100sr")
        }
        btnMenuConnect.setOnClickListener{
            AlertDialog.Builder(this@Menu)
                .setTitle("Connection")
                .setMessage("Would you like to connect with the previous settings?") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    "YES"
                ) { dialog, which ->
                    // Continue with delete operation
                    connect().start()
                } // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(
                    "NO")
                    { dialog, which ->
                        val context = this@Menu
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                .setNeutralButton(
                    android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }



        }
    }