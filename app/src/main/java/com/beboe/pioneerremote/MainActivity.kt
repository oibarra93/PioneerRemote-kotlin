package com.beboe.pioneerremote

import android.content.Context
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
        var btnPS4 = findViewById<Button>(R.id.btnPS4)
        var btnChromecast = findViewById<Chip>(R.id.btnChromecast)
        var btnPC = findViewById<Chip>(R.id.btnPC)
        var btnSpotify = findViewById<Chip>(R.id.btnSpotify)
        var btnTV = findViewById<Chip>(R.id.btnTV)
        var btnBluetooth = findViewById<Chip>(R.id.btnBluetooth)
        var btnTogglePower = findViewById<ToggleButton>(R.id.toggleButtonPower)
        var btnMute = findViewById<ToggleButton>(R.id.btnMute)
        val seek = findViewById<SeekBar>(R.id.seekBarVolume)
        val inputGroup = findViewById<ChipGroup>(R.id.chipGroup)


        //Initialize the network connection by scanning the local network
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkprops = connectivityManager.getLinkProperties(network)
        val ipv4 = networkprops?.linkAddresses?.get(1).toString().split(".","/")
        var targetIP = ""
        val mainLooper = Looper.getMainLooper()
        var ip :MutableList<String> = mutableListOf()
        var prefix = ""
        Thread(Runnable {

            DatagramSocket().use { socket ->
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                ip = socket.getLocalAddress().getHostAddress().split(".") as MutableList<String>
            }

            Handler(mainLooper).post{
                txtOutput.text = ip.toString()
                prefix = ip[0]+"."+ip[1]+"."+ip[2]+"."
                Thread(Runnable{
                    var i = 1
                    do{
                        try{
                            client = Socket()
                            client.connect(InetSocketAddress(prefix + i.toString(), 8102),80)
                        }
                        catch(e:Exception) {
                            print(e.toString())
                            i++
                        }
                    }
                    while (!(client.isConnected)or(i > 254))
                    targetIP = prefix + i.toString()
                    client.close()
                    connect(txtOutput, listOf(targetIP,"8102")).execute()

                    Handler(mainLooper).post {
                        checkVolume(seek).execute()
                        checkInput(inputGroup).execute()
                        checkPower(btnTogglePower).execute()
                    }
                }).start()
            }
        }).start()



        //Button Listener actions
        btnPS4.setOnClickListener {
            changeInput(txtOutput,"05fn").execute()

        }
        btnChromecast.setOnClickListener {
            changeInput(txtOutput,"04fn").execute()

        }
        btnPC.setOnClickListener {
            changeInput(txtOutput,"06fn").execute()

        }
        btnSpotify.setOnClickListener {
            changeInput(txtOutput,"53fn").execute()

        }
        btnBluetooth.setOnClickListener {
            changeInput(txtOutput,"33fn").execute()

        }
        btnTV.setOnClickListener {
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
                    ip.addAll(arrayOf("192.168.0.21", "2323"))
                }
                else{
                    ip.add(textToAdd[0])
                    ip.add("2323")
                }
            }

            txtOutput.text = ip.toString()
            connect(txtOutput, ip).execute()
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
                    txtOutput.text = txtOutput.text.toString() + "\nMute inverted"
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

    }

    inner class connect(textView: TextView, input: List<out String>) : AsyncTask<Unit, Unit, String>() {
        val innerTextView: TextView? = textView
        var text = ""
        val ip = input?.get(0)
        val port = input?.get(1).toInt()
        override fun doInBackground(vararg p0: Unit?): String? {
            if (client.isClosed){
                try{
                        client = Socket()
                        client.connect(InetSocketAddress(ip, port),1000)
                        client.outputStream.write("\r".toByteArray())
                        text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        client.keepAlive = true
                    }
                catch (e : Exception){
                    Toast.makeText(this@MainActivity, "Could not connect!", Toast.LENGTH_SHORT).show()
                }
                }



            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            innerTextView?.text = innerTextView?.text.toString() + "\n" + text + "\nConnected to $ip:$port successfully"
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
            if(client.isConnected){
                innerTextView?.text = innerTextView?.text.toString() + "\n" + text + "\nInput selected: " + command.toString()
            }
            else{
                innerTextView?.text = innerTextView?.text.toString() + "\n" + text + "\nNot connected!"
            }
            //Toast.makeText(this@MainActivity, "sendCommand executed", Toast.LENGTH_SHORT).show()
        }
    }
    inner class checkVolume(volume:SeekBar) : AsyncTask<Unit, Unit, String>() {
        var text = ""
        var volume: SeekBar? = volume
        var m = ""
        val regex = """(\d+)""".toRegex()

        override fun doInBackground(vararg p0: Unit?): String? {
            if (client.isConnected) {
                try {
                    do{
                    client.outputStream.write(("?v" + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                    val value = regex.find(text)
                    m = value?.value.toString()} while(m.contains("04"))
                }
                catch(e:IOException){
                    Toast.makeText(this@MainActivity, "Could not fetch Volume", Toast.LENGTH_SHORT).show()
                }
            }
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(client.isConnected){
                volume?.setProgress(m.toInt())
                Toast.makeText(this@MainActivity, "Volume fetched", Toast.LENGTH_SHORT).show()
            }

            else{
                Toast.makeText(this@MainActivity, "Could not fetch volume, not connected.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    inner class checkInput(group:ChipGroup) : AsyncTask<Unit, Unit, String>() {
        var text = ""
        var group: ChipGroup? = group
        var m = ""
        val regex = """(\d+)""".toRegex()

        override fun doInBackground(vararg p0: Unit?): String? {
            if (client.isConnected) {
                try {

                        client.outputStream.write(("?f" + "\r").toByteArray())
                        text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                        val value = regex.find(text)
                        m = value?.value.toString()
                }
                catch(e:IOException){
                    Toast.makeText(this@MainActivity, "Could not fetch Volume", Toast.LENGTH_SHORT).show()
                }
            }
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(client.isConnected){
                when (m){
                    "04" -> group?.check(R.id.btnChromecast)
                    "33" -> group?.check(R.id.btnBluetooth)
                    "06" -> group?.check(R.id.btnPC)
                    "53" -> group?.check(R.id.btnSpotify)
                    "01" -> group?.check(R.id.btnTV)
                    "05" -> group?.check(R.id.btnPS4)
                }
                Toast.makeText(this@MainActivity, "Volume fetched", Toast.LENGTH_SHORT).show()
            }

            else{
                Toast.makeText(this@MainActivity, "Could not fetch volume, not connected.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    inner class checkPower(power:ToggleButton) : AsyncTask<Unit, Unit, String>() {
        var text = ""
        var power: ToggleButton? = power
        var m = ""
        val regex = """(\d+)""".toRegex()

        override fun doInBackground(vararg p0: Unit?): String? {
            if (client.isConnected) {
                try {

                    client.outputStream.write(("?f" + "\r").toByteArray())
                    text = BufferedReader(InputStreamReader(client.inputStream)).readLine()
                    val value = regex.find(text)
                    m = value?.value.toString()
                }
                catch(e:IOException){
                    Toast.makeText(this@MainActivity, "Could not fetch Volume", Toast.LENGTH_SHORT).show()
                }
            }
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(client.isConnected){
                if(m.contains("PWR0")){
                    power?.isChecked = true
                }
                else if(m.contains("PWR1")){
                    power?.isChecked =false
                }
                Toast.makeText(this@MainActivity, "Volume fetched", Toast.LENGTH_SHORT).show()
            }

            else{
                Toast.makeText(this@MainActivity, "Could not fetch volume, not connected.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    inner class checkPorts(prefix: String, targetIP: String) : AsyncTask<Unit, Unit, String>() {
        var targetIP = String?:targetIP
        //val prefix = String?:prefix
        val prefix = "192.168.0."
        override fun doInBackground(vararg p0: Unit?): String? {

            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }
    }


}