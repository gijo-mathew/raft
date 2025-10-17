package message;

import core.Peer;

import java.io.Serializable;
import java.util.List;

public class ClientCommandResponse implements Serializable {

    private String commandResponse;
    private int commitIndex;
    private int leaderLogSize;
    private List<Peer> peerStatus;


    public ClientCommandResponse() {
    }

    public void setCommandResponse(String commandResponse) {
        this.commandResponse = commandResponse;
    }

    public String getCommandResponse() {
        return commandResponse;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int getLeaderLogSize() {
        return leaderLogSize;
    }

    public void setLeaderLogSize(int leaderLogSize) {
        this.leaderLogSize = leaderLogSize;
    }

    public List<Peer> getPeerStatus() {
        return peerStatus;
    }

    public void setPeerStatus(List<Peer> peerStatus) {
        this.peerStatus = peerStatus;
    }
}
