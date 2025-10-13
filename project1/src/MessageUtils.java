import java.io.*;

public class MessageUtils {

    public static void sendMessage(OutputStream out, byte[] msg) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(msg.length); // 4-byte length prefix
        dos.write(msg);
        dos.flush();
    }

    public static byte[] recvMessage(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        int size = dis.readInt(); // Read message length
        byte[] msg = new byte[size];
        dis.readFully(msg);       // Read exact number of bytes
        return msg;
    }
}
