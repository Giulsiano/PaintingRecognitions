package it.unipi.ing.mim.main;

import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// TODO Chiedi all'utente se indicizzazione o query
		System.out.println("Ricerca o indicizzazione? (R/I)");
		//Scanner lineReader = new Scanner(System.in);
		for (int i = 1; i < 2; ++i) {
			String ans = (i == 0)? "i":"r"; 
			//lineReader.next();
			//lineReader.close();

			if (ans.toLowerCase().equals("r")) {
				if(System.getProperty("os.name").startsWith("Windows"))
					new ElasticImgSearching(Parameters.TOP_K_QUERY).search(".\\wikiartDEBUG\\ni-zan\\bamboo-and-elegant-stone.jpg");
				else
					new ElasticImgSearching(Parameters.TOP_K_QUERY).search("wikiartDEBUG/ni-zan/autumn-wind-in-gemstones-trees.jpg");
			}
			else if (ans.toLowerCase().equals("i")) {
				ElasticImgIndexing.indexAll(args);
			}
			else {
				System.err.println("Not recognized option");
			}
		}
		System.out.println("Program ended");
	}
}
