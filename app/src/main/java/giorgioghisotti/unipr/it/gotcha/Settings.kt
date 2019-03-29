package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Switch

class Settings : AppCompatActivity() {

    private var dnnSwitch: Switch? = null
    private var sdkSwitch: Switch? = null
    private var scalePicturesSwitch: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dnnSwitch = findViewById(R.id.dnn_switch)
        val sharedPreferencesDnn = this.getSharedPreferences("dnn", Context.MODE_PRIVATE)
        val dnntype = sharedPreferencesDnn.getString("dnn_type", resources.getString(R.string.MobileNetSSD))
        when (dnntype) {
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
        val usendk = sharedPreferencesSdk.getBoolean("use_ndk", true)
        when (usendk) {
            true -> {
                sdkSwitch?.isChecked = false
            }
            false -> {
                sdkSwitch?.isChecked = true
            }
        }
        sdkSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    if (isChecked) {
                        sharedPreferencesSdk.edit().putBoolean("use_ndk", false).apply()
                    } else {
                        sharedPreferencesSdk.edit().putBoolean("use_ndk", true).apply()
                    }
                }
        ))

        scalePicturesSwitch = findViewById(R.id.scale_pictures_switch)
        val sharedPreferencesScale = this.getSharedPreferences("scale", Context.MODE_PRIVATE)
        val scale = sharedPreferencesScale.getBoolean("scale", true)
        when (scale) {
            true -> {
                scalePicturesSwitch?.isChecked = false
            }
            false -> {
                scalePicturesSwitch?.isChecked = true
            }
        }
        scalePicturesSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    if (isChecked) {
                        sharedPreferencesScale.edit().putBoolean("scale", false).apply()
                    } else {
                        sharedPreferencesScale.edit().putBoolean("scale", true).apply()
                    }
                }
        ))

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}
