package node;

import config.RaftConfig;
import message.AppendEntryRequest;
import message.Message;
import message.MessageHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RaftServer {

    int nodeNumber;
    RaftConfig.NodeAddress nodeAddress;
    ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private Map<Integer, Socket> socketCache = new HashMap<>();

    public RaftServer(int nodeNumber) {
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
                    receiveAppendEntries(clientSocket);
                });
            }
        });
    }

    public void send(int destinationNodeId, Message message)  {
        this.sendAppendEntries(destinationNodeId, (AppendEntryRequest) message);
    }

    public void receiveMessage(Socket clientSocket) {
        this.receiveAppendEntries(clientSocket);
    }

    public void sendAppendEntries(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        MessageHandler.sendAppendEntry(destinationNodeId, appendEntryRequest);
    }

    public void receiveAppendEntries(Socket clientSocket) {
        MessageHandler.receiveAppendEntry(clientSocket);
    }
}
