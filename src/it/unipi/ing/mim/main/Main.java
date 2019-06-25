package it.unipi.ing.mim.main;

import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;

public class Main {
	
	public static void main(String[] args) throws Exception {
		System.out.println("Ricerca o indicizzazione? (R/I)");
		//Scanner lineReader = new Scanner(System.in);

		for (int i = 1; i < 2; ++i) {
			String ans = (i == 0)? "i":"r"; 
			//lineReader.next();
			//lineReader.close();
			
			if (ans.toLowerCase().equals("r")) {
				ElasticImgSearching eis = new ElasticImgSearching(Parameters.TOP_K_QUERY);
				eis.search(args[0], false);
				eis.close();
			}
			else if (ans.toLowerCase().equals("i")) {
				ElasticImgIndexing eii = new ElasticImgIndexing(Parameters.TOP_K_IDX);
				eii.indexAll(args);
				eii.close();
			}
			else {
				System.err.println("Not recognized option");
			}
		}
		System.out.println("Program ended");
	}
	
	public static void test() {
		System.out.println("INDEXING FROM GUI");
	}
}
