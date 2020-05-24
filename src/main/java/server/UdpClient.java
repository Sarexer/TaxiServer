package server;

import java.io.IOException;
import java.net.*;

public class UdpClient {
    private DatagramSocket socket;
    private InetAddress address;

    private byte[] buf;

    public UdpClient() {
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName("178.46.154.134");
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) {
        buf = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length, address, 12297);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        socket.close();
    }
}
