package controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import model.Model;
import model.ModelBuilder;

public class Controller {
	
	private Task<Void> backgroundWorker;
	private Model userModel;
	private ModelBuilder modelBuilder;

    @FXML
    private Label urlPath;

    @FXML
    private Button selectFolderButton;

    @FXML
    private Label folderPath;

    @FXML
    private Button stopButton;

    @FXML
    private TextArea textLoggingArea;

    @FXML
    private Label percentageLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ListView<String> listURLVisited;
    
    @FXML
    public void initialize() {
    	try {
			this.modelBuilder = new ModelBuilder();
	    	this.userModel = this.modelBuilder.readModelFromJSON();
	    	if(this.userModel != null) {
		    	this.urlPath.setText(this.userModel.getUrlPath());
		    	this.folderPath.setText(this.userModel.getOutputFolder());
		    	
		    	ArrayList<String> visitedSites = getAllVisitedSites(this.userModel.getVisitedSites());
		    	
		    	ObservableList<String> items = FXCollections.observableArrayList(visitedSites);

		    	this.listURLVisited.setItems(items);
	    	}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    

	@FXML
	public void startButtonClicked() {
		
		backgroundWorker = createWorker();
		
		backgroundWorker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent arg0) {
				saveUserPreferenceFromGUI();
			}
			
		});
		
        backgroundWorker.setOnCancelled(new EventHandler<WorkerStateEvent>() {
			
			@Override
			public void handle(WorkerStateEvent arg0) {
				saveUserPreferenceFromGUI();
				updateProgressBar(0);
		    	updateStatus("Stop requested by the user");
			}
		});
		
        Thread backgrounThread = new Thread(backgroundWorker);
        backgrounThread.setDaemon(true);
        backgrounThread.start();
	}
	
	@FXML
	public void stopButtonClicked() {
		
		backgroundWorker.cancel(true);
		progressBar.setProgress(0);
	}
	
	@FXML
	public void searchOutputFolder() {
		
		DirectoryChooser dc = new DirectoryChooser();
		
		if(this.folderPath.getText() != "") {
			dc.setInitialDirectory(new File(this.folderPath.getText()));
		}
		
		File selectedDirectory = dc.showDialog(null);
		
		if(selectedDirectory != null) {
			if(selectedDirectory.exists()) {
				folderPath.setTextFill(Color.BLACK);
				folderPath.setText(selectedDirectory.getAbsolutePath());
			} else {
				folderPath.setTextFill(Color.RED);
				folderPath.setText("Please specify a valid path");
			}
		}
	}
	
	public void updateProgressBar(double progress) {
		//TODO: Check why value is greater than 100 
		if (Platform.isFxApplicationThread()) {
			progressBar.setProgress(progress);
			percentageLabel.setText(String.format("%.2f%%", progress*100));
	    } else {
	        Platform.runLater(new Runnable() {
				@Override
				public void run() {
		        	progressBar.setProgress(progress);
					percentageLabel.setText(String.format("%.2f%%", progress*100));
				}
			});
	    }
	}
	
	private Task<Void> createWorker() {
		Controller ct = this;
		return new Task<Void>() {
		      @Override
		      protected Void call() throws Exception {
		    	  
		        updateStatus("Starting the search...");
				
				final long startTime = System.currentTimeMillis();
				
				progressBar.setProgress(0);
				Engine site  = new Engine(urlPath.getText(), folderPath.getText(), ct);

				site.crawlWebsite();
				
				final long endTime = System.currentTimeMillis();
				
				String formatedTime = formatTime((endTime - startTime)/1000);
				updateStatus(String.format("Total execution time: %s .", formatedTime));
				
				updateStatus("Finsihing the search...");
				
		        return null;
		      }
		    };
	}

	@FXML
	private void displayTextInputForURL() {
		
		TextInputDialog dialog = new TextInputDialog("URL Input");
		dialog.setHeaderText("Manga Website URL");
		dialog.setContentText("Enter web URL for the manga:");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()){
			verifyURLPathExists(result.get());
		}
		
	}
	
	@FXML
	private void displayPopUpError() {
		
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Malformed URL");
		alert.setHeaderText("The URL is malformed and cannot connect");
		alert.setContentText(String.format("URL: %s", urlPath.getText()));

		alert.showAndWait();
	}

	private void verifyURLPathExists(String webPath) {
		
		if(urlPath != null) {
			try {
				URL url = new URL(webPath);
				urlPath.setText(webPath);
			} catch (MalformedURLException ex) {
				displayPopUpError();
				urlPath.setText("Input Manga URL");
			}
		}
	}
		
	public void updateStatus(String message) {
	    if (Platform.isFxApplicationThread()) {
	    	updateText(message);
	    } else {
	        Platform.runLater(() -> updateText(message));
	    }
	}
	
	private void updateText(String output) {  
		LocalDateTime now = LocalDateTime.now(); 
		String outString = String.format("%tr: %s \n", now, output);
		textLoggingArea.appendText(outString);
	}
	
	private void saveUserPreferenceFromGUI() {
		try {
			if(modelBuilder == null) modelBuilder = new ModelBuilder();
			
			HashSet<String> userSites = new HashSet<String>();
			if(userModel != null) {
				userSites = userModel.getVisitedSites();
			}
			userModel = new Model(urlPath.getText(), folderPath.getText());
			userModel.setVisitedSites(userSites);
			userModel.addSite();
			
			if(!userModel.isEmpty()) modelBuilder.writeModelToJSON(userModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String formatTime(long seconds) {
		int hour = 0, min = 0, sec = 0;
		
		if(seconds >= 60) {
			min = (int) (seconds / 60);
			sec = (int) (seconds % 60);
		}else {
			sec = (int) seconds;
		}
		
		if(min >= 60) {
			hour = min / 60;
			min = min % 60;
		}
		
		return String.format("%d:%02d:%02d", hour, min, sec);
	}

	private ArrayList<String> getAllVisitedSites(HashSet<String> visitedSites) {
		// TODO Auto-generated method stub
		ArrayList<String> result = new ArrayList<>();
		for(String site : visitedSites) {
			result.add(site);
		}
		
		return result;
	}
}
