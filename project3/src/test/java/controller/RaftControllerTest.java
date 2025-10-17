package controller;

import core.Peer;
import core.RaftController;
import io.RaftTransport;
import message.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class RaftControllerTest {

    Map<Integer, Peer> leaderPeers;
    Map<Integer, Peer> follower2Peers;
    Map<Integer, Peer> follower3Peers;
    LoopbackTransport transport;
    RaftController leader;
    RaftController follower2;
    RaftController follower3;


    @BeforeEach
    public void setup() {
        transport = new LoopbackTransport();


        leaderPeers = Map.of(2, new Peer(2), 3, new Peer(3));

        follower2Peers = Map.of(1, new Peer(1), 3, new Peer(3));
        follower2 = new RaftController(transport, 2, follower2Peers);

        follower3Peers = Map.of(1, new Peer(1), 2, new Peer(2));
        follower3 = new RaftController(transport, 3, follower3Peers);


        leader = new RaftController(transport, 1,  leaderPeers);
        transport.leader = leader;
        transport.follower2 = follower2;
        transport.follower3 = follower3;
        leader.becomeLeader();
    }

    @Test
    public void testReplicationStartsImmediately() {
        int leaderLogSizeAfterCommand = leader.getRaftLog().getLog().size();

        Awaitility.await()
                .atMost(500, MILLISECONDS)
                .until(() -> transport.getTotalInvocations()>2 );
        int follower2LogSizeAfterCommand = follower2.getRaftLog().getLog().size();
        int follower3LogSizeAfterCommand = follower3.getRaftLog().getLog().size();

        Assertions.assertEquals(leaderLogSizeAfterCommand, follower2LogSizeAfterCommand);
        Assertions.assertEquals(leaderLogSizeAfterCommand, follower3LogSizeAfterCommand);

        Assertions.assertEquals(leaderLogSizeAfterCommand, 1);
        Assertions.assertEquals(follower2LogSizeAfterCommand, 1);
        Assertions.assertEquals(follower3LogSizeAfterCommand, 1);
        Assertions.assertEquals(leader.getCommitIndex(), 0);
    }


    @Test
    public void testAllFollowersReceiveMessages() {

        leaderPeers.forEach((key, peer) -> {
            Assertions.assertEquals(peer.getNextIndex(), leader.getRaftLog().getLog().size(),
                    "Value mismatch for key: " + key);
        });
        ClientCommandRequest clientCommandRequest = new ClientCommandRequest("First command");
        int leaderLogSizeBeforeCommand = leader.getRaftLog().getLog().size();

        int follower2LogSizeBeforeCommand = follower2.getRaftLog().getLog().size();
        Assertions.assertEquals(leaderLogSizeBeforeCommand, follower2LogSizeBeforeCommand);

        int follower3LogSizeBeforeCommand = follower3.getRaftLog().getLog().size();
        Assertions.assertEquals(leaderLogSizeBeforeCommand, follower3LogSizeBeforeCommand);

        leader.onNewClientCommand(clientCommandRequest);

        int leaderLogSizeAfterCommand = leader.getRaftLog().getLog().size();

        Awaitility.await()
                .atMost(500, MILLISECONDS)
                .until(() -> transport.getTotalInvocations()>2 );
        int follower2LogSizeAfterCommand = follower2.getRaftLog().getLog().size();
        int follower3LogSizeAfterCommand = follower3.getRaftLog().getLog().size();

        Assertions.assertEquals(leaderLogSizeAfterCommand, follower2LogSizeAfterCommand);
        Assertions.assertEquals(leaderLogSizeAfterCommand, follower3LogSizeAfterCommand);

        Assertions.assertEquals(leaderLogSizeAfterCommand, leaderLogSizeBeforeCommand+1);
        Assertions.assertEquals(follower2LogSizeAfterCommand, follower2LogSizeBeforeCommand+1);
        Assertions.assertEquals(follower3LogSizeAfterCommand, follower3LogSizeBeforeCommand+1);


        LogEntry lastEntryInLeader = leader.getRaftLog().getLog().get(leader.getRaftLog().getLog().size()-1);
        LogEntry lastEntryInFollower2 = follower2.getRaftLog().getLog().get(follower2.getRaftLog().getLog().size()-1);

        Assertions.assertEquals(lastEntryInLeader.getCommand(), lastEntryInFollower2.getCommand());
        Assertions.assertEquals(lastEntryInLeader.getIndex(), lastEntryInFollower2.getIndex());
        Assertions.assertEquals(lastEntryInLeader.getTerm(), lastEntryInFollower2.getTerm());

        LogEntry lastEntryInFollower3 = follower3.getRaftLog().getLog().get(follower3.getRaftLog().getLog().size()-1);
        Assertions.assertEquals(lastEntryInLeader.getCommand(), lastEntryInFollower3.getCommand());
        Assertions.assertEquals(lastEntryInLeader.getIndex(), lastEntryInFollower3.getIndex());
        Assertions.assertEquals(lastEntryInLeader.getTerm(), lastEntryInFollower3.getTerm());

        Assertions.assertEquals(leaderPeers.get(follower2.getNodeId()).getNextIndex(), follower2.getRaftLog().getLog().size());
        Assertions.assertEquals(leaderPeers.get(follower3.getNodeId()).getNextIndex(), follower3.getRaftLog().getLog().size());

        System.out.println(leader.getCommitIndex());
        Awaitility.await()
                .atMost(1000, MILLISECONDS)
                .until(() -> leader.getCommitIndex() > 0);
        leader.becomeFollower();
        Assertions.assertEquals(1, leader.getCommitIndex());

    }



    static class LoopbackTransport implements RaftTransport {
        RaftController leader;
        RaftController follower2;
        RaftController follower3;

        private final AtomicInteger totalInvocations = new AtomicInteger(0);

        public int getTotalInvocations() {
            return totalInvocations.get();
        }

        @Override
        public AppendEntryResponse sendAppendEntries(int peerId, AppendEntryRequest req) {
            // leader -> follower
            totalInvocations.getAndIncrement();
            if(peerId == 2) {
                boolean isAppended = follower2.onAppendEntriesRequest(req);
                int updatedMatchIndex = isAppended ?
                        req.getPrevLogIndex() + req.getEntries().size() :
                        0;
                // follower -> leader callback
                return new AppendEntryResponse(follower2.getNodeId(),
                        follower2.getCurrentTerm(),
                        isAppended,
                        updatedMatchIndex);
            }
            else if(peerId == 3) {
                boolean isAppended = follower3.onAppendEntriesRequest(req);
                int updatedMatchIndex = isAppended ?
                        req.getPrevLogIndex() + req.getEntries().size() :
                        0;
                // follower -> leader callback
                return new AppendEntryResponse(follower3.getNodeId(),
                        follower3.getCurrentTerm(),
                        isAppended,
                        updatedMatchIndex);
            }
            return null;
        }


        @Override
        public void handleIncomingConnection(Socket s) { /* unused in unit test */ }

        @Override
        public VoteResponse sendRequestVote(int destinationNodeId, VoteRequest req) {
          return null;
        }

        public void setLeader(RaftController leader) {
            this.leader = leader;
        }

    }

}
