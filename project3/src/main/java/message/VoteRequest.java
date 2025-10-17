package message;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteRequest implements Serializable {

    private AtomicInteger term = new AtomicInteger();
    private AtomicInteger candidateId = new AtomicInteger();
    private AtomicInteger lastLogIndex = new AtomicInteger();
    private AtomicInteger lastLogTerm = new AtomicInteger();

    public VoteRequest(int term,
                       int candidateId,
                       int lastLogIndex,
                       int lastLogTerm) {
        this.term.set(term);
        this.candidateId.set(candidateId);
        this.lastLogIndex.set(lastLogIndex);
        this.lastLogTerm.set(lastLogIndex);
    }

    public int getTerm() {
        return term.get();
    }

    public int getCandidateId() {
        return candidateId.get();
    }

    public int getLastLogIndex() {
        return lastLogIndex.get();
    }

    public int getLastLogTerm() {
        return lastLogTerm.get();
    }
}
