package io;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;


public class LightResolver {

    private final Map<String, UDPClient> lightMap = new HashMap<>();

    public LightResolver(int ewPort, int nsPort) throws SocketException {
        lightMap.put("EW", new UDPClient(ewPort));
        lightMap.put("NS", new UDPClient(nsPort));
    }

    public void updateLight(String lightName, String color) {
        UDPClient client = lightMap.get(lightName);
        if (client != null) {
            try {
                client.sendUPDMessage(color);
            } catch (IOException e) {
                System.err.println("Failed to send message to " + lightName + ": " + e.getMessage());
            }
        } else {
            System.err.println("No UDP client found for light: " + lightName);
        }
    }



        /**
         * Close all clients
         */
   public void closeAll() {
       for (UDPClient client : lightMap.values()) {
           client.close();
       }
   }


}

