import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class EchoClient {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345);
        Scanner scanner = new Scanner(System.in);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        while (true) {
            System.out.print("Input > ");
            String msg = scanner.nextLine();
            if (msg.isEmpty()) break;

            MessageUtils.sendMessage(out, msg.getBytes());
            byte[] response = MessageUtils.recvMessage(in);
            System.out.println("Received > " + new String(response));
        }
        socket.close();
    }
}
