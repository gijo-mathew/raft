package core;

public class RaftController {


    /**
     * This happens on the leader
     *
     */
    public void onNewClientCommand(){
        // write command to leader node
    }


    public void updateFollowers(){

        //send response which is handled by appendEntries in followers
    }

    public void onAppendEntriesRequest() {
        //send response which is handled by appendEntryResponse
    }

    public void onAppendEntriesResponse() {

    }


}
