package giorgioghisotti.unipr.it.gotcha

import org.opencv.core.Core.bitwise_and
import org.opencv.core.CvType.*
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc.*

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

        //Wrapper for native function //FAST!!
        fun ndkCut(
                inputImg: Mat,
                outputImg: Mat,
                objRect: Rect
        ) {
            cutObj(
                    inputImgAddr = inputImg.nativeObjAddr,
                    outputImgAddr = outputImg.nativeObjAddr,
                    x = objRect.x,
                    y = objRect.y,
                    width = objRect.width,
                    height = objRect.height
            )
        }

        fun sdkCut( //SLOW!!
                inputImg: Mat,
                outputImg: Mat,
                objRect: Rect
        ) {
            val binMask = Mat()
            val bgd = Mat()
            val fgd = Mat()
            val blurImage = Mat()
            val gCutImg = Mat()

            GaussianBlur(inputImg, blurImage, Size(5.0, 5.0), 0.0, 0.0)

            grabCut(blurImage, gCutImg, objRect, bgd, fgd, 3, GC_INIT_WITH_RECT)

            binMask.create(gCutImg.size(), CV_8UC1)
            val ones = Mat(gCutImg.size(), gCutImg.type())
            ones.setTo(Scalar(1.0))
            bitwise_and(gCutImg, ones, binMask)
            ones.release()

            val data = byteArrayOf(0,0,0,0)
            for (i in 0 until binMask.height()) {
                for (j in 0 until binMask.width()){
                    if (binMask.get(i, j)[0].toInt() == 0){
                        outputImg.put(i, j, data)
                    }
                }
            }
        }
    }
}