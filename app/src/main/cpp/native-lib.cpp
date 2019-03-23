#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <list>
#include <opencv2/stitching.hpp>
#include <opencv2/saliency.hpp>
#include "ximgproc.hpp"
#include "ximgproc/slic.hpp"

#include <vector>
#include <math.h>
#include <float.h>
#include <map>

extern "C" {
void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_getSaliencyMap(
        JNIEnv *env,
        jobject instance,
        jlong inputImgAddr,
        jlong outputImgAddr
) {
    cv::Mat inputImg = *(cv::Mat*) inputImgAddr;
    cv::Mat outputImg = *(cv::Mat*) outputImgAddr;
    cv::cvtColor(inputImg, outputImg, 44);

    cv::Mat average = outputImg.clone();

    /* Blur the image with 3x3 Gaussian */
    GaussianBlur(outputImg, outputImg, cv::Size(5, 5), 0, 0);

    cv::Ptr<cv::saliency::Saliency> fine = cv::saliency::StaticSaliencyFineGrained::create();
    if (!fine->computeSaliency(outputImg, outputImg)) {
        std::cout << "error computing saliency" << std::endl;
    }
    normalize(outputImg, outputImg, 0, 255, cv::NORM_MINMAX, 0, cv::noArray());

    /* Find average value */
    double li = 0;
    double a = 0;
    double b = 0;
    int numpxls = outputImg.rows * outputImg.cols;
    int i, j;
    cv::Vec<unsigned char, 3> *p;
    for (i = 0; i < outputImg.rows; ++i) {
        p = outputImg.ptr<cv::Vec<unsigned char, 3> >(i);
        for (j = 0; j < outputImg.cols; ++j) {
            li += p[j][0];
            a += p[j][1];
            b += p[j][2];
        }
    }

    li = li / (numpxls);
    a = a / (numpxls);
    b = b / (numpxls);

    cv::Vec3b *q;
    cv::Mat tmpimag = cv::Mat::zeros(outputImg.rows, outputImg.cols, CV_8UC1);
    for (i = 0; i < average.rows; ++i) {
        q = outputImg.ptr<cv::Vec3b>(i);
        for (j = 0; j < average.cols; ++j) {
            q[j][0] = uchar(li - q[j][0]);
            q[j][1] = uchar(a - q[j][1]);
            q[j][2] = uchar(b - q[j][2]);
        }
    }
    outputImg = abs(outputImg);
}

void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_binarizeSaliencyMap(
        JNIEnv *env,
        jobject instance,
        jlong salMapAddr,
        jlong spxlLabelAddr,
        jlong outputImgAddr,
        jdouble Thold
) {
    const cv::Mat salMap = *(cv::Mat*) salMapAddr;
    const cv::Mat spxlLabel = *(cv::Mat*) spxlLabelAddr;
    cv::Mat outputImg = *(cv::Mat*) outputImgAddr;
    std::map<int, std::vector<int>> values;
    std::map<int, std::vector<cv::Point>> locations;
    int imgH = salMap.rows;
    int imgW = salMap.cols;

    double min, max;
    minMaxLoc(salMap, &min, &max);

    double minPxl, spxlNum;
    minMaxLoc(spxlLabel, &minPxl, &spxlNum);

    const int *imgPtr;
    const unsigned char *salPtr;
    for (int i = 0; i < imgH; i++) {
        imgPtr = spxlLabel.ptr<const int>(i);
        for (int j = 0; j < imgW; j++) {
            //int curLbl = pxlLabels.at<int>(i, j);
            int curLbl = imgPtr[j];
            //if the label is not in the std::map, add it.
            if (values.find(curLbl) == values.end()) {
                std::vector<int> tempVals;
                std::vector<cv::Point> tempPtns;
                values.insert(std::pair<int, std::vector<int>>(curLbl, tempVals));
                locations.insert(std::pair<int, std::vector<cv::Point>>(curLbl, tempPtns));
            }
            cv::Point ptn;
            ptn.x = j;
            ptn.y = i;
            salPtr = salMap.ptr<const unsigned char>(i);
            int val = salPtr[j];
            values[curLbl].push_back(val);
            locations[curLbl].push_back(ptn);
        }
    }

    //check which superpixels pass the threshold
    std::map<int, std::vector<int>>::iterator itr;
    for (itr = values.begin(); itr != values.end(); itr++) {
        int vecSize;
        double median;

        sort((itr->second).begin(), (itr->second).end());
        //find median value
        vecSize = (itr->second).size();
        //check whether odd or even length std::vector
        if (vecSize % 2 == 0) {
            median = ((itr->second)[vecSize / 2 - 1] + (itr->second)[vecSize / 2]) / 2.0;
        } else {
            median = (itr->second)[vecSize / 2];
        }
        //check if the super pixel is thresholded to foreground
        if (median > (max * Thold)) {
            //make all the relevant super pixels white.
            std::vector<cv::Point>::iterator Pts2Convs = locations[itr->first].begin();
            for (; Pts2Convs != locations[itr->first].end(); Pts2Convs++) {
                outputImg.at<unsigned char>(*Pts2Convs) = 255;
            }
        }
    }
}

cv::Rect Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_findLargestContour(
        JNIEnv *env,
        jobject instance,
        jlong imageAddr,
        jlong spxlSalAddr
) {
    const cv::Mat image = *(cv::Mat*) imageAddr;
    const cv::Mat spxlSal = *(cv::Mat*) spxlSalAddr;
    /* Find contours of the image */
    std::vector<std::vector<cv::Point> > contours;
    std::vector<cv::Vec4i> hierarchy;
    cv::findContours(spxlSal, contours, hierarchy,
                 cv::RETR_TREE, cv::CHAIN_APPROX_SIMPLE, cv::Point(0, 0));

    /* Find contours with largest area */

    double area = 0;
    std::vector<cv::Point> *tracker;
    std::vector<std::vector<cv::Point> >::iterator cntItr = contours.begin();
    for (; cntItr != contours.end(); cntItr++) {
        double tmpArea = cv::contourArea(*cntItr);
        if (tmpArea > area) {
            tracker = &(*cntItr);
            area = tmpArea;
        }
    }

    /* Find the bounding box of the largest contour */
    cv::Rect bnds = boundingRect(*tracker);

    /* pad rectangle */
    int padSize = 10;
    if (bnds.x - padSize >= 0) {
        bnds.x = bnds.x - 10;
        if (bnds.width + 2 * padSize < image.cols) {
            bnds.width = bnds.width + 2 * padSize;
        }
    }
    if (bnds.y - padSize >= 0) {
        bnds.y = bnds.y - 10;
        if (bnds.height + 2 * padSize < image.rows) {
            bnds.height = bnds.height + 2 * padSize;
        }
    }
    return bnds;
}

void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_cleanUpBinary(
        JNIEnv *env,
        jobject instance,
        cv::Mat& binaryImg
) {
    std::vector<std::vector<cv::Point> > contours;
    std::vector<cv::Vec4i> hierarchy;
    double maxArea = 0.0;
    int coolCont = -1;
    contours.clear();
    hierarchy.clear();
    findContours(binaryImg.clone(), contours, hierarchy, cv::RETR_TREE,
                 cv::CHAIN_APPROX_SIMPLE, cv::Point());
    for (int i = 0; i < int(contours.size()); i++) {
        double tmpArea = cv::contourArea(contours[i]);
        if (tmpArea > maxArea) {
            maxArea = tmpArea;
            coolCont = i;
        }
    }
    cv::Mat result;
    result = cv::Mat::zeros(binaryImg.rows, binaryImg.cols, CV_8UC1);
    drawContours(result, contours, coolCont, cv::Scalar(255), cv::FILLED, cv::LINE_4,
                 cv::noArray(), 8, cv::Point());
    binaryImg &= result;
}

void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_bitwiseAnd(
        JNIEnv *env,
        jobject instance,
        jlong inputImgAddr,
        jlong binMaskAddr
) {
    const cv::Mat inputImg = *(cv::Mat*) inputImgAddr;
    cv::Mat binMask = *(cv::Mat*) binMaskAddr;
    binMask.create(inputImg.size(), CV_8UC1);
    binMask = inputImg & 1;
    Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_cleanUpBinary(env, instance, binMask);
    binMask = binMask * 255;
    cv::cvtColor(binMask, binMask, cv::COLOR_GRAY2BGR);
}

void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_getSpxlMod(
        JNIEnv *env,
        jobject instance,
        jlong inputImgAddr,
        jlong binMaskAddr,
        jlong spxlModAddr
) {
    const cv::Mat inputImg = *(cv::Mat*) inputImgAddr;
    cv::Mat binMask = *(cv::Mat*) binMaskAddr;
    cv::Mat spxlMod = *(cv::Mat*) spxlModAddr;
    int i, j;
    cv::Vec3b *p;
    cv::Vec3b *q;
    for (i = 0; i < inputImg.rows; ++i) {
        p = binMask.ptr<cv::Vec3b>(i);
        for (j = 0; j < inputImg.cols; ++j) {
            if (p[j][0] == 255 || p[j][1] == 255 || p[j][2] == 255) {
                p[j][0] = 0;
                p[j][1] = 0;
                p[j][2] = 255;
            }
        }
    }

    for (i = 0; i < inputImg.rows; ++i) {
        p = binMask.ptr<cv::Vec3b>(i);
        q = spxlMod.ptr<cv::Vec3b>(i);
        for (j = 0; j < inputImg.cols; ++j) {
            if (p[j][0] == 255 || p[j][1] == 255 || p[j][2] == 255) {
                q[j][0] = 0;
                q[j][1] = 0;
                q[j][2] = 255;
            }
        }
    }
}

void Java_giorgioghisotti_unipr_it_gotcha_Saliency_00024Companion_getSuperPixels(
        JNIEnv *env,
        jobject instance,
        jlong inputImgAddr,
        jlong outputImgAddr,
        jlong labelsAddr,
        int pixelSize
) {
    const cv::Mat inputImg = *(cv::Mat*) inputImgAddr;
    cv::Mat outputImg = *(cv::Mat*) outputImgAddr;
    cv::Mat labels = *(cv::Mat*) labelsAddr;
    /* Blur the image with 3x3 Gaussian */
    cv::GaussianBlur(inputImg, outputImg, cv::Size(5, 5), 0, 0);

    /* Convert to LAB color space */
    cv::cvtColor(outputImg, outputImg, 44);

    /* Construct superpixel and generate mask*/
    cv::Ptr<cv::ximgproc::SuperpixelSLIC> ptr = cv::ximgproc::createSuperpixelSLIC(
            outputImg, 100,
            pixelSize, 75.0f);

    ptr->iterate(10);
    ptr->enforceLabelConnectivity(10); //reduces small fragments
    ptr->getLabelContourMask(outputImg, true); //returns contour mask
    ptr->getLabels(labels); //assigns labels
}
}
