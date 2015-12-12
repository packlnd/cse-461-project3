import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class Client {
	public static void main(String[] args) {
		new Client();
	}

	public Client() {
		String response = communicateWithServer();
		if (isMaster(response)) {
			fakeServer();
		} else {
			fakeClient();
		}
	}

    private void fakeClient() {
        
    }
    
    private void fakeServer() {

		Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());

		WebcamPanel panel = new WebcamPanel(webcam);
		panel.setFPSDisplayed(true);
		panel.setDisplayDebugInfo(true);
		panel.setImageSizeDisplayed(true);
		panel.setMirrored(true);

		JFrame window = new JFrame("Test webcam panel");
		window.add(panel);
		window.setResizable(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);
    }

    private boolean isMaster(String s) {
        return !s.equals("$");
    }

    private String communicateWithServer() {
        final String host = "localhost";
        final int port = 1234;
        String response = null;
        try {
            Socket serverSocket = new Socket(host, port);
            BufferedReader fromServer =
                new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            response = fromServer.readLine();
            serverSocket.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return response;
    }

}