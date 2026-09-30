package com.tradeguard

import android.Manifest
import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var lossLimit: EditText
    private lateinit var lockMinutes: EditText
    private lateinit var zero: CheckBox
    private lateinit var roiL: EditText; private lateinit var roiT: EditText; private lateinit var roiR: EditText; private lateinit var roiB: EditText
    private val receiver = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { refresh() } }

    override fun onCreate(b: Bundle?) { super.onCreate(b); setContentView(R.layout.activity_main)
        status=findViewById(R.id.status); stats=findViewById(R.id.stats); lossLimit=findViewById(R.id.lossLimit); lockMinutes=findViewById(R.id.lockMinutes); zero=findViewById(R.id.zeroIsLoss)
        roiL=findViewById(R.id.roiLeft); roiT=findViewById(R.id.roiTop); roiR=findViewById(R.id.roiRight); roiB=findViewById(R.id.roiBottom)
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        findViewById<Button>(R.id.btnCapture).setOnClickListener { save(); requestCapture() }
        findViewById<Button>(R.id.btnTestLock).setOnClickListener { save(); GuardPrefs.setLock(this, System.currentTimeMillis()+GuardPrefs.minutes(this)*60000L); sendBroadcast(Intent(ACTION_STATE)); refresh() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { GuardPrefs.reset(this); sendBroadcast(Intent(ACTION_STATE)); refresh() }
        refresh()
    }
    private fun save(){ GuardPrefs.saveSettings(this,lossLimit.text.toString().toIntOrNull()?:4,lockMinutes.text.toString().toIntOrNull()?:3,zero.isChecked,roiL.text.toString().toFloatOrNull()?:45f,roiT.text.toString().toFloatOrNull()?:35f,roiR.text.toString().toFloatOrNull()?:100f,roiB.text.toString().toFloatOrNull()?:75f) }
    private fun requestCapture(){ val mgr=getSystemService(MediaProjectionManager::class.java); startActivityForResult(mgr.createScreenCaptureIntent(),REQ_CAPTURE) }
    @Deprecated("legacy callback is sufficient for this test build")
    override fun onActivityResult(req:Int,res:Int,data:Intent?){ super.onActivityResult(req,res,data); if(req==REQ_CAPTURE && res==RESULT_OK && data!=null){ val s=Intent(this,ScreenSensorService::class.java).putExtra("resultCode",res).putExtra("data",data); startForegroundService(s); Toast.makeText(this,"Screen sensor started",Toast.LENGTH_SHORT).show() } }
    override fun onResume(){ super.onResume(); refresh() }
    private fun refresh(){ status.text=if(GuardPrefs.locked(this)) "Status: 🔒 LOCKED — Quotex protection active" else "Status: 🟢 ACTIVE — monitoring ready"; stats.text="Trades: ${GuardPrefs.total(this)} | Profit: ${GuardPrefs.wins(this)} | Loss: ${GuardPrefs.losses(this)} | Streak: ${GuardPrefs.streak(this)} / ${GuardPrefs.limit(this)}" }
    companion object { const val REQ_CAPTURE=9001; const val ACTION_STATE="com.tradeguard.STATE" }
}
