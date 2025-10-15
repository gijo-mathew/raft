package node;

import config.RaftConfig;
import io.RaftConnection;
import message.AppendEntryRequest;
import message.AppendEntryResponse;
import message.Message;
import message.MessageHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RaftServer {

    int nodeNumber;
    RaftConfig.NodeAddress nodeAddress;
    ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private Map<Integer, RaftConnection> outboundConnectionMap = new ConcurrentHashMap<>();

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
                    handleIncomingConnection(clientSocket);
                });
            }
        });
        System.out.println("Started node" + this.nodeNumber);

    }

    /*public void send(int destinationNodeId, Message message)  {
        this.sendAppendEntries(destinationNodeId, (AppendEntryRequest) message);
    }

    public void receiveMessage(Socket clientSocket) {
        this.handleIncomingConnection(clientSocket);
    }
*/
    public void sendAppendEntries(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        //MessageHandler.sendAppendEntry(destinationNodeId, appendEntryRequest);
        RaftConnection conn = getOutboundConnection(destinationNodeId);
        conn.send(appendEntryRequest);
        AppendEntryResponse response = (AppendEntryResponse) conn.receive();
        if(response.isSuccess()){
            System.out.println("Sent message successfully");
        }
    }

    public void handleIncomingConnection(Socket clientSocket) {
        MessageHandler.receiveAppendEntry(clientSocket);
    }

    public RaftConnection getOutboundConnection(int nodeId) {
        return outboundConnectionMap.computeIfAbsent(nodeId, k -> {
            RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(nodeId);
            try{
                Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());
                return new RaftConnection(socket);
            } catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
}
