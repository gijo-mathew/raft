package node;

import config.RaftConfig;
import message.MessageHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RaftNode {

    int nodeNumber;
    RaftConfig.NodeAddress nodeAddress;
    ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public RaftNode(int nodeNumber) {
        this.nodeNumber = nodeNumber;
        this.nodeAddress = RaftConfig.NODES.get(nodeNumber);
        try {
            this.serverSocket = new ServerSocket(nodeAddress.getPort());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to start node" + nodeNumber);
            throw new RuntimeException(e);
        }

    }

    public void startServer() {
        executor.submit(() -> {
            while(true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> {
                    receiveMessage(clientSocket);
                });
            }
        });
    }

    public void send(int destinationNodeId, String message)  {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(destinationNodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            MessageHandler.sendMessage(out, message);
            MessageHandler.receiveMessage(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessage(Socket clientSocket)  {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            String msg = MessageHandler.receiveMessage(in );
            MessageHandler.sendMessage(out, "OK");
        } catch (IOException e) {
           e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
