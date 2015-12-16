import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class Client {

	private Webcam webcam;
	private JLabel remoteCam;
	private boolean endThread;

	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		new Client();
	}

	public Client() {
		initializeWebcam();
		boolean cam = cameraAvailable();
		buildGUI(cam);
		endThread = false;
		/*String response = communicateWithMasterServer();
		Socket relay = setupConnectionWithRelay(response);
		if (cam)
			sendWebcamFrames(relay);
		getWebcamFrames(relay);*/
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
						if (endThread) {
							break;
						}
						DataOutputStream os = new DataOutputStream(relay.getOutputStream());
						BufferedImage img = webcam.getImage();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(img, "jpg", baos);
						byte[] bytes = baos.toByteArray();
						os.writeInt(bytes.length);
						os.write(bytes);

						Thread.sleep(1000 / 28);
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
				DataInputStream dis;
				try {
					dis = new DataInputStream(relay.getInputStream());
					while (true) {
						if (endThread) {
							break;
						}
						int len = dis.readInt();
						/*
						 * ByteBuffer bbuf = ByteBuffer.allocate(len);
						 * bbuf.order(ByteOrder.BIG_ENDIAN); for (int i=0;
						 * i<len; ++i) bbuf.put(dis.readByte());
						 */
						byte[] buf = new byte[len];
						int read = 0;
						while (read < len) {
							read += dis.read(buf, read, len - read);
						}
						InputStream in = new ByteArrayInputStream(buf);
						BufferedImage img = ImageIO.read(in);
						remoteCam.setIcon(new ImageIcon(img));
					}
				} catch (IOException e1) {
				}
			}
		};
		t.start();
	}

	private void initializeWebcam() {
		webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
	}

	private void buildGUI(boolean cam) {
		WebcamPanel panel = new WebcamPanel(webcam, cam) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(480, 360);
			}
		};
		if (cameraAvailable())
			remoteCam = new JLabel(new ImageIcon(webcam.getImage())) {
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(480, 360);
				}
			};
		else
			remoteCam = new JLabel(new ImageIcon()) {
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(480, 360);
				}
			};

		JFrame window = new JFrame("UW Chat Roulette");
		window.setPreferredSize(new Dimension(965, 400));
		window.add(remoteCam, BorderLayout.EAST);
		window.add(panel, BorderLayout.WEST);
		JButton button = new JButton("New Connection");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				endThread = true;
				try {
					Thread.sleep(300);
				} catch (InterruptedException e1) {
				}
				endThread = false;
				String response = communicateWithMasterServer();
				if (response == null) return;
				Socket relay = setupConnectionWithRelay(response);
				if (cam)
					sendWebcamFrames(relay);
				getWebcamFrames(relay);
			}
		});
		window.add(button, BorderLayout.SOUTH);
		window.setResizable(false);
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
			return null;
		} catch (IOException ie) {
			ie.printStackTrace();
		}
		return response;
	}
}