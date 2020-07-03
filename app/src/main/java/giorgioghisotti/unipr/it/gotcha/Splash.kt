package giorgioghisotti.unipr.it.gotcha

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import androidx.core.app.ActivityCompat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_splash.*
import java.io.File
import java.lang.Exception
import java.util.*

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

    private fun downloadWeights() {
        val sharedPreferencesDownload = this.getSharedPreferences(
                "download_url",
                Context.MODE_PRIVATE
        )
        val sharedPreferencesSkipped = this.getSharedPreferences(
                "skipped",
                Context.MODE_PRIVATE
        )
        sharedPreferencesSkipped.edit().putBoolean("skipped", false).apply()
        val builder = AlertDialog.Builder(this)
        builder.setTitle(
            "Download neural network weights now?"
        )

        builder.setPositiveButton("Download") {
            _, _ -> run {
                try {
                    /** Collect url information */
                    val weightsUrl: Array<String> = resources.getStringArray(R.array.weights_url)
                    val weights: Array<String> = resources.getStringArray(R.array.weights)
                    val weightsName: Array<String> = resources.getStringArray(R.array.weights_name)
                    var urls: Array<String?> = arrayOf()
                    for ((name, url) in weightsName.zip(weightsUrl)) {
                        urls = urls.plus(
                            sharedPreferencesDownload.getString(
                                name,
                                url
                            )
                        )
                    }
                    val sharedPreferences: SharedPreferences = this.getSharedPreferences("sp", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putInt("download_count", weightsUrl.size).apply()
                    val manager: DownloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    /** Download files */
                    for((fileUrl, fileName) in urls.zip(weights)){
                        val path = resources.getString(R.string.weights_path)
                        val request: DownloadManager.Request = DownloadManager.Request(
                                Uri.parse(fileUrl)
                        )
                        request.setDescription("Receiving from $fileUrl ...")
                        request.setTitle("Downloading $fileName")
                        request.setDestinationInExternalPublicDir(path, fileName)
                        manager.enqueue(request)
                    }
                } catch (e: Exception) {
                    sharedPreferencesSkipped.edit().putBoolean("skipped", true).apply()
                    Toast.makeText(
                            this@Splash,
                            "Could not download files!",
                            Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        builder.setNegativeButton("Skip") {
            dialog, _ -> dialog.cancel()
            sharedPreferencesSkipped.edit().putBoolean("skipped", true).apply()
            val myIntent = Intent(this, MainMenu::class.java)
            this.startActivity(myIntent)
        }
        builder.show()
    }

    private fun checkDownloadedWeights() : Boolean {
        val weights: Array<String> = resources.getStringArray(R.array.weights)
        val files: Vector<File> = Vector()
        for (file_name in weights) {
            val path = sDir + "/" + resources.getString(R.string.weights_path)
            val weightFile = File(path + file_name)
            files.addElement(weightFile)
            if (!weightFile.exists()){
                for (file in files) file.delete()
                downloadWeights()
                return false
            }
        }
        val sharedPreferencesSkipped = this.getSharedPreferences(
                "skipped",
                Context.MODE_PRIVATE
        )
        sharedPreferencesSkipped.edit().putBoolean("skipped", false).apply()
        return true
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
                } else if (checkDownloadedWeights()){
                    val myIntent = Intent(this, MainMenu::class.java)
                    this.startActivity(myIntent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val sharedPreferences: SharedPreferences = this.getSharedPreferences("sp", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("reached_menu", false)) {
            sharedPreferences.edit().putBoolean("reached_menu", false).apply()
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
            sharedPreferences.edit().putBoolean("reached_menu", false).apply()
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
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300

        private const val PERMISSION_REQUEST_CODE = 1
    }
}
