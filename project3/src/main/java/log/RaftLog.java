package log;

import message.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class RaftLog {

    private List<LogEntry> log;

    public RaftLog() {
        this.log = new ArrayList<>();
        log.add(new LogEntry(0, "Dummy Command", 0));
    }


    public void appendNewCommand(int leaderTerm, String command) {
        LogEntry logEntry = new LogEntry(leaderTerm, command, log.size());
        log.add(logEntry);
        System.out.println("Added new command at index "+logEntry.getIndex());
    }

    //
    //
    //
    /**
     * 1. TODO - Reply false if term < currentTerm (§5.1)
     * 2. Reply false if log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm (§5.3) -> done
     *  3. If an existing entry conflicts with a new one (same index but different terms),
     *  delete the existing entry and all that follow it
     *   4. Append any new entries not already in the log
     *   5. TODO - . If leaderCommit > commitIndex, set commitIndex =
     *      min(leaderCommit, index of last new entry)
     */

    /**
     * @param prevIndex - specifies the position in the log after which the new entries go.
     *                  (e.g., specifying prev_index=8 means that entries are being appended starting at index 9).
     * @param prevTerm - The prev_term argument specifies the expected term value of the log entry at position prev_index.
     * @param entries - entries is a list of zero or more LogEntry instances that are being added.
     * @return
     */
    public boolean appendEntries(int prevIndex, int prevTerm, List<LogEntry> entries) {
        if(prevIndex > log.size() - 1) {
            System.out.println("Prev index greater than log size. returning..");
            return false; // hole in log
        }
        if(prevIndex<0){
            System.out.println("Prev index <0. returning..");
            return false;
        }

        LogEntry prevItem = log.get(prevIndex);
        if(prevItem.getTerm() != prevTerm) {
            System.out.println("Invalid Prev term <0.");
           return false;
        }

        if(entries.isEmpty()) {
            //System.out.println("Entries are empty.");
            return true;
        }

        int index = prevIndex + 1;
        for (LogEntry entry : entries) {
            if(index < log.size()) {
                LogEntry existing = log.get(index);
                if(existing.getTerm() != entry.getTerm()) {
                    // Conflict: remove existing and all after, then append
                    while(log.size() > index) {
                        log.remove(log.size() - 1);

                        //0,1,2,3,4
                    }
                    log.add(entry);
                    System.out.println("Appended new command at index "+index);
                }
                // else same term -> idempotent
            } else {
                log.add(entry);
                System.out.println("added entry");
            }
            index++;
        }
        return true;
    }


    private void validateInvariants() {

    }

    public List<LogEntry> getLog() {
        return log;
    }

    public List<LogEntry> getLog(int startIndex) {
        List<LogEntry> sublist = log.subList(startIndex, log.size());
        System.out.println("Start index" + startIndex + " sublist size "+sublist.size());
        return new ArrayList<>(sublist);
    }
}
