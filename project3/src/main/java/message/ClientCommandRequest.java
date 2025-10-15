package message;

import java.io.Serializable;

public class ClientCommandRequest implements Serializable {

    String command;

    public ClientCommandRequest(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
