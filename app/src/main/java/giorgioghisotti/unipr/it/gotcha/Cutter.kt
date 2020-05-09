package giorgioghisotti.unipr.it.gotcha

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import giorgioghisotti.unipr.it.gotcha.Saliency.Companion.ndkCut
import giorgioghisotti.unipr.it.gotcha.Saliency.Companion.sdkCut
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.lang.Exception

class Cutter : AppCompatActivity() {
    private var mImageView: ImageView? = null
    private var sourceImage: Bitmap? = null
    private var currentImage: Bitmap? = null
    private var imagePreview: Bitmap? = null
    private var mCutButton: Button? = null
    private var mLeftArrowButton: Button? = null
    private var mRightArrowButton: Button? = null
    private var rects: MutableList<Rect>? = null
    private var index: Int = 0

    // Initialize OpenCV manager.
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    this@Cutter.rects = ArrayList()
                    val mRectIntArray = this@Cutter.intent.getIntegerArrayListExtra("rects")
                    for (i in mRectIntArray.indices step 4) {
                        val x = if (mRectIntArray[i] >= 0) mRectIntArray[i] else 0
                        val y = if (mRectIntArray[i+1] >= 0) mRectIntArray[i+1] else 0
                        val width = if (mRectIntArray[i+2] >= 0) mRectIntArray[i+2] else 0
                        val height = if (mRectIntArray[i+3] >= 0) mRectIntArray[i+3] else 0
                        rects!!.add(
                            Rect(
                                x,
                                y,
                                width,
                                height
                            )
                        )
                    }
                    this@Cutter.mLeftArrowButton = findViewById(R.id.left_arrow_button)
                    this@Cutter.mLeftArrowButton!!.setOnClickListener {
                        this@Cutter.index--
                        if (this@Cutter.index < 0) this@Cutter.index = this@Cutter.rects!!.indices.last
                        this@Cutter.drawRects()
                    }
                    this@Cutter.mRightArrowButton = findViewById(R.id.right_arrow_button)
                    this@Cutter.mRightArrowButton!!.setOnClickListener {
                        this@Cutter.index++
                        if (this@Cutter.index > this@Cutter.rects!!.indices.last) this@Cutter.index = 0
                        this@Cutter.drawRects()
                    }
                    this@Cutter.drawRects()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    fun drawRects() {
        val frame = Mat()
        bitmapToMat(this.imagePreview, frame)
        val factor = this.imagePreview!!.width.toDouble()/
                this.sourceImage!!.width.toDouble()
        for (i in this.rects!!.indices){
            val x = rects!![i].x.toDouble()
            val y = rects!![i].y.toDouble()
            Imgproc.rectangle(
                    frame,
                    Point(
                            x,
                            y
                    ),
                    Point(
                            (rects!![i].x.toDouble() + rects!![i].width.toDouble()) * factor,
                            (rects!![i].y.toDouble() + rects!![i].height.toDouble()) * factor
                    ),
                    if (i == index)
                        Scalar(255.0, 255.0, 127.0)
                    else
                        Scalar(127.0, 127.0, 127.0),
                    8
            )
        }
        matToBitmap(frame, this.imagePreview)
        frame.release()
        this.mImageView!!.setImageBitmap(this.imagePreview)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cutter)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mImageView = findViewById(R.id.cutter_image_view)
        if (imagePreview == null) {
            try {
                var inputStream = this.openFileInput(getString(R.string.source_file))
                sourceImage = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                inputStream = this.openFileInput(getString(R.string.preview_file))
                imagePreview = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mImageView!!.setImageBitmap(imagePreview)
        mImageView!!.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                Log.d("X", motionEvent.x.toString())
                Log.d("Y", motionEvent.y.toString())
                return true
            }
        })

        mCutButton = findViewById(R.id.cut_button)
        mCutButton!!.setOnClickListener {
            this@Cutter.mCutButton!!.isEnabled = false
            cutObj(rects!![index])
        }
    }

    private fun cutObj(rect: Rect) {
        object: Thread() {
            override fun run() {
                val frame = Mat()
                bitmapToMat(sourceImage, frame)
                var out = frame.clone()
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB)
                val sharedPreferencesSdk = this@Cutter.getSharedPreferences("ndk", Context.MODE_PRIVATE)
                val usendk = sharedPreferencesSdk.getBoolean("use_ndk", true)
                try {
                    when (usendk) {
                        true -> {
                            ndkCut(frame, out, rect)
                        }
                        false -> {
                            sdkCut(frame, out, rect)
                        }
                    }
                } catch (e : Exception) {
                    Toast.makeText(this@Cutter, "Unfortunately something went wrong...", Toast.LENGTH_LONG).show()
                    this@Cutter.finish()
                }
                frame.release()
                out = out.submat(rect)
                val bmp: Bitmap = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
                matToBitmap(out, bmp)
                out.release()
                this@Cutter.runOnUiThread {
                    this@Cutter.currentImage = bmp
                    this@Cutter.genPreview()
                    try {
                        //Write file
                        var filename = getString(R.string.preview_file)
                        var stream = this@Cutter.openFileOutput(filename, Context.MODE_PRIVATE)
                        this@Cutter.imagePreview!!.compress(Bitmap.CompressFormat.PNG, 100, stream)

                        //Cleanup
                        stream.close()

                        //Write file
                        filename = getString(R.string.cutout_file)
                        stream = this@Cutter.openFileOutput(filename, Context.MODE_PRIVATE)
                        this@Cutter.currentImage!!.compress(Bitmap.CompressFormat.PNG, 100, stream)

                        //Cleanup
                        stream.close()
                    } catch (e : Exception) {
                        e.printStackTrace()
                    }
                    this@Cutter.mCutButton!!.isEnabled = true
                    val mIntent = Intent(this@Cutter, Export::class.java)
                    this@Cutter.startActivity(mIntent)
                }
            }
        }.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        genPreview()
    }

    override fun onResume() {
        super.onResume()
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
    }

    /**
     * Scale image so it can be shown in an ImageView
     */
    private fun genPreview(){
        if (mImageView == null) return
        mImageView!!.setImageBitmap(imagePreview)
    }
}
