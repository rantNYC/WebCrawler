package controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import model.Model;
import model.ModelBuilder;

public class Controller implements IProcessListener {

	private final String CONNECTION_STRING = "Connected to: ";

	private Model userModel;
	private ModelBuilder modelBuilder;
	private ObservableSet<String> items;
	private ObservableList<String> logger;
	private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), 
																	r -> {
																		Thread t = new Thread(r);
																		t.setDaemon(true);
																		return t;
																	});

	private String currentlyProcessing;

	@FXML
	private TextField urlPathText;

	@FXML
	private Button selectFolderButton;

	@FXML
	private TextField folderPathText;

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
			this.logger = FXCollections.observableArrayList();
			this.textLoggingArea.setItems(logger);
			this.items = FXCollections.observableSet();
			if (this.userModel != null) {
				this.urlPathText.setText(this.userModel.getUrlPath());
				this.folderPathText.setText(this.userModel.getOutputFolder());
				items.addAll(this.userModel.getVisitedSites());
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
		logger.clear();
		startSingleCrawler(this.urlPathText.getText());
	}

	@FXML
	public void startAllButtonClicked() {
		for (String url : listURLVisited.getItems()) {
			startSingleCrawler(url);
		}
	}

	@FXML
	public void stopButtonClicked() {
		executor.shutdownNow();
		try {
			if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
		updateMessage("Stopped task...");
		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@FXML
	public void searchOutputFolder() {

		DirectoryChooser dc = new DirectoryChooser();

		File possibleFile = new File(this.folderPathText.getText());
		if (possibleFile.exists()) {
			dc.setInitialDirectory(possibleFile);
		} else {
			dc.setInitialDirectory(new File(System.getProperty("user.dir")));
		}

		File selectedDirectory = dc.showDialog(null);

		if (selectedDirectory != null) {
			if (selectedDirectory.exists()) {
				folderPathText.setText(selectedDirectory.getAbsolutePath());
			} else {
				folderPathText.setText("Please specify a valid path");
			}
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
		if (event.getClickCount() == 2) {
			final String userSelecteion = this.listURLVisited.getSelectionModel().getSelectedItem();
			if (!CrawlerUtilities.isNullOrEmpty(userSelecteion)) {
				this.urlPathText.setText(userSelecteion);
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
		String errorString = String.format("Error Executing the search:\n%s\n%s", exception.getMessage(),
				errors.toString());
		updateStatus(errorString);
	}

	public void copyToClipboard(MouseEvent event) {
		if (event.getButton() == MouseButton.SECONDARY) {
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();

			content.putString(textLoggingArea.getSelectionModel().getSelectedItem().toString());
			clipboard.setContent(content);
		}
	}

	@Override
	public void updateMessage(String msg) {
		updateStatus(msg);
	}

	@Override
	public void updateProgess(double progress) {
		updateProgressBar(progress);
	}
	
	private void updateProgressBar(final double progress) {
		double toUseProgress;
		if (progress < 0) {
			toUseProgress = 0;
		} else if (progress > 1) {
			toUseProgress = 1;
		} else {
			toUseProgress = progress;
		}
		
		if (Platform.isFxApplicationThread()) {
			progressBar.setProgress(toUseProgress);
			percentageLabel.setText(String.format("%.2f%%", progressBar.getProgress()*100));
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					progressBar.setProgress(toUseProgress);
					percentageLabel.setText(String.format("%.2f%%", progressBar.getProgress()*100));
				}
			});
		}
	}
	
	private void startSingleCrawler(String website) {
		this.startButton.setDisable(true);
		this.startAllButton.setDisable(true);
		updateProgressBar(0);
		setCurrentlyProcessing(website);
		items.add(getCurrentlyProcessing());
		connectionTo.setText(CONNECTION_STRING + getCurrentlyProcessing());
		
		Thread backgrounThread = new Thread(createSingleURLWorker(getCurrentlyProcessing()));
		backgrounThread.setDaemon(true);
		executor.submit(backgrounThread);
	}
	
	@FXML
	private void displayTextInputForURL() {

		TextInputDialog dialog = new TextInputDialog("URL Input");
		dialog.setHeaderText("Website URL");
		dialog.setContentText("Enter web URL to download:");

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			verifyURLPathExists(result.get());
		}

	}

	@FXML
	private void displayPopUpError() {

		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Malformed URL");
		alert.setHeaderText("The URL is malformed and cannot connect");
		alert.setContentText(String.format("URL: %s", urlPathText.getText()));

		alert.showAndWait();
	}

	private void verifyURLPathExists(String webPath) {

		if (urlPathText != null) {
			try {
				@SuppressWarnings("unused")
				URL url = new URL(webPath);
				urlPathText.setText(webPath);
			} catch (MalformedURLException ex) {
				displayPopUpError();
				urlPathText.setText("Input Manga URL");
			}
		}
	}

	private void updateText(String output) {
		synchronized (logger) {
			LocalDateTime now = LocalDateTime.now();
			String outString = String.format("%tr: %s \n", now, output);
			if (logger.size() > 500) {
				int i = 0;
				while (i < 50) {
					logger.remove(i);
					++i;
				}
			}
			logger.add(outString);
			textLoggingArea.scrollTo(textLoggingArea.getItems().size() - 1);
		}
	}

	private void saveUserPreferenceFromGUI() {
		try {
			if (modelBuilder == null)
				modelBuilder = new ModelBuilder();

			HashSet<String> userSites = new HashSet<String>();
			if (userModel != null) {
				userSites = userModel.getVisitedSites();
			}
			userModel = new Model(urlPathText.getText(), folderPathText.getText());
			userModel.setVisitedSites(userSites);
			userModel.addSite();

			if (!userModel.isEmpty())
				modelBuilder.writeModelToJSON(userModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Task<Void> createSingleURLWorker(String processURL) {
		EngineTask bw = new EngineTask(processURL, folderPathText.getText());
		bw.setListener(this);
		bw.setOnRunning(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				updateStatus("Starting the search...");
				final long startTime = System.currentTimeMillis();
				bw.setStartTime(startTime);
			}
		});
		bw.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent arg0) {
				finishTask(bw.getStartTime());
				saveUserPreferenceFromGUI();
			}

		});
		bw.setOnCancelled(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent arg0) {
				finishTask(bw.getStartTime());
				saveUserPreferenceFromGUI();
			}

		});
		bw.setOnFailed(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				finishTask(bw.getStartTime());
				updateStatus(event.getSource().getException());
			}
		});

		return bw;
	}

	private void deleteEntryFromList() {
		final String selectedString = listURLVisited.getSelectionModel().getSelectedItem();
		if (!CrawlerUtilities.isNullOrEmpty(selectedString)) {
			// listURLVisited.getItems().remove(selectedIndex);
			items.remove(selectedString);
			userModel.getVisitedSites().remove(selectedString);
		}
		saveUserPreferenceFromGUI();
	}

	private void enterEntryFromList() {
		final String userSelection = this.listURLVisited.getSelectionModel().getSelectedItem();
		if (!CrawlerUtilities.isNullOrEmpty(userSelection)) {
			this.urlPathText.setText(userSelection);
		}
	}

	private void finishTask(long startTime) {
		updateStatus("Finishing the search...");
		final long endTime = System.currentTimeMillis();
		String formatedTime = CrawlerUtilities.formatTime((endTime - startTime) / 1000);
		updateStatus(String.format("Total execution time: %s .", formatedTime));
		connectionTo.setText(CONNECTION_STRING);
		updateProgressBar(100);
		startButton.setDisable(false);
		startAllButton.setDisable(false);
	}

}
