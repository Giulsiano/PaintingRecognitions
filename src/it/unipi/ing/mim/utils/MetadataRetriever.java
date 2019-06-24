package it.unipi.ing.mim.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class MetadataRetriever {
	public static final String EXT = ".json";
	public static final String IMG_EXT = ".jpg";
	
	public static JsonObject readJsonFile (String path) throws FileNotFoundException, IOException, JsonException {
		String jsonPath = path.substring(0, path.length() - IMG_EXT.length()) + EXT;
		JsonObject json = null; 
		try(FileReader reader = new FileReader(jsonPath)){
			json = (JsonObject) Jsoner.deserialize(reader);
		}
		return json;
	}

}
