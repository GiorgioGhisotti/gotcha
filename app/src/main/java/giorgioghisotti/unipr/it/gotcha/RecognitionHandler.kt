package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.support.v4.app.JobIntentService
import android.support.v4.content.LocalBroadcastManager
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.BufferedInputStream

private const val REC_JOB_ID = 1000

const val BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST"
const val DETECTIONS = "OpenCV Detections"

class RecognitionHandler : JobIntentService() {

    private val classNames: Array<String> = arrayOf("background",
        "aeroplane", "bicycle", "bird", "boat",
        "bottle", "bus", "car", "cat", "chair",
        "cow", "diningtable", "dog", "horse",
        "motorbike", "person", "pottedplant",
        "sheep", "sofa", "train", "tvmonitor")
    private var workingLock : Boolean = false
    private var net : Net? = null

    override fun onHandleWork (intent: Intent) {
        this.workingLock = true //Ignore new intents while one intent is being processed
        val proto: String = intent.getStringExtra("proto")
        val weights: String = intent.getStringExtra("weights")
        this.net = Dnn.readNetFromCaffe(proto, weights)
        if (this.net == null) {
            System.out.println("NET IS NULL")
            this.workingLock = false
            return
        }

        val IN_WIDTH = 300
        val IN_HEIGHT = 300
        val WH_RATIO = 1.0
        val IN_SCALE_FACTOR = 0.007843
        val MEAN_VAL = 127.5
        val THRESHOLD = 0.2

        val dataString = intent.dataString
        System.out.println("RECEIVED INTENT")

        var detection = Detection()
        detection.frame = Mat(intent.getLongExtra("frame", 0)).clone()
        // Forward image through network.
        val blob: Mat = Dnn.blobFromImage(detection.frame, IN_SCALE_FACTOR,
        Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
        Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false)
        (net ?: return).setInput(blob)
        var detections: Mat = (net ?: return).forward()
        var cols = detection.frame.cols()
        var rows = detection.frame.rows()
        var cropSize: Size
        if (cols.toFloat() / rows > WH_RATIO) {
            cropSize = Size(rows * WH_RATIO.toDouble(), rows.toDouble());
        } else {
            cropSize = Size(cols.toDouble(), cols / WH_RATIO.toDouble());
        }
        val y1: Int = (rows - cropSize.height).toInt() / 2
        val y2: Int = (y1 + cropSize.height).toInt()
        val x1: Int = (cols - cropSize.width).toInt() / 2
        val x2: Int = (x1 + cropSize.width).toInt()
        detection.subFrame = detection.frame.submat(y1, y2, x1, x2)
        cols = detection.subFrame.cols();
        rows = detection.subFrame.rows();
        System.out.println("Service Detections total: " + detections.total());
        detections = detections.reshape(1, detections.total().toInt() / 7);

        for (i in 0..detections.rows()-1) {
            detection.confidence = detections.get(i, 2)[0]
            if (detection.confidence > THRESHOLD) {
                System.out.println("Service Confidence: "+ detection.confidence)
                var classId: Int = detections.get(i, 1)[0].toInt()
                var xLeftBottom: Int = (detections.get(i, 3)[0] * cols).toInt()
                var yLeftBottom: Int = (detections.get(i, 4)[0] * rows).toInt()
                var xRightTop: Int = (detections.get(i, 5)[0] * cols).toInt()
                var yRightTop: Int = (detections.get(i, 6)[0] * rows).toInt()
                // Draw rectangle around detected object.
                Imgproc.rectangle(detection.subFrame, Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                Point(xRightTop.toDouble(), yRightTop.toDouble()),
                Scalar(0.toDouble(), 255.toDouble(), 0.toDouble()))
                var label: String = classNames[classId] + ": " + detection.confidence
                var baseLine: IntArray = IntArray(1)
                var labelSize: Size = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine)
                // Draw background for label.
                Imgproc.rectangle(detection.subFrame, Point(xLeftBottom.toDouble(), yLeftBottom - labelSize.height),
                Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0].toDouble()),
                Scalar(255.toDouble(), 255.toDouble(), 255.toDouble()), Core.FILLED)
                // Write class name and confidence.
                Imgproc.putText(detection.subFrame, label, Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                Core.FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0.toDouble(), 0.toDouble(), 0.toDouble()));
            }
        }

        /*
         * Creates a new Intent containing a Uri object
         * BROADCAST_ACTION is a custom Intent action
         */
        val localIntent = Intent(BROADCAST_ACTION).apply {
            // Puts the status into the Intent
            putExtra(DETECTIONS, detection)
        }
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

        this.workingLock = false
    }

    fun enqueueWork(context: Context, work: Intent) {
        if (!this.workingLock) enqueueWork(context, RecognitionHandler::class.java, REC_JOB_ID, work)
    }
}