package io;

import message.AppendEntryRequest;
import message.AppendEntryResponse;
import message.VoteRequest;
import message.VoteResponse;

import java.net.Socket;

public interface RaftTransport {

    AppendEntryResponse sendAppendEntries(int peerId, AppendEntryRequest appendEntryRequest);
    void handleIncomingConnection(Socket clientSocket);

    VoteResponse sendRequestVote(int destinationNodeId, VoteRequest req);
}
