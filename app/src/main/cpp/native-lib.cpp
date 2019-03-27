#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <opencv2/saliency.hpp>
#include "ximgproc.hpp"
#include "ximgproc/slic.hpp"

#include <vector>
#include <math.h>
#include <float.h>
#include <map>

extern "C" {
using namespace cv;
using namespace saliency;
using namespace std;
void getSuperPixels(const Mat &inputImg, Mat &outputImg, Mat &labels, int pixelSize);
void getSaliencyMap(Mat &inputImg, Mat &outputImg);
void binarizeSaliencyMap(const Mat &salMap, const Mat &pxlLabels, Mat &outputImg, double Thold);
void dilateBinary(const Mat &binaryImg, Mat &dilation, int pixelSize);
void cleanUpBinary(Mat &binaryImg);

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
    Mat outputImg = *(Mat*) outputImgAddr;
    Rect objRect = Rect(x, y, width, height);
    Mat blurImage; //Image holds alterations
    Mat gCutImg; //super pixel image
    Mat spxlImg; //Saliency image
    Mat spxlLabel;
    Mat salImg;
    Mat dilatedImg;
    Mat fgd;
    Mat bgd;
    Mat binMask;
    Mat spxlMod = *(Mat*) outputImgAddr;

    /* get information on the image, height and width */
    int imgH = inputImg.rows;
    int imgW = inputImg.cols;

    /* Blur the image with 3x3 Gaussian */
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
                q[j][2] = 0;
            }
        }
    }
}

void getSuperPixels(
        const Mat & inputImg,
        Mat & outputImg,
        Mat & labels,
        int pixelSize
){
    /* Blur the image with 3x3 Gaussian */
    GaussianBlur(inputImg, outputImg, Size(5, 5), 0, 0);

    /* Convert to LAB color space */
    cvtColor(outputImg, outputImg, 44);

    /* Construct superpixel and generate mask*/
    Ptr<ximgproc::SuperpixelSLIC> ptr = ximgproc::createSuperpixelSLIC(
            outputImg, 100,
            pixelSize, 75.0f);

    ptr->iterate(10);
    ptr->enforceLabelConnectivity(10); //reduces small fragments
    ptr->getLabelContourMask(outputImg, true); //returns contour mask
    ptr->getLabels(labels); //assigns labels
}


void getSaliencyMap(
        Mat &inputImg,
        Mat &outputImg
){
    /* Convert to LAB color space */
    cvtColor(inputImg, outputImg, 44);
    Mat average = outputImg.clone();

    /* Blur the image with 3x3 Gaussian */
    GaussianBlur(outputImg, outputImg, Size(5, 5), 0, 0);

    Ptr<Saliency> fine = StaticSaliencyFineGrained::create();
    if(!fine->computeSaliency(outputImg, outputImg)){
        cout << "error computing saliency" << endl;
    }
    normalize(outputImg, outputImg, 0, 255, NORM_MINMAX, 0, noArray());

    /* Find average value */
    double li = 0;
    double a = 0;
    double b = 0;
    int numpxls = outputImg.rows*outputImg.cols;
    int i, j;
    Vec<unsigned char, 3>* p;
    for( i = 0; i < outputImg.rows; ++i)
    {
        p = outputImg.ptr<Vec<unsigned char, 3>>(i);
        for ( j = 0; j < outputImg.cols; ++j)
        {
            li += p[j][0];
            a += p[j][1];
            b += p[j][2];
        }
    }

    li = li/(numpxls);
    a = a/(numpxls);
    b = b/(numpxls);

    Vec3b *q;
    Mat tmpimag = Mat::zeros(outputImg.rows, outputImg.cols, CV_8UC1);
    for( i = 0; i < average.rows; ++i)
    {
        q = outputImg.ptr<Vec3b>(i);
        for ( j = 0; j < average.cols; ++j)
        {
            q[j][0] = (unsigned char)(li-q[j][0]);
            q[j][1] = (unsigned char)(a-q[j][1]);
            q[j][2] = (unsigned char)(b-q[j][2]);
        }
    }
    outputImg = abs(outputImg);
}

void binarizeSaliencyMap(const Mat &salMap, const Mat &pxlLabels,
                         Mat &outputImg, double Thold)
{
    map<int, vector<int>> values;
    map<int, vector<Point>> locations;
    int imgH = salMap.rows;
    int imgW = salMap.cols;

    double min, max;
    minMaxLoc(salMap, &min, &max);

    double minPxl, spxlNum;
    minMaxLoc(pxlLabels, &minPxl, &spxlNum);

    const int *imgPtr;
    const unsigned char *salPtr;
    for(int i = 0; i < imgH; i++){
        imgPtr = pxlLabels.ptr<const int>(i);
        for(int j = 0; j < imgW; j++){
            //int curLbl = pxlLabels.at<int>(i, j);
            int curLbl = imgPtr[j];
            //if the label is not in the map, add it.
            if(values.find(curLbl) == values.end())
            {
                vector<int> tempVals;
                vector<Point> tempPtns;
                values.insert(pair<int, vector<int>>(curLbl, tempVals));
                locations.insert(pair<int, vector<Point>>(curLbl, tempPtns));
            }
            Point ptn;
            ptn.x = j;
            ptn.y = i;
            salPtr = salMap.ptr<const unsigned char>(i);
            int val = salPtr[j];
            //int val = salMap.at<unsigned char>(i, j);
            values[curLbl].push_back(val);
            locations[curLbl].push_back(ptn);
        }
    }


    //check which superpixels pass the threshold
    map<int, vector<int>>::iterator itr;
    for(itr = values.begin(); itr != values.end(); itr++){
        int vecSize;
        double median;

        sort((itr->second).begin(), (itr->second).end());
        //find median value
        vecSize = (itr->second).size();
        //check whether odd or even length vector
        if(vecSize%2 == 0)
        {
            median = ((itr->second)[vecSize/2-1]+(itr->second)[vecSize/2])/2.0;
        }
        else
        {
            median = (itr->second)[vecSize/2];
        }
        //check if the super pixel is thresholded to foreground
        if(median>(max*Thold))
        {
            //make all the relevant super pixels white.
            vector<Point>::iterator Pts2Convs = locations[itr->first].begin();
            for(; Pts2Convs != locations[itr->first].end();Pts2Convs++)
            {
                outputImg.at<unsigned char>(*Pts2Convs) = 255;
            }
        }
    }
}

void dilateBinary(const Mat &binaryImg, Mat &dilation, int pixelSize)
{
    Mat element = getStructuringElement
            (MORPH_ELLIPSE, Size(2*pixelSize, 2*pixelSize), Point(-1, -1));
    dilate(binaryImg, dilation, element);
}

void cleanUpBinary(Mat &binaryImg)
{
    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;
    double maxArea = 0.0;
    int coolCont = -1;
    contours.clear();
    hierarchy.clear();
    findContours(binaryImg.clone(), contours, hierarchy, RETR_TREE,
                 CHAIN_APPROX_SIMPLE, Point());
    cout << contours.size() << endl;
    for(int i = 0; i < int(contours.size()); i++)
    {
        double tmpArea =	contourArea(contours[i]);
        if(tmpArea > maxArea){
            maxArea = tmpArea;
            coolCont = i;
        }
    }
    Mat result;
    result = Mat::zeros(binaryImg.rows, binaryImg.cols, CV_8UC1);
    drawContours(result, contours, coolCont, Scalar(255), FILLED, LINE_4,
                 noArray(), 8, Point());
    binaryImg &= result;
}
}
