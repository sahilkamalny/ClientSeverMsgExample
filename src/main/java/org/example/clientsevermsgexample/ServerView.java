package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ServerView implements Initializable {
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

    private ServerSocket serverSocket;
    private int port = 6666;
    private boolean isRunning = false;
    private List<ClientHandler> clients = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vbox_messages.heightProperty().addListener((observable, oldValue, newValue) -> {
            sp_main.setVvalue((Double) newValue);
        });

        button_send.setOnAction(event -> {
            sendBroadcast();
        });

        tf_message.setOnAction(event -> {
            sendBroadcast();
        });
    }

    private void startServer() {
        isRunning = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                addMessage("System", "Server started on port " + port, false);

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String clientAddress = clientSocket.getInetAddress().getHostAddress();
                        addMessage("System", "New client connected: " + clientAddress, false);

                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clients.add(clientHandler);
                        new Thread(clientHandler).start();

                    } catch (IOException e) {
                        if (isRunning) {
                            addMessage("System", "Error accepting client connection: " + e.getMessage(), false);
                        }
                    }
                }

            } catch (IOException e) {
                addMessage("System", "Server error: " + e.getMessage(), false);
            }
        }).start();
    }

    private void sendBroadcast() {
        String message = tf_message.getText();
        if (message.isEmpty()) return;

        addMessage("Server", message, true);
        broadcastToClients(message);
        tf_message.clear();
    }

    private void broadcastToClients(String message) {
        String fullMessage = "Server: " + message;
        for (ClientHandler client : new ArrayList<>(clients)) {
            if (client != null) {
                client.sendMessage(fullMessage);
            }
        }
    }

    public void setPort(int port) {
        this.port = port;
        startServer();
    }

    private void addMessage(String sender, String messageContent, boolean isSentByServer) {
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 10, 5, 10));

        Text senderText = new Text(sender + ": ");
        TextFlow senderTextFlow = new TextFlow(senderText);

        Text messageText = new Text(messageContent);
        TextFlow messageTextFlow = new TextFlow(messageText);
        messageTextFlow.setPadding(new Insets(5));

        if (isSentByServer) {
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

    public void stopServer() {
        isRunning = false;

        // Close all client connections
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            addMessage("System", "Server stopped", false);
        } catch (IOException e) {
            addMessage("System", "Error stopping server: " + e.getMessage(), false);
        }
    }

    // Inner class to handle each client connection
    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;
        private boolean isActive = true;
        private String clientAddress;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientAddress = socket.getInetAddress().getHostAddress();

            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                addMessage("System", "Error setting up client handler: " + e.getMessage(), false);
                isActive = false;
            }
        }

        @Override
        public void run() {
            try {
                while (isActive) {
                    String message = inputStream.readUTF();

                    if (message.endsWith(": exit")) {
                        addMessage("System", "Client " + clientAddress + " has disconnected", false);
                        break;
                    }

                    // Display the received message in the server window
                    Platform.runLater(() -> {
                        // Extract username and message
                        int colonIndex = message.indexOf(": ");
                        if (colonIndex != -1) {
                            String sender = message.substring(0, colonIndex);
                            String content = message.substring(colonIndex + 2);
                            addMessage(sender, content, false);
                        } else {
                            addMessage("Client", message, false);
                        }
                    });

                    // Broadcast to all other clients
                    for (ClientHandler client : new ArrayList<>(clients)) {
                        if (client != this && client != null) {
                            client.sendMessage(message);
                        }
                    }
                }
            } catch (IOException e) {
                if (isActive) {
                    addMessage("System", "Lost connection to client " + clientAddress + ": " + e.getMessage(), false);
                }
            } finally {
                close();
                clients.remove(this);
            }
        }


        public void sendMessage(String message) {
            try {
                if (isActive && outputStream != null) {
                    outputStream.writeUTF(message);
                }
            } catch (IOException e) {
                addMessage("System", "Error sending message to client: " + e.getMessage(), false);
                isActive = false;
            }
        }

        public void close() {
            isActive = false;

            try {
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
                addMessage("System", "Error closing client connection: " + e.getMessage(), false);
            }
        }
    }
}