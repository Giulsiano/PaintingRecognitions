package gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

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
import javafx.scene.layout.Pane;
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
	String background_color = "#02517d";
	String background_color2 = "#005180";
	String border = "#8ac44a";
	String background_color3 = "white";
	String text_color2 = "#02517d";
	String text_color = "white";
	String text_color_l = "red";
	
	String style = " -fx-background-color: " + background_color3 + 
			"; -fx-text-fill: " + text_color2 +
			"; -fx-font-weight: bold " +
			"; -fx-font-size: 15pt;";
	String style2 = " -fx-background-color: " + background_color + 
			"; -fx-text-fill: " + text_color +
			"; -fx-font-weight: bold " +
			"; -fx-font-size: 15pt" +
			"; -fx-border-color: #8ac44a;";
	String style3 = " -fx-background-color: " + background_color + 
			"; -fx-text-fill: " + text_color_l +
			"; -fx-font-weight: bold " +
			"; -fx-font-size: 15pt;";

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
		indbtn.setPrefSize(buttonw, buttonh);
		indbtn.setText("Start Indexing");
		indbtn.setStyle(style);

		Button srcbtn = new Button();
		srcbtn.setPrefSize(buttonw, buttonh);
		srcbtn.setText("Start Searching");
		srcbtn.setStyle(style);

		TextField indexname = new TextField();
		indexname.setText("enter index name");
		indexname.setAlignment(Pos.CENTER);
		indexname.setStyle(style2);

		TextField pathname = new TextField();
		pathname.setText("Select path to index");
		pathname.setAlignment(Pos.CENTER);
		pathname.setStyle(style2);

		TextField filename = new TextField();
		filename.setText("Select file to search");
		filename.setAlignment(Pos.CENTER);
		filename.setStyle(style2);

		Button outputBut = new Button();
		outputBut.setText("Show the search result");
		outputBut.setStyle(style);

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

		outputBut.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event){
				System.out.println("outputPressed");
				String url="./"+(it.unipi.ing.mim.main.Parameters.RESULTS_HTML).toString();
				try {
					Desktop.getDesktop().open(new File(url));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			});


		//#8bc34a
		Label l = new Label("Ooopss...Image not found :(");

		l.setStyle(style3);
		l.setPrefHeight(50);

		/*ToggleGroup group = new ToggleGroup();
		RadioButton rbyes = new RadioButton("yes");
		RadioButton rbno = new RadioButton("no");
		rbyes.setStyle(style3);
		rbno.setStyle(style3);
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

		});*/


		Pane pane = new Pane();
		GridPane root = new GridPane();
		root.setMaxSize(500, 500);
		root.setPrefSize(500, 500);
		Scene scene = new Scene(root, 966, 652);//, Color.CHOCOLATE); //Color. ... does not work TODO
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		Image img = new Image(new File("./src/gui/background.png").toURI().toString());
		BackgroundImage bgi = new BackgroundImage(
				img, 
				BackgroundRepeat.NO_REPEAT, 
				BackgroundRepeat.NO_REPEAT, 
				BackgroundPosition.DEFAULT, 
				new BackgroundSize(BackgroundSize.AUTO,BackgroundSize.AUTO, false, false, true, false));
		root.setBackground(new Background(bgi));
		root.setPrefSize(50, 50);
		root.setAlignment(Pos.CENTER);
		root.setHgap(10);
		root.setVgap(10);
		root.setPadding(new Insets(25, 25, 25, 25));

		root.add(indexname, 0, 0);
		root.add(pathname, 0, 1);
		root.add(indbtn, 1, 1);
		root.add(filename, 0, 2);
		root.add(srcbtn, 1, 2);
		//root.add(l, 0, 3);
		//root.add(rbyes, 0, 4);
		//root.add(rbno, 1, 4);
		//        for(Node i: root.getChildren()) {
		//        	if(i instanceof Control) {
		//        		Control control = (Control) i;
		//        		control.setStyle(" -fx-background-color: "+ background_color+";");
		//        		//control.setStyle(" -fx-border-color: #00517c;");
		//        		//control.setStyle(" -fx-text-fill: #ffffff;"); //TODO NON FUNZIONSDJBHLFBHJAFGBHJADGHBJFG
		//        	}
		//        }
		//        

		//root.setGridLinesVisible(true);


		srcbtn.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				System.out.println("Search button pressed");
				//selectfile(primaryStage);

				if(!absoluteImageFile.equals("")) {
					Main.main(new String[]{"search", absoluteImageFile,"-i", indexname.getText()});
					if(Main.bestMatchFound()) {
						root.getChildren().remove(l);
						root.getChildren().remove(outputBut);
						root.add(outputBut, 0, 4);
					}
					else
					{
						root.getChildren().remove(outputBut);
						root.getChildren().remove(l);
						root.add(l, 0, 4);
					}
				}
			}
		});

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
