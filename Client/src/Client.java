import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

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
		System.out.println("fakeClient");
		byte[] buf = new byte[1000];
		DatagramPacket dp = new DatagramPacket(buf, buf.length);
		try {
			while (true) {
				DatagramSocket s = new DatagramSocket();
				s.receive(dp);
				System.out.println("Here");
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(dp.getData()));
				JFrame window = new JFrame("fakeClient");
				window.getContentPane().add(new JLabel(new ImageIcon(img)));
				window.setResizable(true);
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.pack();
				window.setVisible(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

	private void fakeServer() {
		System.out.println("fakeServer");
		Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());

		try {
			DatagramSocket s = new DatagramSocket();
			InetAddress hostAddress = InetAddress.getByName("localhost");
			while (true) {
				BufferedImage img = webcam.getImage();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] bytes = baos.toByteArray();
				DatagramPacket out = new DatagramPacket(bytes, bytes.length, hostAddress, 9999);
				s.send(out);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("done");
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
			BufferedReader fromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			response = fromServer.readLine();
			serverSocket.close();
		} catch (IOException ie) {
			ie.printStackTrace();
		}
		return response;
	}

}