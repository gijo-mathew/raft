package config;

import java.util.HashMap;
import java.util.Map;

public class RaftConfig {

    public static Map<Integer, NodeAddress> NODES = new HashMap<>();



    static {
        NODES.put(1, new NodeAddress("localhost", 15000));
        NODES.put(2, new NodeAddress("localhost", 16000));
        //TODO
       /* NODES.put(3, new NodeAddress("localhost",17000));
        NODES.put(4, new NodeAddress("localhost",18000));
        NODES.put(5, new NodeAddress("localhost",19000));*/
    }


    public static class NodeAddress {
        private final int port;
        private final String address;

        public NodeAddress( String address, int port) {
            this.port = port;
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public String getAddress() {
            return address;
        }
    }
}
