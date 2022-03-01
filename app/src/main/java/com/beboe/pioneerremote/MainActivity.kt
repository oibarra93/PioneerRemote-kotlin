package com.beboe.pioneerremote

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {
    companion object{
        var ip :MutableList<String> = mutableListOf()
        var client = Socket()
        fun sendCommand(command: String) = GlobalScope.launch(Dispatchers.IO) {
            var text = ""
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                }
                catch(e:IOException){
                    Log.d("sendCommand","Failed")
                }
            }
            else{

            }
            launch(Dispatchers.Main) {
                if(client.isConnected){

                }
                else{

                }
            }
        }
        fun connect(ip:List<String>) = GlobalScope.launch(Dispatchers.IO) {
            client = Socket()
            try {
                client.connect(InetSocketAddress(ip[0], ip[1].toInt()), 200)
                if(client.isConnected)
                    client.keepAlive = true
            } catch (e: IOException) {
                cancel("Could not connect")
            }
            launch(Dispatchers.Main) {

            }
        }

    }
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        /*savedInstanceState.putBoolean("MyBoolean", true)
        savedInstanceState.putDouble("myDouble", 1.9)
        savedInstanceState.putInt("MyInt", 1)
        savedInstanceState.putString("MyString", "Welcome back to Android")*/
        // etc.
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        val power = savedInstanceState.getBoolean("SAVED_POWER")
        val mute = savedInstanceState.getBoolean("SAVED_MUTE")
        val volume = savedInstanceState.getString("SAVED_VOLUME")
        val ip = savedInstanceState.getString("SAVED_IP")
        val port = savedInstanceState.getString("SAVE_PORT")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var power = savedInstanceState?.getBoolean("SAVED_POWER")
        var mute = savedInstanceState?.getBoolean("SAVED_MUTE")
        var volume = savedInstanceState?.getString("SAVED_VOLUME")
        var ipAddress = savedInstanceState?.getString("SAVED_IP")
        var port = savedInstanceState?.getString("SAVE_PORT")
        //Initialize viewContent variables
        var btnConnect = findViewById<Button>(R.id.btnConnect)
        var txtOutput = findViewById<TextView>(R.id.txtboxResponse)
        var txtInput = findViewById<EditText>(R.id.editTextHostname)
        var btnSendCommand = findViewById<Button>(R.id.btnSendCommand)
        var editTextCommand = findViewById<EditText>(R.id.editTextCommand)
        var btnBT = findViewById<Button>(R.id.btnBT)
        var btniPod = findViewById<Chip>(R.id.btniPod)
        var btnNet = findViewById<Chip>(R.id.btnNet)
        var btnSpotify = findViewById<Chip>(R.id.btnSpotify)
        var btnTuner = findViewById<Chip>(R.id.btnTuner)
        var btnMHL = findViewById<Chip>(R.id.btnMHL)
        var btnBD = findViewById<Chip>(R.id.btnBD)
        var btnSat = findViewById<Chip>(R.id.btnSat)
        var btnHDMI = findViewById<Chip>(R.id.btnHDMI)
        var btnTV = findViewById<Chip>(R.id.btnTV)
        var btnCD = findViewById<Chip>(R.id.btnCD)
        var btnAll =findViewById<Chip>(R.id.btnCyclic)
        var btnDVD = findViewById<Chip>(R.id.btnDVD)
        var btnTogglePower = findViewById<ToggleButton>(R.id.toggleButtonPower)
        var btnMute = findViewById<ToggleButton>(R.id.btnMute)
        val seek = findViewById<SeekBar>(R.id.seekBarVolume)
        val inputGroup = findViewById<ChipGroup>(R.id.chipGroup)
        txtOutput.movementMethod = ScrollingMovementMethod()
        //var power = false
        //var volume = ""
        //var mute = false


        //Initialize the network connection by scanning the local network
        var targetIP = ""
        val mainLooper = Looper.getMainLooper()
        var ip :MutableList<String> = mutableListOf()
        var prefix = ""

        fun getStatus(power:Boolean,volume:String,mute:Boolean) = GlobalScope.launch(Dispatchers.IO) {
            Thread.sleep(100)
            var input = ""
            if (client.isConnected) {
                do {
                    try {
                        client.outputStream.write(("?f\r?f\r").toByteArray())
                        input = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                    } catch (e: IOException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Could not fetch input",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                } while (!(input.contains("FN")))
            }
            launch(Dispatchers.Main) {
                if (client.isConnected) {
                    when (input) {
                        "FN04" -> inputGroup.check(R.id.btnDVD)
                        "FN33" -> inputGroup.check(R.id.btnBT)
                        "FN06" -> inputGroup.check(R.id.btnSat)
                        "FN53" -> inputGroup.check(R.id.btnSpotify)
                        "FN02" -> inputGroup.check(R.id.btnTuner)
                        "FN05" -> inputGroup.check(R.id.btnTV)
                        "FN25" -> inputGroup.check(R.id.btnBD)
                        "FN17" -> inputGroup.check(R.id.btniPod)
                        "FN34" -> inputGroup.check(R.id.btnMHL)
                        "FN44" -> inputGroup.check(R.id.btnNet)
                        "FN45" -> inputGroup.check(R.id.btnNet)
                        "FN38" -> inputGroup.check(R.id.btnNet)
                        "FN41" -> inputGroup.check(R.id.btnNet)
                        "FN01" -> inputGroup.check(R.id.btnCD)
                        "FN19" -> {
                            inputGroup.check(R.id.btnHDMI)
                            btnHDMI.text = "HDMI 1"
                        }
                        "FN20" -> {
                            inputGroup.check(R.id.btnHDMI)
                            btnHDMI.text = "HDMI 2"
                        }
                        "FN21" -> {
                            inputGroup.check(R.id.btnHDMI)
                            btnHDMI.text = "HDMI 3"
                        }
                        "FN22" -> {
                            inputGroup.check(R.id.btnHDMI)
                            btnHDMI.text = "HDMI 4"
                        }
                        "FN23" -> {
                            inputGroup.check(R.id.btnHDMI)
                            btnHDMI.text = "HDMI 5/MHL"
                        }
                    }
                    txtOutput.text = txtOutput.text.toString() + "\nInput fetched $input"
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Could not fetch input, not connected.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                btnTogglePower.isChecked = power
                txtOutput.text = txtOutput.text.toString() + "\nPower is $power"
                var extract = "[0-9]+".toRegex().find(volume)
                var volumeval = extract?.value.toString()
                seek.progress = volumeval.toInt()
                txtOutput.text = txtOutput.text.toString() + "\nVolume is $volumeval"
                btnMute.isChecked = mute
                txtOutput.text = txtOutput.text.toString() + "\nMute is $mute"
            }

        }
        fun init() = GlobalScope.launch(Dispatchers.IO) {
            //Get local ip
            DatagramSocket().use { socket ->
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                ip = socket.getLocalAddress().getHostAddress().split(".") as MutableList<String>
            }
            //Go through local addresses to find receiver
            txtOutput.text = ip.toString()
            prefix = ip[0] + "." + ip[1] + "." + ip[2] + "."

            var i = 1
            do {
                try {
                    client = Socket()
                    client.connect(InetSocketAddress(prefix + i.toString(), 8102), 200)
                } catch (e: Exception) {
                    print(e.toString())
                    i++
                }
            } while (!(client.isConnected) or (i > 254))
            targetIP = prefix + i.toString()
            client = Socket()
            try{
            client.connect(InetSocketAddress(targetIP, 8102), 150)
                if(client.isConnected){
                client.keepAlive = true}}
            catch (e:IOException){
                cancel("Could not connect")
            }


            //Fetch input status
            var input = ""
            Thread.sleep(50)
            if (client.isConnected) {
                try {

                    client.outputStream.write(("?f\r?f\r").toByteArray())
                    input = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                } catch (e: IOException) {
                    Toast.makeText(this@MainActivity, "Could not fetch input", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            //Fetch volume status
            volume = ""
            val regex = Regex("[VOL]+[0-9]+")
            if (client.isConnected) {
                var i = 0
                do{
                    try {
                        client.outputStream.write("?v\r?v\r".toByteArray())
                        var response = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        var extract = regex.find(response)
                        volume = extract?.value.toString()
                        i++
                    } catch (e: IOException) {
                        Toast.makeText(this@MainActivity, "Could not fetch volume", Toast.LENGTH_SHORT)
                            .show()
                    }
                }while(!(volume!!.contains("VOL")) or (i > 25))
            }
            //Fetch power status
            power = false
            if (client.isConnected) {
                var powerresponse = ""
                var i = 0
                do {
                    try {
                        client.outputStream.write("?p\r?p\r".toByteArray())
                        powerresponse =
                            BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        if (powerresponse.contains("PWR0")
                        ) {
                            power = true
                        }
                        i++
                    } catch (e: IOException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Could not fetch power",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                } while (!(powerresponse.contains("PWR")) or (i > 25))
            }
            //Fetch mute status
            mute = false
            if(client.isConnected){
                var i = 0
                var muteresponse = ""
                do{
                    try{
                        client.outputStream.write("?m\r".toByteArray())
                        muteresponse =
                            BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        if (muteresponse.contains("MUT0")
                        ) {
                            mute = true
                        }
                        i++
                    }
                    catch(e:IOException){
                        Toast.makeText(this@MainActivity, "Could not fetch mute status", Toast.LENGTH_SHORT)
                    }
                }while(!(muteresponse.contains("MUT"))or(i > 25))
            }
            //Update UI on main thread
            launch(Dispatchers.Main) {
                getStatus(power!!, volume!!, mute!!).join()
            }
        }

        try{
            init().start()
        }
        catch(e:IOException){
            txtOutput.text = txtOutput.text.toString() + "\nCould not find receiver"
        }
        fun connect(ip:List<String>) = GlobalScope.launch(Dispatchers.IO) {
            client = Socket()
            try {
                client.connect(InetSocketAddress(ip[0], ip[1].toInt()), 200)
                if(client.isConnected)
                    client.keepAlive = true
            } catch (e: IOException) {
                cancel("Could not connect")
            }
            launch(Dispatchers.Main) {
                if (client.isConnected) {
                    txtOutput.text =
                        txtOutput.text.toString() + "\nSuccessfully connected to " + ip[0] + ":" + ip[1]
                    try{
                        power?.let { volume?.let { it1 -> mute?.let { it2 ->
                            getStatus(it, it1,
                                it2
                            ).join()
                        } } }
                    }
                    catch(e:IOException){
                        Log.d("Connection","Could not fetch status")
                    }
                }
            }
        }
        fun sendCommand(textView: TextView, command: String) = GlobalScope.launch(Dispatchers.IO) {
            var text = ""
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                }
                catch(e:IOException){
                    Log.d("sendCommand","Failed")
                }
            }
            launch(Dispatchers.Main) {
                if(client.isConnected){
                    textView.text = textView.text.toString() + "\n" + text + "\nSent " + command.toString() +  " successfully"
                    Toast.makeText(this@MainActivity, "$command executed", Toast.LENGTH_SHORT).show()
                }
                else{
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
                }
                catch(e:IOException){
                    Log.d("changeInput","Failed")
                }
            }
            launch(Dispatchers.Main){
                textView.text = textView.text.toString() + "\n$text" + "\nInput changed to $command"
                Toast.makeText(this@MainActivity, "$command Succeeded", Toast.LENGTH_SHORT).show()
            }
        }
        //Button Listener actions
        btnBT.setOnClickListener {
            changeInput(txtOutput,"33fn")

        }
        btniPod.setOnClickListener {
            changeInput(txtOutput,"17fn")

        }
        btnNet.setOnClickListener {
            changeInput(txtOutput,"26fn")

        }
        btnSpotify.setOnClickListener {
            changeInput(txtOutput,"53fn")

        }
        btnMHL.setOnClickListener {
            changeInput(txtOutput,"34fn")

        }
        btnTuner.setOnClickListener {
            changeInput(txtOutput,"02fn")

        }
        btnAll.setOnClickListener{
            changeInput(txtOutput,"fu")
            try{
                power?.let { it1 -> volume?.let { it2 -> mute?.let { it3 ->
                    getStatus(it1, it2,
                        it3
                    ).start()
                } } }
            }
            catch(e:IOException){
                txtOutput.text = txtOutput.text.toString() + "\nError fetching status"
            }
        }
        btnBD.setOnClickListener{
            changeInput(txtOutput,"25fn")
        }
        btnDVD.setOnClickListener{
            changeInput(txtOutput,"04fn")
        }
        btnSat.setOnClickListener{
            changeInput(txtOutput,"06fn")
        }
        btnHDMI.setOnClickListener{
            changeInput(txtOutput,"31fn")
        }
        btnTV.setOnClickListener{
            changeInput(txtOutput,"05fn")
        }
        btnCD.setOnClickListener{
            changeInput(txtOutput,"01fn")
        }

        btnConnect.setOnClickListener {
            ip.clear()
            var textToAdd = txtInput.text.toString().split(" ",":",limit = 0)
            if(textToAdd.size == 2){
                ip.add(textToAdd[0])
                ip.add(textToAdd[1])
            }
            else if(textToAdd.size == 1){
                if(textToAdd[0].isNullOrEmpty()) {
                    txtOutput.text = txtOutput.text.toString() + "\nYou must specify a host"
                }
                else{
                    ip.add(textToAdd[0])
                    ip.add("8102")
                }
            }

            txtOutput.text = txtOutput.text.toString() +"\n"+ ip.toString()
            if(ip.size > 0){
                try {
                    connect(ip).start()
                }
                catch(e:IOException){
                    txtOutput.text = txtOutput.text.toString() + "\nCould not connect"
                }
            }
        }

        btnSendCommand.setOnClickListener {
            var command = editTextCommand.text.toString().trim()
            sendCommand(txtOutput,command)
        }
        btnTogglePower.setOnClickListener{
            if(btnTogglePower.isChecked){
                sendCommand(txtOutput,"po")
            }
            else{
                sendCommand(txtOutput, "pf")
            }

        }
        btnMute.setOnClickListener{
            Thread(Runnable {
                if (client.isConnected) {
                    if (btnMute.isChecked){
                    client.outputStream.write("mo\r".toByteArray())}
                    else{
                        client.outputStream.write("mf\r".toByteArray())
                    }
                }
                Handler(mainLooper).post {
                    when(btnMute.isChecked) {
                        true -> txtOutput.text = txtOutput.text.toString() + "\nMute on"
                        false -> txtOutput.text = txtOutput.text.toString() + "\nMute off"
                    }
                }}
            ).start()
        }
        seek?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                if(seek.progress >= 100) {
                    sendCommand(txtOutput, seek.progress.toString() + "vl")
                }
                else if(seek.progress >= 10){
                    sendCommand(txtOutput, "0" + seek.progress.toString() + "vl")
                }
                else{
                    sendCommand(txtOutput, "00" + seek.progress.toString() + "vl")
                }
            }
        })

        drawer{

            accountHeader{

                    profile("Pioneer Remote"){
                    icon = R.drawable.logo
                }
            }
            primaryItem("Connection") {
                onClick { _ ->
                    Log.d("Drawer","Click.")
                    /*init().cancel()
                    power?.let { volume?.let { it1 -> mute?.let { it2 ->
                        getStatus(it, it1,
                            it2
                        ).cancel()
                    } } }
                    connect(ip).cancel()*/
                    val extraIp = ip[0]
                    val extraPort = ip[1].toInt()
                    Intent(this@MainActivity, Menu::class.java).also{
                        it.putExtra("EXTRA_IP",extraIp)
                        it.putExtra("EXTRA_PORT", extraPort)
                        startActivity(intent)
                    }

                    false
                }
            }
            divider{}
            primaryItem("Home Menu Controls") {
                onClick { _ ->
                    Log.d("Drawer","Click.")
                    /*init().cancel()
                    power?.let { volume?.let { it1 -> mute?.let { it2 ->
                        getStatus(it, it1,
                            it2
                        ).cancel()
                    } } }
                    connect(ip).cancel()*/
                    val context = this@MainActivity
                    val intent = Intent(context, Menu::class.java)
                    context.startActivity(intent)
                    false
                }
            }
        }
        fun saveInstance() {
            savedInstanceState?.putString("SAVED_IP", ip[0])
            savedInstanceState?.putString("SAVED_PORT", ip[1])
            power?.let { savedInstanceState?.putBoolean("SAVED_POWER", it) }
            mute?.let { savedInstanceState?.putBoolean("SAVED_MUTE", it) }
            savedInstanceState?.putString("SAVED_VOLUME", volume)
        }
        saveInstance()

    }



    /*inner class sendCommand(textView: TextView, command: String) : AsyncTask<Unit, Unit, String>() {
        val innerTextView: TextView? = textView
        var text = ""
        var command: String? = command

        override fun doInBackground(vararg p0: Unit?): String? {
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                }
                catch(e:IOException){
                    Toast.makeText(this@MainActivity, "sendCommand failed", Toast.LENGTH_SHORT).show()
                }
            }
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(client.isConnected){
                innerTextView?.text = innerTextView?.text.toString() + "\n" + text + "\nSent " + command.toString() +  " successfully"
                Toast.makeText(this@MainActivity, "sendCommand executed", Toast.LENGTH_SHORT).show()
            }
            else{
                innerTextView?.text = innerTextView?.text.toString() + "\n" + text + "\nNot connected!"
            }
        }
    }*/
    /*inner class changeInput(textView: TextView, command: String) : AsyncTask<Unit, Unit, String>() {
        val innerTextView: TextView? = textView
        var text = ""
        var command: String? = command

        override fun doInBackground(vararg p0: Unit?): String? {
            if (client.isConnected) {
                try {
                    client.outputStream.write(("" + command + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                }
                catch(e:IOException){
                    Toast.makeText(this@MainActivity, "changeInput Failed", Toast.LENGTH_SHORT).show()
                }
            }
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }
    }*/
}