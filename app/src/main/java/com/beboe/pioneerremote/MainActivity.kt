package com.beboe.pioneerremote

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
        var client = Socket()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        var power = false
        var volume = ""
        var mute = false


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
                    Toast.makeText(this@MainActivity, "Input fetched: "+input, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Could not fetch input, not connected.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                btnTogglePower.isChecked = power
                var extract = "[0-9]+".toRegex().find(volume)
                var volumeval = extract?.value.toString()
                seek.progress = volumeval.toInt()
                btnMute.isChecked = mute
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
                //Toast.makeText(this@MainActivity, "Could not connect", Toast.LENGTH_SHORT)
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
                }while(!(volume.contains("VOL")) or (i > 25))
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
                getStatus(power,volume,mute).join()
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
                //txtOutput.text = txtOutput.text.toString() + "\nCould not connect"
            }
            launch(Dispatchers.Main) {
                if (client.isConnected) {
                    txtOutput.text =
                        txtOutput.text.toString() + "\nSuccessfully connected to " + ip[0] + ":" + ip[1]
                    try{
                        getStatus(power,volume,mute).join()
                    }
                    catch(e:IOException){
                        txtOutput.text = txtOutput.text.toString() + "\nCould not fetch status"
                    }
                }
            }
        }
        //Button Listener actions
        btnBT.setOnClickListener {
            changeInput(txtOutput,"33fn").execute()

        }
        btniPod.setOnClickListener {
            changeInput(txtOutput,"17fn").execute()

        }
        btnNet.setOnClickListener {
            changeInput(txtOutput,"26fn").execute()

        }
        btnSpotify.setOnClickListener {
            changeInput(txtOutput,"53fn").execute()

        }
        btnMHL.setOnClickListener {
            changeInput(txtOutput,"34fn").execute()

        }
        btnTuner.setOnClickListener {
            changeInput(txtOutput,"02fn").execute()

        }
        btnAll.setOnClickListener{
            changeInput(txtOutput,"fu").execute()
            try{
                getStatus(power, volume, mute).start()
            }
            catch(e:IOException){
                txtOutput.text = txtOutput.text.toString() + "\nError fetching status"
            }
        }
        btnBD.setOnClickListener{
            changeInput(txtOutput,"25fn").execute()
        }
        btnDVD.setOnClickListener{
            changeInput(txtOutput,"04fn").execute()
        }
        btnSat.setOnClickListener{
            changeInput(txtOutput,"06fn").execute()
        }
        btnHDMI.setOnClickListener{
            changeInput(txtOutput,"31fn").execute()
        }
        btnTV.setOnClickListener{
            changeInput(txtOutput,"05fn").execute()
        }
        btnCD.setOnClickListener{
            changeInput(txtOutput,"01fn").execute()
        }

        btnConnect.setOnClickListener {
            var ip: MutableList<String> = mutableListOf<String>()
            var textToAdd = txtInput.text.toString().split(" ",limit = 0)
            if(textToAdd.size == 2){
                ip.add(textToAdd[0])
                ip.add(textToAdd[1])
            }
            else if(textToAdd.size == 1){
                if(textToAdd[0].isNullOrEmpty()) {
                    txtOutput.text = txtOutput.text.toString() + "\nYou must specify a host"
                    ip.add("192.168.0.21")
                    ip.add("8102")
                    txtOutput.text = ip.toString()
                }
                else{
                    ip.add(textToAdd[0])
                    ip.add("8102")
                }
            }

            txtOutput.text = ip.toString()
            if (client.isConnected){
                client.close()
            }
            try {
                connect(ip).start()
            }
            catch(e:IOException){
                txtOutput.text = txtOutput.text.toString() + "\nCould not connect"
            }
        }
        btnSendCommand.setOnClickListener {
            var command = editTextCommand.text.toString().trim()
            sendCommand(txtOutput,command).execute()
        }
        btnTogglePower.setOnClickListener{
            if(btnTogglePower.isChecked){
                sendCommand(txtOutput,"po").execute()
            }
            else{
                sendCommand(txtOutput, "pf").execute()
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
                    sendCommand(txtOutput, seek.progress.toString() + "vl").execute()
                }
                else if(seek.progress >= 10){
                    sendCommand(txtOutput, "0" + seek.progress.toString() + "vl").execute()
                }
                else{
                    sendCommand(txtOutput, "00" + seek.progress.toString() + "vl").execute()
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
                    init().cancel()
                    getStatus(power, volume, mute).cancel()
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
                    val context = this@MainActivity
                    val intent = Intent(context, Menu::class.java)
                    context.startActivity(intent)
                    false
                }
            }
        }


    }
    inner class sendCommand(textView: TextView, command: String) : AsyncTask<Unit, Unit, String>() {
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
    }
    inner class changeInput(textView: TextView, command: String) : AsyncTask<Unit, Unit, String>() {
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
    }
}