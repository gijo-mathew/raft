package io;

import message.AppendEntryRequest;
import message.AppendEntryResponse;

import java.net.Socket;

public interface RaftTransport {

    AppendEntryResponse sendAppendEntries(int peerId, AppendEntryRequest appendEntryRequest);
    void handleIncomingConnection(Socket clientSocket);

    //void sendRequestVote(int peerId, RequestVoteReq req);
}
