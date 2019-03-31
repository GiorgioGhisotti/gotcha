package giorgioghisotti.unipr.it.gotcha

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Export : AppCompatActivity() {
    private var saveImageButton: Button? = null
    private var exportImageView: ImageView? = null
    private var cutoutImage: Bitmap? = null
    private var outFileName = "output"
    private val sDir = Environment.getExternalStorageDirectory().absolutePath

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val inputStream = this.openFileInput(getString(R.string.cutout_file))
        cutoutImage = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        exportImageView = findViewById(R.id.export_image_view)
        exportImageView?.setImageBitmap(cutoutImage)

        saveImageButton = findViewById(R.id.save_image_button)
        saveImageButton?.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("File name:")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("OK") {
                _, _ -> run {
                    try {
                        this@Export.outFileName = input.text.toString()
                        val stream = FileOutputStream(
                                File("$sDir/Pictures/$outFileName.png"),
                                false
                        )
                        cutoutImage!!.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                stream
                        )
                        stream.close()
                        Toast.makeText(
                                this@Export,
                                "File saved!",
                                Toast.LENGTH_LONG
                        ).show()
                        val mIntent = Intent(this@Export, MainMenu::class.java)
                        this@Export.startActivity(mIntent)
                    } catch (e: IOException) {
                        Toast.makeText(
                                this@Export,
                                "Could not save file! Make sure this app has storage permissions",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            builder.setNegativeButton("Cancel") {
                dialog, _ -> dialog.cancel()
            }
            builder.show()
        }
    }
}
