package org.zlab.dinv.runtimechecker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Runtime {
    // maintain the violated invariants
    // a large array which represents the id
    // when instrumenting the inv, we also need to add a single identifier to it.

    public static class ViolationInfo implements Serializable {
        private final Map<Integer, Integer> map;

        public ViolationInfo(Map<Integer, Integer> map) {
            this.map = map;
        }

        public Map<Integer, Integer> getMap() {
            return map;
        }
    }

    private static final ConcurrentMap<Integer, Integer> violations = new ConcurrentHashMap<>();

    static {
        System.out.println("Invariant rt initialized!");
        violations.put(-1, -1);

        try {
            dumpViolationServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // FIXME: write the violation to disk (only for testing purpose: comment out later)
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(violations);
//                System.out.println(json);
                // write to file
                System.out.println("[hklog] system hook: dump violations");
                File file = new File("violations.json");
                mapper.writeValue(file, violations);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public static void addViolation(int invId) {
        int oriCount = 0;
        if (violations.containsKey(invId)) {
            oriCount = violations.get(invId);
        }
        violations.put(invId, ++oriCount);
    }

    private static final int PORT = 62000; // the port to listen on

    public static void dumpViolationServer() throws IOException {

        Thread serverThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                while (true) {
                    System.out.println("[hklog] Invariant Runtime waiting!");
                    Socket clientSocket = serverSocket.accept();
                    // handle client connection
                    System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // create a reader for the client input

                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    // PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); // create a writer for the server output

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received command: " + inputLine);

                        // process the command and generate a response
                        Object response = processCommand(inputLine);

                        out.writeObject(response); // send the response to the client
                        System.out.println("Sent response: " + response);
                    }

                    // clean up resources
                    out.close();
                    in.close();
                    clientSocket.close();
                    System.out.println("Client disconnected");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
//        serverThread.join();
    }

    private static Object processCommand(String command) {
        // only return the violations
        return new ViolationInfo(new HashMap<>(violations));
    }

    public static Object getFirstItem(Object collection) {
        return null;
    }

    public static Object getLastItem(Object collection) {
        return null;
    }

}
