package message;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private final int term;
    private final String command; // Could be generalized for any app

    public LogEntry(int term, String command) {
        this.term = term;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }

    public String getCommand() {
        return command;
    }
}
