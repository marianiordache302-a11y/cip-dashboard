package ro.cipdashboard.app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var errors: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status=findViewById(R.id.txtStatus)
        errors=findViewById(R.id.txtErrors)
        findViewById<Button>(R.id.btnConnect).setOnClickListener { connect() }
        findViewById<Button>(R.id.btnDiagnosis).setOnClickListener {
            errors.text="Diagnoza OBD2 va fi activată după conexiunea ELM327."
        }
    }

    private fun connect() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val p=arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            if (p.any { ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this,p,10); return
            }
        }
        val a=BluetoothAdapter.getDefaultAdapter()
        if (a==null) { status.text="● Bluetooth indisponibil"; return }
        if (!a.isEnabled) { status.text="● Activează Bluetooth"; return }
        val d=try { a.bondedDevices.firstOrNull { (it.name?:"").contains("ELM",true) || (it.name?:"").contains("OBD",true) } } catch(_:Exception){null}
        if(d!=null) {
            status.text="● ELM327 detectat"
            status.setTextColor(getColor(R.color.green))
            errors.text="Adaptor detectat: ${d.name}. Următorul modul va citi datele ECU."
        } else {
            status.text="● ELM327 nu este împerecheat"
            status.setTextColor(getColor(R.color.red))
            errors.text="Împerechează ELM327 în Setări > Bluetooth și apasă din nou."
        }
    }
}