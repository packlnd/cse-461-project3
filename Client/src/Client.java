import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class Client {
	private BufferedImage noWebcam;
	private BufferedImage noConnection;
	private Webcam webcam;
	private JLabel remoteCam;
	private boolean endThread;
	private boolean cam;
	
	public static void main(String[] args) {
		try {
            // Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e) {}
		new Client();
	}

	public Client() {
		try {
			noWebcam = ImageIO.read(new File("nowebcam.jpg"));
			noConnection = ImageIO.read(new File("waiting.jpg"));
		} catch (IOException e) {}
		initializeWebcam();
		cam = cameraAvailable();
		buildGUI();
		endThread = false;
		
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
						BufferedImage img = null;
						if (cam) {
							img = webcam.getImage();
						} else {
							img = noWebcam;
						}
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(img, "jpg", baos);
						byte[] bytes = baos.toByteArray();
						os.writeInt(bytes.length);
						os.write(bytes);
						
						Thread.sleep(1000/28);
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
						    /*ByteBuffer bbuf = ByteBuffer.allocate(len);
						    bbuf.order(ByteOrder.BIG_ENDIAN);
						    for (int i=0; i<len; ++i)
						    	bbuf.put(dis.readByte());*/
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

	private void buildGUI() {
		JComponent panel = null;
		if (cam) {
			panel = new WebcamPanel(webcam, cam) {
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(480, 360);
				}
			};
		} else {
			panel = new JLabel();
			((JLabel) panel).setIcon(new ImageIcon(noWebcam));
		}
		/*if (cameraAvailable())
			remoteCam = new JLabel(new ImageIcon(webcam.getImage())) {
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(480, 360);
				}
			};
		else
			remoteCam = new JLabel(new ImageIcon(noWebcam)) {
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(480, 360);
				}
			};*/

		remoteCam = new JLabel(new ImageIcon(noConnection)) {
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
				remoteCam.setIcon(new ImageIcon(noConnection));
				String response = communicateWithMasterServer();
				Socket relay = setupConnectionWithRelay(response);
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
			System.exit(0);
		} catch (IOException ie) {
			ie.printStackTrace();
		}
		return response;
	}
}
