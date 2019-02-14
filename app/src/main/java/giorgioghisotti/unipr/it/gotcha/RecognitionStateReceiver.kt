package giorgioghisotti.unipr.it.gotcha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.opencv.core.Mat

class RecognitionStateReceiver : BroadcastReceiver() {

    private var detection: Detection? = null    //IMPORTANT - service is started before OpenCV is initialized
                                                //which means we can't use opencv functions here

    override fun onReceive(context: Context?, intent: Intent?) {
//       if (intent != null) {
//           this.detections = Mat(intent.getLongExtra(DETECTIONS, 0)).clone()
//       }
    }

//   fun getDetections() : Mat {
//       return this.detections
//   }
}