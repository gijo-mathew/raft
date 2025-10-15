package core;

import log.RaftLog;
import message.AppendEntryRequest;
import message.AppendEntryResponse;
import message.ClientCommandRequest;
import message.ClientCommandResponse;

public class RaftController {

    private RaftLog raftLog;
    private final int nodeId;
    private int leaderTerm;
    private String role;


    public RaftController(int nodeId) {
        this.raftLog = new RaftLog();
        this.nodeId = nodeId;
    }

    /**
     * This happens on the leader
     *
     */
    public void onNewClientCommand(String command){
        // write command to leader node
        raftLog.appendNewCommand(leaderTerm, command);

        //for each follower -> send AE message

    }


    public void updateFollowers(){

        //send response which is handled by appendEntries in followers
    }

    public void onAppendEntriesRequest() {
        //send response which is handled by appendEntryResponse
    }

    public void onAppendEntriesResponse() {

    }


    public Object handleMessage(Object message) {
        if( message instanceof AppendEntryRequest){
            AppendEntryRequest appendEntryRequest = (AppendEntryRequest) message;
            System.out.println("Received AppendEntryRequest command " + appendEntryRequest.getEntries().get(0).getCommand());
            AppendEntryResponse appendEntryResponse = new AppendEntryResponse(0, true, 1 );
            System.out.println("Returning AppendEntryResponse " + appendEntryRequest.getEntries().get(0).getCommand() + "and response OK");
            return appendEntryResponse;
        }
        else if( message instanceof ClientCommandRequest) {
            ClientCommandRequest clientCommandRequest = (ClientCommandRequest) message;
            System.out.println("Received client command request"+ clientCommandRequest.getCommand());
            ClientCommandResponse clientCommandResponse = new ClientCommandResponse("success");
            System.out.println("Returning ClientCommandResponse response");
            return clientCommandResponse;
        }else {
            throw new RuntimeException("Invalid message");
        }
    }
}
