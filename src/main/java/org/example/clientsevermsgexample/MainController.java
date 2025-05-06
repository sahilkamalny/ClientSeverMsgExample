package org.example.clientsevermsgexample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private ComboBox<String> dropdownPort;  // Add the String type parameter


    @FXML
    private Button clearBtn;

    @FXML
    private TextArea resultArea;

    @FXML
    private Label server_lbl;

    @FXML
    private Button testBtn;

    @FXML
    private Label test_lbl;

    @FXML
    private TextField urlName;

    @FXML
    private Button user1_client;

    @FXML
    private Button user2_server;

    private ServerView serverController;
    private Stage serverStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7",     // ping
                "13",     // daytime
                "21",     // ftp
                "23",     // telnet
                "71",     // finger
                "80",     // http
                "119",    // nntp (news)
                "161"    // snmp
        );

        // Set default values
        if (urlName.getText().isEmpty()) {
            urlName.setText("localhost");
        }
    }


    @FXML
    void startClientGeneric(ActionEvent event) throws IOException {
        startClientWithUsername("Client");
    }

    @FXML
    void startUser1Client(ActionEvent event) throws IOException {
        startClientWithUsername("User 1");
    }

    @FXML
    void startUser2Client(ActionEvent event) throws IOException {
        startClientWithUsername("User 2");
    }

    private void startClientWithUsername(String username) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("client-view.fxml"));
            Parent root = loader.load();
            ClientView clientController = loader.getController();

            // Get selected port or default to 161
            int port = 161;
            if (dropdownPort.getValue() != null) {
                try {
                    port = Integer.parseInt(dropdownPort.getValue().toString());
                } catch (NumberFormatException e) {
                    resultArea.appendText("Invalid port number. Using default port 161.\n");
                }
            }

            try {
                Socket testSocket = new Socket("localhost", port);
                testSocket.close();
            } catch (IOException e) {
                resultArea.appendText("Error: Server must be started first on port " + port + "\n");
                return;
            }

            clientController.setUsername(username);
            clientController.setPort(port);

            Stage clientStage = new Stage();
            clientStage.setTitle("Chat Client - " + username + " (Port " + port + ")");
            clientStage.setScene(new Scene(root));
            clientStage.setOnCloseRequest(e -> {
                clientController.shutdown();
            });
            clientStage.show();

            resultArea.appendText(username + " client started on port " + port + "\n");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void checkConnection(ActionEvent event) {
        String host = urlName.getText();
        if (host.isEmpty() || dropdownPort.getValue() == null) {
            resultArea.setText("Please enter a hostname and select a port.");
            return;
        }

        int port = Integer.parseInt(dropdownPort.getValue().toString());

        try {
            resultArea.appendText("Attempting to connect to " + host + " on port " + port + "...\n");
            Socket sock = new Socket(host, port);
            resultArea.appendText(host + " is listening on port " + port + "\n");
            sock.close();
        } catch (UnknownHostException e) {
            resultArea.setText("Error: " + e.getMessage() + "\n");
            resultArea.appendText("The hostname '" + host + "' is invalid.\n");
            resultArea.appendText("Try using 'localhost' or '127.0.0.1' instead.\n");
        } catch (Exception e) {
            resultArea.appendText(host + " is not listening on port " + port + "\n");
        }
    }

    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.setText("");
    }

    @FXML
    void startServer(ActionEvent event) {
        // Get selected port or default to 161
        int port = 161;
        if (dropdownPort.getValue() != null) {
            try {
                port = Integer.parseInt(dropdownPort.getValue());
            } catch (NumberFormatException e) {
                resultArea.appendText("Invalid port number. Using default port 161.\n");
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("server-view.fxml"));
            Parent root = loader.load();
            serverController = loader.getController();

            // Set the port before starting the server
            serverController.setPort(port);

            serverStage = new Stage();
            serverStage.setTitle("Chat Server - Port " + port);
            serverStage.setScene(new Scene(root));
            serverStage.setOnCloseRequest(e -> {
                if (serverController != null) {
                    serverController.stopServer();
                }
            });
            serverStage.show();

            resultArea.appendText("Server started on port " + port + "\n");

        } catch (IOException e) {
            resultArea.appendText("Error starting server: " + e.getMessage() + "\n");
        }
    }
}