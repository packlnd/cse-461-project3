import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

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
		boolean cam = cameraAvailable();
		buildGUI(cam);
		String response = communicateWithMasterServer();
		Socket relay = setupConnectionWithRelay(response);
		if (cam)
			sendWebcamFrames(relay);
		getWebcamFrames(relay);
	}

	private Socket setupConnectionWithRelay(String response) {
		String[] s = response.split(" ");
		Socket socket = null;
		try {
			socket = new Socket(s[0], 11236);
			int token = Integer.parseInt(s[1]);
			System.out.println("Token is: " + token);
			OutputStream os = socket.getOutputStream();
			os.write(ByteBuffer.allocate(4).putInt(token).array());
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
						DataOutputStream os = new DataOutputStream(relay.getOutputStream());
						BufferedImage img = webcam.getImage();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(img, "jpg", baos);
						byte[] bytes = baos.toByteArray();
						System.out.println(bytes.length);
						os.writeInt(bytes.length);
						os.write(bytes);
						
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
						DataInputStream dis = new DataInputStream(relay.getInputStream());
						int len = dis.readInt();
						System.out.println(len);
					    /*ByteBuffer bbuf = ByteBuffer.allocate(len);
					    bbuf.order(ByteOrder.BIG_ENDIAN);
					    for (int i=0; i<len; ++i)
					    	bbuf.put(dis.readByte());*/
						byte[] buf = new byte[len];
						dis.read(buf, 0, len);
						InputStream in = new ByteArrayInputStream(buf);
						BufferedImage img = ImageIO.read(in);
						remoteCam.setIcon(new ImageIcon(img));
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

	private void buildGUI(boolean cam) {
		WebcamPanel panel = new WebcamPanel(webcam, cam);
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

	private String communicateWithMasterServer() {
		final String host = "attu1.cs.washington.edu";
		final int port = 11235;
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
