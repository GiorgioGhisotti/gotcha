#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

extern "C" {
using namespace cv;
using namespace std;

void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_cutObj(
        JNIEnv *env,
        jobject instance,
        jlong inputImgAddr,
        jlong outputImgAddr,
        int x,
        int y,
        int width,
        int height
){
    Mat inputImg = *(Mat*) inputImgAddr;
    Rect objRect = Rect(x, y, width, height);
    Mat blurImage; //Image holds alterations
    Mat gCutImg; //super pixel image
    Mat fgd;
    Mat bgd;
    Mat binMask;
    Mat spxlMod = *(Mat*) outputImgAddr;

    /* get information on the image, height and width */
    int imgH = inputImg.rows;
    int imgW = inputImg.cols;

    /* Blur the image with 5x5 Gaussian */
    GaussianBlur(inputImg, blurImage, Size(5, 5), 0, 0);

    /* Use the above rectangle to grab cut the object */
    grabCut(blurImage, gCutImg, objRect, bgd, fgd, 3, GC_INIT_WITH_RECT);
    binMask.create(gCutImg.size(), CV_8UC1);
    binMask = gCutImg & 1;

    /* place super pixel mask on image*/
    int i, j;
    Vec4b* q;
    for( i = 0; i < imgH; ++i)
    {
        q = spxlMod.ptr<Vec4b>(i);
        for ( j = 0; j < imgW; ++j){
            if(binMask.at<unsigned char>(i, j) == 0){
                q[j][0] = 0;
                q[j][1] = 0;
                q[j][2] = 0;
                q[j][3] = 0;
            }
        }
    }
}
}
