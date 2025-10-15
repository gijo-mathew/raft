package node;

import config.RaftConfig;
import core.RaftController;
import io.RaftConnection;
import message.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RaftServer {

    int nodeNumber;
    RaftConfig.NodeAddress nodeAddress;
    ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private RaftController raftController;

    private Map<Integer, RaftConnection> outboundConnectionMap = new ConcurrentHashMap<>();

    private Map<Integer, Socket> socketCache = new HashMap<>();

    public RaftServer(int nodeNumber) {
        this.nodeNumber = nodeNumber;
        this.nodeAddress = RaftConfig.NODES.get(nodeNumber);
        this.raftController = new RaftController(nodeNumber);
        try {
            this.serverSocket = new ServerSocket(nodeAddress.getPort());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to start node" + nodeNumber);
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) throws IOException {
        int nodeId = Integer.parseInt(args[0]);
        RaftServer raftServer = new RaftServer(nodeId);
        raftServer.startServer();
        System.out.println("started server "+ nodeId);
    }

    public void startServer() {
        executor.submit(() -> {
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection between " + nodeNumber + " & "+ clientSocket.getRemoteSocketAddress());
                executor.submit(() -> {
                    handleIncomingConnection(clientSocket);
                });
            }
        });
        System.out.println("Started node" + this.nodeNumber);

    }

    public void handleIncomingConnection(Socket clientSocket) {
        //MessageHandler.receiveAppendEntry(clientSocket);
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            System.out.println("Receiving message from socket");
            Object input = in.readObject();
            Object output = raftController.handleMessage(input);
            out.writeObject(output);
            out.flush();
            System.out.println("Flushed output to socket");
        }catch (IOException e) {
           throw new RuntimeException(" Error"+e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    /*public void send(int destinationNodeId, Message message)  {
        this.sendAppendEntries(destinationNodeId, (AppendEntryRequest) message);
    }

    public void receiveMessage(Socket clientSocket) {
        this.handleIncomingConnection(clientSocket);
    }
*/
    public void sendAppendEntries(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        MessageHandler.sendAppendEntry(destinationNodeId, appendEntryRequest);
       /* RaftConnection conn = getOutboundConnection(destinationNodeId);
        System.out.println("conn" + conn.hashCode());
        conn.send(appendEntryRequest);
        AppendEntryResponse response = (AppendEntryResponse) conn.receive();
        if(response.isSuccess()){
            System.out.println("Sent message successfully");
        }*/
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
