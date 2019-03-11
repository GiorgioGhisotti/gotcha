package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch

class MainMenu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val sharedPreferences: SharedPreferences = this.getSharedPreferences("sp", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("reached_menu", true).apply()

        val liveCameraButton: Button = findViewById(R.id.live_camera_button)
        liveCameraButton.setOnClickListener {
            val mIntent = Intent(this, NetCameraView::class.java)
            this.startActivity(mIntent)
        }
        val imageEditorButton: Button = findViewById(R.id.image_editor_button)
        imageEditorButton.setOnClickListener {
            val mIntent = Intent(this, ImageEditor::class.java)
            this.startActivity(mIntent)
        }
        val dnnToggleSwitch : Switch = findViewById(R.id.dnn_toggle)
        val sharedPreferencesDnn = this.getSharedPreferences("dnn", Context.MODE_PRIVATE)
        val dnn_type = sharedPreferences.getString("dnn_type", resources.getString(R.string.MobileNetSSD))
        when (dnn_type) {
            resources.getString(R.string.MobileNetSSD) -> {
                dnnToggleSwitch.isChecked = false
            }
            resources.getString(R.string.YOLO) -> {
                dnnToggleSwitch.isChecked = true
            }
        }
        dnnToggleSwitch.textOff = "MobileNetSSD"
        dnnToggleSwitch.textOn = "YOLOv3"
        dnnToggleSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(buttonView: CompoundButton, isChecked: Boolean) {
                    val sharedPreferences = this@MainMenu.getSharedPreferences("dnn", Context.MODE_PRIVATE)
                    if (isChecked) {
                        sharedPreferences.edit().putString("dnn_type", resources.getString(R.string.YOLO)).apply()
                    } else {
                        sharedPreferences.edit().putString("dnn_type", resources.getString(R.string.MobileNetSSD)).apply()
                    }
                }
        ))
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}
