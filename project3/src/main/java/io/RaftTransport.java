package io;

import message.AppendEntryRequest;

import java.net.Socket;

public interface RaftTransport {

    void sendAppendEntries(int peerId, AppendEntryRequest appendEntryRequest);
    void handleIncomingConnection(Socket clientSocket);

    //void sendRequestVote(int peerId, RequestVoteReq req);
}
