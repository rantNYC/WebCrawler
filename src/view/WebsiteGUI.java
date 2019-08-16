package view;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WebsiteGUI extends Application {

	protected final int SCREEN_WIDTH   = 900;
	protected final int SCREEN_HEIGHT  = 600;
	
	@Override
	public void start(Stage primaryStage) {
		
		Parent root;
		try {
			root = (Parent)FXMLLoader.load(getClass().getResource("Main Window.fxml"));
			primaryStage.setTitle("Manga Crawler");
			Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
			
	        primaryStage.setScene(scene);
	        primaryStage.show();
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
