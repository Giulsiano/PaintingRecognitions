package it.unipi.ing.mim.deep.seq;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.tools.StreamManagement;
import it.unipi.ing.mim.features.FeaturesExtraction;
import it.unipi.ing.mim.features.KeyPointsDetector;
import it.unipi.ing.mim.main.Parameters;
import it.unipi.ing.mim.utils.MatConverter;

public class SeqImageStorage {
	
	private final File descFile = Parameters.DESCRIPTOR_FILE;
	
	private List<String> imageNames = new LinkedList<String>();
	private List<Integer> keypointPerImage = new LinkedList<Integer>();

	public void extractFeatures(Path imgFolder) throws FileNotFoundException{
		MatConverter matConverter = new MatConverter();
		String filename = imgFolder.toString();
		KeyPointsDetector detector = new KeyPointsDetector(KeyPointsDetector.SIFT_FEATURES);
		FeaturesExtraction extractor = new FeaturesExtraction(detector.getKeypointDetector());
		int imgCounter = 0;
		// For each directory into the main image directory
		try {
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
							System.out.println("Processing image #" + (++imgCounter) + ": " + filename);
							float[][] features = getRandomFeatures(descriptor);
							if (features == null || features.length == 0) {
								System.err.println("!!!! "+ filename + ": Problem computing features. Features' matrix is empty");
								continue;
							}
							// Save image name and number of extracted features
							keypointPerImage.add(features.length);
							imageNames.add(filename.toString());
							StreamManagement.store(new ImgDescriptor(features, filename), descFile);
						}
					}
				}
			}
			StreamManagement.store(keypointPerImage, Parameters.KEYPOINT_PER_IMAGE_FILE);
			StreamManagement.store(imageNames, Parameters.IMAGE_NAMES_FILE);
		}
		catch (IOException e) {
			System.err.println("IOException file " + filename);
			e.printStackTrace();
		}
	}
	
	
	public float[][] getRandomFeatures (Mat features){
		long descriptorRows = features.rows();
		float[][] randomFeatures = null;
		if (descriptorRows > 0) {
			MatConverter matConverter = new MatConverter();
			
			// Get unique random numbers from RNG
			Set<Integer> randomRows = new HashSet<Integer>(Parameters.RANDOM_KEYPOINT_NUM);
			long times = Math.min(descriptorRows, Parameters.RANDOM_KEYPOINT_NUM);
			for (long i = 0; i < times; ++i) {
				int randValue = (int) (Math.random() * descriptorRows);
				if (!randomRows.add(randValue)) --i;
			}
			// Make the matrix of whole features by taking random rows from the feature matrix
			Mat featMat = new Mat();
			randomRows.forEach((randRow) -> { featMat.push_back(features.row(randRow)); } );
			randomFeatures = matConverter.mat2float(featMat);
		}
		return randomFeatures;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getImageNames() throws FileNotFoundException, ClassNotFoundException, IOException {
		if (imageNames == null && Parameters.IMAGE_NAMES_FILE.exists()) {
			this.imageNames = (List<String>)StreamManagement.load(Parameters.IMAGE_NAMES_FILE, List.class);
		}
		else {
			this.imageNames = new LinkedList<String>();
			while (true) {
				try {
					ImgDescriptor descriptor = (ImgDescriptor) StreamManagement.load(descFile, ImgDescriptor.class);
					imageNames.add(descriptor.getId());
				}
				catch (EOFException e) {
					break;
				}
			}
		}
		return this.imageNames;
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getKeypointPerImage() throws FileNotFoundException, ClassNotFoundException, IOException {
		if (keypointPerImage == null && Parameters.KEYPOINT_PER_IMAGE_FILE.exists()) {
			this.keypointPerImage = (List<Integer>) StreamManagement.load(Parameters.KEYPOINT_PER_IMAGE_FILE, List.class);
		}
		else {
			this.keypointPerImage = new LinkedList<Integer>();
			while (true) {
				try {
					ImgDescriptor descriptor = (ImgDescriptor) StreamManagement.load(descFile, ImgDescriptor.class);
					keypointPerImage.add(descriptor.getFeatures().length);
				}
				catch (EOFException e) {
					break;
				}
			}
		}
		return keypointPerImage;
	}
}
