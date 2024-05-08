import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerUDP {
    private static final int SERVER_PORT = 1111;
    private static final String SERVER_ADDRESS="127.0.0.1";
    private final DatagramSocket socket;
    private final DatabaseManager databaseManager;
    private final Gson gson;
    private final Set<String> registeredClients;

    public ServerUDP() throws IOException {
        socket = new DatagramSocket(SERVER_PORT);
        databaseManager = new DatabaseManager();
        gson = new Gson();
        registeredClients = new HashSet<>();
    }

    public void start() {
        while (true) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + message);

                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                String messageType = jsonMessage.get("type").getAsString();

                switch (messageType) {
                    case "registration":
                        handleRegistration(jsonMessage);
                        break;
                    case "chat":
                        handleChatMessage(jsonMessage);
                        break;
                    case "client_start":
                        handleClientStart(jsonMessage);
                        break;
                    default:
                        System.out.println("Invalid message type: " + messageType);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRegistration(JsonObject jsonMessage) {
        String username = jsonMessage.get("username").getAsString();
        String password = jsonMessage.get("password").getAsString();

        boolean registrationSuccess = false;
        int port=0000;
        try {
            // Obtenez l'adresse IP et le port à partir du JSON reçu
            String ip = jsonMessage.get("ip").getAsString();
            port = jsonMessage.get("port").getAsInt();

            // Utilisez l'adresse IP et le port pour l'enregistrement de l'utilisateur
            registrationSuccess = databaseManager.registerUser(username, password, ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Envoyez la réponse d'inscription au client
        String responseMessage = registrationSuccess ? "Registration successful" : "Registration failed";
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("type", "registration_response");
        responseJson.addProperty("message", responseMessage);
        responseJson.addProperty("username", username);
        sendResponse(responseJson,port);
    }

    private void sendResponse(JsonObject jsonMessage,int port) {
        try {
            byte[] buffer = jsonMessage.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(SERVER_ADDRESS), port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendResponse(JsonObject jsonMessage, String friendUsername) {
        try {
            byte[] buffer = jsonMessage.toString().getBytes();
            String friendIP = databaseManager.getClientIPAddress(friendUsername);
            int friendPort = databaseManager.getClientPort(friendUsername);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(friendIP), friendPort);
            socket.send(packet);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }


    private void handleChatMessage(JsonObject jsonMessage) {
        if (!jsonMessage.has("username") || !jsonMessage.has("message")) {
            System.out.println("Invalid chat message format: " + jsonMessage.toString());
            return;
        }

        String username = jsonMessage.get("username").getAsString();
        String message = jsonMessage.get("message").getAsString();
        String userReceiver = jsonMessage.get("receiver_username").getAsString();


        try {
            if (!databaseManager.isClientRegistered(username)) {
                System.out.println("Unregistered client attempted to send a message.");
                return;
            }
            if(!databaseManager.checkIfAmi(username,userReceiver)){
                System.out.println("you are not freinds.");

                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Received chat message from " + username + ": " + message);
        transferMessage(jsonMessage);
    }
    private void handleClientStart(JsonObject jsonMessage) {
        String username = jsonMessage.get("username").getAsString();
        notifyFriends(username);
    }
    private void notifyFriends(String username) {
            // Get the friends of the given username
            List<String> friends = databaseManager.getFriends(username);

            // Send notification to each friend
            for (String friend : friends) {
                JsonObject notification = new JsonObject();
                notification.addProperty("type", "client_notification");
                notification.addProperty("message", username + " is now connected.");
                sendResponse(notification, friend);
            }
    }

    private void transferMessage(JsonObject jsonMessage) {
        String receiverUsername = jsonMessage.get("receiver_username").getAsString();
        String message = jsonMessage.get("message").getAsString();

        try {
            String receiverIP = databaseManager.getClientIPAddress(receiverUsername);
            int receiverPort = databaseManager.getClientPort(receiverUsername);

            JsonObject transferMessage = new JsonObject();
            transferMessage.addProperty("type", "chat");
            transferMessage.addProperty("username", jsonMessage.get("username").getAsString());
            transferMessage.addProperty("message", message);

            byte[] buffer = transferMessage.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(receiverIP), receiverPort);
            socket.send(packet);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}