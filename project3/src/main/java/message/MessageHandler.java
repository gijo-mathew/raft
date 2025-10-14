package message;

import java.io.*;

public class MessageHandler {

    public static void sendMessage(OutputStream outputStream, String msg) throws IOException {
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeInt(msg.getBytes().length); // 4-byte length prefix
        dos.write(msg.getBytes());
        dos.flush();
    }

    public static String receiveMessage(InputStream inputStream) throws IOException {
        DataInput dataInputStream = new DataInputStream(inputStream);
        int length = dataInputStream.readInt();
        byte[] bytes = new byte[length];
        dataInputStream.readFully(bytes);
        String message = new String(bytes);
        System.out.println(message);
        return  message;
    }

}
