package io;

import controller.StopEvent;
import controller.TrafficEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class ButtonListener implements Runnable {

    private final int listenPort; // UDP port the light sends button presses to
    private final LinkedBlockingQueue<TrafficEvent> eventQueue;

    public ButtonListener(int listenPort, LinkedBlockingQueue<TrafficEvent> eventQueue) {
        this.listenPort = listenPort;
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            byte[] buffer = new byte[1024];

            System.out.println("Button listener started on port " + listenPort);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // blocks until message arrives

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Received button press from light: " + message);

                // Determine which light triggered the stop event
                String lightId = message.equalsIgnoreCase("EW") ? "EW" : "NS";

                // Push StopEvent into the queue
                eventQueue.put(new StopEvent(lightId));
            }
        } catch (SocketException e) {
            System.err.println("Socket exception in button listener: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error in button listener: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

