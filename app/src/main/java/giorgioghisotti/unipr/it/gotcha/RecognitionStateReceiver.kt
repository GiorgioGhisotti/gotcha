package giorgioghisotti.unipr.it.gotcha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecognitionStateReceiver : BroadcastReceiver() {

    private var detection: Detection = Detection()

    override fun onReceive(context: Context?, intent: Intent?) {
       if (intent != null) {
           this.detection = intent.getSerializableExtra(DETECTIONS) as Detection
       }
    }

   fun getDetections() : Detection {
       return this.detection
   }
}