import java.net.*;
import java.io.*;

public class Client {
    final String host = "localhost";
    final int port = 1234;

    public Client() {
        String response = communicateWithServer();
        // SETUP UDP
    }

    private String communicateWithServer() {
        String response = null;
        try {
            Socket serverSocket = new Socket(host, port);
            DataOutputStream toServer =
                new DataOutputStream(serverSocket.getOutputStream());
            BufferedReader fromServer =
                new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            response = fromServer.readLine();
            serverSocket.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return response;
    }

    public static void main(String[] args) {
        new Client();
    }
}
