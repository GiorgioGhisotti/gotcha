package giorgioghisotti.unipr.it.gotcha

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File

class Settings : AppCompatActivity() {

    private var dnnSwitch: Switch? = null
    private var sdkSwitch: Switch? = null
    private var scalePicturesSwitch: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dnnSwitch = findViewById(R.id.dnn_switch)
        val sharedPreferencesDnn = this.getSharedPreferences("dnn", Context.MODE_PRIVATE)
        when (sharedPreferencesDnn.getString(
                "dnn_type",
                resources.getString(R.string.MobileNetSSD)
        )) {
            resources.getString(R.string.MobileNetSSD) -> {
                dnnSwitch?.isChecked = false
            }
            resources.getString(R.string.YOLO) -> {
                dnnSwitch?.isChecked = true
            }
        }
        dnnSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    if (isChecked) {
                        sharedPreferencesDnn.edit().putString("dnn_type", resources.getString(R.string.YOLO)).apply()
                    } else {
                        sharedPreferencesDnn.edit().putString("dnn_type", resources.getString(R.string.MobileNetSSD)).apply()
                    }
                }
        ))

        sdkSwitch = findViewById(R.id.ndk_switch)
        val sharedPreferencesSdk = this.getSharedPreferences("ndk", Context.MODE_PRIVATE)
        sdkSwitch?.isChecked = !sharedPreferencesSdk.getBoolean("use_ndk", true)
        sdkSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    sharedPreferencesSdk.edit().putBoolean("use_ndk", !isChecked).apply()
                }
        ))

        scalePicturesSwitch = findViewById(R.id.scale_pictures_switch)
        val sharedPreferencesScale = this.getSharedPreferences("scale", Context.MODE_PRIVATE)
        scalePicturesSwitch?.isChecked = !sharedPreferencesScale.getBoolean("scale", true)
        scalePicturesSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    sharedPreferencesScale.edit().putBoolean("scale", !isChecked).apply()
                }
        ))

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    companion object {
        const val SAVE_PATH_RESULT = 0
    }
}
