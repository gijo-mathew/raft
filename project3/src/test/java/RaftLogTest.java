
import log.RaftLog;
import message.LogEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class RaftLogTest {

    RaftLog raftLog = new RaftLog();

    @Test
    public void testLogAppendSuccess() {
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(new LogEntry(1, "First Command", 1));
        Assertions.assertTrue(raftLog.appendEntries(0, 1, logEntries));
        Assertions.assertEquals(raftLog.getLog().size(), 2);
        Assertions.assertEquals(raftLog.getLog().get(1).getCommand(), "First Command");
    }

    @Test
    public void testLogAppendFailureForInValidPrevIndex() {
        testLogAppendSuccess();
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(new LogEntry(1, "Second Command", 2));
        Assertions.assertTrue(raftLog.appendEntries(0, 1, logEntries));
        Assertions.assertEquals(raftLog.getLog().size(), 2);
    }

    @Test
    public void testConflict() {
        testLogAppendSuccess();
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(new LogEntry(2, "Second Command", 1));
        Assertions.assertTrue(raftLog.appendEntries(0, 1, logEntries));
        Assertions.assertEquals(raftLog.getLog().size(), 2);
    }

    @Test
    public void testLogAppendIdempotency() {
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(new LogEntry(1, "First Command", 1));
        Assertions.assertTrue(raftLog.appendEntries(0, 1, logEntries));
        Assertions.assertTrue(raftLog.appendEntries(0, 1, logEntries));
        Assertions.assertEquals(raftLog.getLog().size(), 2);
    }

    @Test
    public void testMultipleLogAppendSuccess() {
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(new LogEntry(1, "First Command First term", 1));
        logEntries.add(new LogEntry(1, "Second Command First term", 2));
        logEntries.add(new LogEntry(1, "Third Command First term", 3));
        logEntries.add(new LogEntry(2, "Fourth Command First term", 4));
        Assertions.assertTrue(raftLog.appendEntries(0, 1, logEntries));
        Assertions.assertEquals(raftLog.getLog().size(), 5);
        Assertions.assertEquals(raftLog.getLog().get(2).getCommand(),"Second Command First term");

    }

    @Test
    public void testMultiTermConflictResolutionSuccess() {
        testMultipleLogAppendSuccess();
        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(new LogEntry(2, "Second Command Second term", 2));
        logEntries.add(new LogEntry(2, "Third Command Second term", 3));
        logEntries.add(new LogEntry(2, "Fourth Command Second term", 4));
        logEntries.add(new LogEntry(2, "Fifth Command Second term", 5));
        Assertions.assertTrue(raftLog.appendEntries(1, 1, logEntries));
        Assertions.assertEquals(raftLog.getLog().size(), 6);
        Assertions.assertEquals(raftLog.getLog().get(2).getCommand(),"Second Command Second term");
    }

}
