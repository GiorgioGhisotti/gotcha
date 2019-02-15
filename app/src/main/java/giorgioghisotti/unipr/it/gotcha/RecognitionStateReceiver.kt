package giorgioghisotti.unipr.it.gotcha

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecognitionStateReceiver : BroadcastReceiver() {

    private var detection: Detection = Detection()
    private var messageCount: Int = 1

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            System.out.println("*************\n\nRECEIVED DETECTIONS\n\n*************")
            this.detection = intent.getSerializableExtra(DETECTIONS) as Detection
            System.out.println(detection.subFrame.cols())
        }
        this.messageCount++
    }

    fun getDetection() : Detection {
        return this.detection
    }

    fun getMessageCount() : Int {
        return this.messageCount
    }
}