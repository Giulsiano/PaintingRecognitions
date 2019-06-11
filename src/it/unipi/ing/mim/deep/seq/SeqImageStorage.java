package it.unipi.ing.mim.deep.seq;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.Feature2D;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.utils.MatConverter;

public class SeqImageStorage {
	
	private final File descFile = Parameters.DESCRIPTOR_FILE;
	private List<String> imageNames = new LinkedList<String>();
	private List<Integer> keypointPerImage = new LinkedList<Integer>();

	public void extractFeatures(Path imgFolder) throws FileNotFoundException{
		String filename = imgFolder.toString();
		KeyPointsDetector detector = new KeyPointsDetector(KeyPointsDetector.SIFT_FEATURES);
		FeaturesExtraction extractor = new FeaturesExtraction(detector.getKeypointDetector());
		int i = 0;
		try (ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(descFile))){
			// For each directory into the main image directory
			for (Path dir : Files.newDirectoryStream(imgFolder)) {
				if(!dir.toString().endsWith(".DS_Store")) {
					// For each file into the directory
					for (Path file : Files.newDirectoryStream(dir)) {
						filename = file.toString();
						if (filename.toLowerCase().endsWith(".jpg")) {
							// Compute descriptors of the image
							Mat image = imread(filename);
							KeyPointVector keypoints = detector.detectKeypoints(image);
							Mat descriptor = extractor.extractDescriptor(image, keypoints);
							
							// Store on file each descriptor's feature normalized. ImgDescriptor normalize
							// the matrix into the constructor
							System.out.println("Processing image #" + (++i) + ": " + filename);
							float[][] features = MatConverter.mat2float(descriptor);
							if (features == null || features.length == 0) {
								System.err.println("!!!! "+ filename + ": Problem computing features. Features' matrix is empty");
								continue;
							}
							// Save image name and number of extracted features
							keypointPerImage.add(features.length);
							imageNames.add(filename.toString());
							ois.writeObject(new ImgDescriptor(features, filename));
							ois.flush();
							System.gc();
						}
					}
				}
			}
		}
		catch (IOException e) {
			System.err.println("IOException for " + filename);
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public List<String> getImageNames() {
		return imageNames;
	}

	public List<Integer> getKeypointPerImage() {
		return keypointPerImage;
	}
}
