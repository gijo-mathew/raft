package server;

import java.util.concurrent.ConcurrentHashMap;

public class KeyValueServer {

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap();


    public String get(String key) {
        return map.getOrDefault(key, "No value present for given key");
    }

    public String put(String key, String obj) {
       return map.put(key, obj );
    }

    public Object delete(String key) {
        return map.remove(key);
    }
}
