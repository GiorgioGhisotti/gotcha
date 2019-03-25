package giorgioghisotti.unipr.it.gotcha

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

        const val BLUR_KERNEL_SIZE = 5.0
        const val SPXL_SIZE = 15
        const val TVAL = 0.25

        fun cutObj(
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
    }
}