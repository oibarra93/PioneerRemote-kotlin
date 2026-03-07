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
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


data class ConnectionStatus(val power: Boolean, val mute: Boolean, val volume: String)

@OptIn(DelicateCoroutinesApi::class)
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var myVib: Vibrator
    private lateinit var seek: SeekBar
    private lateinit var txtOutput: TextView
    private lateinit var txtInput: EditText
    private lateinit var btnTogglePower: ToggleButton
    private lateinit var btnMute: ToggleButton
    private lateinit var btnAll: Chip
    private lateinit var inputGroup: ChipGroup



    private var address: MutableList<String> = mutableListOf()

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        fun sendCommand(command: String) = GlobalScope.launch(Dispatchers.IO) {
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    client.outputStream.flush()
                } catch (e: IOException) {
                    Log.e("sendCommand", "Failed")
                }
            }
        }

        var client = Socket()
    }
        private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @SuppressLint("SetTextI18n", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()

        if (savedInstanceState == null) {
            findViewById<NavigationView>(R.id.nav_view).setCheckedItem(R.id.nav_main)
        }

        if (!isAppIgnoringBatteryOptimizations(this)) {
            showBatteryOptimizationDialog()
        }

        try {
            initConnection()
        } catch (e: IOException) {
            txtOutput.append("\nCould not find receiver")
        }
    }

    private fun setupUI() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawer = findViewById(R.id.drawer_layout)
        txtOutput = findViewById(R.id.txtboxResponse)
        txtInput = findViewById(R.id.editTextHostname)
        btnTogglePower = findViewById(R.id.toggleButtonPower)
        btnMute = findViewById(R.id.btnMute)
        btnAll = findViewById(R.id.btnCyclic)
        seek = findViewById(R.id.seekBarVolume)
        inputGroup = findViewById(R.id.chipGroup)
        myVib = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Ensure the TextView is selectable and can scroll within the ScrollView
        txtOutput.movementMethod = ScrollingMovementMethod()
        txtOutput.setTextIsSelectable(true)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.bringToFront()

        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        setupButtons()
        setupSeekBar()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    fun sendCommand(textView: TextView, command: String) = GlobalScope.launch(Dispatchers.IO) {
        var text = ""
        if (client.isConnected) {
            try {
                client.outputStream.write(("" + command + "\r").toByteArray())
                client.outputStream.flush()
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
                val response = filterAndConvert(text)
                txtOutput.append("\n" + response)
                if (response != null) {
                    Log.i("Response:",response)
                }
                txtOutput.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            } else {
                textView.text = textView.text.toString() + "\n" + text + "\nNot connected!"
            }
        }
    }


    private fun setupButtons() {
        findViewById<Button>(R.id.btnConnect).setOnClickListener { connectToServer() }
        findViewById<Button>(R.id.btnSendCommand).setOnClickListener { sendUserCommand() }
        setupChipListeners()
        setupToggleButtons()
    }

    private fun setupChipListeners() {
        val chipCommands = mapOf(
            R.id.btnBT to "33fn", R.id.btniPod to "17fn", R.id.btnNet to "26fn",
            R.id.btnSpotify to "53fn", R.id.btnMHL to "34fn", R.id.btnTuner to "02fn",
            R.id.btnCyclic to "fu", R.id.btnBD to "25fn", R.id.btnDVD to "04fn",
            R.id.btnSat to "06fn", R.id.btnHDMI to "31fn", R.id.btnTV to "05fn",
            R.id.btnCD to "01fn"
        )

        for ((id, command) in chipCommands) {
            findViewById<Chip>(id).setOnClickListener {
                myVib.vibrate(50)
                changeInput(txtOutput, command)
            }
        }

        findViewById<Chip>(R.id.btnCyclic).setOnClickListener {
            myVib.vibrate(50)
            changeInput(txtOutput, "fu")
            fetchStatus()
        }
    }

    private fun setupToggleButtons() {
        btnTogglePower.setOnClickListener {
            myVib.vibrate(50)
            val command = if (btnTogglePower.isChecked) "po" else "pf"
            sendCommand(txtOutput, command)
        }

        btnMute.setOnClickListener {
            myVib.vibrate(50)
            lifecycleScope.launch(Dispatchers.IO) {
                if (client.isConnected) {
                    val command = if (btnMute.isChecked) "mo" else "mf"
                    client.outputStream.write("$command\r".toByteArray())
                    client.outputStream.flush()
                }
                withContext(Dispatchers.Main) {
                    txtOutput.append("\nMute ${if (btnMute.isChecked) "on" else "off"}")
                }
            }
        }
    }

    private fun setupSeekBar() {
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                myVib.vibrate(50)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                myVib.vibrate(50)
                val command = when {
                    seek.progress >= 100 -> "${seek.progress}vl"
                    seek.progress >= 10 -> "0${seek.progress}vl"
                    else -> "00${seek.progress}vl"
                }
                sendCommand(txtOutput, command)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun connectToServer() {
        txtOutput.text = "\nInitializing connection..."
        myVib.vibrate(50)
        val ip: MutableList<String> = txtInput.text.toString().split(" ", ":").toMutableList()
        if (ip.size == 1) ip.add("8102")
        if (ip[0].isEmpty()) {
            val connected = initConnection()
            txtOutput.append("\nHostname is empty, trying Autoconnect...")
            if(connected){
                txtOutput.append("Could not connect, are you on the same network?")
            }
            return
        }

        txtOutput.append("\n$ip")
        if (client.inetAddress?.hostAddress != ip[0] || client.port != ip[1].toInt()) {
            lifecycleScope.launch {
                try {
                    client.close()
                    connect(ip[0], ip[1].toInt())
                } catch (e: IOException) {
                    txtOutput.append("\nCould not connect")
                }
            }
        } else {
            txtOutput.append("\nAlready connected to: ${ip[0]}:${ip[1]}")
        }
        hideKeyboard(txtInput)
    }

    private fun sendUserCommand() {
        myVib.vibrate(50)
        val command = findViewById<EditText>(R.id.editTextCommand).text.toString().trim()
        sendCommand(txtOutput, command)
    }

    private suspend fun connect(ip: String, port: Int) = withContext(Dispatchers.IO) {
        client = Socket()
        try {
            client.connect(InetSocketAddress(ip, port), 500)
            if (client.isConnected) {
                client.keepAlive = true
                address.add(ip)
                address.add(port.toString())
            }
        } catch (e: IOException) {
            Log.e("Connection", "Failed to connect to $ip:$port", e)
        }
        withContext(Dispatchers.Main) {
            if (client.isConnected) {
                txtOutput.append("\nSuccessfully connected to: $ip:$port")
                fetchStatus()
            } else {
                txtOutput.append("\nCould not connect to: $ip:$port")
            }
        }
    }

    private fun changeInput(textView: TextView, command: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var text = ""
            if (client.isConnected) {
                try {
                    client.outputStream.write("$command\r".toByteArray())
                    client.outputStream.flush()
                    // Read the complete response
                    val reader = BufferedReader(InputStreamReader(client.inputStream))
                    val responseBuilder = StringBuilder()

                    // Adjust loop as needed to handle multi-line responses
                    var line: String?
                    while (true) {
                        line = reader.readLine()
                        if (line == null || line.isEmpty()) break // End of stream or empty line
                        responseBuilder.append(line)
                    }

                    text = responseBuilder.toString()

                } catch (e: IOException) {
                    Log.e("changeInput", "Failed to change input", e)
                }
            }
            withContext(Dispatchers.Main) {
                textView.append("\n$text\nInput changed to $command")
                Toast.makeText(this@MainActivity, "$command succeeded", Toast.LENGTH_SHORT).show()

                // Check if the response is valid and process it
                if (text.length > 4) {
                    // Filter out specific strings if needed
                        val response = filterAndConvert(text)
                        textView.append("\n" +
                                "Stereo response: $response")
                        if (response != null) {
                            Log.i("Response", response)
                        }

                } else {
                    Log.i("Response", "Response too short: $text")
                }
            }
        }
    }

    // Function to process the input string
    fun filterAndConvert(input: String): String? {
        // Function to convert hex string to ASCII string
        fun hexToAscii(hexStr: String): String {
            val output = StringBuilder()
            var i = 0
            var hexStr = hexStr.substring(3)
            while (i < hexStr.length) {
                // Check if there are at least 2 characters left to process
                if (i + 2 <= hexStr.length) {
                    val str = hexStr.substring(i, i + 2)
                    val decimal = str.toInt(16)
                    output.append(decimal.toChar())
                    i += 2
                } else {
                    // Handle cases where there's an odd number of characters
                    // Optionally log or handle this case based on your needs
                    break
                }
            }
            return output.toString()
        }


        // Remove non-hexadecimal characters (like leading/trailing spaces)
        val cleanedInput = input.replace(Regex("[^0-9A-Fa-f]"), "")

        // Convert the cleaned string to ASCII
        return if (cleanedInput.isNotEmpty()) {
            hexToAscii(cleanedInput)
        } else {
            null // Return null if the resulting string is empty
        }
    }


    private fun fetchStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val status = fetchConnectionStatus()
            withContext(Dispatchers.Main) {
                updateUIWithStatus(status)
            }
        }
    }

    private fun fetchConnectionStatus(): ConnectionStatus {
        var power = false
        var mute = false
        var volume = ""


        if (client.isConnected) {
            power = fetchStatusCommand("p") { it.contains("PWR0") }
            mute = fetchStatusCommand("m") { it.contains("MUT0") }
            volume = fetchVolumeStatus()
            getStatus()
        }

        return ConnectionStatus(power, mute, volume)
    }

    private fun getStatus(){
        GlobalScope.launch(Dispatchers.IO) {
            val input: String
            if (client.isConnected) {
                try {
                    // Send command to fetch input status
                    client.outputStream.write("?f\r".toByteArray())
                    client.outputStream.flush()

                    // Read response from the server
                    input = BufferedReader(InputStreamReader(client.inputStream)).readLine()

                    // Check input status and update UI
                    launch(Dispatchers.Main) {
                        handleInputStatus(input)
                    }
                } catch (e: IOException) {
                    Log.e("Connection", "Could not fetch input")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleInputStatus(input: String){
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
        txtOutput.text = txtOutput.text.toString() + "\nInput fetched: $buttonText"
    }


    private fun fetchStatusCommand(command: String, condition: (String) -> Boolean): Boolean {
        var result = false
        var response: String
        repeat(1) {
            if (client.isConnected) {
                try {
                    client.outputStream.write("?$command\r".toByteArray())
                    client.outputStream.flush()
                    response = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                    if (condition(response)) {
                        result = true
                        return@repeat
                    }
                } catch (e: IOException) {
                    Log.e("fetchStatusCommand", "Failed to fetch status for $command", e)
                }
            }
        }
        return result
    }

    private fun fetchVolumeStatus(): String {
        var volume = ""
        val regex = Regex("[VOL]+[0-9]+")
        repeat(25) {
            if (client.isConnected) {
                try {
                    client.outputStream.write("?v\r".toByteArray())
                    client.outputStream.flush()
                    val response = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                    regex.find(response)?.let { match ->
                        volume = match.value
                        return@repeat
                    }
                } catch (e: IOException) {
                    Log.e("fetchVolumeStatus", "Failed to fetch volume", e)
                }
            }
        }
        return volume
    }

    private fun updateUIWithStatus(status: ConnectionStatus) {
        val volumeVal = status.volume.filter { it.isDigit() }.toIntOrNull() ?: 0
        btnTogglePower.isChecked = status.power
        btnMute.isChecked = status.mute
        seek.progress = volumeVal

        txtOutput.apply {
            append("\nPower: ${if (status.power) "On" else "Off"}")
            append("\nMute: ${if (status.mute) "On" else "Off"}")
            append("\nVolume: $volumeVal")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            myVib.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            myVib.vibrate(200)
        }
    }

    private fun initConnection() : Boolean {
        var connection = false
        lifecycleScope.launch {
            try {
                val ip = getLocalIPAddress()
                val targetIP = scanLocalNetwork(ip)
                connect(targetIP, 8102)
                connection = true
            } catch (e: Exception) {
                connection = false
                Log.e("initConnection", "Error initializing connection", e)
                txtOutput.append("\nError initializing connection")
            }


        }
        return connection
    }

    private suspend fun getLocalIPAddress(): String {
        var ipAddress: String
        withContext(Dispatchers.IO) {
            DatagramSocket().use { socket ->
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                ipAddress = socket.localAddress.hostAddress ?: ""
            }
        }
        return ipAddress
    }

    private suspend fun scanLocalNetwork(ip: String): String {
        val prefix = ip.substringBeforeLast('.')
        val answer = Channel<Int>()
        val jobs = mutableListOf<Job>()
        var hit: Int

        withContext(Dispatchers.IO) {
            for (i in 1..254) {
                jobs.add(launch {
                    try {
                        Socket().use { connection ->
                            connection.connect(InetSocketAddress("$prefix.$i", 8102), 500)
                            answer.send(i)
                        }
                    } catch (e: Exception) {
                        Log.e("scanLocalNetwork", "Could not connect to address $i", e)
                    }
                })
            }

            hit = answer.receive()
            answer.close()
            jobs.forEach { it.cancel() }
            //return@withContext "$prefix.$hit"
        }
        return "$prefix.$hit"
    }

    private fun isAppIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Check if the app is ignoring battery optimization

    private fun showBatteryOptimizationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Disable Battery Optimization")
        builder.setMessage("To keep the connection stable, please disable battery optimization for this app." +
                "\nBattery Optimization -> All Apps -> Pioneer Remote -> Don't Optimize")
        builder.setPositiveButton("OK") { dialog, _ ->
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(this, intent, null)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
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
                    startActivity(this, it, null)
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

