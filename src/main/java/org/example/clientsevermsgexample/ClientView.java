package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientView implements Initializable {
    @FXML
    private Button button_send;

    @FXML
    private TextField tf_message;

    @FXML
    private ScrollPane sp_main;

    @FXML
    private VBox vbox_messages;

    @FXML
    private AnchorPane ap_main;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private boolean connected = false;
    private Thread receiveThread;
    private String username;

    public void setUsername(String username) {
        this.username = username;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vbox_messages.heightProperty().addListener((observable, oldValue, newValue) -> {
            sp_main.setVvalue((Double) newValue);
        });

        button_send.setOnAction(event -> {
            sendMessage();
        });

        tf_message.setOnAction(event -> {
            sendMessage();
        });
    }

    private int port = 161;  // Default port

    public void setPort(int port) {
        this.port = port;
        Platform.runLater(this::connectToServer);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", port);
            addMessage("System", "Connected to server", false);

            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            connected = true;

            startReceiving();

        } catch (IOException e) {
            addMessage("System", "Failed to connect to server: " + e.getMessage(), false);
        }
    }

    private void startReceiving() {
        receiveThread = new Thread(() -> {
            try {
                while (connected) {
                    String message = inputStream.readUTF();
                    Platform.runLater(() -> {
                        // If the message starts with "Server:", display as is
                        // Otherwise, display just the message
                        if (message.startsWith("Server:")) {
                            addMessage("Server", message.substring(7).trim(), false);
                        } else {
                            // Extract username and message
                            int colonIndex = message.indexOf(": ");
                            if (colonIndex != -1) {
                                String sender = message.substring(0, colonIndex);
                                String content = message.substring(colonIndex + 2);
                                addMessage(sender, content, false);
                            } else {
                                addMessage("System", message, false);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                if (connected) {
                    Platform.runLater(() -> {
                        addMessage("System", "Lost connection to server: " + e.getMessage(), false);
                    });
                    disconnect();
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }


    // In ClientView.java
    private void sendMessage() {
        String messageToSend = tf_message.getText();
        if (messageToSend.isEmpty()) return;

        if (!connected) {
            addMessage("System", "Not connected to server", false);
            return;
        }

        try {
            // Send both username and message
            String fullMessage = username + ": " + messageToSend;
            outputStream.writeUTF(fullMessage);
            addMessage(username, messageToSend, true);
            tf_message.clear();
        } catch (IOException e) {
            addMessage("System", "Failed to send message: " + e.getMessage(), false);
            disconnect();
        }
    }


    private void addMessage(String sender, String messageContent, boolean isSentByMe) {
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 10, 5, 10));

        Text senderText = new Text(sender + ": ");
        TextFlow senderTextFlow = new TextFlow(senderText);

        Text messageText = new Text(messageContent);
        TextFlow messageTextFlow = new TextFlow(messageText);
        messageTextFlow.setPadding(new Insets(5));

        if (isSentByMe) {
            hBox.setAlignment(Pos.CENTER_RIGHT);
            messageTextFlow.setStyle("-fx-background-color: #90EE90; -fx-background-radius: 10px;");
        } else {
            hBox.setAlignment(Pos.CENTER_LEFT);
            messageTextFlow.setStyle("-fx-background-color: #F0F0F0; -fx-background-radius: 10px;");
        }

        hBox.getChildren().addAll(senderTextFlow, messageTextFlow);
        Platform.runLater(() -> {
            vbox_messages.getChildren().add(hBox);
        });
    }

    private void disconnect() {
        connected = false;

        try {
            if (receiveThread != null) {
                receiveThread.interrupt();
            }

            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
        }
    }

    // Call this method when window is closing
    public void shutdown() {
        disconnect();
    }
}