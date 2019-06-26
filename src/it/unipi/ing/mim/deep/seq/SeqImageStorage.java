package it.unipi.ing.mim.deep.seq;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
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
import it.unipi.ing.mim.utils.ResizeImage;

public class SeqImageStorage {
	
	private final File descriptorFile = Parameters.DESCRIPTOR_FILE;
	private final File keypointFile = Parameters.KEYPOINT_PER_IMAGE_FILE;
	private final File imageNameFile = Parameters.IMAGE_NAMES_FILE;
	
	private List<String> imageNames = new LinkedList<String>();
	private List<Integer> keypointPerImage = new LinkedList<Integer>();
	
	public void extractFeatures(Path imgFolder) throws FileNotFoundException{
		String filename = imgFolder.toString();
		FeaturesExtraction extractor = new FeaturesExtraction(FeaturesExtraction.SIFT_FEATURES);
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
						    System.out.println("Processing image #" + (++imgCounter) + ": " + filename);
							Mat descriptor = extractor.extractDescriptor(imread(filename));
						    if (descriptor.empty()) {
						        System.err.println("Can't compute features for " + filename);
						        continue;
						    }
							// Get only some random feature from computed ones
							float[][] features = getRandomFeatures(descriptor);
							
							// Save image name and number of extracted features
							keypointPerImage.add(features.length);
							imageNames.add(filename);
							StreamManagement.append(new ImgDescriptor(features, filename), 
							                        descriptorFile, 
							                        ImgDescriptor.class);
						}
					}
				}
			}
			StreamManagement.store(keypointPerImage, keypointFile, List.class);
			StreamManagement.store(imageNames, imageNameFile, List.class);
		}
		catch (IOException e) {
			System.err.println("IOException file " + filename);
			e.printStackTrace();
		}
	}
	
	public float[][] getRandomFeatures (Mat descriptor){
		long descriptorRows = descriptor.rows();
		float[][] randomFeatures = null;
		MatConverter matConverter = new MatConverter();
		if (descriptorRows <= Parameters.RANDOM_KEYPOINT_NUM) {
		    randomFeatures = matConverter.mat2float(descriptor);
		}
		else {
		    // Get unique random numbers from RNG
		    Set<Integer> randomRows = new HashSet<Integer>(Parameters.RANDOM_KEYPOINT_NUM);
		    long times = Parameters.RANDOM_KEYPOINT_NUM;
		    for (long i = 0; i < times; ++i) {
		        int randValue = (int) (Math.random() * descriptorRows);
		        if (!randomRows.add(randValue)) --i;
		    }
		    // Make the matrix of whole features by taking random rows from the feature matrix
		    Mat featMat = new Mat();
		    randomRows.forEach((randRow) -> featMat.push_back(descriptor.row(randRow)));
		    randomFeatures = matConverter.mat2float(featMat);				
		}
		return randomFeatures;
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getImageNames() throws FileNotFoundException, ClassNotFoundException, IOException {
		if (imageNames.isEmpty()) {
			if (imageNameFile.exists()) {
				this.imageNames = (List<String>)StreamManagement.load(imageNameFile, List.class);
			}
			else {
				this.imageNames = new LinkedList<String>();
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile));
				while (true) {
					try {
						ImgDescriptor descriptor = (ImgDescriptor) ois.readObject();
						imageNames.add(descriptor.getId());
					}
					catch (EOFException e) {
						break;
					}
				}
				ois.close();
			}
		}
		return this.imageNames;
	}
	
	@SuppressWarnings("unchecked")
	public List<Integer> getKeypointPerImage() throws FileNotFoundException, ClassNotFoundException, IOException {
		if (keypointPerImage.isEmpty() ) {
			if (keypointFile.exists()) {
				this.keypointPerImage = (List<Integer>) StreamManagement.load(keypointFile, List.class);				
			}
			else {
				this.keypointPerImage = new LinkedList<Integer>();
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(descriptorFile));
				while (true) {
					try {
						ImgDescriptor descriptor = (ImgDescriptor) ois.readObject();
						keypointPerImage.add(descriptor.getFeatures().length);
					}
					catch (EOFException e) {
						break;
					}
				}
				ois.close();
			}
		}
		return keypointPerImage;
	}
}
