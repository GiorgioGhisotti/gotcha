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
            var mIntent = Intent(this, NetCameraView::class.java)
            this.startActivity(mIntent)
        }
    }
}
