package com.tradeguard

import android.app.*
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.*
import android.graphics.*
import android.hardware.display.*
import android.media.projection.MediaProjectionManager
import android.media.*
import android.media.projection.MediaProjection
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class ScreenSensorService: Service(){
    private var projection:MediaProjection?=null; private var display:VirtualDisplay?=null; private var reader:ImageReader?=null
    private val exec=Executors.newSingleThreadExecutor(); private val busy=AtomicBoolean(false); private var lastAccepted=""; private var lastSeen=""; private var stable=0; private var lastAcceptedAt=0L
    private var screenW=0; private var screenH=0
    private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate(){super.onCreate();createChannel();startForeground(71,notification(),if(Build.VERSION.SDK_INT>=29)ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0)}
    override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int{
        val rc=i?.getIntExtra("resultCode",Activity.RESULT_CANCELED)?:Activity.RESULT_CANCELED
        val data=if(Build.VERSION.SDK_INT>=33)i?.getParcelableExtra("data",Intent::class.java) else @Suppress("DEPRECATION") i?.getParcelableExtra("data")
        if(rc==Activity.RESULT_OK && data!=null && projection==null) startProjection(rc,data)
        return START_STICKY
    }
    private fun startProjection(rc:Int,data:Intent){
        val pm=getSystemService(MediaProjectionManager::class.java);projection=pm.getMediaProjection(rc,data)
        val dm=resources.displayMetrics;screenW=dm.widthPixels;screenH=dm.heightPixels
        reader=ImageReader.newInstance(screenW,screenH,PixelFormat.RGBA_8888,2)
        reader!!.setOnImageAvailableListener({r-> if(!busy.compareAndSet(false,true)) {r.acquireLatestImage()?.close();return@setOnImageAvailableListener}; val im=r.acquireLatestImage(); if(im==null){busy.set(false);return@setOnImageAvailableListener}; exec.execute{try{process(im)}catch(_:Throwable){}finally{im.close();busy.set(false)}} },Handler(Looper.getMainLooper()))
        display=projection!!.createVirtualDisplay("TradeGuard",screenW,screenH,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null)
    }
    private fun process(image:Image){
        if(GuardPrefs.foregroundPackage(this)!=GuardAccessibilityService.QUOTEX_PACKAGE) return
        val bmp=imageToBitmap(image)?:return
        val roi=GuardPrefs.roi(this); val l=(bmp.width*roi[0]/100f).toInt().coerceIn(0,bmp.width-1); val t=(bmp.height*roi[1]/100f).toInt().coerceIn(0,bmp.height-1); val rr=(bmp.width*roi[2]/100f).toInt().coerceIn(l+1,bmp.width); val b=(bmp.height*roi[3]/100f).toInt().coerceIn(t+1,bmp.height)
        val crop=Bitmap.createBitmap(bmp,l,t,rr-l,b-t); bmp.recycle()
        recognizer.process(InputImage.fromBitmap(crop,0)).addOnSuccessListener{res->handleText(res.text)}.addOnFailureListener{}
    }
    private fun handleText(raw:String){
        val text=raw.replace('\n',' ').replace(Regex("\\s+")," ").trim()
        if(!Regex("RESULT\\s*\\(?\\s*P\\s*/\\s*L\\s*\\)?",RegexOption.IGNORE_CASE).containsMatchIn(text)) { if(text.isEmpty()) lastSeen=""; return }
        val anchor=Regex("RESULT\\s*\\(?\\s*P\\s*/\\s*L\\s*\\)?",RegexOption.IGNORE_CASE).find(text) ?: return
        val tail=text.substring(anchor.range.last+1).take(80)
        val m=Regex("([+-]?\\s*\\d+(?:[.,]\\d{1,6})?)").find(tail) ?: return
        val token=m.groupValues[1].replace(" ","").replace(',','.')
        val value=token.toDoubleOrNull()?:return
        val kind=when{value>0.0->"P:$token";value<0.0->"L:$token";GuardPrefs.zeroIsLoss(this)->"L:0.00";else->"Z:0.00"}
        if(kind==lastAccepted && System.currentTimeMillis()-lastAcceptedAt<15000)return
        if(kind==lastSeen){stable++}else{lastSeen=kind;stable=1}
        if(stable<2)return
        lastAccepted=kind;lastAcceptedAt=System.currentTimeMillis();stable=0
        when{kind.startsWith("P:")->GuardPrefs.record(this,true,false);kind.startsWith("L:")->GuardPrefs.record(this,false,true);else->return}
        if(GuardPrefs.streak(this)>=GuardPrefs.limit(this)){
            GuardPrefs.setLock(this,System.currentTimeMillis()+GuardPrefs.minutes(this)*60000L)
        }
        sendBroadcast(Intent(MainActivity.ACTION_STATE).setPackage(packageName))
    }
    private fun imageToBitmap(im:Image):Bitmap?{ val plane=im.planes.firstOrNull()?:return null; val buf=plane.buffer; val ps=plane.pixelStride; val rs=plane.rowStride; val pad=rs-ps*im.width; val tmp=Bitmap.createBitmap(im.width+pad/ps,im.height,Bitmap.Config.ARGB_8888); tmp.copyPixelsFromBuffer(buf); return Bitmap.createBitmap(tmp,0,0,im.width,im.height).also{if(it!==tmp)tmp.recycle()} }
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("tg","TradeGuard Sensor",NotificationManager.IMPORTANCE_LOW))}}
    private fun notification():Notification=NotificationCompat.Builder(this,"tg").setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("TradeGuard sensor active").setContentText("Watching Quotex screen locally").setOngoing(true).build()
    override fun onBind(i:Intent?)=null
    override fun onDestroy(){display?.release();reader?.close();projection?.stop();exec.shutdownNow();recognizer.close();super.onDestroy()}
}
