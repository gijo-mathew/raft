package log;

import message.LogEntry;

import java.util.List;

public class RaftLog {

    /**
     * @param prevIndex - specifies the position in the log after which the new entries go.
     *                  (e.g., specifying prev_index=8 means that entries are being appended starting at index 9).
     * @param prevTerm - The prev_term argument specifies the expected term value of the log entry at position prev_index.
     * @param entries - entries is a list of zero or more LogEntry instances that are being added.
     * @return
     */
    public boolean appendEntries(int prevIndex, int prevTerm, List<LogEntry> entries) {


        return false;
    }

    private void validateInvariants() {

    }
}
