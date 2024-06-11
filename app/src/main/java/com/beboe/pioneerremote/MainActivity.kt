@file:Suppress("SpellCheckingInspection")

package com.beboe.pioneerremote

import android.annotation.SuppressLint
import android.os.PowerManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawer: DrawerLayout
    private lateinit var myVib: Vibrator
    private lateinit var seek: SeekBar
    companion object {

        var address: MutableList<String> = mutableListOf()
        var client = Socket()

        @OptIn(DelicateCoroutinesApi::class)
        fun sendCommand(command: String) = GlobalScope.launch(Dispatchers.IO) {
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                } catch (e: IOException) {
                    Log.e("sendCommand", "Failed")
                }
            }
        }

    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @SuppressLint("SetTextI18n", "CutPasteId")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawer = findViewById(R.id.drawer_layout)
        val nagivationView = findViewById<NavigationView>(R.id.nav_view)
        nagivationView.setNavigationItemSelectedListener(this)
        nagivationView.bringToFront()
        val toggle = ActionBarDrawerToggle(
            this@MainActivity,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        if (savedInstanceState == null) {
            nagivationView.setCheckedItem(R.id.nav_main)
        }
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        var power = savedInstanceState?.getBoolean("SAVED_POWER")
        var mute = savedInstanceState?.getBoolean("SAVED_MUTE")
        var volume = savedInstanceState?.getString("SAVED_VOLUME")
        myVib = this.getSystemService(VIBRATOR_SERVICE) as Vibrator
        //Initialize viewContent variables
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val txtOutput = findViewById<TextView>(R.id.txtboxResponse)
        val txtInput = findViewById<EditText>(R.id.editTextHostname)
        val btnSendCommand = findViewById<Button>(R.id.btnSendCommand)
        val editTextCommand = findViewById<EditText>(R.id.editTextCommand)
        val btnBT = findViewById<Button>(R.id.btnBT)
        val btniPod = findViewById<Chip>(R.id.btniPod)
        val btnNet = findViewById<Chip>(R.id.btnNet)
        val btnSpotify = findViewById<Chip>(R.id.btnSpotify)
        val btnTuner = findViewById<Chip>(R.id.btnTuner)
        val btnMHL = findViewById<Chip>(R.id.btnMHL)
        val btnBD = findViewById<Chip>(R.id.btnBD)
        val btnSat = findViewById<Chip>(R.id.btnSat)
        val btnHDMI = findViewById<Chip>(R.id.btnHDMI)
        val btnTV = findViewById<Chip>(R.id.btnTV)
        val btnCD = findViewById<Chip>(R.id.btnCD)
        val btnAll = findViewById<Chip>(R.id.btnCyclic)
        val btnDVD = findViewById<Chip>(R.id.btnDVD)
        val btnTogglePower = findViewById<ToggleButton>(R.id.toggleButtonPower)
        val btnMute = findViewById<ToggleButton>(R.id.btnMute)
        seek = findViewById(R.id.seekBarVolume)
        val inputGroup = findViewById<ChipGroup>(R.id.chipGroup)
        txtOutput.movementMethod = ScrollingMovementMethod()

        //Initialize the network connection by scanning the local network
        var targetIP = ""
        val mainLooper = Looper.getMainLooper()
        var ip: MutableList<String> = mutableListOf()
        var prefix: String

        fun isAppIgnoringBatteryOptimizations(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }

        // Check if the app is ignoring battery optimization

        fun showBatteryOptimizationDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Disable Battery Optimization")
            builder.setMessage("To keep the connection stable, please disable battery optimization for this app." +
                    "\nBattery Optimization -> All Apps -> Pioneer Remote -> Don't Optimize")
            builder.setPositiveButton("OK") { dialog, _ ->
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }
        if (!isAppIgnoringBatteryOptimizations(this)) {
        showBatteryOptimizationDialog()}

        @SuppressLint("SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.O)
        fun getStatus(power: Boolean, volume: String, mute: Boolean) =
            GlobalScope.launch(Dispatchers.IO) {
                var input = ""
                if (client.isConnected) {
                    do {
                        try {
                            client.outputStream.write(("?f\r").toByteArray())
                            input = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        } catch (e: IOException) {
                            //Toast.makeText(this@MainActivity,"Could not fetch input",LENGTH_SHORT).show()
                            Log.e("Connection", "Could not fetch input")
                        }
                    } while (!(input.contains("FN")))
                }
                launch(Dispatchers.Main) {
                    if (client.isConnected) {
                        val buttonText = when (input) {
                            "FN04" -> {
                                inputGroup.check(R.id.btnDVD)
                                findViewById<Button>(R.id.btnDVD).text.toString()
                            }
                            "FN33" -> {
                                inputGroup.check(R.id.btnBT)
                                findViewById<Button>(R.id.btnBT).text.toString()
                            }
                            "FN06" -> {
                                inputGroup.check(R.id.btnSat)
                                findViewById<Button>(R.id.btnSat).text.toString()
                            }
                            "FN53" -> {
                                inputGroup.check(R.id.btnSpotify)
                                findViewById<Button>(R.id.btnSpotify).text.toString()
                            }
                            "FN02" -> {
                                inputGroup.check(R.id.btnTuner)
                                findViewById<Button>(R.id.btnTuner).text.toString()
                            }
                            "FN05" -> {
                                inputGroup.check(R.id.btnTV)
                                findViewById<Button>(R.id.btnTV).text.toString()
                            }
                            "FN25" -> {
                                inputGroup.check(R.id.btnBD)
                                findViewById<Button>(R.id.btnBD).text.toString()
                            }
                            "FN17" -> {
                                inputGroup.check(R.id.btniPod)
                                findViewById<Button>(R.id.btniPod).text.toString()
                            }
                            "FN34" -> {
                                inputGroup.check(R.id.btnMHL)
                                findViewById<Button>(R.id.btnMHL).text.toString()
                            }
                            "FN44", "FN45", "FN38", "FN41" -> {
                                inputGroup.check(R.id.btnNet)
                                findViewById<Button>(R.id.btnNet).text.toString()
                            }
                            "FN01" -> {
                                inputGroup.check(R.id.btnCD)
                                findViewById<Button>(R.id.btnCD).text.toString()
                            }
                            "FN19" -> {
                                inputGroup.check(R.id.btnHDMI)
                                val text = "HDMI 1"
                                findViewById<Button>(R.id.btnHDMI).text = text
                                text
                            }
                            "FN20" -> {
                                inputGroup.check(R.id.btnHDMI)
                                val text = "HDMI 2"
                                findViewById<Button>(R.id.btnHDMI).text = text
                                text
                            }
                            "FN21" -> {
                                inputGroup.check(R.id.btnHDMI)
                                val text = "HDMI 3"
                                findViewById<Button>(R.id.btnHDMI).text = text
                                text
                            }
                            "FN22" -> {
                                inputGroup.check(R.id.btnHDMI)
                                val text = "HDMI 4"
                                findViewById<Button>(R.id.btnHDMI).text = text
                                text
                            }
                            "FN23" -> {
                                inputGroup.check(R.id.btnHDMI)
                                val text = "HDMI 5/MHL"
                                findViewById<Button>(R.id.btnHDMI).text = text
                                text
                            }
                            else -> {
                                "Unknown"
                            }
                        }
                        txtOutput.text = txtOutput.text.toString() + "\nInput fetched $buttonText"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            myVib.vibrate(
                                VibrationEffect.createOneShot(
                                    200,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            myVib.vibrate(200)
                        }

                    } else {
                        Log.e("Connection", "Could not fetch input, not connected")
                    }

                    btnTogglePower.isChecked = power
                    txtOutput.text = txtOutput.text.toString() + "\nPower: ${if (power) "On" else "Off"}"
                    val extract = "[0-9]+".toRegex().find(volume)
                    val volumeval = extract?.value.toString()
                    seek.progress = volumeval.toInt()
                    btnMute.isChecked = mute
                    txtOutput.text = txtOutput.text.toString() + "\nMute: ${if (mute) "On" else "Off"}"
                    txtOutput.text = txtOutput.text.toString() + "\nVolume is $volumeval"
                }

            }
        fun init(){GlobalScope.launch(Dispatchers.IO) {try {
            // Get local IP
            DatagramSocket().use { socket ->
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                ip = socket.localAddress?.hostAddress?.split(".")?.toMutableList() ?: mutableListOf()
            }

            // Update UI with the IP address
            withContext(Dispatchers.Main) {
                txtOutput.text = ip.toString()
            }

            prefix = "${ip[0]}.${ip[1]}.${ip[2]}."
            val answer = Channel<Int>()

            // Launch coroutines to find receiver
            for (i in 1..254) {
                launch {
                    try {
                        Socket().use { connection ->
                            connection.connect(InetSocketAddress("$prefix$i", 8102), 500)
                            answer.send(i)
                        }
                    } catch (e: Exception) {
                        Log.e("Connection", "Could not connect to address $i")
                    }
                }
            }

            val hit = answer.receive()
            answer.close()

            targetIP = "$prefix$hit"
            client = Socket()

            try {
                client.connect(InetSocketAddress(targetIP, 8102), 200)
                if (client.isConnected) {
                    client.keepAlive = true
                }
            } catch (e: IOException) {
                Log.e("Connection", "Could not connect to target IP")
                return@launch
            }

        } catch (e: Exception) {
            Log.e("Init", "Error initializing connection", e)
        }


            //Fetch volume status
            volume = ""
            val regex = Regex("[VOL]+[0-9]+")
            if (client.isConnected) {
                var i = 0
                do {
                    try {
                        client.outputStream.write("?v\r".toByteArray())
                        val response =
                            BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        val extract = regex.find(response)
                        volume = extract?.value.toString()
                        i++
                    } catch (e: IOException) {
                        //Toast.makeText(this@MainActivity, "Could not fetch volume", Toast.LENGTH_SHORT).show()
                        Log.e("Connection", "Could not fetch volume")
                    }
                } while (!(volume!!.contains("VOL")) or (i > 25))
            }
            //Fetch power status
            power = false
            if (client.isConnected) {
                var powerresponse = ""
                var i = 0
                do {
                    try {
                        client.outputStream.write("?p\r".toByteArray())
                        powerresponse =
                            BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        if (powerresponse.contains("PWR0")
                        ) {
                            power = true
                        }
                        i++
                    } catch (e: IOException) {
                        //Toast.makeText(this@MainActivity,"Could not fetch power",Toast.LENGTH_SHORT).show()
                        Log.e("Connection", "Could not fetch power")
                    }
                } while (!(powerresponse.contains("PWR")) or (i > 25))
            }
            //Fetch mute status
            mute = false
            if (client.isConnected) {
                var i = 0
                var muteresponse = ""
                do {
                    try {
                        client.outputStream.write("?m\r".toByteArray())
                        muteresponse =
                            BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        if (muteresponse.contains("MUT0")
                        ) {
                            mute = true
                        }
                        i++
                    } catch (e: IOException) {
                        //Toast.makeText(this@MainActivity, "Could not fetch mute status", Toast.LENGTH_SHORT).show()
                        Log.e("Connection", "Could not fetch mute status")
                    }
                } while (!(muteresponse.contains("MUT")) or (i > 25))
            }
            //Update UI on main thread
            launch(Dispatchers.Main) {
                getStatus(power!!, volume!!, mute!!).join()
                if(client.isConnected)
                txtOutput.text = txtOutput.text.toString() + "\nConnected to:\n$targetIP:8102"
            }
        }}

        try {
            init()
        } catch (e: IOException) {
            txtOutput.text = txtOutput.text.toString() + "\nCould not find receiver"
        }
        fun connect(ip: List<String>) = GlobalScope.launch(Dispatchers.IO) {
            client = Socket()
            try {
                client.connect(InetSocketAddress(ip[0], ip[1].toInt()), 500)
                if (client.isConnected) {
                    client.keepAlive = true
                    address.add(ip[0])
                    address.add(ip[1])
                }
            } catch (e: IOException) {
                Log.e("Connection", e.toString())
                //cancel("Could not connect")
            }

            launch(Dispatchers.Main) {
                if (client.isConnected) {
                    txtOutput.text =
                        txtOutput.text.toString() + "\nSuccessfully connected to:\n" + ip[0] + ":" + ip[1]
                    try {
                        power?.let {
                            volume?.let { it1 ->
                                mute?.let { it2 ->
                                    getStatus(it, it1, it2).start()
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Log.e("Connection", "Could not fetch status")
                    }
                } else {
                    txtOutput.text =
                        txtOutput.text.toString() + "\nCould not connect to:\n" + ip[0] + ":" + ip[1]
                }
            }
        }

        fun sendCommand(textView: TextView, command: String) = GlobalScope.launch(Dispatchers.IO) {
            var text = ""
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                } catch (e: IOException) {
                    Log.e("sendCommand", "Failed")
                }
            }
            launch(Dispatchers.Main) {
                if (client.isConnected) {
                    textView.text =
                        textView.text.toString() + "\n" + text + "\nSent " + command + " successfully"
                    Toast.makeText(this@MainActivity, "$command executed", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    textView.text = textView.text.toString() + "\n" + text + "\nNot connected!"
                }
            }
        }

        fun changeInput(textView: TextView, command: String) = GlobalScope.launch(Dispatchers.IO) {
            var text = ""
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                } catch (e: IOException) {
                    Log.e("changeInput", "Failed")
                }
            }
            launch(Dispatchers.Main) {
                textView.text = textView.text.toString() + "\n$text" + "\nInput changed to $command"
                Toast.makeText(this@MainActivity, "$command Succeeded", Toast.LENGTH_SHORT).show()
            }
        }
        //Button Listener actions
        btnBT.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "33fn")

        }
        btniPod.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "17fn")

        }
        btnNet.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "26fn")

        }
        btnSpotify.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "53fn")

        }
        btnMHL.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "34fn")

        }
        btnTuner.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "02fn")

        }
        btnAll.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "fu")
            try {
                power?.let { it1 ->
                    volume?.let { it2 ->
                        mute?.let { it3 ->
                            getStatus(
                                it1, it2,
                                it3
                            ).start()
                        }
                    }
                }
            } catch (e: IOException) {
                txtOutput.text = txtOutput.text.toString() + "\nError fetching status"
            }
        }
        btnBD.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "25fn")
        }
        btnDVD.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "04fn")
        }
        btnSat.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "06fn")
        }
        btnHDMI.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "31fn")
        }
        btnTV.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "05fn")
        }
        btnCD.setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "01fn")
        }
        txtInput.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                myVib.vibrate(50)
                ip.clear()
                val textToAdd = txtInput.text.toString().split(" ", ":", limit = 0)
                if (textToAdd.size == 2) {
                    ip.add(textToAdd[0])
                    ip.add(textToAdd[1])
                } else if (textToAdd.size == 1) {
                    if (textToAdd[0].isEmpty()) {
                        txtOutput.text = txtOutput.text.toString() + "\nYou must specify a host"
                    } else {
                        ip.add(textToAdd[0])
                        ip.add("8102")
                    }
                }

                txtOutput.text = txtOutput.text.toString() + "\n" + ip.toString()
                if (ip.size > 0) {
                    if (client.inetAddress?.toString() != ip[0] && client.port.toString() != ip[1]) {
                        try {
                            client.close()
                            connect(ip).start()
                        } catch (e: IOException) {
                            txtOutput.text = txtOutput.text.toString() + "\nCould not connect"
                        }
                    } else {
                        txtOutput.text =
                            txtOutput.text.toString() + "\nAlready connected to:\n${ip[0]}:${ip[1]}"
                    }
                }

                hideKeyboard(v)
                return@OnKeyListener true
            }
            false
        })
        btnConnect.setOnClickListener {
        myVib.vibrate(50)
            ip.clear()
            val textToAdd = txtInput.text.toString().split(" ", ":", limit = 0)
            if (textToAdd.size == 2) {
                ip.add(textToAdd[0])
                ip.add(textToAdd[1])
             }else if (textToAdd.size == 1) {
                if (textToAdd[0].isEmpty()) {
                        try {
                            GlobalScope.launch(Dispatchers.Main){
                                lifecycleScope.launch {
                                    try {
                                        init()
                                    } catch (e: Exception) {
                                        Log.e("FindIP", "Error finding IP", e)
                                    }
                                }
                                Log.i("IP is", ip.toString())
                            }
                    }
                        catch (e:Exception){
                            Log.e(e.toString(),e.toString())
                        }
                    txtOutput.text = txtOutput.text.toString() + "\n${ip}"
                } else {
                    ip.add(textToAdd[0])
                    ip.add("8102")
                }
            }

            txtOutput.text = txtOutput.text.toString() + "\n" + ip.toString()
            if (ip.size > 0) {
                if (client.inetAddress?.toString() != ip[0]) {
                    try {
                        client.close()
                        connect(ip).start()
                    } catch (e: Exception) {
                        Log.e(e.toString(),e.toString())
                        txtOutput.text = txtOutput.text.toString() + "\nCould not connect"
                    }
                } else {
                    txtOutput.text =
                        txtOutput.text.toString() + "\nAlready connected to:\n${ip[0]}:${ip[1]}"
                }
            }
        }
        editTextCommand.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                myVib.vibrate(50)
                val command = editTextCommand.text.toString().trim()
                sendCommand(txtOutput, command)
                return@OnKeyListener true
            }
            false
        })

        btnSendCommand.setOnClickListener {
            myVib.vibrate(50)
            val command = editTextCommand.text.toString().trim()
            sendCommand(txtOutput, command)
        }
        btnTogglePower.setOnClickListener {
            myVib.vibrate(50)
            if (btnTogglePower.isChecked) {
                sendCommand(txtOutput, "po")
            } else {
                sendCommand(txtOutput, "pf")
            }

        }
        btnMute.setOnClickListener {
            myVib.vibrate(50)
            Thread(Runnable {
                if (client.isConnected) {
                    if (btnMute.isChecked) {
                        client.outputStream.write("mo\r".toByteArray())
                    } else {
                        client.outputStream.write("mf\r".toByteArray())
                    }
                }
                Handler(mainLooper).post {
                    when (btnMute.isChecked) {
                        true -> txtOutput.text = txtOutput.text.toString() + "\nMute on"
                        false -> txtOutput.text = txtOutput.text.toString() + "\nMute off"
                    }
                }
            }
            ).start()
        }
        seek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seek: SeekBar,
                progress: Int, fromUser: Boolean
            ) {
                // write custom code for progress is changed
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
                myVib.vibrate(50)
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                myVib.vibrate(50)
                when {
                    seek.progress >= 100 -> {
                        sendCommand(txtOutput, seek.progress.toString() + "vl")
                    }
                    seek.progress >= 10 -> {
                        sendCommand(txtOutput, "0" + seek.progress.toString() + "vl")
                    }
                    else -> {
                        sendCommand(txtOutput, "00" + seek.progress.toString() + "vl")
                    }
                }
            }
        })

        fun saveInstance() {
            savedInstanceState?.putString("SAVED_IP", address[0])
            savedInstanceState?.putString("SAVED_PORT", address[1])
            power?.let { savedInstanceState?.putBoolean("SAVED_POWER", it) }
            mute?.let { savedInstanceState?.putBoolean("SAVED_MUTE", it) }
            savedInstanceState?.putString("SAVED_VOLUME", volume)
        }
        saveInstance()



    }



    @RequiresApi(Build.VERSION_CODES.S)
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        myVib = this.getSystemService(VIBRATOR_SERVICE) as Vibrator
        when (item.itemId) {
            R.id.nav_main -> {
                myVib.vibrate(50)
                Log.e("Main", "Click.")
            }

            R.id.nav_menu -> {
                myVib.vibrate(50)
                Log.e("Menu", "Click.")
                Intent(this, Menu::class.java).also {
                    if (address.size == 2) {
                        it.putExtra("EXTRA_IP", address[0])
                        it.putExtra("EXTRA_PORT", address[1].toInt())
                    }
                    startActivity(it)
                    finish()
                }
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true

    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if(client.isConnected){
                if(seek.progress in 0..185){
                    val vol = seek.progress - 10
                    when {
                        seek.progress >= 100 -> {
                            sendCommand(vol.toString() + "vl")
                        }
                        seek.progress >= 10 -> {
                            sendCommand("0" + vol.toString() + "vl")
                        }
                        else -> {
                            sendCommand("00" + vol.toString() + "vl")
                        }
                    }
                    seek.progress = vol
                }
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if(client.isConnected){
                if(seek.progress in 0..185){
                val vol = seek.progress + 10
                when {
                    seek.progress >= 100 -> {
                        sendCommand(vol.toString() + "vl")
                    }
                    seek.progress >= 10 -> {
                        sendCommand("0" + vol.toString() + "vl")
                    }
                    else -> {
                        sendCommand("00" + vol.toString() + "vl")
                    }
                }
                seek.progress = vol
            }
            }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START)
                } else {
                    super.onBackPressed()
                }
            }
        }
        return true
    }

}