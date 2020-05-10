package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
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

        val imageEditorButton: Button = findViewById(R.id.image_editor_button)
        imageEditorButton.setOnClickListener {
            val mIntent = Intent(this, ImageEditor::class.java)
            this.startActivity(mIntent)
        }
        val sharedPreferencesSkipped = this.getSharedPreferences(
            "skipped",
            Context.MODE_PRIVATE
        )
        if(sharedPreferencesSkipped.getBoolean("skipped", true)) imageEditorButton.isEnabled = false

        val settingsButton: Button = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            val mIntent = Intent(this, Settings::class.java)
            this.startActivity(mIntent)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}
