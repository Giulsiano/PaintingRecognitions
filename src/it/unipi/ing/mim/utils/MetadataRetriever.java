package it.unipi.ing.mim.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import it.unipi.ing.mim.img.elasticsearch.Fields;

public class MetadataRetriever {
	
	public static JsonObject readJsonFile (String path) throws FileNotFoundException, IOException, JsonException {
		JsonObject json = null; 
		try(FileReader reader = new FileReader(path)){
			json = (JsonObject) Jsoner.deserialize(reader);
		}
		return json;
	}

}
