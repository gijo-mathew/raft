package core;

import config.RaftConfig;
import io.RaftTransport;
import log.RaftLog;
import message.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftController {

    private final RaftTransport raftTransport;
    private final RaftLog raftLog;
    private final int nodeId;
    private AtomicInteger currentTerm = new AtomicInteger();
    private volatile String role;
    private final Map<Integer, Peer> peers;
    private volatile int commitIndex;

    private AtomicInteger votes = new AtomicInteger(0);
    private AtomicInteger voteGrantedFor = new AtomicInteger();

    private volatile ExecutorService logReplicationExecutorService;
    private volatile ExecutorService voteRequestorExecutorService;
    private final int electionTimeout;
    private ScheduledFuture<?> electionTimeoutTask;  // ← Handle to scheduled task
    private ScheduledExecutorService electionTimeoutExecutor;

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
        this.setCurrentTerm(0);

        this.electionTimeout = RaftConfig.ELECTION_TIMEOUT_MIN_MS +
                new Random().nextInt(RaftConfig.ELECTION_TIMEOUT_MAX_MS -
                        RaftConfig.ELECTION_TIMEOUT_MIN_MS);
        this.electionTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        resetElectionTimeout();

    }

    public synchronized void resetElectionTimeout() {
        // Cancel existing timeout if any
        if (electionTimeoutTask != null && !electionTimeoutTask.isDone()) {
            electionTimeoutTask.cancel(false);
            //System.out.println("Node " + nodeId + " cancelled existing election timeout");
        }

        // Don't schedule if leader
        if (role.equals("leader")) {
            System.out.println("Node " + nodeId + " is leader - no election timeout needed");
            return;
        }

        // Schedule new single-shot timeout
        electionTimeoutTask = electionTimeoutExecutor.schedule(
                this::onElectionTimeout,
                electionTimeout,
                TimeUnit.MILLISECONDS
        );

        //System.out.println("Node " + nodeId + " election timeout scheduled for " +electionTimeout + "ms");
    }

    private void onElectionTimeout() {
        //System.out.println("Node " + nodeId + " election timeout fired in term " + currentTerm);

        // Start election if follower or candidate
        if (role.equals("follower") || role.equals("candidate")) {
            System.out.println("Node " + nodeId + " starting election");
            becomeCandidate();
        } else {
            System.out.println("Node " + nodeId + " is leader - ignoring timeout");
        }
    }


    /**
     * This happens on the leader
     *
     */
    public ClientCommandResponse onNewClientCommand(ClientCommandRequest command){
        // write command to leader node
        ClientCommandResponse clientCommandResponse = new ClientCommandResponse();
        if(!role.equalsIgnoreCase("leader")){
            System.out.println("Not leader");
            clientCommandResponse.setCommandResponse("Not leader");
        }else {
            clientCommandResponse.setCommandResponse("ok");
            raftLog.appendNewCommand(getCurrentTerm(), command.getCommand());
        }
        clientCommandResponse.setLeaderLogSize(raftLog.getLog().size());
        clientCommandResponse.setCommitIndex(getCommitIndex());
        List<Peer> peerList = peers.values().stream().toList();
        clientCommandResponse.setPeerStatus(peerList);
        return clientCommandResponse;
        //for each follower -> send AE message
    }

    public void becomeLeader() {
        this.role = "leader";
        /*for each server, index of the next log entry
        to send to that server (initialized to leader
        last log index + 1)
        initializing to size as we have dummy command in all nodes at index zero
        */
        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancel(false);
            System.out.println("Node " + nodeId + " is now leader - cancelled election timeout");
        }
        peers.values().forEach(peer -> peer.setNextIndex(raftLog.getLog().size()));

        logReplicationExecutorService = Executors.newFixedThreadPool(peers.size()+1);
        for (Peer peer : peers.values()) {
            logReplicationExecutorService.submit(() -> executeReplicationLoop(peer));
        }
        logReplicationExecutorService.submit(this::executeCommitLoop);

        //updateFollowers();


    }

    public void executeCommitLoop() {

        while (this.role.equalsIgnoreCase("leader")) {
            try {
                updateCommitIndexonLeader();
                Thread.sleep(RaftConfig.COMMIT_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

    }

    public void updateCommitIndexonLeader() {
        int lastLogIndex = raftLog.getLog().size() - 1;

        // Search from commitIndex+1 up to lastLogIndex
        for (int n = commitIndex + 1; n <= lastLogIndex; n++) {
            LogEntry entry = raftLog.getLog().get(n);

            // Only commit entries from current term
            if (entry.getTerm() != getCurrentTerm()) {
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
        System.out.println("Sending append entry request to node "+peer.nodeId + " wiht nextIndex "+nextIndex);
        List<LogEntry> logEntries = raftLog.getLog(nextIndex);
        AppendEntryRequest appendEntryRequest = new AppendEntryRequest(
                getCurrentTerm(), nodeId, prevIndex, prevTerm, logEntries,  commitIndex);
        AppendEntryResponse appendEntryResponse =raftTransport.sendAppendEntries(peer.nodeId, appendEntryRequest);
        System.out.println("sent append entry request to node " +peer.nodeId + " from" + Thread.currentThread().getName());
        return appendEntryResponse;
    }

    public void becomeCandidate() {
        this.role = "candidate";
        this.setCurrentTerm(getCurrentTerm()+1);
        voteGrantedFor.set(nodeId);
        votes.set(1);

        int lastLogIndex = getRaftLog().getLog().size() - 1;
        int lastLogTerm = getRaftLog().getLog().get(lastLogIndex).getTerm();

        VoteRequest voteRequest = new VoteRequest(getCurrentTerm(),
                nodeId,
                lastLogIndex,
                lastLogTerm
        );

        //send vote request for each candidate
        voteRequestorExecutorService = Executors.newFixedThreadPool(peers.size());
        sendVoteRequest(voteRequest);
        resetElectionTimeout();


    }

    public void sendVoteRequest(VoteRequest voteRequest) {
        List<Future<VoteResponse>> voteResponseFutureList= new ArrayList<>();
        for(Peer peer : peers.values()) {
            Future<VoteResponse> voteResponseFuture = voteRequestorExecutorService.submit(() ->
                    raftTransport.sendRequestVote(peer.getNodeId(),  voteRequest));
            voteResponseFutureList.add(voteResponseFuture);
        }
        int majority = (1 + peers.size())/2 +1;

        for(Future<VoteResponse> voteResponseFuture : voteResponseFutureList) {
            if (!role.equals("candidate")) {
                System.out.println("No longer candidate, stopping election");
                voteRequestorExecutorService.shutdown();
                return;
            }
            try {

                VoteResponse voteResponse = voteResponseFuture.get(RaftConfig.SOCKET_CONNECTION_TIMEOUT_MS +
                        RaftConfig.SOCKET_READ_TIMEOUT_MS + 100, TimeUnit.MILLISECONDS);

                if(voteResponse!=null) {

                    if (voteResponse.getCurrentTerm() > getCurrentTerm()) {
                        System.out.println("Discovered higher term " + voteResponse.getCurrentTerm());
                        setCurrentTerm(voteResponse.getCurrentTerm());
                        becomeFollower();
                        return;  // Stop election
                    }
                    if(voteResponse.getVoteGranted()) {
                        votes.getAndIncrement();
                        if(votes.get()>=majority) {
                            System.out.println("Won election with " + votes.get() + " votes");
                            becomeLeader();
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception for sendVoteRequest");
            } catch (ExecutionException e) {
                System.out.println("Runtime exception for sendVoteRequest "+e.getMessage());
            } catch (TimeoutException e) {
                System.out.println("Timeout exception for vote request");
            }
        }

        if(role.equalsIgnoreCase("candidate")) {
            System.out.println("Lost election with " + votes.get() + "/" + majority + " votes");

        }
        shutdownExecutorsAsync();
    }

    public void becomeFollower() {
        this.role = "follower";
        this.votes.set(0);
        System.out.println("Making "+nodeId + "follower");

        shutdownExecutorsAsync();
        resetElectionTimeout();

    }

    private void shutdownExecutorsAsync() {
        final ExecutorService voteExecutor = voteRequestorExecutorService;
        final ExecutorService replicationExecutor = logReplicationExecutorService;
        new Thread(() -> {
            if (voteExecutor != null) {
                voteExecutor.shutdown();  // Interrupt all threads
                try {
                    voteExecutor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (replicationExecutor != null) {
                replicationExecutor.shutdown();  // Interrupt all threads
                try {
                    replicationExecutor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "node-" + nodeId + "-async-shutdown").start();

    }

    public VoteResponse onVoteRequest(VoteRequest voteRequest) {
        boolean voteGranted = false;

        // Rule 1: Reject if request term is older than our term
        if (voteRequest.getTerm() < getCurrentTerm()) {
            System.out.println("Rejecting vote - term too old");
            return new VoteResponse(false, nodeId, voteRequest.getCandidateId(), getCurrentTerm());
        }

        // Rule 2: If request term is newer, update our term and become follower
        if (voteRequest.getTerm() > getCurrentTerm()) {
            setCurrentTerm(voteRequest.getTerm());
            becomeFollower();
            voteGrantedFor.set(-1);  // Reset vote in new term
        }

        // Rule 3: Check if we already voted in this term
        int currentVotedFor = voteGrantedFor.get();
        if (currentVotedFor != -1 && currentVotedFor != voteRequest.getCandidateId()) {
            System.out.println("Rejecting vote - already voted for " + currentVotedFor);
            return new VoteResponse(false, nodeId, voteRequest.getCandidateId(), getCurrentTerm());
        }

        // Rule 4: Check if candidate's log is at least as up-to-date as ours
        int ourLastLogIndex = raftLog.getLog().size() - 1;
        int ourLastLogTerm = raftLog.getLog().get(ourLastLogIndex).getTerm();

        boolean candidateLogIsUpToDate =
                (voteRequest.getLastLogTerm() > ourLastLogTerm) ||
                        (voteRequest.getLastLogTerm() == ourLastLogTerm &&
                                voteRequest.getLastLogIndex() >= ourLastLogIndex);

        if (!candidateLogIsUpToDate) {
            System.out.println("Rejecting vote - candidate log not up-to-date");
            return new VoteResponse(false, nodeId, voteRequest.getCandidateId(), getCurrentTerm());
        }

        // All checks passed - grant vote!
        voteGranted = true;
        voteGrantedFor.set(voteRequest.getCandidateId());
        System.out.println("Granting vote to candidate " + voteRequest.getCandidateId());

        return new VoteResponse(voteGranted, nodeId, voteRequest.getCandidateId(), getCurrentTerm());
    }


    public boolean onAppendEntriesRequest(AppendEntryRequest appendEntryRequest) {

        return raftLog.appendEntries(appendEntryRequest.getPrevLogIndex(),
                appendEntryRequest.getPrevLogTerm(),
                appendEntryRequest.getEntries());
        //send response which is handled by appendEntryResponse
    }



    public void onAppendEntriesResponse(AppendEntryResponse appendEntryResponse) {

        if(appendEntryResponse.isSuccess()) {
            System.out.println("Append entry response success from node "+appendEntryResponse.getFollowerId() + "" +
                    " with match index" +  appendEntryResponse.getMatchIndex() + " thread " + Thread.currentThread().getName());
            //System.out.println("Append entry response match index"+ appendEntryResponse.getMatchIndex());
            peers.get(appendEntryResponse.getFollowerId()).setNextIndex(appendEntryResponse.getMatchIndex()+1);
            peers.get(appendEntryResponse.getFollowerId()).setMatchIndex(appendEntryResponse.getMatchIndex());
        }else {
            System.out.println("Append entry response success from node "+appendEntryResponse.getFollowerId() + "" +
                    " with match index" +  appendEntryResponse.getMatchIndex() + " thread " + Thread.currentThread().getName());
            int nextIndex = peers.get(appendEntryResponse.getFollowerId()).getNextIndex();
            peers.get(appendEntryResponse.getFollowerId()).setNextIndex(nextIndex-1);
        }


        /*else:
            # It failed
        self.next_index[msg.follower_id] -= 1
        self.update_single_follower(msg.follower_id)   # Retry*/

    }

    public Object handleMessage(Object message) {
        //System.out.println("handling message "+ message.getClass());
        if(message instanceof AppendEntryRequest appendEntryRequest){
            //System.out.println("Received AppendEntryRequest command ");

            resetElectionTimeout();

            if (appendEntryRequest.getTerm() > getCurrentTerm()) {
                System.out.println("Node " + nodeId + " updating term from " + getCurrentTerm() +
                        " to " + appendEntryRequest.getTerm());
                setCurrentTerm(appendEntryRequest.getTerm());
                becomeFollower();
            }
            if(appendEntryRequest.getTerm() < getCurrentTerm()) {
                return new AppendEntryResponse(nodeId, getCurrentTerm(), false, commitIndex);
            }
            boolean isAppendSuccess = onAppendEntriesRequest(appendEntryRequest);

            if (isAppendSuccess && appendEntryRequest.getCommitIndex() > commitIndex) {
                int lastNewEntryIndex = appendEntryRequest.getPrevLogIndex() +
                        appendEntryRequest.getEntries().size();
                commitIndex = Math.min(appendEntryRequest.getCommitIndex(), lastNewEntryIndex);
                //System.out.println("Node " + nodeId + " updated commitIndex to " + commitIndex);
            }
            int updatedMatchIndex = isAppendSuccess ?
                    appendEntryRequest.getPrevLogIndex() + appendEntryRequest.getEntries().size() :
                    0;
            AppendEntryResponse appendEntryResponse = new AppendEntryResponse(nodeId,
                    getCurrentTerm(),
                    isAppendSuccess,
                    updatedMatchIndex);
            //System.out.println("Returning AppendEntryResponse");
            return appendEntryResponse;
        }
        else if(message instanceof ClientCommandRequest clientCommandRequest) {
            System.out.println("Received client command request"+ clientCommandRequest.getCommand());
            return onNewClientCommand(clientCommandRequest);
        }else if(message instanceof AppendEntryResponse appendEntryResponse) {
            System.out.println("Received AppendEntry Response");
            return appendEntryResponse;
        } else if(message instanceof VoteRequest voteRequest) {
            System.out.println("Received VoteRequest from " + voteRequest.getCandidateId());
            return onVoteRequest(voteRequest);
        } else {
            throw new RuntimeException("Invalid message");
        }
    }

    public RaftLog getRaftLog() {
        return raftLog;
    }

    public int getCurrentTerm() {
        return currentTerm.get();
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

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm.set(currentTerm) ;
    }

    public void shutdown() {
        System.out.println("Node " + nodeId + " shutting down");

        try {
            // Cancel election timeout
            if (electionTimeoutTask != null) {
                electionTimeoutTask.cancel(true);
            }
            // Shutdown election timeout executor
            if (electionTimeoutExecutor != null) {
                electionTimeoutExecutor.shutdownNow();
                electionTimeoutExecutor.awaitTermination(2, TimeUnit.SECONDS);
            }
            // Shutdown replication executor (synchronously for shutdown)
            if (logReplicationExecutorService != null) {
                logReplicationExecutorService.shutdownNow();
                logReplicationExecutorService.awaitTermination(2, TimeUnit.SECONDS);
                logReplicationExecutorService = null;
            }
            // Shutdown vote executor (synchronously for shutdown)
            if (voteRequestorExecutorService != null) {
                voteRequestorExecutorService.shutdownNow();
                voteRequestorExecutorService.awaitTermination(2, TimeUnit.SECONDS);
                voteRequestorExecutorService = null;
            }
            System.out.println("Node " + nodeId + " shutdown complete");

        } catch (InterruptedException e) {
            System.err.println("Interrupted during shutdown");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
