package giorgioghisotti.unipr.it.gotcha;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
	
	public static final float FONT_SCALE = (float)1.0;
	public static final double THRESHOLD = 0.2;
	public static final int IN_WIDTH = 300;
	public static final int IN_HEIGHT = 300;
	public static final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
	
	private static final RecognitionHandler recognitionHandler = new RecognitionHandler();
	private static final String TAG = "OpenCV/Sample/MobileNet";
	private static final String[] classNames = {"background",
			"aeroplane", "bicycle", "bird", "boat",
			"bottle", "bus", "car", "cat", "chair",
			"cow", "diningtable", "dog", "horse",
			"motorbike", "person", "pottedplant",
			"sheep", "sofa", "train", "tvmonitor"};
	private Net net;
	private CameraBridgeViewBase mOpenCvCameraView;
	private RecognitionStateReceiver mRecognitionStateReceiver;
	private int count = 0;
	private Detection detection;
	private Mat subFrame;
	
	// Initialize OpenCV manager.
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");
					mOpenCvCameraView.enableView();
					mRecognitionStateReceiver = new RecognitionStateReceiver();
					IntentFilter recognitionIntentFilter = new IntentFilter("com.example.android.threadsample.BROADCAST");
					LocalBroadcastManager.getInstance(getApplicationContext())
							.registerReceiver(mRecognitionStateReceiver, recognitionIntentFilter);
					detection = new Detection();
					subFrame = new Mat(0,0,0);
					break;
				}
				default: {
					super.onManagerConnected(status);
					break;
				}
			}
		}
	};
	
	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Set up camera listener.
		mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.CameraView);
		mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //prevent screen from locking
	}
	
	@Override
	public void onCameraViewStarted(int width, int height) {
		String proto = getPath("MobileNetSSD_deploy.prototxt", this);
		String weights = getPath("mobilenet.caffemodel", this);
		net = Dnn.readNetFromCaffe(proto, weights);
		Log.i(TAG, "Network loaded successfully");
		
	}
	
	@Override
	public void onCameraViewStopped() {
	
	}
	
	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		// Get a new frame
		Mat frame = inputFrame.rgba();
		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
		
		/**
		 * Retreive the last computation's result and send new frame data to processing service
		 * but only if it has finished its previous computation.
		 * We don't want to process frames in a queue because by the time they are done
		 * they will no longer be relevant.
		 */
		if (this.count < mRecognitionStateReceiver.getMessageCount()) {
			
			this.count = mRecognitionStateReceiver.getMessageCount();
			
			this.detection.release();
			this.detection = mRecognitionStateReceiver.getDetection();

			Intent frameIntent = new Intent();
			frameIntent.putExtra("net", net.getNativeObjAddr());
			frameIntent.putExtra("frame", frame.getNativeObjAddr());

			recognitionHandler.enqueueWork(getApplicationContext(), frameIntent);
		}
		
		/**
		 * Crop frame to fit the neural network's specification
		 */
		int cols = frame.cols();
		int rows = frame.rows();
		Size cropSize;
		if ((float)cols / rows > WH_RATIO) {
			cropSize = new Size(rows * WH_RATIO, rows);
		} else {
			cropSize = new Size(cols, cols / WH_RATIO);
		}
		int y1 = (int)(rows - cropSize.height) / 2;
		int y2 = (int)(y1 + cropSize.height);
		int x1 = (int)(cols - cropSize.width) / 2;
		int x2 = (int)(x1 + cropSize.width);
		subFrame.release();
		subFrame = frame.submat(y1, y2, x1, x2);
		cols = subFrame.cols();
		rows = subFrame.rows();
		
		/**
		 * Draw rectangles around detected objects (above a certain level of confidence)
		 */
		Mat detections = detection.getDetected();
		for (int i = 0; i < detections.rows(); ++i) {
			double confidence = detections.get(i, 2)[0];
			if (confidence > THRESHOLD) {
				System.out.println("Confidence: "+ confidence);
				int classId = (int)detections.get(i, 1)[0];
				int xLeftBottom = (int)(detections.get(i, 3)[0] * cols);
				int yLeftBottom = (int)(detections.get(i, 4)[0] * rows);
				int xRightTop   = (int)(detections.get(i, 5)[0] * cols);
				int yRightTop   = (int)(detections.get(i, 6)[0] * rows);
				
				Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom),
						new Point(xRightTop, yRightTop),
						new Scalar(0, 255, 0));
				String label = classNames[classId] + ": " + confidence;
				int[] baseLine = new int[1];
				Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, FONT_SCALE, 1, baseLine);
				
				Imgproc.rectangle(subFrame, new Point(xLeftBottom, yLeftBottom - labelSize.height),
						new Point(xLeftBottom + labelSize.width, yLeftBottom + baseLine[0]),
						new Scalar(0, 0, 0), Core.FILLED);
				
				Imgproc.putText(subFrame, label, new Point(xLeftBottom, yLeftBottom),
						Core.FONT_HERSHEY_SIMPLEX, FONT_SCALE, new Scalar(255, 255, 255));
			}
		}
		return frame;
	}
	
	/**
	 * @param file  File path in the assets folder
	 * @param context   Application context
	 * @return  Returns the file's path on the device's filesystem
	 */
	private static String getPath(String file, Context context) {
		AssetManager assetManager = context.getAssets();
		BufferedInputStream inputStream;
		try {
			inputStream = new BufferedInputStream(assetManager.open(file));
			byte[] data = new byte[inputStream.available()];
			inputStream.read(data);
			inputStream.close();
			
			File outFile = new File(context.getFilesDir(), file);
			FileOutputStream os = new FileOutputStream(outFile);
			os.write(data);
			os.close();
			
			return outFile.getAbsolutePath();
		} catch (IOException ex) {
			Log.i(TAG, "Failed to upload a file");
		}
		return "";
	}
}
