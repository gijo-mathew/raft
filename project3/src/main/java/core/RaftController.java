package core;

import io.RaftTransport;
import log.RaftLog;
import message.*;

import java.util.List;
import java.util.Map;

public class RaftController {

    private final RaftTransport raftTransport;
    private final RaftLog raftLog;
    private final int nodeId;
    private int currentTerm;
    private String role;
    //TODO votedfor
    private int commitIndex;
    private final Map<Integer, Peer> peers;
    private int leaderCommit;

    //index of the last entry in the leader’s local log.
    private int leaderLastLogIndex;

    public RaftController(RaftTransport raftTransport, int nodeId, Map<Integer, Peer> peers) {
        this.raftTransport = raftTransport;
        this.raftLog = new RaftLog();
        this.nodeId = nodeId;
        this.role = "follower";
        this.peers = peers;
        /*
        * Initially current term to zero
        * */
        this.currentTerm = 0;
    }

    public void becomeLeader() {
        this.role = "leader";
        /*for each server, index of the next log entry
        to send to that server (initialized to leader
        last log index + 1)
        initializing to size as we have dummy command in all nodes at index zero
        */
        peers.values().forEach(peer -> peer.setNextIndex(raftLog.getLog().size()));
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    /**
     * This happens on the leader
     *
     */
    public void onNewClientCommand(ClientCommandRequest command){
        // write command to leader node
        if(!role.equalsIgnoreCase("leader")){
            System.out.println("Not leader");
            throw new RuntimeException("Not leader");
        }
        raftLog.appendNewCommand(currentTerm, command.getCommand());
        updateFollowers();
        //for each follower -> send AE message
    }


    public void updateFollowers(){

        for(Peer peer: peers.values()){

            int nextIndex = peer.nextIndex; //on start -> 1
            int prevIndex = nextIndex-1; // on start -> 0
            LogEntry prevEntry = raftLog.getLog().get(prevIndex);
            int prevTerm = prevEntry.getTerm();
            // handle empty logentry if follower is up to date
            //TODO - failure case
            System.out.println("Sending append entry request to node "+peer.nodeId + " wiht nextIndex "+nextIndex);
            List<LogEntry> logEntries = raftLog.getLog(nextIndex);
            AppendEntryRequest appendEntryRequest = new AppendEntryRequest(
                    currentTerm, nodeId, prevIndex, prevTerm, logEntries,  leaderCommit);
            AppendEntryResponse appendEntryResponse =raftTransport.sendAppendEntries(peer.nodeId, appendEntryRequest);
            System.out.println("sent append entry request to node "+peer.nodeId);
            onAppendEntriesResponse(appendEntryResponse);
        }


        //send response which is handled by appendEntries in followers
    }

    public boolean onAppendEntriesRequest(AppendEntryRequest appendEntryRequest) {
        return raftLog.appendEntries(appendEntryRequest.getPrevLogIndex(),
                appendEntryRequest.getPrevLogTerm(),
                appendEntryRequest.getEntries());
        //send response which is handled by appendEntryResponse
    }

    public void onAppendEntriesResponse(AppendEntryResponse appendEntryResponse) {

        System.out.println("Updating append entry response");
        if(appendEntryResponse.isSuccess()) {
            System.out.println("Append entry response success from node "+appendEntryResponse.getFollowerId());
            peers.get(appendEntryResponse.getFollowerId()).setNextIndex(appendEntryResponse.getMatchIndex()+1);
        }else {
            System.out.println("Append entry response failed for node "+appendEntryResponse.getFollowerId());
            int nextIndex = peers.get(appendEntryResponse.getFollowerId()).getNextIndex();
            peers.get(appendEntryResponse.getFollowerId()).setNextIndex(nextIndex-1);
        }


        /*else:
            # It failed
        self.next_index[msg.follower_id] -= 1
        self.update_single_follower(msg.follower_id)   # Retry*/

    }


    public Object handleMessage(Object message) {
        System.out.println("handling message "+ message.getClass());
        if(message instanceof AppendEntryRequest appendEntryRequest){
            System.out.println("Received AppendEntryRequest command ");

            boolean isAppendSuccess = onAppendEntriesRequest(appendEntryRequest);

            AppendEntryResponse appendEntryResponse = new AppendEntryResponse(nodeId,
                    0,
                    isAppendSuccess,
                    appendEntryRequest.getPrevLogIndex() + appendEntryRequest.getEntries().size());
            System.out.println("Returning AppendEntryResponse " + appendEntryRequest.getEntries().get(0).getCommand() + "and response OK");
            return appendEntryResponse;
        }
        else if(message instanceof ClientCommandRequest clientCommandRequest) {
            System.out.println("Received client command request"+ clientCommandRequest.getCommand());

            onNewClientCommand(clientCommandRequest);

            ClientCommandResponse clientCommandResponse = new ClientCommandResponse("success");
            System.out.println("Returning ClientCommandResponse response");
            return clientCommandResponse;
        }else if(message instanceof AppendEntryResponse appendEntryResponse) {
            System.out.println("Received AppendEntry Response");
            return appendEntryResponse;
        }
        else {
            throw new RuntimeException("Invalid message");
        }
    }

    public RaftLog getRaftLog() {
        return raftLog;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public int getLeaderLastLogIndex() {
        return leaderLastLogIndex;
    }

    public int getNodeId() {
        return nodeId;
    }
}
