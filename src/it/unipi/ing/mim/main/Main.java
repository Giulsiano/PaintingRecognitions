package it.unipi.ing.mim.main;

import java.util.Scanner;

import it.unipi.ing.mim.img.elasticsearch.ElasticImgIndexing;

public class Main {
	
	public static void main(String[] args) throws Exception {
		// TODO Chiedi all'utente se indicizzazione o query
		System.out.println("Ricerca o indicizzazione? (R/I)");
		//Scanner lineReader = new Scanner(System.in);
		String ans = "i";//lineReader.next();
		//lineReader.close();
		if (ans.toLowerCase().equals("r")) {
			
		}
		else if (ans.toLowerCase().equals("i")){
			ElasticImgIndexing.indexAll(args);
		}
		else {
			System.err.println("Not recognized option");
		}
		System.out.println("Program ended");
	}
}
