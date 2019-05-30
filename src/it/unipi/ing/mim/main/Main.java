package it.unipi.ing.mim.main;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import com.mathworks.engine.*;
public class Main {

	public static void main(String[] args) throws IOException {
		// example for running Matlab code
        MatlabEngine eng;
        Main main = new Main();
		try {
//			eng = MatlabEngine.startMatlab();
//			eng.putVariable("x", 7.0);
//	        eng.putVariable("y", 3.0);
//	        eng.eval("z = complex(x, y);");
//	        double x = eng.getVariable("x");
//	        System.out.println(x);
	        
	        // Example for scan a directory
	        List<Mat> images = main.scanImgDirectory();
	        for (Mat image : images) System.out.println(image.toString());
        }
		catch(Exception e) {}
	        
//		} catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	private List<Mat> scanImgDirectory() throws IOException {
		List<Mat> images = new LinkedList<Mat>();
		long i = 1;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Parameters.imgDir)) {
	        for (Path directory : ds) {
	        	DirectoryStream<Path> childs = Files.newDirectoryStream(directory);
	        	for (Path file : childs) {
	        		if (!Files.isDirectory(file) && file.toString().toLowerCase().endsWith(".jpg")) {
		            		Mat image = imread(file.toString());
			            	images.add(image);
			            	System.out.println((i++) + " " + image.toString());
	            	}
	        	}
            }
        }
        return images;
    }
}
