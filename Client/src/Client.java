import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

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
import javax.swing.JTextField;
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
	private ArrayList<String> chat;
	private JLabel chatBox;
	private int chatsToRead;
	
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
		chat = new ArrayList<String>();
		buildGUI();
		endThread = false;
		chatsToRead = 0;
	}

	private Socket setupConnectionWithRelay(String response) {
		Socket socket = null;
		try {
			String[] s = response.split(" ");
			socket = new Socket(s[0], 11236);
			int token = Integer.parseInt(s[1]);
			OutputStream os = socket.getOutputStream();
			os.write(ByteBuffer.allocate(4).putInt(token).array());
		} catch (Exception e) {
		}
		return socket;
	}
	
	private void updateChat() {
		int max = 0;
		if (chat.size() > 10) {
			max = chat.size() - 10;
		}
		String text = "<html><body>";
		for (int i = max; i < chat.size(); i++) {
			text += chat.get(i) + "<br>";
		}
		for (int i = 0; i < 10 - chat.size() + max; i++) {
			text += "<br>";
		}
		chatBox.setText(text + "</body></html>");
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
						
						baos = new ByteArrayOutputStream();
						while (chatsToRead > 0) {
							baos.write((chat.get(chat.size() - chatsToRead) + "\n").getBytes());
							chatsToRead--;
						}
						bytes = baos.toByteArray();
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
						byte[] buf = new byte[len];
						int read = 0;
						while (read < len) {
							read += dis.read(buf, read, len - read);
						}
						InputStream in = new ByteArrayInputStream(buf);
						BufferedImage img = ImageIO.read(in);
						remoteCam.setIcon(new ImageIcon(img));
						
						len = dis.readInt();
						if (len > 0) {
							buf = new byte[len];
							read = 0;
							while (read < len) {
								read += dis.read(buf, read, len - read);
							}
							chat.add(new String(buf, "UTF-8"));
							updateChat();
						}
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
		panel.setMaximumSize(panel.getMaximumSize());
		panel.setMinimumSize(panel.getMinimumSize());
		remoteCam = new JLabel(new ImageIcon(noConnection)) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(480, 360);
			}
		};
		remoteCam.setMaximumSize(remoteCam.getMaximumSize());
		remoteCam.setMinimumSize(remoteCam.getMinimumSize());
		
		JFrame window = new JFrame("UW Chat Roulette");
		window.setPreferredSize(new Dimension(965, 585));
		window.add(remoteCam, BorderLayout.EAST);
		window.add(panel, BorderLayout.WEST);
		
		JTextField name = new JTextField("Name", 20);
		name.setMinimumSize(name.getPreferredSize());
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
				chat = new ArrayList<String>();
				chatsToRead = 0;
				updateChat();
				remoteCam.setIcon(new ImageIcon(noConnection));
				String response = communicateWithMasterServer();
				Socket relay = setupConnectionWithRelay(response);
				sendWebcamFrames(relay);
				getWebcamFrames(relay);
			}
		});
		chatBox = new JLabel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(380, 160);
			}
			
			@Override
			public Dimension getMinimumSize() {
				return new Dimension(380, 160);
			}
		};
		updateChat();
		JTextField chatField = new JTextField("", 50);
		chatField.setMinimumSize(chatField.getPreferredSize());
		chatField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					chat.add(name.getText() + ": " + chatField.getText());
					chatField.setText("");
					chatsToRead++;
					updateChat();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		JPanel otherStuff = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(480, 225);
			}
		};
		otherStuff.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		otherStuff.add(name, c);
		c.gridy = 1;
		otherStuff.add(chatBox, c);
		c.gridy = 2;
		otherStuff.add(chatField, c);
		c.gridy = 3;
		otherStuff.add(button, c);
		
		window.add(otherStuff, BorderLayout.SOUTH);
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
