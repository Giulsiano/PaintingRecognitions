package it.unipi.ing.mim.main;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Class that contains all the configuration parameters for this program to work
 * @author Various
 *
 */
public class Parameters {
	public static final float threshold = 0.9f;
	public static final String DB_PATH = "data/database.db";
	public static final String MODEL = "empty model!";
	public static final Path imgDir = FileSystems.getDefault().getPath("../wikiart"); 
	public static final int MAX_DISTANCE_THRESHOLD = 35;
}
