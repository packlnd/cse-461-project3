import java.net.Socket;
import java.io.*;

public class Client {
    final String host = "localhost";
    final int port = 1234;

    public Client() {
        try {
            Socket socket = new Socket(host, port);
            DataOutputStream toServer =
                new DataOutputStream(socket.getOutputStream());
            BufferedReader fromServer =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toServer.writeBytes("Hello world");
            String response = fromServer.readLine();
            System.out.println(response);
            socket.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
