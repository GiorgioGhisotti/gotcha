package giorgioghisotti.unipr.it.gotcha

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class NetCameraView : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var net: Net? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var subFrame: Mat? = null
    private var paused: Boolean = false
    private var mNetProcessing: NetProcessing? = null
    private var detections: Mat? = null
    private var frameCache: Mat? = null

    // Initialize OpenCV manager.
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    mOpenCvCameraView!!.enableView()
                    subFrame = Mat(0, 0, 0)
                    detections = Mat(0,0,0)
                    frameCache = Mat(0,0,0)
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        paused = true
    }

    public override fun onResume() {
        super.onResume()
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        // Set up camera listener.
        mOpenCvCameraView = findViewById(R.id.CameraView)
        mOpenCvCameraView!!.visibility = CameraBridgeViewBase.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)   //prevent screen from locking
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        val proto = getPath("MobileNetSSD_deploy.prototxt", this)
        val weights = getPath("mobilenet.caffemodel", this)
        net = Dnn.readNetFromCaffe(proto, weights)
        mNetProcessing = NetProcessing(net)
        Log.i(TAG, "Network loaded successfully")
    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // Get a new frame
        var frame = inputFrame.rgba()
        if (frame.cols() == 0 || frame.empty()) frame = frameCache
        else {
            frameCache!!.release()
            frameCache = frame.clone()
        }
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

        /**
         * Retreive the last computation's result and process new data
         * but only if the previous thread has completed its computation.
         * We don't want to process frames in a queue because by the time they are done
         * they will no longer be relevant.
         */
        if (!mNetProcessing!!.busy) {
            detections!!.release()
            detections = mNetProcessing!!.detections.clone()
            mNetProcessing!!.detections.release()
            mNetProcessing = NetProcessing(net)
            mNetProcessing!!.frame = frame.clone()
            mNetProcessing!!.start()
        }

        /**
         * Crop frame to fit the neural network's specification
         */
        var cols = frame.cols()
        var rows = frame.rows()
        val cropSize: Size
        cropSize = if (cols.toFloat() / rows > WH_RATIO) {
            Size((rows * WH_RATIO).toDouble(), rows.toDouble())
        } else {
            Size(cols.toDouble(), (cols / WH_RATIO).toDouble())
        }
        val y1 = (rows - cropSize.height).toInt() / 2
        val y2 = (y1 + cropSize.height).toInt()
        val x1 = (cols - cropSize.width).toInt() / 2
        val x2 = (x1 + cropSize.width).toInt()
        subFrame!!.release()
        subFrame = frame.submat(y1, y2, x1, x2)
        cols = subFrame!!.cols()
        rows = subFrame!!.rows()

        /**
         * Draw rectangles around detected objects (above a certain level of confidence)
         */
//        val detections = detection!!.detected
        for (i in 0 until detections!!.rows()) {
            val confidence = detections!!.get(i, 2)[0]
            if (confidence > THRESHOLD) {
                println("Confidence: $confidence")
                val classId = detections!!.get(i, 1)[0].toInt()
                val xLeftBottom = (detections!!.get(i, 3)[0] * cols).toInt()
                val yLeftBottom = (detections!!.get(i, 4)[0] * rows).toInt()
                val xRightTop = (detections!!.get(i, 5)[0] * cols).toInt()
                val yRightTop = (detections!!.get(i, 6)[0] * rows).toInt()

                Imgproc.rectangle(subFrame!!, Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                        Point(xRightTop.toDouble(), yRightTop.toDouble()),
                        Scalar(0.0, 255.0, 0.0))
                val label = classNames[classId] + ": " + confidence
                val baseLine = IntArray(1)
                val labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, FONT_SCALE.toDouble(), 1, baseLine)

                Imgproc.rectangle(subFrame!!, Point(xLeftBottom.toDouble(), yLeftBottom - labelSize.height),
                        Point(xLeftBottom + labelSize.width, (yLeftBottom + baseLine[0]).toDouble()),
                        Scalar(0.0, 0.0, 0.0), Core.FILLED)

                Imgproc.putText(subFrame!!, label, Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                        Core.FONT_HERSHEY_SIMPLEX, FONT_SCALE.toDouble(), Scalar(255.0, 255.0, 255.0))
            }
        }
        return frame
    }

    companion object {

        private const val FONT_SCALE = 1.0.toFloat()
        private const val THRESHOLD = 0.2
        private const val IN_WIDTH = 300
        private const val IN_HEIGHT = 300
        private const val WH_RATIO = IN_WIDTH.toFloat() / IN_HEIGHT
        private const val IN_SCALE_FACTOR = 0.007843
        private const val MEAN_VAL = 127.5

        private const val TAG = R.string.tag.toString()
        private val classNames = arrayOf("background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair", "cow", "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep", "sofa", "train", "tvmonitor")

        /**
         * @param file  File path in the assets folder
         * @param context   Application context
         * @return  Returns the file's path on the device's filesystem
         */
        private fun getPath(file: String, context: Context): String {
            val assetManager = context.assets
            val inputStream: BufferedInputStream
            try {
                inputStream = BufferedInputStream(assetManager.open(file))
                val data = ByteArray(inputStream.available())
                inputStream.read(data)
                inputStream.close()

                val outFile = File(context.filesDir, file)
                val os = FileOutputStream(outFile)
                os.write(data)
                os.close()

                return outFile.absolutePath
            } catch (ex: IOException) {
                Log.i(TAG, "Failed to upload a file")
            }

            return ""
        }
    }

    private class NetProcessing(nnet: Net?) : Thread() {
        var busy: Boolean = false
        var frame: Mat = Mat(0,0,0)
        var detections: Mat = Mat(0,0,0)
        val net: Net? = nnet

        override fun run() {
            if (frame.empty()) return
            busy = true
            super.run()
            if (net == null) return
            val blob: Mat = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                    Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
                    Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false)
            net.setInput(blob)
            detections = net.forward()
            blob.release()
            frame.release()
            detections = detections.reshape(1, detections.total().toInt() / 7)
            busy = false
        }
    }
}
