package it.unipi.ing.mim.deep.tools;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.main.Centroid;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.sun.glass.ui.CommonDialogs.Type;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;

public class StreamManagement {
	
	public static void store (Object o, File f) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f, true))) { 
        	oos.writeObject(o);
		 }
	}
	
	public static <T> List<T> loadList (File file, Class<T> clazz) throws ClassNotFoundException, IOException{
		List<T> objects = new LinkedList<>();
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))){
			while (true) {
				try{
					T object = clazz.cast(ois.readObject());
					objects.add(object);
				}
				catch (EOFException c) {
					break;
				}
			}
		}
		return objects;
	}
	
	public static <K, V> Map<K, V> loadMap(File file) throws FileNotFoundException, IOException, ClassNotFoundException{
		Map<K, V> map = new HashMap<K, V>();
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {        
			map = (Map<K, V>) ois.readObject();	
		}
		return map;
	}
	
	public static <T> T load (File file, Class<T> clazz) throws FileNotFoundException, IOException, ClassNotFoundException{
		T t = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {        
			t = clazz.cast(ois.readObject());
		}
		return t;
	}
	
	public static void store(List<ImgDescriptor> ids, File storageFile) throws IOException {
		storageFile.getParentFile().mkdir();
		 try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) { 
        	oos.writeObject(ids);
		 }
	}
	
//	public static Map<String, SimpleEntry<Integer, Integer>[]> load (File postingListFile) throws FileNotFoundException, IOException, ClassNotFoundException{
//		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(postingListFile))) {        
//			return (Map<String, SimpleEntry<Integer, Integer>[]>) ois.readObject();	
//		}
//	}
//	
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
}