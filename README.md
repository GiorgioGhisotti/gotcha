# **GOTCHA**

<h1 align=center>
<img src="logo/horizontal.png" width=40%>
</h1>

> **Table of contents**
>
> * [Description](#description)
>	* [Examples](#examples)
> * [Building and running](#building-and-running)

Description
---

Gotcha is an Android app that finds and extracts objects from pictures, allowing the user to export the resulting image as a png file. It uses OpenCV deep neural networks to distinguish objects and the GrabCut algorithm to extract objects accurately.

### Examples

![example0](img/example0.jpg?raw=true "Example 0")\
![example1](img/example1.jpg?raw=true "Example 1")\
![example2](img/example2.jpg?raw=true "Example 2")\
![example3](img/example3.jpg?raw=true "Example 3")\
![example4](img/example4.jpg?raw=true "Example 4")\
![example5](img/example5.jpg?raw=true "Example 5")\
![example6](img/example6.jpg?raw=true "Example 6")

Building and running
---

To run this you'll need to install the OpenCV Manager 3.4.3 app on your phone.

Minimum SDK: 21

To build this project you'll need the OpenCV-android-sdk folder (extracted from the [sdk zip](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.4.3/opencv-3.4.3-android-sdk.zip/download)) in the root directory of the project. You can also modify the CMakeLists.txt file to use a different path.

### Android >10
---
Due to changes to the way Android allows access to external directories the weights download feature does not work properly on android versions above 10. I may fix this in the future but in the meanwhile this problem can be circumvented by downloading the files manually and copying them to Android/data/giorgioghisotti.unipr.it.gotcha/files/weights.

**WORK IN PROGRESS**
