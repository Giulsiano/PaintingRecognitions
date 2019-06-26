package it.unipi.ing.mim.main;

import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;

public class Main {
	
	public static void main(String[] args) {
		if (args.length < 2) printHelp();
		else {
		    String indexName = "-i".equals(args[args.length - 2]) ? args[args.length - 1] :
		                                                            Parameters.INDEX_NAME;
			try {
				switch (args[0]) {
				    case "search":
				        ElasticImgSearching eis = 
				                        new ElasticImgSearching(Parameters.TOP_K_QUERY, indexName);
				        eis.search(args[1]);
				        eis.close();
				        break;

				    case "index":
				        ElasticImgIndexing eii = 
			                            new ElasticImgIndexing(Parameters.TOP_K_IDX, indexName);
				        eii.indexAll(args[1]);
				        eii.close();
				        break;

				    default:
				        printHelp();
				        break;
				}
				System.out.println("Program ended");
				System.exit(0);
			} 
			catch (Exception e) {
				System.err.println("Program generated an exception: " + e.getClass().getName() 
						+ ":\n" + e.getMessage() + "\nExiting");
			}
		}
	}
	
	private static void printHelp() {
		System.out.println("Available commands:");
		System.out.println("search path/to/image [-i index_name]");
		System.out.println("\t\tAsk the program to retrieve");
		System.out.println("\t\tthe most similar image present");
		System.out.println("\t\tinto the index called index_name");
		System.out.println("\t\tinstead of default one (" + Parameters.INDEX_NAME +")");
		System.out.println();
		System.out.println("index dir [-i index_name]");
		System.out.println("\t\tStart indexing each image into dir");
		System.out.println("\t\tOptionally you can tell the program");
		System.out.println("\t\tto index objects using index_name");
		System.out.println("\t\tinstead of default one (" + Parameters.INDEX_NAME +").");
		System.out.println("\t\tdir must contain subdirectories that");
		System.out.println("\t\tcontains images to index");
		System.out.println();
		System.out.println("statistics -tp TPdir -tn TNdir [-i index_name]");
		System.out.println("\t\tStart gathering statistics using");
		System.out.println("\t\timages in TPdir to compute true");
		System.out.println("\t\tpositives and images in TNdir for");
		System.out.println("\t\tcomputing true negative. Optionally");
		System.out.println("\t\tthe user can use index_name index");
		System.out.println("\t\tinstead of default one (" + Parameters.INDEX_NAME +")");
	}

	public static void test() {
		System.out.println("INDEXING FROM GUI");
	}
}
