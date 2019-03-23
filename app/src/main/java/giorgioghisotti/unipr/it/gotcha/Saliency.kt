package giorgioghisotti.unipr.it.gotcha

import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC1
import org.opencv.imgproc.Imgproc.*
import java.util.*

class Saliency {
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
        //Native functions, implemented in native-lib.cpp
        external fun getSaliencyMap(inputImgAddr: Long, outputImgAddr: Long)
        external fun binarizeSaliencyMap(salMapAddr: Long, pxlLabelsAddr: Long, outputImgAddr: Long, Thold: Double)
        external fun getSuperPixels(inputImgAddr: Long, outputImgAddr: Long, labelsAddr: Long, pixelSize: Int)
        external fun findLargestContour(imageAddr: Long, spxlSalAddr: Long) : Rect
        external fun bitwiseAnd(inputImgAddr: Long, binMaskAddr: Long)
        external fun getSpxlMod(inputImgAddr: Long, binMaskAddr: Long, spxlModAddr: Long)

        const val BLUR_KERNEL_SIZE = 5.0
        const val SPXL_SIZE = 15
        const val TVAL = 0.25

        fun grabObject(image: Mat) : Vector<Mat> {
            val spxlSal: Mat = Mat.zeros(image.rows(), image.cols(), CV_8UC1)
            val spxlImg = Mat(0,0, CV_8UC1)
            val spxlLabel = Mat(0,0, CV_8UC1)
            val spxlMod = Mat(0,0, CV_8UC1)
            val salImg = Mat(0,0, CV_8UC1)
            val dilatedImg = Mat(0,0, CV_8UC1)
            val blurImage = Mat(0,0, CV_8UC1)
            val gCutImg = Mat(0,0, CV_8UC1)
            val bgd = Mat(0,0, CV_8UC1)
            val fgd = Mat(0,0, CV_8UC1)
            var binMask = Mat(0,0, CV_8UC1)
            GaussianBlur(
                    image,
                    blurImage,
                    Size(BLUR_KERNEL_SIZE, BLUR_KERNEL_SIZE),
                    0.0,
                    0.0
            )
            getSuperPixels(image.nativeObjAddr, spxlImg.nativeObjAddr, spxlLabel.nativeObjAddr, SPXL_SIZE)
            getSaliencyMap(image.nativeObjAddr, salImg.nativeObjAddr)
            binarizeSaliencyMap(salImg.nativeObjAddr, spxlLabel.nativeObjAddr, spxlSal.nativeObjAddr, TVAL)

            dilate(
                spxlSal,
                dilatedImg,
                getStructuringElement(
                    MORPH_ELLIPSE,
                    Size(2 * SPXL_SIZE.toDouble(), 2 * SPXL_SIZE.toDouble()),
                    Point(-1.0, -1.0)
                )
            )

            val bnds: Rect = findLargestContour(image.nativeObjAddr, spxlSal.nativeObjAddr)
            grabCut(blurImage, gCutImg, bnds, bgd, fgd, 3, GC_INIT_WITH_RECT)
            binMask.create(gCutImg.size(), CV_8UC1)
            bitwiseAnd(gCutImg.nativeObjAddr, binMask.nativeObjAddr)
            getSpxlMod(image.nativeObjAddr, binMask.nativeObjAddr, spxlMod.nativeObjAddr)
            val output: Vector<Mat> = Vector<Mat>().also {
                it.add(binMask)
                it.add(salImg)
                it.add(spxlMod)
                it.add(spxlSal)
                it.add(spxlImg)
            }
            return output
        }
    }
}