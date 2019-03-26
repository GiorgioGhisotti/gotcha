package giorgioghisotti.unipr.it.gotcha

import android.graphics.Bitmap
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Mat
import org.opencv.core.Rect

class Saliency {
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
        //Native functions, implemented in native-lib.cpp
        external fun cutObj(
                inputImgAddr: Long,
                outputImgAddr: Long,
                x: Int,
                y: Int,
                width: Int,
                height: Int
        )

        //Wrapper for native function
        fun cutObj(
                inputImg: Mat,
                outputImg: Mat,
                objRect: Rect
        ) {
            val inputSub = inputImg.submat(objRect)
            val bmp: Bitmap = Bitmap.createBitmap(inputSub.cols(), inputSub.rows(), Bitmap.Config.ARGB_8888)
            matToBitmap(inputSub, bmp)
            cutObj(
                    inputImgAddr = inputImg.nativeObjAddr,
                    outputImgAddr = outputImg.nativeObjAddr,
                    x = objRect.x,
                    y = objRect.y,
                    width = objRect.width,
                    height = objRect.height
            )
        }
    }
}