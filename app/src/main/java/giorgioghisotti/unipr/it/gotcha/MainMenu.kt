package giorgioghisotti.unipr.it.gotcha

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainMenu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
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
    }
}
