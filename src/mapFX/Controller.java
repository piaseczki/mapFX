package mapFX;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Controller {

    private String defaultDataSourceUrl = "http://daily.digpro.se/bios/servlet/bios.servlets.web.RecruitmentTestServlet";
    private Timeline loadRecordsPeriodically;
    private int reloadInterval = 30;
    private int secondsLeft = -1;

    @FXML
    private TextField textField;

    @FXML
    private Label label;

    @FXML
    private Button button;

    @FXML
    private Pane canvas;

    @FXML
    private void initialize() {
        textField.setText(defaultDataSourceUrl);
        label.setText("PRESS START BUTTON TO MAP RECORDS");
    }

    @FXML
    private void handleButtonAction() {
        if (button.getText().equals("START")) {
            runPeriodically();
        } else if (button.getText().equals("STOP")) {
            stopRunPeriodically("PRESS START TO RELOAD RECORDS");
        }
    }

    @FXML
    private void handleAboutButtonAction() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setWidth(300);
        dialogStage.setHeight(150);
        dialogStage.setTitle("About");
        Button closeButton = new Button("Close");
        closeButton.setOnAction(new EventHandler<ActionEvent>(){

            @Override
            public void handle(ActionEvent arg0) {
                dialogStage.close();
            }
        });
        Text text = new Text("Developed by Jan Piasecki\nPhone +48 537 952 996\nEmail piaseczki@gmail.com\n");
        VBox vbox = new VBox(text, closeButton);
        vbox.setAlignment(Pos.BOTTOM_RIGHT);
        vbox.setPadding(new Insets(15));
        dialogStage.setScene(new Scene(vbox));
        dialogStage.show();
    }

    private void runPeriodically() {
        loadRecordsPeriodically = new Timeline(new KeyFrame(Duration.ZERO, new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        if (secondsLeft < 0) {
                            secondsLeft = reloadInterval;
                            loadRecords(textField.getText());
                        } else {
                            if (!button.getText().equals("LOADING")) {
                                label.setText(String.format("AUTO RELOAD IN %2d SEC.",secondsLeft));
                            }
                            secondsLeft--;
                        }
                    }
                }), new KeyFrame(Duration.seconds(1)));
        loadRecordsPeriodically.setCycleCount(Timeline.INDEFINITE);
        loadRecordsPeriodically.play();
    }

    private void loadRecords(String dataSourceUrl) {
        label.setText("");
        button.setText("LOADING");
        resetCanvas();

        Task<Void> task1 = new Task() {

            @Override
            public Void call() throws Exception {
                try (InputStream is = new URL(dataSourceUrl).openStream()) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.charAt(0) != '#') {
                            String[] recordAttributes = line.split(", ");
                            int x = Integer.parseInt(recordAttributes[0]);
                            int y = Integer.parseInt(recordAttributes[1]);
                            String name = recordAttributes[2];
                            if (name == null || name.isEmpty()) {
                                throw new Exception("Record name is empty.");
                            }

                            Record record = new Record(x, y, name);
                            System.out.println(record);

                            Platform.runLater(new Runnable() {

                                @Override
                                public void run() {
                                    label.setText(record.toString());
                                    record.printOnCanvas(canvas);
                                }
                            });
                        }
                    }

                } catch (NumberFormatException|IOException e) {
                    e.printStackTrace();
                    throw new Exception(e);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                label.setText("AUTO RELOAD IN "+ secondsLeft +" SEC.");
                button.setText("STOP");
                System.out.println("RECORDS LOADED AT " +
                        new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
            }

            @Override
            protected void failed() {
                stopRunPeriodically("FAILED TO LOAD RECORDS");
                System.out.println("FAILED TO LOAD RECORDS AT " +
                        new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
            }
        };

        Thread thread1 = new Thread(task1);
        thread1.setDaemon(true);
        thread1.start();
    }

    private void stopRunPeriodically(String message) {
        loadRecordsPeriodically.stop();
        button.setText("START");
        label.setText(message);
        secondsLeft = -1;
    }

    private void resetCanvas() {
        canvas.getChildren().clear();
        Line horizontalLine = new Line(0, canvas.getHeight() * 0.75, canvas.getWidth(), canvas.getHeight() * 0.75);
        Line verticalLine = new Line(canvas.getWidth() * 0.5, 0, canvas.getWidth() * 0.5, canvas.getHeight());
        Label origin = new Label("0,0");
        origin.setTranslateX(canvas.getWidth() * 0.5 + 2);
        origin.setTranslateY(canvas.getHeight() * 0.75);
        canvas.getChildren().addAll(horizontalLine, verticalLine, origin);
    }
}

class Record {
    private int x;
    private int y;
    private String name;

    public Record(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("X %4d | Y %4d | NAME %8s", x, y, name);
    }

    public void printOnCanvas(Pane canvas) {
        double transX = canvas.getWidth() * 0.5 + x * 0.5;
        double transY = canvas.getHeight() * 0.75 - y * 0.5;
        Circle point = new Circle(8);
        point.setTranslateX(transX);
        point.setTranslateY(transY);
        Tooltip tooltip = new Tooltip(this.toString());
        tooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(point, tooltip);
        canvas.getChildren().add(point);
    }
}

