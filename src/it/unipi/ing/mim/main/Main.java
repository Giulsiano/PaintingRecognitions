package it.unipi.ing.mim.main;

import java.nio.file.FileSystems;
import java.util.Scanner;

import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// TODO Chiedi all'utente se indicizzazione o query
		System.out.println("Ricerca o indicizzazione? (R/I)");
		//Scanner lineReader = new Scanner(System.in);
		String ans = "i";//lineReader.next();
		//lineReader.close();
		if (ans.toLowerCase().equals("r")) {
			if(System.getProperty("os.name").startsWith("Windows"))
				ElasticImgSearching.search("wikiartDEBUG\\ni-zan\\autumn-wind-in-gemstones-trees.jpg");
			else
				ElasticImgSearching.search("wikiartDEBUG/ni-zan/autumn-wind-in-gemstones-trees.jpg");
		}
		else if (ans.toLowerCase().equals("i")) {
			final String dir = System.getProperty("user.dir");
	        System.out.println("current dir = " + dir);
			//Path("wikiartDEBUG");
			ElasticImgIndexing.indexAll(args);
		}
		else {
			System.err.println("Not recognized option");
		}
		System.out.println("Program ended");
	}
}
