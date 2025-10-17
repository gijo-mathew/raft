package node;

import config.RaftConfig;
import core.Peer;
import core.RaftController;
import io.RaftConnection;
import io.RaftTransport;
import message.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RaftServer implements RaftTransport {

    int nodeNumber;
    RaftConfig.NodeAddress nodeAddress;
    ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private RaftController raftController;

    private Map<Integer, RaftConnection> outboundConnectionMap = new ConcurrentHashMap<>();

    private Map<Integer, Socket> socketCache = new HashMap<>();

    public RaftServer(int nodeNumber, Map<Integer, Peer> peers) {
        this.nodeNumber = nodeNumber;
        this.nodeAddress = RaftConfig.NODES.get(nodeNumber);
        this.raftController = new RaftController(this, nodeNumber, peers);
        try {
            this.serverSocket = new ServerSocket(nodeAddress.getPort());
            System.out.println("starting server "+ nodeNumber + " on port " + nodeAddress.getPort() + " as follower");
        } catch (Exception e) {

            System.out.println("Unable to start node" + nodeNumber);
            if (raftController != null) {
                raftController.shutdown();
            }
            e.printStackTrace();
            System.exit(1);
        }
    }

    public RaftServer(int nodeNumber, boolean isLeader, Map<Integer, Peer> peers) {
        this.nodeNumber = nodeNumber;
        this.nodeAddress = RaftConfig.NODES.get(nodeNumber);
        this.raftController = new RaftController(this, nodeNumber, peers);
        if(isLeader) {
            raftController.becomeLeader();
        }
        try {
            this.serverSocket = new ServerSocket(nodeAddress.getPort());
            System.out.println("starting server "+ nodeNumber + " on port " + nodeAddress.getPort() + " as leader");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to start node" + nodeNumber);
            if (raftController != null) {
                raftController.shutdown();
            }
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        int nodeId = Integer.parseInt(args[0]);
        Map<Integer, Peer> peers = RaftConfig.NODES
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != nodeId)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry ->  new Peer(entry.getKey())
                ));
        System.out.print("Peer list for node "+nodeId + " -> ");
        peers.entrySet().forEach(peer -> System.out.println(peer.getValue().getNodeId() + ", "));
        RaftServer raftServer;
        if(args.length>1) {
            boolean isLeader = Boolean.parseBoolean(args[1]);
            raftServer = new RaftServer(nodeId, isLeader, peers);
        }else {
            raftServer = new RaftServer(nodeId, peers);
        }


        raftServer.startServer();
    }

    public void startServer() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered");
                if (raftController != null) {
                    raftController.shutdown();
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }));

            executor.submit(() -> {
                while(true) {
                    Socket clientSocket = serverSocket.accept();
                    //System.out.println("New connection between " + nodeNumber + " & "+ clientSocket.getRemoteSocketAddress());
                    executor.submit(() -> {
                        handleIncomingConnection(clientSocket);
                    });
                }
            });
            System.out.println("Started node" + this.nodeNumber);
        } catch (Exception e) {
            System.err.println("FATAL: Server error");
            e.printStackTrace();
            if (raftController != null) {
                raftController.shutdown();
            }
            System.exit(1);
        }


    }

    /*public void handleIncomingConnection(Socket clientSocket) {
        //MessageHandler.receiveAppendEntry(clientSocket);
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
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

    }*/

    /*public void handleIncomingConnection(Socket clientSocket) {
        try {
            // 1. INPUT STREAM FIRST (Server waits for client's OOS header)
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("Receiving message from socket");

            // 2. READ OBJECT
            Object input = in.readObject();

            // 3. PROCESS/HANDLE MESSAGE
            Object output = raftController.handleMessage(input);

            // 4. OUTPUT STREAM NEXT (Server sends its response)
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.writeObject(output);
            out.flush();
            System.out.println("Flushed output to socket");
        } catch (IOException e) {
            throw new RuntimeException(" Error"+e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }*/

    /*public void handleIncomingConnection(Socket clientSocket) {
        try {
            // CREATE OUTPUT STREAM FIRST (same as client)
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush(); // Send header immediately

            // THEN create input stream
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("Receiving message from socket");

            // Read the request
            Object input = in.readObject();

            // Process the request
            Object output = raftController.handleMessage(input);

            // Send response
            out.writeObject(output);
            out.flush();
            System.out.println("Flushed output to socket");

        } catch (IOException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }*/

    public void handleIncomingConnection(Socket clientSocket) {
        //System.out.println("=== New connection from: " + clientSocket.getRemoteSocketAddress() + " ===");
        try {
            //System.out.println("Creating OOS...");
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            //System.out.println("OOS created. Creating OIS...");
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            //System.out.println("OIS created. Reading object...");

            Object input = in.readObject();
            //System.out.println("Received: " + input.getClass().getSimpleName());

            Object output = raftController.handleMessage(input);
            //System.out.println("Processed. Sending response...");

            out.writeObject(output);
            out.flush();
            //System.out.println("Response sent.");

        } catch (IOException e) {
            boolean wasInterrupted = Thread.interrupted();  // Also clears the flag
            System.err.println(" IOException caught!");
            System.err.println("   Exception type: " + e.getClass().getName());
            System.err.println("   Exception message: " + e.getMessage());
            System.err.println("   Thread was interrupted: " + wasInterrupted);
            System.err.println("   Thread: " + Thread.currentThread().getName());
            e.printStackTrace();
            throw new RuntimeException("Error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            //  Check if this IOException was caused by interrupt
            boolean wasInterrupted = Thread.interrupted();  // Also clears the flag
            System.err.println("   IOException caught!");
            System.err.println("   Exception type: " + e.getClass().getName());
            System.err.println("   Exception message: " + e.getMessage());
            System.err.println("   Thread was interrupted: " + wasInterrupted);
            System.err.println("   Thread: " + Thread.currentThread().getName());
            e.printStackTrace();
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public VoteResponse sendRequestVote(int destinationNodeId, VoteRequest req) {
        return SocketsUtil.sendVoteRequest(destinationNodeId, req);
    }


    /*public void send(int destinationNodeId, Message message)  {
        this.sendAppendEntries(destinationNodeId, (AppendEntryRequest) message);
    }

    public void receiveMessage(Socket clientSocket) {
        this.handleIncomingConnection(clientSocket);
    }
*/
    public AppendEntryResponse sendAppendEntries(int destinationNodeId, AppendEntryRequest appendEntryRequest) {
        return SocketsUtil.sendAppendEntry(destinationNodeId, appendEntryRequest);
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

    public String getRole() {
        return raftController.getRole();
    }
}
