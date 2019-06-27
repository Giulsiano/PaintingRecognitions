package gui;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Gui extends Application{

	private JFrame frame;
	JScrollPane sp;
	String absoluteImagePath = "";
	JTextArea txtArea = new JTextArea(200, 200);
	it.unipi.ing.mim.main.Parameters mypar = new it.unipi.ing.mim.main.Parameters();
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		//EventQueue.invokeLater(new Runnable() {

		//PlatformImpl.startup(()->{});
		
		launch(args);
		//new JFXPanel();
//		Platform.runLater(new Runnable() {
//			public void run() {
//				try {
//					Gui window = new Gui();
//					window.frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				Platform.exit();
//			}
//		});
	}

	/**
	 * Create the application.
	 */
//	public Gui() {
//		initialize();
//	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(Stage primaryStage) {
		Button indbtn = new Button();
        indbtn.setText("Start Indexing");
        indbtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Puppa forte World!");
            }
        });
        
        Button srcbtn = new Button();
        srcbtn.setText("Start Searching");
        srcbtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
            	System.out.println("Search button pressed");
				selectfile(primaryStage);
				
				if(!absoluteImagePath.equals("")) {
					ElasticImgSearching eis;
					try {
						eis = new ElasticImgSearching(mypar.TOP_K_QUERY);
						eis.search(absoluteImagePath, false);
						eis.close();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
            }
        });
        
        ScrollPane s1 = new ScrollPane();
        TextArea txtarea = new TextArea();
        
        txtArea.setEditable(false);
        
        s1.setContent(txtarea);
        
        GridPane root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(10);
        root.setVgap(10);
        root.setPadding(new Insets(25, 25, 25, 25));
        
        

        Scene scene = new Scene(root, 300, 275);
        primaryStage.setScene(scene);
        
        root.add(indbtn, 0, 0);
        root.add(srcbtn, 0, 1);
        root.add(s1, 0, 2);

        root.setGridLinesVisible(true);
        
        primaryStage.setTitle("Painting Recognition");
        primaryStage.setScene(scene);
        primaryStage.show();
	}

        
		
		
/////////////////////////////////////////////////////////////////////////////////////////		
/*		final JFXPanel panel = new JFXPanel();
		
		frame = new JFrame();
		frame.add(panel);
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JScrollPane sp = new JScrollPane(txtArea);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		txtArea.setEditable(false);
		txtArea.setLineWrap(true);
		txtArea.setWrapStyleWord(true);
		
		Button btnNewButton = new Button("Index");
		btnNewButton.setOnAction(new EventHandler<ActionEvent>(){ //addActionListener(new ActionListener() {
			public void handle(ActionEvent e) {
				Main.test();
					
				ElasticImgIndexing eii;
				try {
					eii = new ElasticImgIndexing(mypar.TOP_K_IDX);
					if(eii.isESIndexExist(mypar.INDEX_NAME)) {
						//open new window
						Alert alert = new Alert(AlertType.CONFIRMATION, "Delete " + mypar.INDEX_NAME + " ?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
						alert.showAndWait();

						if (alert.getResult() == ButtonType.YES) {
						    //do stuff
							System.out.println("Indice cancellato: puppa!");
						}
					}
					
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				txtArea.append("Start Indexing pressed\n");
								
				for(int i = 0; i<20; i++) {
					txtArea.append("we uaglio'!\n");
				}
				txtArea.append("\n");
				e.consume();
			}
		});

		frame.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				ColumnSpec.decode("450px:grow"),},
			new RowSpec[] {
				RowSpec.decode("35px"),
				RowSpec.decode("35px"),
				RowSpec.decode("200px:grow"),}));
		frame.getContentPane().add("1, 1, fill, top", btnNewButton);
		frame.getContentPane().add(sp, "1, 3, fill, fill");
		
		Button btnNewButton_1 = new Button("Search");

		btnNewButton_1.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				System.out.println("Search button pressed");
				selectfile();
				
				if(!absoluteImagePath.equals("")) {
					ElasticImgSearching eis;
					try {
						eis = new ElasticImgSearching(mypar.TOP_K_QUERY);
						eis.search(absoluteImagePath, false);
						eis.close();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				e.consume();
			}
		});
		frame.getContentPane().add(btnNewButton_1, "1, 2, fill, center");
		frame.add(btnNewButton_1, "1, 2, fill, center");

		frame.pack();

	}
	*/
	private void selectfile(Stage primaryStage) {
		System.out.println("selectFile called");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("JPEG Files", "*.jpg"));
		File selectedFile = fileChooser.showOpenDialog(primaryStage);
		if (selectedFile != null) {
			absoluteImagePath = selectedFile.getAbsolutePath();
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		initialize(primaryStage);
	}

}
