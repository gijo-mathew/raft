package core;

import config.RaftConfig;
import io.RaftTransport;
import log.RaftLog;
import message.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RaftController {

    private final RaftTransport raftTransport;
    private final RaftLog raftLog;
    private final int nodeId;
    private int currentTerm;
    private volatile String role;
    //TODO votedfor
    private final Map<Integer, Peer> peers;
    private volatile int commitIndex;
    private ExecutorService executorService;

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
        //for each follower -> send AE message
    }

    public void becomeLeader() {
        this.role = "leader";
        /*for each server, index of the next log entry
        to send to that server (initialized to leader
        last log index + 1)
        initializing to size as we have dummy command in all nodes at index zero
        */
        peers.values().forEach(peer -> peer.setNextIndex(raftLog.getLog().size()));

        executorService = Executors.newFixedThreadPool(peers.size()+1);
        for (Peer peer : peers.values()) {
            executorService.submit(() -> executeReplicationLoop(peer));
        }
        executorService.submit(this::executeCommitLoop);

        //updateFollowers();
    }

    public void executeCommitLoop() {

        while (this.role.equalsIgnoreCase("leader")) {
            try {
                updateCommitIndex();
                Thread.sleep(RaftConfig.COMMIT_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    }

    public void updateCommitIndex() {
        int lastLogIndex = raftLog.getLog().size() - 1;

        // Search from commitIndex+1 up to lastLogIndex
        for (int n = commitIndex + 1; n <= lastLogIndex; n++) {
            LogEntry entry = raftLog.getLog().get(n);

            // Only commit entries from current term
            if (entry.getTerm() != currentTerm) {
                continue;
            }

            // Count how many servers have this entry
            int replicationCount = 1;  // Start with 1 for leader itself

            for (Peer peer : peers.values()) {
                if (peer.getMatchIndex() >= n) {
                    replicationCount++;
                }
            }

            // Check if majority
            int majority = (peers.size() + 1) / 2 + 1;  // +1 includes leader
            if (replicationCount >= majority) {
                commitIndex = n;
                System.out.println("Committed up to index " + n);
                // Continue checking higher indices
            } else {
                // If this index doesn't have majority,
                // higher indices won't either (monotonic property)
                break;
            }
        }

    }

    public void executeReplicationLoop(Peer peer) {
        while(role.equalsIgnoreCase("leader")){

            try {
                //TODO ADD TIMEOUT for response
                AppendEntryResponse response = sendAppendEntryToFollower(peer);
                if (response != null) {
                    onAppendEntriesResponse(response);
                }
            } catch (Exception e) {
                System.err.println("Error replicating to peer " + peer.nodeId + ": " + e.getMessage());
                // Continue loop - don't let one error kill the thread
            }

            try {
                Thread.sleep(RaftConfig.HEARTBEAT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public AppendEntryResponse sendAppendEntryToFollower(Peer peer) {
        int nextIndex = peer.getNextIndex(); //on start -> 1
        int prevIndex = nextIndex-1; // on start -> 0
        LogEntry prevEntry = raftLog.getLog().get(prevIndex);
        int prevTerm = prevEntry.getTerm();
        // handle empty logentry if follower is up to date
        //TODO - failure case
        System.out.println("Sending append entry request to node "+peer.nodeId + " wiht nextIndex "+nextIndex);
        List<LogEntry> logEntries = raftLog.getLog(nextIndex);
        AppendEntryRequest appendEntryRequest = new AppendEntryRequest(
                currentTerm, nodeId, prevIndex, prevTerm, logEntries,  commitIndex);
        AppendEntryResponse appendEntryResponse =raftTransport.sendAppendEntries(peer.nodeId, appendEntryRequest);
        System.out.println("sent append entry request to node " +peer.nodeId + " from" + Thread.currentThread().getName());
        return appendEntryResponse;
    }

    public void becomeFollower() {
        this.role = "follower";
        System.out.println("Making "+nodeId + "follower");
        if (executorService != null) {
            executorService.shutdownNow();  // Interrupt all threads
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }
    }

    public void updateFollowers(){


        for(Peer peer: peers.values()){

            int nextIndex = peer.getNextIndex(); //on start -> 1
            int prevIndex = nextIndex-1; // on start -> 0
            LogEntry prevEntry = raftLog.getLog().get(prevIndex);
            int prevTerm = prevEntry.getTerm();
            // handle empty logentry if follower is up to date
            //TODO - failure case
            System.out.println("Sending append entry request to node "+peer.nodeId + " wiht nextIndex "+nextIndex);
            List<LogEntry> logEntries = raftLog.getLog(nextIndex);
            AppendEntryRequest appendEntryRequest = new AppendEntryRequest(
                    currentTerm, nodeId, prevIndex, prevTerm, logEntries,  commitIndex);
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

        System.out.println("Updating append entry response from thread" + Thread.currentThread().getName());
        if(appendEntryResponse.isSuccess()) {
            System.out.println("Append entry response success from node "+appendEntryResponse.getFollowerId());
            System.out.println("Append entry response match index"+ appendEntryResponse.getMatchIndex());
            peers.get(appendEntryResponse.getFollowerId()).setNextIndex(appendEntryResponse.getMatchIndex()+1);
            peers.get(appendEntryResponse.getFollowerId()).setMatchIndex(appendEntryResponse.getMatchIndex());
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
            //TODO -> update commit index
            this.commitIndex = Math.min(appendEntryRequest.getCommitIndex(), raftLog.getLog().size());
            int updatedMatchIndex = isAppendSuccess ?
                    appendEntryRequest.getPrevLogIndex() + appendEntryRequest.getEntries().size() :
                    0;
            AppendEntryResponse appendEntryResponse = new AppendEntryResponse(nodeId,
                    currentTerm,
                    isAppendSuccess,
                    updatedMatchIndex);
            System.out.println("Returning AppendEntryResponse");
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

    public int getLeaderLastLogIndex() {
        return leaderLastLogIndex;
    }

    public int getNodeId() {
        return nodeId;
    }

    public Map<Integer, Peer> getPeers() {
        return peers;
    }

    public String getRole() {
        return role;
    }
}
