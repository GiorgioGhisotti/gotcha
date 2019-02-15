package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.support.v4.content.LocalBroadcastManager
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net

private const val REC_JOB_ID = 1000

const val BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST"
const val DETECTIONS = "OpenCV Detections"

const val IN_WIDTH = 300
const val IN_HEIGHT = 300
const val WH_RATIO = 1.0
const val IN_SCALE_FACTOR = 0.007843
const val MEAN_VAL = 127.5


class RecognitionHandler : JobIntentService() {

    override fun onHandleWork (intent: Intent) {
        val net: Net? = Net.__fromPtr__(intent.getLongExtra("net",0))
        val detection = Detection()
        detection.frame = Mat(intent.getLongExtra("frame", 0)).clone()

        if (net == null) {
            return
        }

        // Forward image through network.
        val blob: Mat = Dnn.blobFromImage(detection.frame, IN_SCALE_FACTOR,
        Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
        Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false)
        net.setInput(blob)
        detection.detected = net.forward()
        blob.release()
        val cols = detection.frame.cols()
        val rows = detection.frame.rows()
        val cropSize: Size
        cropSize = if (cols.toFloat() / rows > WH_RATIO) {
            Size(rows * WH_RATIO, rows.toDouble())
        } else {
            Size(cols.toDouble(), cols / WH_RATIO)
        }
        val y1: Int = (rows - cropSize.height).toInt() / 2
        val y2: Int = (y1 + cropSize.height).toInt()
        val x1: Int = (cols - cropSize.width).toInt() / 2
        val x2: Int = (x1 + cropSize.width).toInt()
        detection.subFrame = detection.frame.submat(y1, y2, x1, x2)
        detection.detected = detection.detected.reshape(1, detection.detected.total().toInt() / 7)

        /**
         * Creates a new Intent containing a Uri object
         * BROADCAST_ACTION is a custom Intent action
         */
        val localIntent = Intent(BROADCAST_ACTION).apply {
            putExtra(DETECTIONS, detection)
        }

        //Send computation results to broadcast manager so they are accessible to the main activity
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }

    /**
     * Convenience call to actual enqueueWork function
     */
    fun enqueueWork(context: Context, work: Intent) {
        enqueueWork(context, RecognitionHandler::class.java, REC_JOB_ID, work)
    }
}