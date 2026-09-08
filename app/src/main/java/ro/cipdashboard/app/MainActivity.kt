package ro.cipdashboard.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var txtStatus: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnDtc: Button
    private lateinit var txtRpm: TextView
    private lateinit var txtTemp: TextView
    private lateinit var txtSpeed: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    // UUID standard pentru adaptorul ELM327 / Serial Port Profile (SPP)
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inițializare elemente UI
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        txtStatus = findViewById(R.id.txtStatus)
        btnConnect = findViewById(R.id.btnConnect)
        btnDtc = findViewById(R.id.btnDtc)
        txtRpm = findViewById(R.id.txtRpm)
        txtTemp = findViewById(R.id.txtTemp)
        txtSpeed = findViewById(R.id.txtSpeed)

        // Configurare meniu lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_dtc -> {
                    drawerLayout.closeDrawers()
                    readTroubleCodes()
                    true
                }
                else -> false
            }
        }

        // Acțiune buton Conectare ELM327
        btnConnect.setOnClickListener {
            connectToElm327()
        }

        // Acțiune buton Diagnoză DTC
        btnDtc.setOnClickListener {
            readTroubleCodes()
        }
    }

    private fun connectToElm327() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Dispozitivul nu suporta Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permisiunea Bluetooth este necesara", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val elmDevice = pairedDevices?.find { 
            it.name.contains("OBD", ignoreCase = true) || it.name.contains("ELM", ignoreCase = true) 
        }

        if (elmDevice == null) {
            Toast.makeText(this, "Niciun adaptor OBD/ELM imperecheat găsit!", Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            try {
                bluetoothSocket = elmDevice.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                runOnUiThread {
                    txtStatus.text = "● Conectat la ${elmDevice.name}"
                    txtStatus.setTextColor(android.graphics.Color.GREEN)
                    Toast.makeText(this, "Conexiune reusita!", Toast.LENGTH_SHORT).show()
                }

                // Inițializare ELM327
                sendObdCommand("AT Z\r")
                sendObdCommand("AT SP 0\r")

            } catch (e: Exception) {
                runOnUiThread {
                    txtStatus.text = "● Eroare Conexiune"
                    txtStatus.setTextColor(android.graphics.Color.RED)
                    Toast.makeText(this, "Eroare: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun sendObdCommand(command: String) {
        try {
            outputStream?.write(command.toByteArray())
            Thread.sleep(100)
            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer) ?: 0
            val response = String(buffer, 0, bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readTroubleCodes() {
        if (bluetoothSocket?.isConnected == true) {
            Thread {
                try {
                    outputStream?.write("03\r".toByteArray())
                    Thread.sleep(200)
                    val buffer = ByteArray(1024)
                    val bytes = inputStream?.read(buffer) ?: 0
                    val response = String(buffer, 0, bytes)

                    runOnUiThread {
                        Toast.makeText(this, "Erori citite: $response", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Eroare citire DTC", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Conectează mai întâi adaptorul ELM327!", Toast.LENGTH_SHORT).show()
        }
    }
}
