package com.beboe.pioneerremote

import android.app.AlertDialog
import android.os.Vibrator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class Menu : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawer: DrawerLayout
    private lateinit var myVib: Vibrator
    companion object{

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        var toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawer = findViewById(R.id.drawer_layout)
        var nagivationView = findViewById<NavigationView>(R.id.nav_view)
        nagivationView.setNavigationItemSelectedListener(this)
        nagivationView.bringToFront();
        var toggle = ActionBarDrawerToggle(
            this@Menu,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        nagivationView.setCheckedItem(R.id.nav_menu)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        myVib = this.getSystemService(VIBRATOR_SERVICE) as Vibrator
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
                if (ip.isNullOrEmpty()) {

                    Log.e("Menu", "ip is Null or Empty")
                    //ip = "192.168.0.21"
                }
                else
                    MainActivity.client.connect(InetSocketAddress(ip, port), 200)
                if (MainActivity.client.isConnected)
                    MainActivity.client.keepAlive = true
            } catch (e: IOException) {
                cancel("Could not connect")
            }
            launch(Dispatchers.Main) {
                if (MainActivity.client.isConnected)
                    Toast.makeText(this@Menu, "Connected", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@Menu, "IP is Null or Empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnAudioMenu.setOnClickListener {
            myVib.vibrate(50)
            MainActivity.sendCommand("apa")
        }
        btnVideoMenu.setOnClickListener {
            myVib.vibrate(50)
            MainActivity.sendCommand("vpa")
        }
        btnHomeMenu.setOnClickListener {
            myVib.vibrate(50)
            MainActivity.sendCommand("hm")
        }
        btnReturn.setOnClickListener {
            myVib.vibrate(50)
            MainActivity.sendCommand("crt")
        }
        btnOk.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("cen")
        }
        btnUp.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("cup")
        }
        btnRight.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("cri")
        }
        btnDown.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("cdn")
        }
        btnLeft.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("cle")
        }
        btnAuto.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("0005sr")
        }
        btnSurr.setOnClickListener{myVib.vibrate(50)
            MainActivity.sendCommand("0010sr")
        }
        btnAdv.setOnClickListener{myVib.vibrate(50)
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
                    myVib.vibrate(50)
                    // Continue with local connect
                    connect().start()
                } // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(
                    "NO")
                    { dialog, which ->
                        myVib.vibrate(50)
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

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            run {
                Log.e("Back Button", "Click.")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                false
            }
            super.onBackPressed()

        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        myVib = this.getSystemService(VIBRATOR_SERVICE) as Vibrator
        when (item.itemId) {
            R.id.nav_main -> {
                myVib.vibrate(50)
                Log.e("Main", "Click.")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() }

            R.id.nav_menu -> {
                myVib.vibrate(50)
                Log.e("Menu", "Click.")
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true

    }
}