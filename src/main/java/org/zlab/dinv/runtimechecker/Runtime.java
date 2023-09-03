package org.zlab.dinv.runtimechecker;

// import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Runtime {
    // maintain the violated invariants
    // a large array which represents the id
    // when instrumenting the inv, we also need to add a single identifier to it.

    public static class ViolationInfo implements Serializable {
        private final int[] violations;

        public ViolationInfo(int[] violations) {
            this.violations = violations;
        }

        public int[] getViolations() {
            return violations;
        }
    }

    private static final int[] violations = new int[1000];

    static {
        System.out.println("Invariant rt initialized!");

        try {
            dumpViolationServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // FIXME: write the violation to disk (only for testing purpose: comment out
        // later)
        // java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        // ObjectMapper mapper = new ObjectMapper();
        // try {
        // String json = mapper.writeValueAsString(violations);
        // // System.out.println(json);
        // // write to file
        // System.out.println("[hklog] system hook: dump violations");
        // File file = new File("violations.json");
        // mapper.writeValue(file, violations);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }));
    }

    public static void addViolation(int invId) {
        violations[invId]++;
    }

    private static final int PORT = 62000; // the port to listen on

    public static void dumpViolationServer() throws IOException {

        Thread serverThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                while (true) {
                    System.out.println("[hklog] Invariant Runtime waiting!");
                    Socket clientSocket = serverSocket.accept();
                    // handle client connection in a new thread
                    new Thread(() -> {
                        try {
                            System.out.println("Client connected from "
                                    + clientSocket.getInetAddress().getHostAddress());

                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()));
                            ObjectOutputStream out = new ObjectOutputStream(
                                    clientSocket.getOutputStream());

                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                System.out.println("Received command: " + inputLine);

                                // process the command and generate a response
                                Object response = processCommand(inputLine);

                                out.writeObject(response); // send the response to the client
                                System.out.println("Sent response: " + response);
                            }
                        } catch (IOException e) {
                            System.out.println("Error in client connection: " + e);
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start(); // start the new thread
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        // serverThread.join();
    }

    private static Object processCommand(String command) {
        // only return the violations
        return new ViolationInfo(violations);
    }

    // return hashcode of the item
    public static int getFirstItem(Object collection) {
        if (collection instanceof List) {
            List<?> list = (List<?>) collection;
            if (!list.isEmpty()) {
                return list.get(0).hashCode();
            }
        } else if (collection instanceof Object[]) {
            Object[] array = (Object[]) collection;
            if (array.length > 0) {
                return array[0].hashCode();
            }
        } else if (collection instanceof int[]) {
            int[] array = (int[]) collection;
            if (array.length > 0) {
                return array[0];
            }
        } else if (collection instanceof byte[]) {
            byte[] array = (byte[]) collection;
            if (array.length > 0) {
                return array[0];
            }
        }
        return Integer.MIN_VALUE;
    }

    public static int getLastItem(Object collection) {
        if (collection instanceof List) {
            List<?> list = (List<?>) collection;
            if (!list.isEmpty()) {
                return list.get(list.size() - 1).hashCode();
            }
        } else if (collection instanceof Object[]) {
            Object[] array = (Object[]) collection;
            if (array.length > 0) {
                return array[array.length - 1].hashCode();
            }
        } else if (collection instanceof int[]) {
            int[] array = (int[]) collection;
            if (array.length > 0) {
                return array[array.length - 1]; // int value itself serves as a hash code
            }
        } else if (collection instanceof byte[]) {
            byte[] array = (byte[]) collection;
            if (array.length > 0) {
                return array[array.length - 1];
            }
        }
        return Integer.MIN_VALUE;
    }

}
