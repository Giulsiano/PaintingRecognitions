package gui;

import java.io.File;

import javax.swing.JScrollPane;

import it.unipi.ing.mim.main.Main;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Gui extends Application{
	
	double buttonh = 30;
	double buttonw = 200;
	int areaw = 20;

	String absoluteImagePath = "";
	String absoluteImageFile = "";
	it.unipi.ing.mim.main.Parameters mypar = new it.unipi.ing.mim.main.Parameters();
	String LABEL_COLOR = "#8bc34a";
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(Stage primaryStage) {
		Button indbtn = new Button();
		indbtn.setMaxSize(buttonw, buttonh);
		indbtn.setMinSize(buttonw, buttonh);
		indbtn.setText("Start Indexing");

		Button srcbtn = new Button();
		srcbtn.setPrefSize(buttonw, buttonh);
		srcbtn.setText("Start Searching");

		TextField indexname = new TextField();
		indexname.setText("index_name");
		
		TextField pathname = new TextField();
		pathname.setText("Select path to index");
		pathname.setAlignment(Pos.CENTER);
		
		TextField filename = new TextField();
		filename.setText("Select file to search");
		filename.setAlignment(Pos.CENTER);
		
		
        indbtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Puppa forte World!");
                //selectpath(primaryStage);
                
                if(!absoluteImagePath.equals("")) {
                	Main.main(new String[]{"index", absoluteImagePath,"-i", indexname.getText()});
				}
            }
        });
        
        srcbtn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
            	System.out.println("Search button pressed");
				//selectfile(primaryStage);
				
				if(!absoluteImageFile.equals("")) {
					Main.main(new String[]{"search", absoluteImageFile,"-i", indexname.getText()});
				}
            }
        });
                
        indexname.setOnMousePressed(EventHandler -> {
    	    //System.out.println("plutto ");
            //selectpath(primaryStage);
    	    //indexname.setText("");
        });
        
        pathname.setOnMousePressed(EventHandler -> {
    	    System.out.println("pippa ");
            selectpath(primaryStage);
            if(!absoluteImagePath.equals(""))
            	pathname.setText(absoluteImagePath);
        });
        
        filename.setOnMousePressed(EventHandler -> {
    	    System.out.println("pippa ");
            selectfile(primaryStage);
            if(!absoluteImageFile.equals(""))
            	filename.setText(absoluteImageFile);
        });

        //#8bc34a
        Label l = new Label("Do you want see the result image?");
        
        l.setTextFill(Color.web(LABEL_COLOR));
        l.setPrefHeight(50);
        
        ToggleGroup group = new ToggleGroup();
        RadioButton rbyes = new RadioButton("yes");
        RadioButton rbno = new RadioButton("no");
        rbyes.setUserData("yes");
        rbno.setUserData("no");
        rbyes.setToggleGroup(group);
        rbno.setToggleGroup(group);
        rbyes.setSelected(false);
        rbno.setSelected(true);
        
        group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
        	public void changed(ObservableValue<? extends Toggle> ov,
        			Toggle old_toggle, Toggle new_toggle) {
        		if(group.getSelectedToggle() !=null) {
        			if(group.getSelectedToggle().getUserData().toString().equals("yes")) {
        				Main.showMatchWindow = true;
        			}
        			else Main.showMatchWindow = false;
        		}
        	}
        
        });
        
        GridPane root = new GridPane();
        Image img = new Image(new File("/Users/valeriotanferna/git/PaintingRecognitions/src/gui/background.png").toURI().toString());
        BackgroundImage bgi = new BackgroundImage(
        		img, 
        		BackgroundRepeat.NO_REPEAT, 
        		BackgroundRepeat.NO_REPEAT, 
        		BackgroundPosition.DEFAULT, 
        		new BackgroundSize(BackgroundSize.AUTO,BackgroundSize.AUTO, false, false, true, false));
        root.setBackground(new Background(bgi));
        root.setPrefSize(500, 500);
        root.setAlignment(Pos.CENTER);
        root.setHgap(10);
        root.setVgap(10);
        root.setPadding(new Insets(25, 25, 25, 25));
        
        

        Scene scene = new Scene(root, 966, 652);
        primaryStage.setScene(scene);
        
        root.add(indexname, 0, 0);
        root.add(pathname, 0, 1);
        root.add(indbtn, 1, 1);
        root.add(filename, 0, 2);
        root.add(srcbtn, 1, 2);
        root.add(l, 0, 3);
        root.add(rbyes, 0, 4);
        root.add(rbno, 1, 4);

        root.setGridLinesVisible(true);

        primaryStage.setTitle("Painting Recognition");
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void selectfile(Stage primaryStage) {
		System.out.println("selectFile called");
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("JPEG Files", "*.jpg"));
		File selectedFile = fileChooser.showOpenDialog(primaryStage);
		if (selectedFile != null) {
			absoluteImageFile = selectedFile.getAbsolutePath();
		}
	}

	private void selectpath(Stage primaryStage) {
		System.out.println("selectFile called");
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Open Resource Path");
		File selectedDirectory = directoryChooser.showDialog(primaryStage);
		
		if (selectedDirectory != null) {
			absoluteImagePath = selectedDirectory.getAbsolutePath();
		}
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		initialize(primaryStage);
	}

}
