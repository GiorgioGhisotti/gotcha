#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include <list>
#include <opencv2/stitching.hpp>
#include <opencv2/saliency.hpp>

void getSaliencyMap(cv::Mat &inputImg, cv::Mat &outputImg) {
    cv::cvtColor(inputImg, outputImg, 44);

    cv::Mat average = outputImg.clone();

    /* Blur the image with 3x3 Gaussian */
    GaussianBlur(outputImg, outputImg, cv::Size(5, 5), 0, 0);

    cv::Ptr<cv::saliency::Saliency> fine = cv::saliency::StaticSaliencyFineGrained::create();
    if(!fine->computeSaliency(outputImg, outputImg)){
        std::cout << "error computing saliency" << std::endl;
    }
    normalize(outputImg, outputImg, 0, 255, cv::NORM_MINMAX, 0, cv::noArray());

    /* Find average value */
    double li = 0;
    double a = 0;
    double b = 0;
    int numpxls = outputImg.rows*outputImg.cols;
    int i, j;
    cv::Vec<unsigned char, 3>* p;
    for( i = 0; i < outputImg.rows; ++i)
    {
        p = outputImg.ptr< cv::Vec< unsigned char, 3 > >(i);
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

    cv::Vec3b *q;
    cv::Mat tmpimag = cv::Mat::zeros(outputImg.rows, outputImg.cols, CV_8UC1);
    for( i = 0; i < average.rows; ++i)
    {
        q = outputImg.ptr<cv::Vec3b>(i);
        for ( j = 0; j < average.cols; ++j)
        {
            q[j][0] = uchar(li-q[j][0]);
            q[j][1] = uchar(a-q[j][1]);
            q[j][2] = uchar(b-q[j][2]);
        }
    }
    outputImg = abs(outputImg);
}
