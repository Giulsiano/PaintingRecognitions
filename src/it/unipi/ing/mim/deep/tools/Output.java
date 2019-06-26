package it.unipi.ing.mim.deep.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.img.elasticsearch.Fields;
import it.unipi.ing.mim.main.Parameters;

public class Output {

	public static final int COLUMNS = 2;
	
	public static void main(String[] args) {
		Map<String, Object> m = new HashMap<>();
		m.put(Fields.ARTIST_NAME, "John Smith");
		m.put(Fields.TITLE, "John Smith");
		m.put(Fields.YEAR, "Dum�n");
		String qryUri = "file:///" + Parameters.SRC_FOLDER.getAbsolutePath() + "/photo5764702944778891453.jpg";
		String imgUri = "file:///" + Parameters.SRC_FOLDER.getAbsolutePath() + "/ni-zan/autumn-wind-in-gemstones-trees.jpg";
		File outfile = new File("john_smith.html");
		toHTML(m, qryUri, imgUri, outfile);
	}
	
	public static void toHTML(Map<String, Object> metadata, String qryURI, String imgURI, File outFile) {
		String html = "<html><head><title>Painting Recognition</title></head>\n" +
					  "<body>\n<div align='center'\"><h1>Painting Recognition</h1></div>\n" +
					  "<table align='center'>\n";
		System.out.println("Query - " + qryURI + "\tImage - " + imgURI);
		html += "<tr><th><h2><strong>Query</strong></h2></th><th><h2><strong>Result</strong></h2></th></tr>";
		for (int i = 0; i < 2; i++) {
			
			if (i % COLUMNS == 0) {
				if (i != 0)
					html += "</tr>\n";
				html += "<tr>\n";
			}
			html += "<td><img align='center' style=\"border:10px solid white\" height='500' title='" + imgURI + 
					"' src='" + ((i == 0) ? qryURI : imgURI) + "'></td>\n";
		}
		html += "</tr>\n</table>\n";
		html += "<div align='center' name=\"metadata\"><strong>Artist:</strong> "+ (String) metadata.get(Fields.ARTIST_NAME) + "</br>" +
				"<strong>Title:</strong> "+ (String) metadata.get(Fields.TITLE) + "</br>" +
				"<strong>Year:</strong> "+ (String)  metadata.get(Fields.YEAR)==null ? "Unknow": metadata.get(Fields.YEAR) + "</br>" +
				"</div>";
		html += "</body>\n</html>";
		
		try {
	        string2File(html, outFile);
			System.out.println("html generated");
        } catch (IOException e) {
	        e.printStackTrace();
        }
	}

	public static void toHTML(List<ImgDescriptor> ids, String baseURI, File outputFile) {
		String html = "<html>\n<body>\n<table align='center'>\n";

		for (int i = 0; i < ids.size(); i++) {
			System.out.println(i + " - " + (float) ids.get(i).getDist() + "\t" + ids.get(i).getId() );
			
			if (i % COLUMNS == 0) {
				if (i != 0)
					html += "</tr>\n";
				html += "<tr>\n";
			}
			html += "<td><img align='center' border='0' height='160' title='" + ids.get(i).getId() + ", dist: "
			        + ids.get(i).getDist() + "' src='" + baseURI + ids.get(i).getId() + "'></td>\n";
		}
		if (ids.size() != 0)
			html += "</tr>\n";

		html += "</table>\n</body>\n</html>";
		
		try {
	        string2File(html, outputFile);
			System.out.print("html generated");
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}

	private static void string2File(String text, File file) throws IOException {
		try (FileWriter fileWriter = new FileWriter(file)) {
			fileWriter.write(text);
		}
	}
}
