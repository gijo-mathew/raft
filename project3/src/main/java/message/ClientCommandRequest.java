package message;

public class ClientCommandRequest {

    String command;

    public ClientCommandRequest(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
