package message;

import config.RaftConfig;

import java.io.*;
import java.net.Socket;

public class MessageHandler {

    public static void sendMessage(int destinationNodeId, String message ) throws IOException {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(destinationNodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeInt(message.getBytes().length); // 4-byte length prefix
            dos.write(message.getBytes());
            dos.flush();
            receiveMessage(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void sendMessage(OutputStream outputStream, String message) {

        try {
            DataOutputStream dos = new DataOutputStream(outputStream);
            dos.writeInt(message.getBytes().length); // 4-byte length prefix
            dos.write(message.getBytes());
            dos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void receiveMessage(InputStream inputStream) {

        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int size = dataInputStream.readInt();
            byte[] bytes = new byte[size];
            dataInputStream.readFully(bytes);
            String message = new String(bytes);
            System.out.println(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String receiveMessage(Socket clientSocket) throws IOException {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            DataInput dataInputStream = new DataInputStream(in);
            int length = dataInputStream.readInt();
            byte[] bytes = new byte[length];
            dataInputStream.readFully(bytes);
            String message = new String(bytes);
            System.out.println(message);
            sendMessage(out, "OK");
            return message;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return  "Failed to send message";
    }

    public static void sendAppendEntry(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(destinationNodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(appendEntryRequest);
            out.flush();
            AppendEntryResponse appendEntryResponse = (AppendEntryResponse)in.readObject();
            System.out.println("Sent message ?:"+appendEntryResponse.isSuccess());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void receiveAppendEntry(Socket clientSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            AppendEntryRequest myObject = (AppendEntryRequest) in.readObject();
            System.out.println("Received command " + myObject.getEntries().get(0).getCommand());
            AppendEntryResponse appendEntryResponse = new AppendEntryResponse(0, true, 1 );
            out.writeObject(appendEntryResponse);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
