package giorgioghisotti.unipr.it.gotcha

import android.app.Activity
import android.content.Context
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
import org.opencv.utils.Converters
import java.io.File
import java.io.IOException
import giorgioghisotti.unipr.it.gotcha.Saliency
import org.opencv.core.CvType.CV_64FC1
import org.opencv.core.CvType.CV_8UC1
import org.opencv.imgproc.Imgproc.GC_INIT_WITH_RECT
import org.opencv.imgproc.Imgproc.grabCut
import kotlin.math.abs

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

    // Initialize OpenCV manager.
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    this@ImageEditor.genPreview()
                    if (net == null) initializeNet()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    private fun initializeNet() {
        val path = sDir + "/" + resources.getString(R.string.weights_path)
        val sharedPreferences = this.getSharedPreferences("dnn", Context.MODE_PRIVATE) ?: return
        val dnn_type = sharedPreferences.getString("dnn_type", resources.getString(R.string.MobileNetSSD))
        when (dnn_type) {
            resources.getString(R.string.MobileNetSSD) -> {
                val proto = File(path + resources.getString(R.string.MobileNetSSD_config_file)).absolutePath
                val weights = File(path + resources.getString(R.string.MobileNetSSD_model_file)).absolutePath
                net = Dnn.readNetFromCaffe(proto, weights)
            }
            resources.getString(R.string.YOLO) -> {
                val cfg = File(path + resources.getString(R.string.YOLOv3_config_file)).absolutePath
                val weights = File(path + resources.getString(R.string.YOLOv3_model_file)).absolutePath
                net = Dnn.readNetFromDarknet(cfg, weights)
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
                NetProcessing(net, sourceImage)
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
                    currentImage = null
                    sourceImage = null
                    try {
                        sourceImage = MediaStore.Images.Media.getBitmap(
                                this.contentResolver, data.data
                        )
                    } catch (e: OutOfMemoryError) {
                        Toast.makeText(this, "Sorry, this image is too big!", Toast.LENGTH_LONG).show()
                    }
                    currentImage = sourceImage
                } else return

                genPreview()
            } else if(requestCode == RESULT_PICTURE && resultCode == Activity.RESULT_OK){
                if (data != null && data.data!= null && mImageView != null) {
                    sourceImage = null
                    currentImage = null
                    try {
                        sourceImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.data)
                    } catch (e: OutOfMemoryError) {
                        Toast.makeText(this, "Sorry, this image is too big!", Toast.LENGTH_LONG).show()
                    }
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

    private fun getOutputNames(net : Net) : List<String>  {
        val names : MutableList<String> = ArrayList()

        val outLayers : List<Int> = net.unconnectedOutLayers.toList()
        val layersNames : List<String> = net.getLayerNames()

        outLayers.forEach{names.add(layersNames.get(it - 1))}
        return names
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
                val bmp32: Bitmap?
                try {
                    bmp32 = bmp?.copy(Bitmap.Config.ARGB_8888, false)  //required format for bitmaptomat
                } catch (e: OutOfMemoryError) {
                    Toast.makeText(this@ImageEditor, "The image is too large!", Toast.LENGTH_LONG).show()
                    this@ImageEditor.mFindObjectButton!!.isEnabled = true
                    return
                }
                bmp = null    //avoid filling the heap
                val frame = Mat(0,0,0)
                bitmapToMat(bmp32, frame)
                bmp32?.recycle() //avoid filling the heap
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)

                val rects : MutableList<Rect> = ArrayList()

                val sharedPreferencesDnn = this@ImageEditor.getSharedPreferences("dnn", Context.MODE_PRIVATE) ?: return
                val dnn_type = sharedPreferencesDnn.getString("dnn_type", resources.getString(R.string.MobileNetSSD))
                when (dnn_type) {
                    resources.getString(R.string.MobileNetSSD) -> {
                        val blob: Mat = Dnn.blobFromImage(frame, IN_SCALE_FACTOR_MNSSD,
                                Size(IN_WIDTH_MNSSD.toDouble(), IN_HEIGHT_MNSSD.toDouble()),
                                Scalar(MEAN_VAL_MNSSD, MEAN_VAL_MNSSD, MEAN_VAL_MNSSD), true, false)
                        net.setInput(blob)

                        var detections: Mat = net.forward()
                        blob.release()
                        val tot = detections.total().toInt()
                        detections = detections.reshape(1, tot / 7)

                        /**
                         * Draw rectangles around detected objects (above a certain level of confidence)
                         */
                        for (i in 0 until detections.rows()) {
                            val confidence = detections.get(i, 2)[0]
                            if (confidence > THRESHOLD_MNSSD) {
                                println("Confidence: $confidence")
                                val classId = detections.get(i, 1)[0].toInt()
                                val xLeftBottom = (detections.get(i, 3)[0] * frame.cols()).toInt()
                                val yLeftBottom = (detections.get(i, 4)[0] * frame.rows()).toInt()
                                val xRightTop = (detections.get(i, 5)[0] * frame.cols()).toInt()
                                val yRightTop = (detections.get(i, 6)[0] * frame.rows()).toInt()

                                Imgproc.rectangle(frame, Point(xLeftBottom.toDouble(), yLeftBottom.toDouble()),
                                        Point(xRightTop.toDouble(), yRightTop.toDouble()),
                                        Scalar(0.0, 255.0, 0.0), RECT_THICKNESS*scaleFactor(frame.cols()))

                                rects.add(Rect(
                                        xLeftBottom,
                                        yRightTop - abs(yLeftBottom-yRightTop),
                                        abs(xRightTop-xLeftBottom),
                                        abs(yLeftBottom-yRightTop)
                                ))

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
                        detections.release()
                    }
                    resources.getString(R.string.YOLO) -> {
                        val blob: Mat = Dnn.blobFromImage(frame, IN_SCALE_FACTOR_YOLO,
                                Size(IN_WIDTH_YOLO.toDouble(), IN_HEIGHT_YOLO.toDouble()),
                                Scalar(MEAN_VAL_YOLO, MEAN_VAL_YOLO, MEAN_VAL_YOLO), true, false)
                        net.setInput(blob)
                        val result: MutableList<Mat> = ArrayList()
                        for (outname in getOutputNames(net)){
                            result.add(net.forward(outname))
                        }
                        blob.release()

                        val classIds : MutableList<Int> = ArrayList()
                        val confs : MutableList<Float> = ArrayList()

                        for (i in 0..result.size - 1){
                            val level = result.get(i)
                            for (j in 0..level.rows() - 1){
                                val row = level.row(j)
                                val scores = row.colRange(5, level.cols())
                                val mm = Core.minMaxLoc(scores)
                                val confidence = mm.maxVal.toFloat()
                                val classIdPoint = mm.maxLoc
                                if (confidence > THRESHOLD_YOLO){
                                    val centerX = (row.get(0,0)[0] * frame.cols()).toInt()
                                    val centerY = (row.get(0,1)[0] * frame.rows()).toInt()
                                    val width   = (row.get(0,2)[0] * frame.cols()).toInt()
                                    val height  = (row.get(0,3)[0] * frame.rows()).toInt()
                                    val left    = centerX - width  / 2
                                    val top     = centerY - height / 2


                                    classIds.add(classIdPoint.x.toInt())
                                    confs.add(confidence)
                                    rects.add(Rect(left, top, width, height))
                                }
                            }
                        }
                        // Apply non-maximum suppression procedure.
                        val nmsThresh = 0.4f
                        val confidences = MatOfFloat(Converters.vector_float_to_Mat(confs))
                        val boxesArray : Array<Rect> = rects.toTypedArray()
                        val boxes = MatOfRect(*boxesArray)
                        val indices = MatOfInt()
                        Dnn.NMSBoxes(boxes, confidences, THRESHOLD_YOLO, nmsThresh, indices)

                        // Draw result boxes:
                        val ind = indices.toArray()
                        for (i in 0..ind.size - 1){
                            val idx = ind[i]
                            val box = boxesArray[idx]
                            Imgproc.rectangle(frame, box.tl(), box.br(),
                                    Scalar(0.0,0.0,255.0),
                                    RECT_THICKNESS*scaleFactor(frame.cols()))
                        }
                        for (mat in result){
                            mat.release()
                        }
                    }
                }
                try {
                    bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888)
                } catch (e: OutOfMemoryError) {
                    Toast.makeText(this@ImageEditor, "The image is too large!", Toast.LENGTH_LONG).show()
                    this@ImageEditor.mFindObjectButton!!.isEnabled = true
                    return
                }
                matToBitmap(frame, bmp)
                if(!rects.isEmpty()){
                    val out: Mat = Mat(frame.size(), frame.type())
                    Saliency.cutObj(frame, out, rects[0])
                    bmp = null
                    bmp = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
                    matToBitmap(out, bmp)
                    out.release()
                }
                frame.release()
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
        private const val FONT_SCALE = 1.0f
        private const val THRESHOLD_MNSSD = 0.5f
        private const val THRESHOLD_YOLO = 0.5f
        private const val IN_WIDTH_MNSSD = 300
        private const val IN_HEIGHT_MNSSD = 300
        private const val IN_WIDTH_YOLO = 416
        private const val IN_HEIGHT_YOLO = 416
        private const val IN_SCALE_FACTOR_MNSSD = 0.007843
        private const val IN_SCALE_FACTOR_YOLO = 0.00392
        private const val MEAN_VAL_MNSSD = 127.5
        private const val MEAN_VAL_YOLO = 0.0
        private const val RECT_THICKNESS = 3
        private const val FONT_THICKNESS = 2
        private const val NORMAL_SIZE = 500

        private val classNames = arrayOf("background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus", "car", "cat", "chair", "cow", "diningtable", "dog", "horse", "motorbike", "person", "pottedplant", "sheep", "sofa", "train", "tvmonitor")

        private const val RESULT_LOAD_IMG = 1
        private const val RESULT_PICTURE = 2
    }
}
