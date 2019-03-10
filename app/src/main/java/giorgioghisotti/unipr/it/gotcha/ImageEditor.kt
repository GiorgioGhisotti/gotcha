package giorgioghisotti.unipr.it.gotcha

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException

class ImageEditor : AppCompatActivity() {

    private var mImageView: ImageView? = null
    private var mOpenImageButton: Button? = null
    private var mTakePictureButton: Button? = null
    private var mFindObjectButton: Button? = null
    private var sourceImage: Bitmap? = null
    private var currentImage: Bitmap? = null
    private var imagePreview: Bitmap? = null
    private var net: Net? = null
    private var busy: Boolean = false
    private val sDir = Environment.getExternalStorageDirectory().absolutePath
    private val mobileNetSSDModelPath: String = "/Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/MobileNetSSD/MobileNetSSD.caffemodel"
    private val mobileNetSSDConfigPath: String = "/Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/MobileNetSSD/MobileNetSSD.prototxt"
    private val yoloV3ModelPath: String = "/Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/YOLO/YOLOv3.weights"
    private val yoloV3ConfigPath: String = "/Android/data/giorgioghisotti.unipr.it.gotcha/files/weights/YOLO/YOLOv3.cfg"

    // Initialize OpenCV manager.
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    this@ImageEditor.genPreview()
                    if (net == null) {
                        val mobileSSDConfig = File(sDir + mobileNetSSDConfigPath)
                        val mobileSSDModel = File(sDir + mobileNetSSDModelPath)
                        val proto = mobileSSDConfig.absolutePath
                        val weights = mobileSSDModel.absolutePath
                        net = Dnn.readNetFromCaffe(proto, weights)
                    }
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mImageView = findViewById(R.id.image_editor_view)

        mOpenImageButton = findViewById(R.id.select_image_button)
        mOpenImageButton!!.setOnClickListener {
            getImgFromGallery()
        }
        mTakePictureButton = findViewById(R.id.take_picture_button)
        mTakePictureButton!!.setOnClickListener {
            dispatchTakePictureIntent()
        }
        mFindObjectButton = findViewById(R.id.find_object_button)
        mFindObjectButton!!.setOnClickListener {
            if(!busy) {
                mFindObjectButton!!.isEnabled = false
                try {
                    NetProcessing(net, sourceImage)
                } catch (e: OutOfMemoryError) {
                    Toast.makeText(this, "The image is too large!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (mImageView != null) mImageView!!.setImageBitmap(imagePreview)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        genPreview()
    }

    override fun onResume() {
        super.onResume()
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
    }

    private fun getImgFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG)
    }

    var currentPhotoPath: String = ""

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String? = "tmp"//getDateInstance().format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(this, "Error creating file!", Toast.LENGTH_LONG).show()
                    return
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            applicationContext.packageName + ".giorgioghisotti.unipr.it.gotcha.provider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, RESULT_PICTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try{
            if(requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK){
                if (data != null && data.data != null && mImageView != null) {
                    sourceImage = MediaStore.Images.Media.getBitmap(
                            this.contentResolver, data.data
                    )
                    currentImage = sourceImage
                } else return

                genPreview()
            } else if(requestCode == RESULT_PICTURE && resultCode == Activity.RESULT_OK){
                if (data != null && data.data!= null && mImageView != null) {
                    val imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.data)
                    sourceImage = imageBitmap
                    currentImage = sourceImage

                    genPreview()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Something went wrong: "+e.toString(), Toast.LENGTH_LONG).show()
        }

    }

    /**
     * Scale image so it can be shown in an ImageView
     */
    private fun genPreview(){
        if (mImageView == null || currentImage == null) return

        if (mImageView!!.height < currentImage!!.height || mImageView!!.width < currentImage!!.width) {
            val scale = mImageView!!.height.toFloat() / currentImage!!.height.toFloat()
            if (currentImage != null) {
                imagePreview = Bitmap.createScaledBitmap(currentImage!!,
                        (scale * currentImage!!.width).toInt(),
                        (scale * currentImage!!.height).toInt(), true)
            }
        } else imagePreview = currentImage

        mImageView!!.setImageBitmap(imagePreview)

    }

    private fun scaleFactor(size: Int) : Int {
        windowManager.defaultDisplay.getMetrics(metrics)
        return if(size/ (NORMAL_SIZE*metrics.density).toInt() < 1) {
            1
        } else {
            size/ (NORMAL_SIZE*metrics.density).toInt()
        }
    }

