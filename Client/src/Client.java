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
		try {
			//TODO: Client's should act the same. No need for fake server. Problem is localhost with only one camera.
			// ALso get address to peer from response.
			if (isMaster(response)) {
				fakeServer();
			} else {
				fakeClient();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fakeClient() throws Exception {
		DatagramSocket ds = new DatagramSocket(3000);
		
		//TODO: 25000 is large. Don't think we want 25k bytes per packet.
		byte[] buf = new byte[25000];
		DatagramPacket dp = new DatagramPacket(buf, 25000);
		
		//TODO: Loop receive
		ds.receive(dp);
		InputStream in = new ByteArrayInputStream(dp.getData());
		BufferedImage img = ImageIO.read(in);
		JFrame frame = new JFrame();
		frame.getContentPane().add(new JLabel(new ImageIcon(img)));
		frame.pack();
		frame.setVisible(true);
		ds.close();
	}

	private void fakeServer() throws Exception {

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

		//TODO: Loop, compress
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