package message;

import java.io.Serializable;
import java.util.List;

public class AppendEntryRequest extends Message implements Serializable {

    private final int term;
    private final int leaderId;
    private final int prevLogIndex;
    private final int prevLogTerm;
    private final List<LogEntry> entries;
    private final int commitIndex;

    public AppendEntryRequest(int term, int leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int commitIndex) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.commitIndex = commitIndex;
    }

    public int getTerm() {
        return term;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public int getCommitIndex() {
        return commitIndex;
    }


}