    private fun NetProcessing(nnet: Net?, bbmp: Bitmap?) {
        val net: Net? = nnet
        var bmp: Bitmap? = bbmp

        busy = true

        object: Thread() {
            override fun run() {
                super.run()
                if (bmp == null || net == null){
                    this@ImageEditor.runOnUiThread(object: Runnable {
                        override fun run() {
                            this@ImageEditor.mFindObjectButton!!.isEnabled = true
                        }
                    })
                    this@ImageEditor.busy = false
                    return
                }
                this@ImageEditor.currentImage = null    //avoid filling the heap
                val bmp32 = bmp!!.copy(Bitmap.Config.ARGB_8888, false)  //required format for bitmaptomat
                val frame = Mat(0,0,0)
                bitmapToMat(bmp32, frame)
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

                val blob: Mat = Dnn.blobFromImage(frame, IN_SCALE_FACTOR,
                        Size(IN_WIDTH.toDouble(), IN_HEIGHT.toDouble()),
                        Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false)
                net.setInput(blob)
                var detections: Mat = net.forward()
                var detections1: Mat = net.forward("detection_out")
                var detections2: Mat = net.forward("conv17_2_mbox_priorbox")
                blob.release()
                val tot = detections.total().toInt()
                val tot1 = detections1.total().toInt()
                val tot2 = detections2.total().toInt()
                detections2 = detections2.reshape(1, tot2)
                for (i in 0..tot2-1) {
                    val n: DoubleArray? = detections2.get(i, 0)
                    println("Detections2, index " + i + ": " + n!![0])
                }
                detections = detections.reshape(1, tot / 7)

                /**
                 * Draw rectangles around detected objects (above a certain level of confidence)
                 */
                for (i in 0 until detections.rows()) {
                    val confidence = detections.get(i, 2)[0]
                    if (confidence > THRESHOLD) {
                        println("Confidence: $confidence")
                        val classId = detections.get(i, 1)[0].toInt()
                        val xLeftBottom = (detections.get(i, 3)[0] * frame.cols()).toInt()
                        val yLeftBottom = (detections.get(i, 4)[0] * frame.rows()).toInt()
                        val xRightTop = (detections.get(i, 5)[0] * frame.cols()).toInt()
                        val yRightTop = (detections.get(i, 6)[0] * frame.rows()).toInt()

                        Imgproc.rectangle(frame, Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                                Point(xRightTop.toDouble(), yRightTop.toDouble()),
                                Scalar(0.0, 255.0, 0.0), RECT_THICKNESS*scaleFactor(frame.cols()))

                        val label = classNames[classId] + ": " + String.format("%.2f", confidence)
                        val baseLine = IntArray(1)
                        val labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX,
                                FONT_SCALE.toDouble()*scaleFactor(frame.cols()),
                                1, baseLine)

                        Imgproc.rectangle(frame, Point(xLeftBottom.toDouble(), yLeftBottom - labelSize.height),
                                Point(xLeftBottom + labelSize.width, (yLeftBottom + baseLine[0]).toDouble()),
                                Scalar(0.0, 0.0, 0.0), Core.FILLED)

                        Imgproc.putText(frame, label,
                                Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                                Core.FONT_HERSHEY_SIMPLEX,
                                FONT_SCALE.toDouble()*scaleFactor(frame.cols()),
                                Scalar(255.0, 255.0, 255.0),
                                FONT_THICKNESS*scaleFactor(frame.cols()))

                    }
                }
                bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888)
                matToBitmap(frame, bmp)
                frame.release()
                detections.release()
                this@ImageEditor.runOnUiThread(object: Runnable {
                    override fun run() {
                        this@ImageEditor.currentImage = bmp
                        genPreview()
                        this@ImageEditor.mFindObjectButton!!.isEnabled = true
                    }
                })
                this@ImageEditor.busy = false
            }
        }.start()
    }

    private var metrics = DisplayMetrics()

    companion object {
        private const val FONT_SCALE = 1.0.toFloat()
        private const val THRESHOLD = 0.5
        private const val IN_WIDTH = 300
        private const val IN_HEIGHT = 300
        private const val IN_SCALE_FACTOR = 0.007843
        private const val MEAN_VAL = 127.5
        private const val RECT_THICKNESS = 3
        private const val FONT_THICKNESS = 2
        private const val NORMAL_SIZE = 500

        private val classNames = arrayOf("background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair", "cow", "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep", "sofa", "train", "tvmonitor")

        private const val RESULT_LOAD_IMG = 1
        private const val RESULT_PICTURE = 2
    }
}
