import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) {
        try {
            ServerUDP server = new ServerUDP();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
