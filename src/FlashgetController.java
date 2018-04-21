import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FlashgetController {

	@FXML
	private Label fileNameLabel;
	@FXML
	private Button downloadButton;
	@FXML
	private Button clearButton;
	@FXML
	private Button cancelButton;
	@FXML
	private TextField urlTextField;
	@FXML
	private ProgressIndicator totalPercentIndicator;
	@FXML
	private ProgressBar p1;
	@FXML
	private ProgressBar p2;
	@FXML
	private ProgressBar p3;
	@FXML
	private ProgressBar p4;
	@FXML
	private ProgressBar p5;
	@FXML
	private Label currentSizeLabel;
	@FXML
	private Label totalSizeLabel;

	private int numThread = 5;
	private Task<Long> downloadTask;
	private List<ProgressBar> progressList = new ArrayList<>();
	private List<Task<Long>> tasks = new ArrayList<>();

	@FXML
	public void initialize() {
		progressList.add(p1);
		progressList.add(p2);
		progressList.add(p3);
		progressList.add(p4);
		progressList.add(p5);

	}

	public void handlePutUrl(ActionEvent event) {
		try {
			List<DoubleBinding> temp = new ArrayList<>();
			String stringUrl = this.urlTextField.getText();
			URL url = new URL(stringUrl);
			long fileSize = url.openConnection().getContentLengthLong();
			if (fileSize == -1) {
				findNotFound();
			} else {

				long seperateFileSize = fileSize / 5;

				this.totalSizeLabel.setText(fileSize + "");
				File file = openFileChooser(stringUrl);
				System.out.println(file);

				ExecutorService executor = Executors.newFixedThreadPool(numThread);

				for (int i = 0; i < numThread; i++) {
					System.out.println(i);
					downloadTask = new DownloadTask(url, file, i * seperateFileSize, seperateFileSize);
					progressList.get(i).progressProperty().bind(downloadTask.progressProperty());

					if (i == numThread - 1) {
						downloadTask = new DownloadTask(url, file, i * seperateFileSize,
								fileSize - (i * seperateFileSize));
						progressList.get(i).progressProperty().bind(downloadTask.progressProperty());
						temp.add(downloadTask.progressProperty().multiply(fileSize - (i * seperateFileSize)));
					}
					if (i == 0) {
						temp.add(downloadTask.progressProperty().multiply(seperateFileSize));
						this.totalPercentIndicator.progressProperty().bind(downloadTask.progressProperty());
					} else if (i > 0 && i < numThread - 1) {
						temp.add(downloadTask.progressProperty().multiply(seperateFileSize));
						this.totalPercentIndicator.progressProperty().add(downloadTask.progressProperty());
					}
					tasks.add(downloadTask);
					executor.submit(downloadTask);

				}

				this.currentSizeLabel.textProperty().bind(Bindings.format("%.0f",
						(temp.get(0).add(temp.get(1)).add(temp.get(2)).add(temp.get(3)).add(temp.get(4)))));

			}
		} catch (MalformedURLException e) {
			findNotFound();
		} catch (IOException e) {
			findNotFound();
		}

	}

	public void handleClear(ActionEvent event) {
		this.urlTextField.setText("");
		this.fileNameLabel.setText("");
		handleCancel(event);
		for (ProgressBar pb : progressList) {
			pb.progressProperty().unbind();
		}
		totalPercentIndicator.progressProperty().unbind();

	}

	public void handleCancel(ActionEvent event) {
		for (Task<Long> t : tasks) {
			t.cancel();
		}
	}

	private File openFileChooser(String stringUrl) {
		String fileName = stringUrl.substring(stringUrl.lastIndexOf('/') + 1);
		this.fileNameLabel.setText(fileName);
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Where you want to save it");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));
		chooser.setInitialFileName(fileName);
		return chooser.showSaveDialog(null);
	}

	private void findNotFound() {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText("URL can't access!!");
		alert.showAndWait();
	}
}
