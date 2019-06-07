package it.unipi.ing.mim.deep.tools;

import it.unipi.ing.mim.deep.ImgDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;

public class FeaturesStorage {

	public static void store(List<ImgDescriptor> ids, File storageFile) throws IOException {
		storageFile.getParentFile().mkdir();
		 try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) { 
        	oos.writeObject(ids);
		 }
	}
	
	@SuppressWarnings("unchecked")
	public static List<ImgDescriptor> load(File storageFile) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storageFile))) {        
			return (List<ImgDescriptor>) ois.readObject();	
		}
	}
	
	public static void store(ImgDescriptor ids, File storageFile) throws IOException {
		 try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) { 
        	oos.writeObject(ids);
		 }
	}
	
	@SuppressWarnings("unchecked")
	public static ImgDescriptor load(Path storageFile) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storageFile.toString()))) {        
			return (ImgDescriptor) ois.readObject();	
		}
	}
	
	public static void storeFeaturesOnly(float[][] features, File storageFile) throws IOException {
		 try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) { 
			 oos.writeObject(features);
		 }
	}

}