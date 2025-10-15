package message;

import java.io.Serializable;

public class ClientCommandResponse implements Serializable {

    private final String commandResponse;

    public ClientCommandResponse(String commandResponse) {
        this.commandResponse = commandResponse;
    }

    public String getCommandResponse() {
        return commandResponse;
    }
}
