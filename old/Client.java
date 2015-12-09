import java.net.*;
import java.io.*;
import java.util.Vector;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import java.awt.*;

public class Client {
    // "server"
    final String host = "localhost";
    final int port = 1234;
    //private static String PORT = "10000";
    //private static InetAddress addr;
    static final Format[] FORMATS = { new VideoFormat("rgb") };
    static final ContentDescriptor CONTENT_DESCRIPTOR = new ContentDescriptor("raw.rtp");

    // "client"
    Player player = null;
    private MediaLocator mediaLocator;
    //private static String PORT = "10000";
    //private static InetAddress addr;

    public Client() {
        String response = communicateWithServer();
        if (isMaster(response)) {
            fakeServer();
        } else {
            fakeClient();
        }
    }

    private void fakeServer() {
        try {
            Vector list = CaptureDeviceManager.getDeviceList(null);
            for (Object cdi : list)
                System.out.println(((CaptureDeviceInfo)cdi).getName());
            CaptureDeviceInfo wcI = (CaptureDeviceInfo)list.get(0);
            System.out.println(wcI.getName());
            DataSource source = Manager.createDataSource(wcI.getLocator());
            Format outputFormat[] = new Format[2];
            outputFormat[0] = new VideoFormat(VideoFormat.H263_RTP);
            Processor mediaProcessor = Manager.createRealizedProcessor(
                    new ProcessorModel(source, outputFormat, CONTENT_DESCRIPTOR));
            MediaLocator outputMediaLocator = new MediaLocator("rtp://localhost:20001/video");
            DataSink dataSink = Manager.createDataSink(mediaProcessor.getDataOutput(), outputMediaLocator);

            System.out.println(mediaProcessor);
            mediaProcessor.start();
            dataSink.open();
            dataSink.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void fakeClient() {
        mediaLocator = new MediaLocator("rtp://localhost:20001/video");
        //setLayout(new BorderLayout());
        try {
            player = Manager.createPlayer(mediaLocator);
            player.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isMaster(String s) {
        return !s.equals("$");
    }

    private String communicateWithServer() {
        String response = null;
        try {
            Socket serverSocket = new Socket(host, port);
            DataOutputStream toServer =
                new DataOutputStream(serverSocket.getOutputStream());
            BufferedReader fromServer =
                new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            response = fromServer.readLine();
            serverSocket.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return response;
    }

    public static void main(String[] args) {
        new Client();
    }
}
