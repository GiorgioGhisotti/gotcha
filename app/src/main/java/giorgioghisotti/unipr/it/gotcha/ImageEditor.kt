package giorgioghisotti.unipr.it.gotcha

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast

class ImageEditor : AppCompatActivity() {

    var mImageView: ImageView? = null
    var mOpenImageButton: Button? = null
    var currentImage: Bitmap? = null
    var imagePreview: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor)

        mImageView = findViewById(R.id.image_editor_view)
        mOpenImageButton = findViewById(R.id.select_image_button)
        mOpenImageButton!!.setOnClickListener {
            getImgFromGallery()
        }
        if (mImageView != null) mImageView!!.setImageBitmap(imagePreview)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        genPreview()
    }

    override fun onResume() {
        super.onResume()
        genPreview()
    }

    private fun getImgFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try{
            if(requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK){
                if (data != null && data.data != null && mImageView != null) {
                    currentImage = MediaStore.Images.Media.getBitmap(
                            this.contentResolver, data.data
                    )
                } else return

                genPreview()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Something went wrong: "+e.toString(), Toast.LENGTH_LONG).show()
        }

    }

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

    companion object {

        private const val RESULT_LOAD_IMG = 1

    }
}
