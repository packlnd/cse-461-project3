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
import javax.swing.JOptionPane;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class Client extends JLabel {

	private Webcam webcam;
	private JLabel remoteCam;
	
	public static void main(String[] args) {
		new Client();
	}

	public Client() {
		super();
		initializeWebcam();
		buildGUI();
		String response = communicateWithMasterServer();
		Socket relay = setupConnectionWithRelay(response);
		if (cameraAvailable())
			sendWebcamFrames(relay);
		getWebcamFrames(relay);
	}

	private Socket setupConnectionWithRelay(String response) {
		String[] s = response.split("$");
		Socket socket = null;
		try {
			socket = new Socket(s[0], 1236);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			bw.write(s[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return socket;
	}
	
	private void sendWebcamFrames(Socket relay) {
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						OutputStream os = relay.getOutputStream();
						BufferedImage img = webcam.getImage();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(img, "jpg", baos);
						byte[] bytes = baos.toByteArray();
						os.write(bytes.length);
						os.write('\n');
						os.write(bytes);
						os.write('\n');
						
						Thread.sleep(500);
					} catch (Exception e) {
					}
				}
			}
		};
		t.start();
	}

	private void getWebcamFrames(Socket relay) {
		Thread t = new Thread() {
			public void run() {
				while (true) {
					try {
						BufferedReader bsr = new BufferedReader(new InputStreamReader(relay.getInputStream()));
						System.out.println(bsr.readLine());
						/*InputStream in = new ByteArrayInputStream(dp.getData());
						BufferedImage img = ImageIO.read(in);
						remoteCam.setIcon(new ImageIcon(img));*/
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

		this.setPreferredSize(new Dimension(500, 500));
		this.add(panel);
		this.add(remoteCam);
	}
	
	private boolean cameraAvailable() {
		return !webcam.getLock().isLocked();
	}

	private String communicateWithMasterServer() {
		final String host = "attu1.cs.washington.edu";
		final int port = 1235;
		String response = null;
		try {
			Socket serverSocket = new Socket(host, port);
			BufferedReader fromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			response = fromServer.readLine();
			serverSocket.close();
		} catch (ConnectException ce) {
			JOptionPane.showMessageDialog(null, "Cannot connect to " + host + " on " + port);
			System.exit(0);
		} catch (IOException ie) {
			ie.printStackTrace();
		}
		return response;
	}
}