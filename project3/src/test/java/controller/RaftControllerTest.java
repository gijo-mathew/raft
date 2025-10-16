package controller;

import core.Peer;
import core.RaftController;
import io.RaftTransport;
import message.AppendEntryRequest;
import message.AppendEntryResponse;
import message.ClientCommandRequest;
import message.LogEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import java.util.List;
import java.util.Map;

public class RaftControllerTest {

    @Test
    public void testReplicationSuccess() {

        Map<Integer, Peer> leaderPeers = Map.of(2, new Peer(2));
        Map<Integer, Peer> followerPeers = Map.of(2, new Peer(2));

        var transport = new LoopbackTransport();
        RaftController follower = new RaftController(null, 2, followerPeers);
        RaftController leader = new RaftController(transport, 1,  leaderPeers);

        transport.leader = leader;
        transport.follower = follower;

        leader.becomeLeader();


        leaderPeers.forEach((key, peer) -> {
            Assertions.assertEquals(peer.getNextIndex(), leader.getRaftLog().getLog().size(),
                    "Value mismatch for key: " + key);
        });


        ClientCommandRequest clientCommandRequest = new ClientCommandRequest("First command");
        int leaderLogSizeBeforeCommand = leader.getRaftLog().getLog().size();
        int followerLogSizeBeforeCommand = follower.getRaftLog().getLog().size();
        Assertions.assertEquals(leaderLogSizeBeforeCommand, followerLogSizeBeforeCommand);

        leader.onNewClientCommand(clientCommandRequest);

        int leaderLogSizeAfterCommand = leader.getRaftLog().getLog().size();
        int followerLogSizeAfterCommand = follower.getRaftLog().getLog().size();
        Assertions.assertEquals(leaderLogSizeAfterCommand, followerLogSizeAfterCommand);
        Assertions.assertEquals(leaderLogSizeAfterCommand, leaderLogSizeBeforeCommand+1);
        Assertions.assertEquals(followerLogSizeAfterCommand, followerLogSizeBeforeCommand+1);


        Assertions.assertEquals(leader.getRaftLog().getLog().size(), follower.getRaftLog().getLog().size());
        LogEntry lastEntryInLeader = leader.getRaftLog().getLog().get(leader.getRaftLog().getLog().size()-1);
        LogEntry lastEntryInFollower = follower.getRaftLog().getLog().get(follower.getRaftLog().getLog().size()-1);

        Assertions.assertEquals(lastEntryInLeader.getCommand(), lastEntryInFollower.getCommand());
        Assertions.assertEquals(lastEntryInLeader.getIndex(), lastEntryInFollower.getIndex());
        Assertions.assertEquals(lastEntryInLeader.getTerm(), lastEntryInFollower.getTerm());

        Assertions.assertEquals(leaderPeers.get(follower.getNodeId()).getNextIndex(), follower.getRaftLog().getLog().size());
    }

    static class LoopbackTransport implements RaftTransport {
        RaftController leader;
        RaftController follower;

        @Override
        public AppendEntryResponse sendAppendEntries(int peerId, AppendEntryRequest req) {
            // leader -> follower
            boolean isAppended = follower.onAppendEntriesRequest(req);
            // follower -> leader callback
           return new AppendEntryResponse(follower.getNodeId(),
                    follower .getCurrentTerm(),
                    isAppended,
                    1);
        }


        @Override
        public void handleIncomingConnection(Socket s) { /* unused in unit test */ }
    }

}
