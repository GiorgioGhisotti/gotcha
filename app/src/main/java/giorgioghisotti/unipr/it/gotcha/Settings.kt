package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.io.IOException

class Settings : AppCompatActivity() {

    private var dnnSwitch: Switch? = null
    private var sdkSwitch: Switch? = null
    private var scalePicturesSwitch: Switch? = null
    private var setDownloadUrlButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        /** whether the app should use mobilenetssd or yolov3 */
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
                        sharedPreferencesDnn.edit().putString(
                                "dnn_type",
                                resources.getString(R.string.YOLO)
                        ).apply()
                    } else {
                        sharedPreferencesDnn.edit().putString("dnn_type", resources.getString(R.string.MobileNetSSD)).apply()
                    }
                }
        ))

        /** whether the app should use native code or not */
        sdkSwitch = findViewById(R.id.ndk_switch)
        val sharedPreferencesSdk = this.getSharedPreferences("ndk", Context.MODE_PRIVATE)
        sdkSwitch?.isChecked = !sharedPreferencesSdk.getBoolean("use_ndk", true)
        sdkSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    sharedPreferencesSdk.edit().putBoolean("use_ndk", !isChecked).apply()
                }
        ))

        /** whether the app should scale pictures for processing */
        scalePicturesSwitch = findViewById(R.id.scale_pictures_switch)
        val sharedPreferencesScale = this.getSharedPreferences("scale", Context.MODE_PRIVATE)
        scalePicturesSwitch?.isChecked = !sharedPreferencesScale.getBoolean("scale", true)
        scalePicturesSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener(
                fun(_: CompoundButton, isChecked: Boolean) {
                    sharedPreferencesScale.edit().putBoolean("scale", !isChecked).apply()
                }
        ))

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        /** file download urls */
        setDownloadUrlButton = findViewById(R.id.download_url_button)
        val sharedPreferencesDownload = this.getSharedPreferences(
                "download_url",
                Context.MODE_PRIVATE
        )
        setDownloadUrlButton?.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Weight file server URLs:")
            val dialogView = this.layoutInflater.inflate(R.layout.url_dialog, null)

            val mobileNetSSDModelUrlInput = dialogView.findViewById<EditText>(R.id.MNSSDM)
            mobileNetSSDModelUrlInput.inputType = InputType.TYPE_CLASS_TEXT
            mobileNetSSDModelUrlInput.setText(
                    sharedPreferencesDownload.getString(
                        resources.getString(R.string.MobileNetSSD_model_name),
                        resources.getString(R.string.MobileNetSSD_model_file_url)
                )
            )
            val mobileNetSSDConfigUrlInput = dialogView.findViewById<EditText>(R.id.MNSSDC)
            mobileNetSSDConfigUrlInput.inputType = InputType.TYPE_CLASS_TEXT
            mobileNetSSDConfigUrlInput.setText(
                    sharedPreferencesDownload.getString(
                            resources.getString(R.string.MobileNetSSD_config_name),
                            resources.getString(R.string.MobileNetSSD_config_file_url)
                    )
            )
            val yoloV3ModelUrlInput = dialogView.findViewById<EditText>(R.id.YV3M)
            yoloV3ModelUrlInput.inputType = InputType.TYPE_CLASS_TEXT
            yoloV3ModelUrlInput.setText(
                    sharedPreferencesDownload.getString(
                            resources.getString(R.string.YOLOv3_model_name),
                            resources.getString(R.string.YOLOv3_model_file_url)
                    )
            )
            val yoloV3ConfigUrlInput = dialogView.findViewById<EditText>(R.id.YV3C)
            yoloV3ConfigUrlInput.inputType = InputType.TYPE_CLASS_TEXT
            yoloV3ConfigUrlInput.setText(
                    sharedPreferencesDownload.getString(
                            resources.getString(R.string.YOLOv3_config_name),
                            resources.getString(R.string.YOLOv3_config_file_url)
                    )
            )

            builder.setView(dialogView)
            builder.setPositiveButton("OK") {
                _, _ -> run {
                    try {
                        sharedPreferencesDownload.edit().putString(
                                resources.getString(R.string.MobileNetSSD_model_name),
                                mobileNetSSDModelUrlInput.text.toString()
                        ).apply()
                        sharedPreferencesDownload.edit().putString(
                                resources.getString(R.string.MobileNetSSD_config_name),
                                mobileNetSSDConfigUrlInput.text.toString()
                        ).apply()
                        sharedPreferencesDownload.edit().putString(
                                resources.getString(R.string.YOLOv3_model_name),
                                yoloV3ModelUrlInput.text.toString()
                        ).apply()
                        sharedPreferencesDownload.edit().putString(
                                resources.getString(R.string.YOLOv3_config_name),
                                yoloV3ConfigUrlInput.text.toString()
                        ).apply()
                    } catch (e: IOException) {
                        Toast.makeText(
                                this@Settings,
                                "Could not update preferences! This is a bug, please report it.",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            builder.setNegativeButton("Cancel") {
                dialog, _ -> dialog.cancel()
            }
            builder.setNeutralButton("Defaults"){
                _, _ -> //TODO
            }
            builder.show()
        }
    }
}
