package message;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private final int term;
    private final int index;
    private final String command; // Could be generalized for any app

    public LogEntry(int term, String command, int index) {
        this.term = term;
        this.command = command;
        this.index= index;
    }

    public int getTerm() {
        return term;
    }

    public String getCommand() {
        return command;
    }

    public int getIndex() {
        return index;
    }
}
