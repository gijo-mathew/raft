package bootstrap;

import message.AppendEntryRequest;
import message.LogEntry;
import node.RaftServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class NodeBootStrapRunner {

    public static void main(String[] args) throws IOException {
        int nodeId = Integer.parseInt(args[0]);
        RaftServer raftServer = new RaftServer(nodeId);
        raftServer.startServer();

        Scanner scanner = new Scanner(System.in);
        int index = 0;
        while(true) {
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
        }
    }
}
