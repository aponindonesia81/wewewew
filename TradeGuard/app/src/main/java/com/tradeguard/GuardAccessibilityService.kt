package com.tradeguard

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import android.os.Handler
import android.os.Looper

class GuardAccessibilityService : AccessibilityService() {
    private var overlay: View? = null
    private var currentPackage = ""
    private val handler=Handler(Looper.getMainLooper())
    private val check=object:Runnable{override fun run(){updateOverlay();handler.postDelayed(this,700)}}
    override fun onServiceConnected(){ super.onServiceConnected(); handler.post(check) }
    override fun onAccessibilityEvent(event:AccessibilityEvent?){ currentPackage=event?.packageName?.toString() ?: currentPackage; GuardPrefs.setForegroundPackage(this,currentPackage); updateOverlay() }
    override fun onInterrupt(){}
    private fun isQuotex():Boolean=currentPackage==QUOTEX_PACKAGE
    private fun updateOverlay(){ if(GuardPrefs.locked(this) && isQuotex()) showOverlay() else hideOverlay() }
    private fun showOverlay(){ if(overlay!=null)return
        val root=FrameLayout(this).apply{setBackgroundColor(Color.argb(8,0,0,0));isClickable=true;isFocusable=false;setOnTouchListener{_,_->true}}
        val banner=TextView(this).apply{ text="🔒 TradeGuard\nTrading temporarily locked";setTextColor(Color.WHITE);setTextSize(14f);setPadding(22,14,22,14);setBackgroundColor(Color.argb(220,20,22,38)) }
        val lp=FrameLayout.LayoutParams(-2,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL);lp.topMargin=34;root.addView(banner,lp)
        val wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val params=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT)
        wm.addView(root,params);overlay=root
    }
    private fun hideOverlay(){val v=overlay?:return;try{(getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)}catch(_:Exception){};overlay=null}
    override fun onDestroy(){handler.removeCallbacks(check);hideOverlay();super.onDestroy()}
    companion object{const val QUOTEX_PACKAGE="io.quotex.x"}
}
