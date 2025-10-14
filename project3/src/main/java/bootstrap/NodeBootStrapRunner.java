package bootstrap;

import node.RaftNode;

import java.io.IOException;
import java.util.Scanner;

public class NodeBootStrapRunner {

    public static void main(String[] args) throws IOException {
        int nodeId = Integer.parseInt(args[0]);
        RaftNode raftNode = new RaftNode(nodeId);
        raftNode.startServer();

        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.print("Node" + nodeId + ">");
            String line = scanner.nextLine();
            String[] parts = line.split(" ", 2);
            int destinationNode = Integer.parseInt(parts[0]);
            String message = parts[1];
            raftNode.send(destinationNode, message);
        }
    }
}
