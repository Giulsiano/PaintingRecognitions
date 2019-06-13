package it.unipi.ing.mim.deep.tools;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.utils.AppendableObjectOutputStream;

public class StreamManagement {
	
	public static <T> void append (Object o, File f, Class<T> c) throws IOException {
		if (!f.exists()) {
			try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f, true))){}
		}
		try (AppendableObjectOutputStream oos = new AppendableObjectOutputStream(new FileOutputStream(f, true))) { 
			oos.writeObject(c.cast(o));
		}			
	}
	
	public static <T> void store (Object o, File f, Class<T> c) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f, true))) { 
        	oos.writeObject(c.cast(o));
		 }
	}
	
	public static <T> List<T> loadList (File file, Class<T> c) throws ClassNotFoundException, IOException{
		List<T> objects = new LinkedList<>();
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))){
			while (true) {
				try{
					T object = c.cast(ois.readObject());
					objects.add(object);
				}
				catch (EOFException e) {
					break;
				}
			}
		}
		return objects;
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> loadMap(File file) throws FileNotFoundException, IOException, ClassNotFoundException{
		Map<K, V> map = new HashMap<K, V>();
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {        
			map = (Map<K, V>) ois.readObject();	
		}
		return map;
	}
	
	public static <T> T load (File file, Class<T> c) throws FileNotFoundException, IOException, ClassNotFoundException{
		T t = null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {        
			t = c.cast(ois.readObject());
		}
		return t;
	}
	
	public static void store(ImgDescriptor ids, File storageFile) throws IOException {
		//storageFile.getParentFile().mkdir();
		 try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile, true))) { 
        	oos.writeObject(ids);
		 }
	}
}