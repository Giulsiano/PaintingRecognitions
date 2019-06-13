package it.unipi.ing.mim.features;

import static org.bytedeco.opencv.global.opencv_core.CV_32FC2;
import static org.bytedeco.opencv.global.opencv_core.perspectiveTransform;
import static org.bytedeco.opencv.global.opencv_imgproc.line;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class BoundingBox {

	public static void addBoundingBox(Mat imgScene, Mat imgObject, Mat homography) {
		addBoundingBox(imgScene, imgObject, homography, 0);
	}

	public static void addBoundingBox(Mat imgScene, Mat imgObject, Mat homography, int shift) {
		Mat objCorners = new Mat(4, 1, CV_32FC2);
		Mat sceneCorners = new Mat(4, 1, CV_32FC2);

		FloatIndexer objCornersIdx = objCorners.createIndexer();

		// System.out.println(i + " - " + p1.x() +"," + p1.y());
		objCornersIdx.put(0, 0, new float[] { 0, 0 });
		objCornersIdx.put(1, 0, new float[] { imgObject.cols(), 0 });
		objCornersIdx.put(2, 0, new float[] { imgObject.cols(), imgObject.rows() });
		objCornersIdx.put(3, 0, new float[] { 0, imgObject.rows() });

		// obj_corners:input
		perspectiveTransform(objCorners, sceneCorners, homography);

		Scalar scalar = new Scalar(255, 0, 0, 0);

		FloatIndexer sceneCornersIdx = sceneCorners.createIndexer();

		int x0 = (int) sceneCornersIdx.get(0, 0, 0);
		int y0 = (int) sceneCornersIdx.get(0, 0, 1);
		x0 += shift;

		int x1 = (int) sceneCornersIdx.get(1, 0, 0);
		int y1 = (int) sceneCornersIdx.get(1, 0, 1);
		x1 += shift;
		line(imgScene, new Point(x0, y0), new Point(x1, y1), scalar);

		x0 = x1;
		y0 = y1;
		x1 = (int) sceneCornersIdx.get(2, 0, 0);
		y1 = (int) sceneCornersIdx.get(2, 0, 1);
		x1 += shift;
		line(imgScene, new Point(x0, y0), new Point(x1, y1), scalar);

		x0 = x1;
		y0 = y1;
		x1 = (int) sceneCornersIdx.get(3, 0, 0);
		y1 = (int) sceneCornersIdx.get(3, 0, 1);
		x1 += shift;
		line(imgScene, new Point(x0, y0), new Point(x1, y1), scalar);

		x0 = x1;
		y0 = y1;
		x1 = (int) sceneCornersIdx.get(0, 0, 0);
		y1 = (int) sceneCornersIdx.get(0, 0, 1);
		x1 += shift;
		line(imgScene, new Point(x0, y0), new Point(x1, y1), scalar);
	}
	
	private static OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();

	public static void imshow(String title, Mat img) {
		CanvasFrame canvas = new CanvasFrame(title);
		canvas.showImage(toMat.convert(img));
	}
}
