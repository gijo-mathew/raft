package message;

import java.io.Serializable;

public class AppendEntryResponse implements Serializable {
    private final int term;
    private final boolean success;
    private final int matchIndex;

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
}
