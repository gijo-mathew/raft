package message;

import config.RaftConfig;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

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

    /*public static void sendAppendEntry(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(destinationNodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            System.out.println("Sending append entry message");
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
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
    }*/



    /*public static void sendAppendEntry(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(destinationNodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            // Create OUTPUT stream first
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Send header immediately

            // Then create INPUT stream
            System.out.println("Sending append entry");
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Now communicate
            out.writeObject(appendEntryRequest);
            out.flush();

            ClientCommandResponse clientCommandResponse = (ClientCommandResponse) in.readObject();
            System.out.println("Response: " + clientCommandResponse.getCommandResponse());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
*/

    public static AppendEntryResponse sendAppendEntry(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(destinationNodeId);
        System.out.println("Connecting to node " + destinationNodeId + " at " + nodeAddress.getAddress() + ":" + nodeAddress.getPort());

        Socket socket = null;
        try{
            socket = new Socket();
            socket.connect(
                    new InetSocketAddress(nodeAddress.getAddress(), nodeAddress.getPort()),
                    RaftConfig.SOCKET_CONNECTION_TIMEOUT_MS  // e.g., 50ms
            );

            // Set read timeout
            socket.setSoTimeout(RaftConfig.SOCKET_READ_TIMEOUT_MS);  // e.g., 100ms
            System.out.println("Connected! Creating OOS...");
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            System.out.println("OOS created. Creating OIS...");
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("OIS created. Writing object...");

            out.writeObject(appendEntryRequest);
            out.flush();
            System.out.println("Object written. Waiting for response...");

            Object response = in.readObject();
            System.out.println(" AppendEntryResponse Response received: " + response);
            return (AppendEntryResponse)response;

        }catch (SocketTimeoutException e) {
            System.err.println("Timeout communicating with node " + destinationNodeId +
                    " (connection or read timeout)");
            return null;

        } catch (ConnectException e) {
            System.err.println("Cannot connect to node " + destinationNodeId +
                    " - node might be down or not listening");
            return null;

        } catch (IOException e) {
            System.err.println("IOException in sendAppendEntry to node " + destinationNodeId);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
        return null;
    }

    public static Object receiveMessageFromSocket(Socket clientSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            return in.readObject();
        } catch (IOException e) {
            System.out.println("Error "+e.getMessage());
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessageToSocket(Socket clientSocket, Object object) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(object);
            out.flush();
            System.out.println("Flushed output to socket");
        } catch (IOException e) {
            System.out.println("Error "+e.getMessage());
            throw new RuntimeException(e);
        }

    }

    /*public static void receiveAppendEntry(Socket clientSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

            Object object = in.readObject();
            if( object instanceof AppendEntryRequest ){
                AppendEntryRequest appendEntryRequest = (AppendEntryRequest) object;
                System.out.println("Received AppendEntryRequest command " + appendEntryRequest.getEntries().get(0).getCommand());
                AppendEntryResponse appendEntryResponse = new AppendEntryResponse(0, true, 1 );
                out.writeObject(appendEntryResponse);
                out.flush();
                System.out.println("Received AppendEntryRequest command " + appendEntryRequest.getEntries().get(0).getCommand() + "and response OK");
            }
            if( object instanceof ClientCommandRequest) {
                ClientCommandRequest clientCommandRequest = (ClientCommandRequest) object;
                System.out.println("Received client command request"+ clientCommandRequest.getCommand());
                ClientCommandResponse clientCommandResponse = new ClientCommandResponse("success");
                out.writeObject(clientCommandResponse);
                out.flush();
                System.out.println("Sent response");
            }


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
    }*/


}
