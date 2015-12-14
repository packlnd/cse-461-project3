import java.awt.Button;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Display extends JFrame{
	private JLabel client;
	
	public Display(String name, Client c) {
		super(name);
		this.setLayout(new GridLayout(2, 1));
		client = c;
		this.add(client);
		Button b = new Button("New Connection");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				client = new JLabel();
				client.add(new Button("Changed"));
			}
		});
		this.add(b);
		this.setSize(400, 400);
	}
	
	 public static void main(String[] args) {
		 Display d = new Display("Webcam", new Client());
		 d.setVisible(true);
	 }
}
