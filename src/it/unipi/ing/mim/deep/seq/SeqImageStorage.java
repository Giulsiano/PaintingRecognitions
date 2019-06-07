package it.unipi.ing.mim.deep.seq;

import static org.bytedeco.opencv.global.opencv_core.write;
import static org.bytedeco.opencv.global.opencv_core.read;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bytedeco.opencv.opencv_core.FileStorage;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;

public class SeqImageStorage {
	
	private final File descFile = new File(Parameters.DESCRIPTOR_FILE);
	private final File featFile = new File(Parameters.FEATURE_FILE);

	public void extractFeatures(Path imgFolder) throws FileNotFoundException{
		String filename = imgFolder.toString();
		PrintWriter featFile = new PrintWriter(this.featFile);
		KeyPointsDetector detector = new KeyPointsDetector();
		FeaturesExtraction extractor = new FeaturesExtraction();
		try {
			// For each directory into the main image directory
			ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(descFile));
			for (Path dir : Files.newDirectoryStream(imgFolder)) {
				
				// For each file into the directory
				for (Path file : Files.newDirectoryStream(dir)) {
					filename = file.toString();
					if (filename.toLowerCase().endsWith(".jpg")) {
						// Compute the descriptors of the image
						
						Mat image = imread(filename);
						KeyPointVector keypoints = detector.detectKeypoints(image);
						Mat descriptor = extractor.extractDescriptor(image, keypoints);

						// Store on file each descriptor's feature normalized. ImgDescriptor normalize
						// the matrix into the constructor
						System.out.println("Saving keypoints for " + filename);
						ImgDescriptor ids = new ImgDescriptor(descriptor, filename);
						ids.setId(filename);
					}
				}
			}
		}
		catch (IOException e) {
			System.out.println("IOException for " + filename);
		}
		featFile.close();
	}		
}
