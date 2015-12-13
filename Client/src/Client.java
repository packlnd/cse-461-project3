import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class Client {

	private Webcam webcam;
	private JLabel remoteCam;

	public static void main(String[] args) {
		new Client();
	}

	public Client() {
		initializeWebcam();
		buildGUI();
		//String response = communicateWithServer();
		//TODO: Get host name from response
		if (cameraAvailable())
			sendFrames();
		getFrames();
	}

	private void sendFrames() {
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						BufferedImage img = webcam.getImage();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(img, "jpg", baos);
						byte[] bytes = baos.toByteArray();
						DatagramSocket ds;
						InetAddress ip = InetAddress.getByName("localhost");
						ds = new DatagramSocket();
						System.out.println(bytes.length);
						DatagramPacket dp = new DatagramPacket(bytes, bytes.length, ip, 3000);
						ds.send(dp);
						ds.close();
						Thread.sleep(500);
					} catch (Exception e) {
					}
				}
			}
		};
		t.start();
	}

	private void getFrames() {
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						DatagramSocket ds = new DatagramSocket(3000);
						byte[] buf = new byte[25000];
						DatagramPacket dp = new DatagramPacket(buf, 25000);
						ds.receive(dp);
						InputStream in = new ByteArrayInputStream(dp.getData());
						BufferedImage img = ImageIO.read(in);
						remoteCam = new JLabel(new ImageIcon(img));
					} catch (Exception e) {
					}
				}
			}
		};
		t.run();
	}

	private void initializeWebcam() {
		webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
	}

	private void buildGUI() {
		WebcamPanel panel = new WebcamPanel(webcam, cameraAvailable());
		panel.setSize(new Dimension(250, 500));
		if (cameraAvailable())
			remoteCam = new JLabel(new ImageIcon(webcam.getImage()));
		else
			remoteCam = new JLabel(new ImageIcon());
		remoteCam.setPreferredSize(new Dimension(250, 500));
		remoteCam.setMinimumSize(new Dimension(250, 500));
		remoteCam.setMaximumSize(new Dimension(250, 500));
		remoteCam.setBackground(Color.BLACK);

		JFrame window = new JFrame("Test webcam panel");
		window.setPreferredSize(new Dimension(500, 500));
		window.add(panel);
		window.add(remoteCam);
		window.setResizable(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);
	}
	
	private boolean cameraAvailable() {
		return !webcam.getLock().isLocked();
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