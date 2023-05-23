package com.hapi.avcapture.screen

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import com.hapi.eglbase.SurfaceProvider
import com.hapi.avparam.AVLog
import com.hapi.avcapture.VideoFrame

class ScreenRecordService : Service() {
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplayMediaRecorder: VirtualDisplay? = null
    private var mHandlerThread: HandlerThread? = null
    private var mWorkHandler: Handler? = null

    inner class MediaProjectionBinder : Binder() {
        val service: ScreenRecordService
            get() = this@ScreenRecordService
    }

    @SuppressLint("WrongConstant")
    fun start(
        with: Int,
        height: Int,
        resultCode: Int,
        resultData: Intent?,
        surfaceProvider: SurfaceProvider,
        listener: ImageListener
    ) {
        mHandlerThread = HandlerThread("ScreenRecordService")
        mHandlerThread!!.start()
        mWorkHandler = Handler(mHandlerThread!!.looper)
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        if (mediaProjectionManager == null) {
            stopSelf()
            return
        }
        mediaProjection = mediaProjectionManager!!.getMediaProjection(resultCode, resultData!!)
        if (mediaProjection == null) {
            stopSelf()
            return
        }
        virtualDisplayMediaRecorder = mediaProjection!!.createVirtualDisplay(
            "screen-mirror",
            with,
            height,
            Resources.getSystem().displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surfaceProvider.createSurface(with, height),
            null,
            mWorkHandler
        )
        surfaceProvider.setOnFrameAvailableListener { framebuffer,time ->
            val frame = VideoFrame(
                with,
                height,
                0,
                framebuffer
            ).apply {
                timestamp  = time
            }
            listener.onImageAvailable(frame)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        AVLog.d("MediaProjectionService", "onBind ")
        return MediaProjectionBinder()
        // return mMessenger.getBinder();
    }

    override fun onDestroy() {
        stopMediaRecorder()
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        AVLog.d("MediaProjectionService", "onCreate")
        // 获取服务通知
        val notification = createForegroundNotification()
        //将服务置于启动状态 ,NOTIFICATION_ID指的是创建的通知的ID
        startForeground(1, notification)
    }

    private fun createForegroundNotification(): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // 唯一的通知通道的id.
        val notificationChannelId = "notification_channel_id_01"
        // Android8.0以上的系统，新建消息通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //用户可见的通道名称
            val channelName = "Foreground Service Notification"
            //通道的重要程度
            val importance = NotificationManager.IMPORTANCE_NONE
            val notificationChannel =
                NotificationChannel(notificationChannelId, channelName, importance)
            notificationChannel.description = "Channel description"
            //LED灯
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            //震动
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(this, notificationChannelId)
        //通知小图标
        // builder.setSmallIcon(R.drawable.ic_launcher);
        //通知标题
        builder.setContentTitle("RoomRecordService")
        //设定通知显示的时间
        builder.setWhen(System.currentTimeMillis())
        //创建通知并返回
        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        AVLog.d("MediaProjectionService", "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 结束 媒体录制
     */
    fun stopMediaRecorder() {
        if (virtualDisplayMediaRecorder != null) {
            virtualDisplayMediaRecorder!!.release()
            virtualDisplayMediaRecorder = null
        }
        if (mediaProjection != null) {
            mediaProjection!!.stop()
            mediaProjection = null
        }
        if (mediaProjectionManager != null) {
            mediaProjectionManager = null
        }
        mHandlerThread?.quit()
        stopForeground(true)
    }

    companion object {
        const val ID_MEDIA_PROJECTION = 10031386

        //  int newW = (w/32)*32;
        val screenWidth: Int
            get() =//  int newW = (w/32)*32;
                Resources.getSystem().displayMetrics.widthPixels / 2 / 16 * 16

        //  int newH = (h/32)*32;
        val screenHeight: Int
            get() =//  int newH = (h/32)*32;
                Resources.getSystem().displayMetrics.heightPixels / 2 / 16 * 16

        internal fun bindService(
            activity: FragmentActivity,
            serviceConnection: ScreenRecordServiceConnection
        ) {
            val mediaProjectionManager =
                activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val fragment: RequestFragment =
                RequestFragmentHelper.getPermissionReqFragment(activity)
            val createVirtualDisplay: (requestCode: Int, resultCode: Int) -> Boolean =
                { requestCode: Int, resultCode: Int ->
                    var isGet = true
                    if (requestCode != ID_MEDIA_PROJECTION) {
                        serviceConnection.onStartError(
                            Constants.ERROR_CODE_NO_PERMISSION,
                            "no permission"
                        )
                        isGet = false
                    }
                    if (resultCode != Activity.RESULT_OK) {
                        serviceConnection.onStartError(
                            Constants.ERROR_CODE_NO_PERMISSION,
                            "no permission"
                        )
                        isGet = false
                    }
                    isGet
                }
            fragment.call = { requestCode: Int, resultCode: Int, data: Intent? ->
                if (createVirtualDisplay(requestCode, resultCode)) {
                    serviceConnection.resultCode = resultCode
                    serviceConnection.requestCode = requestCode
                    serviceConnection.data = data
                    // 绑定服务
                    val intent = Intent(activity.applicationContext, ScreenRecordService::class.java)
                    val bindService = activity.applicationContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                    AVLog.d("MediaProjectionService", "bindService $bindService")
                }
            }
            fragment.startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                ID_MEDIA_PROJECTION
            )
        }

        /**
         * 解绑Service
         *
         * @param context           context
         */
        internal fun unbindService(context: Context) {
            context.stopService(Intent(context, ScreenRecordService::class.java))
        }
    }

    abstract class ScreenRecordServiceConnection : ServiceConnection {
        var requestCode: Int = 0
        var resultCode: Int = 0
        var data: Intent? = null
        abstract fun onStartError(code: Int, msg: String)
    }
}