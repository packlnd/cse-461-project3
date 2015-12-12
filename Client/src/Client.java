import java.io.File;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;

public class Client {
	public static void main(String[] args) {
		Webcam webcam = Webcam.getDefault();
		webcam.open();
		try {
			ImageIO.write(webcam.getImage(), "PNG", new File("hello-world.png"));
		} catch (Exception e) {}
	}
}