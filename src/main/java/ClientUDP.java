import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientUDP {
    private DatagramSocket socket=null;
    private final int SERVER_PORT=1111;
    private  int clientPort =9999;
    private String clientAddress ="127.0.0.1";
    private final Gson gson;
    private final String defaultUsername;
    private final String defaultPassword;
    private String receiverUsername="";
    private final DatabaseManager databaseManager;
    private final ExecutorService executorService;

    public ClientUDP(String defaultUsername, String defaultPassword) {
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
        this.databaseManager = new DatabaseManager();

        try {
            if (databaseManager.isClientRegistered(defaultUsername)) {
                // Si le client est déjà enregistré, récupère son port et son adresse IP
                this.clientPort = databaseManager.getClientPort(defaultUsername);
                this.clientAddress = databaseManager.getClientIPAddress(defaultUsername);
                socket = new DatagramSocket(clientPort);
            } else {
                // Si le client n'est pas enregistré, génère un port et une adresse IP aléatoires et enregistre le client
                this.clientPort = (int) (Math.random() * 65536); // Les ports valides sont compris entre 0 et 65535
                this.clientAddress = "127.0.0.1";
                socket = new DatagramSocket(clientPort);
                register();
                System.out.println("registred");
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }

        gson = new Gson();
        executorService = Executors.newSingleThreadExecutor();
    }


    public void start() {
        Scanner scanner = new Scanner(System.in);
        try {
            notifyFriendsAsync();

            System.out.print("Enter receiver's username: ");
            receiverUsername = scanner.nextLine();

            new Thread(this::receiveMessages).start();

            sendMessages(scanner);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private void notifyFriendsAsync() {
        executorService.submit(() -> {
            sendJsonMessage("client_start", defaultUsername, defaultPassword, "Client started", "", "", 0);
        });
    }


    private void sendMessages(Scanner scanner) {
        try {
            while (true) {
                if (System.in.available() > 0) {
                    //System.out.print("Enter your message: ");
                    String message = scanner.nextLine();
                    if (message.equalsIgnoreCase("register")) {
                        register();
                    } else {
                        sendMessage(message);
                    }
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void receiveMessages() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                JsonObject jsonObject = gson.fromJson(receivedMessage, JsonObject.class);
                String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "Unknown";

                System.out.println("\nMessage: " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        sendJsonMessage("chat", defaultUsername, defaultPassword, message, receiverUsername,"",0);
    }

    public void register() {
        sendJsonMessage("registration", defaultUsername, defaultPassword, "I want to register", receiverUsername,clientAddress,clientPort);
    }

    private void sendJsonMessage(String type, String username, String password, String message, String receiverUsername, String ip, int port) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", type);
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("password", password);
        jsonObject.addProperty("receiver_username", receiverUsername);
        jsonObject.addProperty("message", message);
        jsonObject.addProperty("ip", ip);
        jsonObject.addProperty("port", port);
        try {
            byte[] buffer = jsonObject.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(clientAddress), SERVER_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
