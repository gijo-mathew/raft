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

public class RaftControllerTest {

    @Test
    public void testReplicationSuccess() {

        List<Peer> leaderPeers = List.of(new Peer(2));
        List<Peer> followerPeers = List.of(new Peer(1));

        var transport = new LoopbackTransport();
        RaftController follower = new RaftController(null, 2, followerPeers);
        RaftController leader = new RaftController(transport, 1,  leaderPeers);

        transport.leader = leader;
        transport.follower = follower;

        leader.becomeLeader();

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
    }

    static class LoopbackTransport implements RaftTransport {
        RaftController leader;
        RaftController follower;

        @Override
        public void sendAppendEntries(int peerId, AppendEntryRequest req) {
            // leader -> follower
            boolean isAppended = follower.onAppendEntriesRequest(req);
            // follower -> leader callback
            leader.onAppendEntriesResponse(new AppendEntryResponse(follower.getCurrentTerm(), isAppended, 1));
        }


        @Override
        public void handleIncomingConnection(Socket s) { /* unused in unit test */ }
    }

}
