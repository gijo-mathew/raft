package message;

public class ClientCommandResponse {

    private final String commandResponse;

    public ClientCommandResponse(String commandResponse) {
        this.commandResponse = commandResponse;
    }

    public String getCommandResponse() {
        return commandResponse;
    }
}
