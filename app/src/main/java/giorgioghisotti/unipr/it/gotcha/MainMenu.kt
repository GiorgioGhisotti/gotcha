package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainMenu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        val liveCameraButton: Button = findViewById(R.id.live_camera_button)
        liveCameraButton.setOnClickListener {
            var mIntent = Intent(this, NetCameraView::class.java)
            this.startActivity(mIntent)
        }
        val imageEditorButton: Button = findViewById(R.id.image_editor_button)
        imageEditorButton.setOnClickListener {
            var mIntent = Intent(this, ImageEditor::class.java)
            this.startActivity(mIntent)
        }
    }

    /**
     * @param file  File path in the assets folder
     * @param context   Application context
     * @return  Returns the file's path on the device's filesystem
     */
    private fun getPath(file: String, context: Context): String {
        val assetManager = context.assets
        val inputStream: BufferedInputStream
        try {
            inputStream = BufferedInputStream(assetManager.open(file))
            val data = ByteArray(inputStream.available())
            inputStream.read(data)
            inputStream.close()

            val outFile = File(context.filesDir, file)
            val os = FileOutputStream(outFile)
            os.write(data)
            os.close()

            return outFile.absolutePath
        } catch (ex: IOException) {
            Log.i(TAG, "Failed to upload a file")
        }

        return ""
    }

    companion object {
        private const val TAG = R.string.tag.toString()
    }
}
