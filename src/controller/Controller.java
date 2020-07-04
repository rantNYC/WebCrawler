package controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import controller.Engine.ChapterLink;
import controller.utilities.CrawlerUtilities;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import model.Model;
import model.ModelBuilder;

public class Controller {

	private Thread backgrounThread;
	private Model userModel;
	private ModelBuilder modelBuilder;
	private ObservableSet<String> items;
	private ObservableList<String> logger;

	private String currentlyProcessing;

	@FXML
	private Label urlPath;

	@FXML
	private Button selectFolderButton;

	@FXML
	private Label folderPath;

	@FXML
	private Button urlButton;

	@FXML
	private ListView<String> textLoggingArea; 

	@FXML
	private Label percentageLabel;

	@FXML
	private Label connectionTo;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private ListView<String> listURLVisited;

	@FXML
	private Button startButton;

	@FXML
	private Button stopButton;

	@FXML
	private Button startAllButton;

	@FXML
	public void initialize() {
		try {
			this.modelBuilder = new ModelBuilder();
			this.userModel = this.modelBuilder.readModelFromJSON();
			if(this.userModel != null) {
				this.urlPath.setText(this.userModel.getUrlPath());
				this.folderPath.setText(this.userModel.getOutputFolder());

				Set<String> visitedSites = getAllVisitedSites(this.userModel.getVisitedSites());

				items = FXCollections.observableSet(visitedSites);
				logger = FXCollections.observableArrayList();
				this.textLoggingArea.setItems(logger);
				this.listURLVisited.setItems(FXCollections.observableArrayList(items));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getCurrentlyProcessing() {
		return currentlyProcessing;
	}

	public void setCurrentlyProcessing(String currentlyProcessing) {
		this.currentlyProcessing = currentlyProcessing;
	}

	@FXML
	public void startButtonClicked() {
		startSingleCrawler(this.urlPath.getText());
	}

	private void startSingleCrawler(String website) {
		this.startButton.setDisable(true);
		this.startAllButton.setDisable(true);
		setCurrentlyProcessing(website);
		items.add(getCurrentlyProcessing());
		connectionTo.setText(connectionTo.getText() + getCurrentlyProcessing());

		backgrounThread = new Thread(createSingleURLWorker(getCurrentlyProcessing()));
		backgrounThread.setDaemon(true);
		backgrounThread.start();
	}

	@FXML
	public void startAllButtonClicked() {
		for(String url: listURLVisited.getItems()) {
			startSingleCrawler(url);
		}

	}

	@FXML
	public void stopButtonClicked() {
		backgrounThread.interrupt();
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

	public void updateProgressBar(final double progress) {
		//TODO: Check why value is greater than 100 
		String progressToString;
		if(progress <= 0) {
			progressToString = "0%";
		} else if (progress >= 100) {
			progressToString = "100%";
		}else {
			progressToString = String.format("%.2f%%", progress*100);

		}
		if (Platform.isFxApplicationThread()) {	
			progressBar.setProgress(progress);
			percentageLabel.setText(progressToString);
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					progressBar.setProgress(progress);
					percentageLabel.setText(progressToString);
				}
			});
		}
	}

	@FXML
	public void handleKeyPressed(KeyEvent event) {
		switch (event.getCode()) {
		case DELETE:
			deleteEntryFromList();
			break;
		case ENTER:
			enterEntryFromList();
		default:
			return;
		}
	}

	public void handleClick(MouseEvent event) {
		if(event.getClickCount() == 2){
			final String userSelecteion = this.listURLVisited.getSelectionModel().getSelectedItem();
			if(!CrawlerUtilities.isNullOrEmpty(userSelecteion)) {
				this.urlPath.setText(userSelecteion);
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

	public void updateStatus(Throwable exception) {
		StringWriter errors = new StringWriter();
		exception.printStackTrace(new PrintWriter(errors));
		String errorString = String.format("%s\n%s\n", exception.getMessage(), 
				errors.toString());
		updateStatus(errorString);
	}

	public void copyToClipboard(MouseEvent event) {
		if(event.getButton() == MouseButton.SECONDARY) {
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();

			content.putString(textLoggingArea.getSelectionModel().getSelectedItem().toString());
			clipboard.setContent(content);
		}
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
				@SuppressWarnings("unused")
				URL url = new URL(webPath);
				urlPath.setText(webPath);
			} catch (MalformedURLException ex) {
				displayPopUpError();
				urlPath.setText("Input Manga URL");
			}
		}
	}

	private void updateText(String output) {  
		synchronized(logger) {
			LocalDateTime now = LocalDateTime.now(); 
			String outString = String.format("%tr: %s \n", now, output);
			if(logger.size() > 500) {
				int i = 0;
				while(i < 50) {
					logger.remove(i);
					++i;
				}
			}
			logger.add(outString);
			textLoggingArea.scrollTo(textLoggingArea.getItems().size()-1);
		}
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

	private Task<Void> createSingleURLWorker(String processURL) {
		Controller ct = this;
		Task<Void> bw =  new Task<Void>() {
			@Override
			protected Void call() throws Exception {

				updateStatus("Starting the search...");

				final long startTime = System.currentTimeMillis();
				try {
					Engine site  = new Engine(processURL, folderPath.getText());
					//					site.setFxmlController(ct);
					site.crawlWebsite();
					updateStatus("Total Chapters: " + site.getTotalChapters());
					for(ChapterLink entry : site.getChapterToLink()) {
						
						progressBar.setProgress(0);
						int page = 0;
						String chapterName = entry.getChapterName();
						try {
							List<String> pages = site.processChapterToPages(entry.getChapterLink(), chapterName);
							int totalPages = pages.size();
							float growRate = (float) (1.0 / totalPages);
							updateStatus(String.format("Processing %s with total pages %d", chapterName, totalPages));

							for(String image : pages) {
								site.saveImage(image, chapterName, page+1, entry.getChapterLink());
								updateProgressBar(++page * growRate);
								if(backgrounThread.isInterrupted()) {
									updateStatus("Stopping the search by user request");
									return null;
								}
							}	
						} catch (Exception ex) {
							updateStatus("Error processing: " + chapterName);
							updateStatus(ex);
						}
					}

				} finally {
					final long endTime = System.currentTimeMillis();
					updateStatus("Finishing the search...");
					String formatedTime = CrawlerUtilities.formatTime((endTime - startTime)/1000);
					updateStatus(String.format("Total execution time: %s .", formatedTime));
				}

				return null;
			}

		};

		bw.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent arg0) {
				finishTask();
				saveUserPreferenceFromGUI();
			}

		});

		bw.setOnCancelled(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent arg0) {
				finishTask();
				saveUserPreferenceFromGUI();
			}

		});

		bw.setOnFailed(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				finishTask();
				updateStatus(event.getSource().getException());
			}
		});

		return bw;
	}

	private Set<String> getAllVisitedSites(HashSet<String> visitedSites) {
		// TODO Auto-generated method stub
		Set<String> result = new HashSet<String>();
		for(String site : visitedSites) {
			result.add(site);
		}

		return result;
	}

	private void deleteEntryFromList() {
		final String selectedString = listURLVisited.getSelectionModel().getSelectedItem();
		if(!CrawlerUtilities.isNullOrEmpty(selectedString)){
			//listURLVisited.getItems().remove(selectedIndex);
			items.remove(selectedString);
			userModel.getVisitedSites().remove(selectedString);
		}
		saveUserPreferenceFromGUI();
	}

	private void enterEntryFromList() {
		final String userSelection = this.listURLVisited.getSelectionModel().getSelectedItem();
		if(!CrawlerUtilities.isNullOrEmpty(userSelection)) {
			this.urlPath.setText(userSelection);
		}
	}

	private void finishTask() {
		updateProgressBar(0);
		startButton.setDisable(false);
		startAllButton.setDisable(false);
	}

}
