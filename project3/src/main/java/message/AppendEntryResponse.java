package message;

import java.io.Serializable;

public class AppendEntryResponse implements Serializable {
    private final int term;
    private final boolean success;
    private final int matchIndex;
    private int from;
    private int to;

    public AppendEntryResponse(int term, boolean success, int matchIndex) {
        this.term = term;
        this.success = success;
        this.matchIndex = matchIndex;
    }

    public int getTerm() {
        return term;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }
}
