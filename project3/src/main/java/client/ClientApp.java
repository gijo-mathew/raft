package client;

import config.RaftConfig;
import message.ClientCommandRequest;
import message.ClientCommandResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) throws IOException {
        //int nodeId = Integer.parseInt(args[0]);
        Scanner scanner = new Scanner(System.in);
        int index = 0;
        /*while(true) {
            System.out.print("Node" + nodeId + ">");
            String line = scanner.nextLine();
            String[] parts = line.split(" ", 2);
            int destinationNode = Integer.parseInt(parts[0]);
            String message = parts[1];
            List<LogEntry> logEntries = Arrays.asList(new LogEntry(0, message,0)) ;
            AppendEntryRequest appendEntryRequest = new AppendEntryRequest(0,
                    "0" ,
                    0,
                    ++index,  logEntries, 0 );
            raftServer.sendAppendEntries(destinationNode, appendEntryRequest);
        }*/

        while(true) {
            System.out.print("Client command>");
            String line = scanner.nextLine();
            String[] parts = line.split(" ", 2);
            int destinationNode = Integer.parseInt(parts[0]);
            String message = parts[1];
            ClientCommandRequest clientCommandRequest = new ClientCommandRequest(message);
            sendCommand(clientCommandRequest, destinationNode);

        }
    }

    /*private static void sendCommand(ClientCommandRequest clientCommandRequest, int nodeId) {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(nodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(clientCommandRequest);
            out.flush();
            ClientCommandResponse clientCommandResponse = (ClientCommandResponse) in.readObject();
            System.out.println("Response: " +clientCommandResponse.getCommandResponse() );
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }*/

    private static void sendCommand(ClientCommandRequest clientCommandRequest, int nodeId) {
        RaftConfig.NodeAddress nodeAddress = RaftConfig.NODES.get(nodeId);
        try(Socket socket = new Socket(nodeAddress.getAddress(), nodeAddress.getPort());){
            // Create OUTPUT stream first
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Send header immediately

            // Then create INPUT stream
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Now communicate
            out.writeObject(clientCommandRequest);
            out.flush();

            ClientCommandResponse clientCommandResponse = (ClientCommandResponse) in.readObject();
            System.out.println("Response: " + clientCommandResponse.getCommandResponse());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
