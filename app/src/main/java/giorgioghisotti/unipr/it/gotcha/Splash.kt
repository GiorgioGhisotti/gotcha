package giorgioghisotti.unipr.it.gotcha

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_splash.*
import java.io.File

class Splash : AppCompatActivity() {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    private val sDir = Environment.getExternalStorageDirectory().absolutePath + "/"
    private var mobileNetSSDModelPath: String? = null
    private var mobileNetSSDConfigPath: String? = null
    private var yoloV3ModelPath: String? = null
    private var yoloV3ConfigPath: String? = null

    private fun download() {
        val mobileNetSSDModelRequest: DownloadManager.Request = DownloadManager.Request(
                Uri.parse("https://s3.eu-west-3.amazonaws.com/gotcha-weights/weights/MobileNetSSD/MobileNetSSD.caffemodel")
        )
        mobileNetSSDModelRequest.setDescription("Downloading MobileNetSSD weights")
        mobileNetSSDModelRequest.setTitle("MobileNetSSD weights")
        val mobileNetSSDConfigRequest: DownloadManager.Request = DownloadManager.Request(
                Uri.parse("https://s3.eu-west-3.amazonaws.com/gotcha-weights/weights/MobileNetSSD/MobileNetSSD.prototxt")
        )
        mobileNetSSDConfigRequest.setDescription("Downloading MobileNetSSD configuration")
        mobileNetSSDConfigRequest.setTitle("MobileNetSSD configuration")
        val yoloV3ConfigRequest: DownloadManager.Request = DownloadManager.Request(
                Uri.parse("https://s3.eu-west-3.amazonaws.com/gotcha-weights/weights/YOLO/yolov3.cfg")
        )
        yoloV3ConfigRequest.setDescription("Downloading YOLOv3 configuration")
        yoloV3ConfigRequest.setTitle("YOLOv3 configuration")
        val yoloV3ModelRequest: DownloadManager.Request = DownloadManager.Request(
                Uri.parse("https://s3.eu-west-3.amazonaws.com/gotcha-weights/weights/YOLO/yolov3.weights")
        )
        yoloV3ModelRequest.setDescription("Downloading YOLOv3 model")
        yoloV3ModelRequest.setTitle("YOLOv3 model")

        mobileNetSSDModelRequest.setDestinationInExternalPublicDir(
                "Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/MobileNetSSD/", "MobileNetSSD.caffemodel")
        mobileNetSSDConfigRequest.setDestinationInExternalPublicDir(
                "Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/MobileNetSSD/", "MobileNetSSD.prototxt")
        yoloV3ModelRequest.setDestinationInExternalPublicDir(
                "Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/YOLO/", "YOLOv3.weights")
        yoloV3ConfigRequest.setDestinationInExternalPublicDir(
                "Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/YOLO/", "YOLOv3.cfg")

        val manager: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val sharedPreferences: SharedPreferences = this.getSharedPreferences("sp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("last_item", "YOLOv3.weights").apply()

        val MobileNetSSDConfigRequestID = manager.enqueue(mobileNetSSDConfigRequest)
        sharedPreferences.edit().putLong("MobileNetSSD.prototxt", MobileNetSSDConfigRequestID).commit()
        val MobileNetSSDModelRequestID = manager.enqueue(mobileNetSSDModelRequest)
        sharedPreferences.edit().putLong("MobileNetSSD.caffemodel", MobileNetSSDModelRequestID).commit()
        val yoloV3ModelRequestID = manager.enqueue(yoloV3ModelRequest)
        sharedPreferences.edit().putLong("YOLOv3.weights", yoloV3ModelRequestID).commit()
        val yoloV3ConfigRequestID = manager.enqueue(yoloV3ConfigRequest)
        sharedPreferences.edit().putLong("YOLOv3.cfg", yoloV3ConfigRequestID).commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty()
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED
                        || grantResults[1] != PackageManager.PERMISSION_GRANTED
                        || grantResults[2] != PackageManager.PERMISSION_GRANTED
                        || grantResults[3] != PackageManager.PERMISSION_GRANTED
                        || grantResults[4] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "Sorry, this app requires camera and storage access to work!",
                            Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val mobileSSDConfig = File(sDir + mobileNetSSDConfigPath)
                    val mobileSSDModel = File(sDir + mobileNetSSDModelPath)
                    val yoloV3Config = File(sDir + yoloV3ConfigPath)
                    val yoloV3Model = File(sDir + yoloV3ModelPath)
                    if (!mobileSSDConfig.exists() || !mobileSSDModel.exists() ||
                            !yoloV3Config.exists() || !yoloV3Model.exists()) download()
                    else {
                        val myIntent = Intent(this, MainMenu::class.java)
                        this.startActivity(myIntent)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mobileNetSSDModelPath = resources.getString(R.string.MobileNetSSD_Model) +
                resources.getString(R.string.MobileNetSSD_model_file)
        mobileNetSSDConfigPath = resources.getString(R.string.MobileNetSSD_Config) +
                resources.getString(R.string.MobileNetSSD_config_file)
        yoloV3ModelPath = resources.getString(R.string.YOLOv3_Model) +
                resources.getString(R.string.YOLOv3_model_file)
        yoloV3ConfigPath = resources.getString(R.string.YOLOv3_Config) +
                resources.getString(R.string.YOLOv3_config_file)
        setContentView(R.layout.activity_splash)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val sharedPreferences: SharedPreferences = this.getSharedPreferences("sp", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("reached_menu", false)) {
            sharedPreferences.edit().putBoolean("reached_menu", false).commit()
        }

        mVisible = true

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences: SharedPreferences = this.getSharedPreferences("sp", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("reached_menu", false)) {
            sharedPreferences.edit().putBoolean("reached_menu", false).commit()
            finish()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300

        private val PERMISSION_REQUEST_CODE = 1
    }
}
