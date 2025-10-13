package server;

import util.MessageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class EchoServer {
    static KeyValueServer keyValueServer = new KeyValueServer();
    private static volatile boolean running = true;


    public static void main(String[] args) throws IOException {


        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server started...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            running=false;
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Exception "+e.getMessage());
            }
        }));


        while (running) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start(); // Thread per client
        }
    }

    private static void handleClient(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            while (true) {
                byte[] msg = MessageUtils.recvMessage(in);
                String output = parseMessage(msg);
                MessageUtils.sendMessage(out, output.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static String parseMessage(byte[] msg) {

        String commandStr = new String(msg, StandardCharsets.UTF_8);
        System.out.println(commandStr); // "get key1"
        String[] parts= commandStr.split(" ");
        if(parts.length<=1) {
            return "invalid command";
        }
        String command = parts[0];
        switch (command) {
            case "get":
                if (parts.length < 2) {
                    System.out.println("Invalid GET command");
                    return "invalid get command";
                } else {
                    String key = parts[1];
                    System.out.println("GET key: " + key);
                    return keyValueServer.get(key);
                }
            case "set":

                if (parts.length < 3) {
                    System.out.println("Invalid SET command");
                    return "invalid put command";
                } else {
                    String key = parts[1];
                    String value = parts[2];
                    System.out.println("SET key: " + key + " value: " + value);
                    keyValueServer.put(key, value);
                    return "OK";
                }
            case "delete":
                if (parts.length < 2) {
                    System.out.println("Invalid Delete command");
                    return "invalid delete command";
                } else {
                    String key = parts[1];
                    System.out.println("DELETE key: " );
                    keyValueServer.delete(key);
                    return "OK";
                }
            default:
                return "invalid command";
        }
    }
}
