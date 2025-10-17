package core;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Peer implements Serializable {

    int nodeId;

    AtomicInteger nextIndex = new AtomicInteger(0);
    AtomicInteger matchIndex = new AtomicInteger(0);

    boolean voteGranted;

    public Peer(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public int getNextIndex() {
        return nextIndex.get();
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex.set(nextIndex);
    }

    public int getMatchIndex() {
        return matchIndex.get();
    }

    public void setMatchIndex(int matchIndex) {
        this.matchIndex.set(matchIndex);
    }
}
