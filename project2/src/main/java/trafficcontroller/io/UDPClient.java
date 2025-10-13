package trafficcontroller.io;

import java.io.IOException;
import java.net.*;

public class UDPClient {

    private final DatagramSocket socket;
    private final int port;

    public UDPClient(int port) throws SocketException {
        socket = new DatagramSocket();
        this.port = port;
    }

    public String sendUPDMessage(String message) throws IOException {
         // can bind to any free port

        byte[] buffer = message.getBytes();

        InetAddress address = InetAddress.getByName("localhost"); // Python host
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, this.port);
        System.out.println("Sent message" + message + " port " + packet.getPort());
        socket.send(packet); // send UDP packet

        return "Sent message";
    }

    public void close() {
        socket.close();
    }

}

