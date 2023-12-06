package org.zlab.ocov.tracker;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Runtime {
    /**
     * Collect & update coverage information, dump coverage when program finishes.
     */
    public static Path baseClassPath = Paths.get(
            "/Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/serializedFields_alg1.json");
    public static Path topObjectsPath = Paths
            .get("/Users/hanke/Desktop/Project/ssg-runtime/input/topObjects_cass.json");
    public static ObjectCoverage objectCoverage = new ObjectCoverage(baseClassPath, topObjectsPath);

    public static String filePath = "/Users/hanke/Desktop/Project/cassandra/cassandra1/coverage.log";

    public static BufferedWriter writer;
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
            formatCoverageTracker();
            log("Invariant Runtime initialized!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        try {
            String timeStamp = dateFormat.format(new Date());
            writer.write(timeStamp + " - " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean update(Object obj) {
        boolean val = objectCoverage.update(obj);
        Runtime.log("Update coverage ret = " + val);
        return val;
    }

    private static final int PORT = 62000; // the port to listen on

    public static void formatCoverageTracker() throws IOException {
        Thread serverThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                while (true) {
                    log("[hklog] Invariant Runtime waiting!");
                    Socket clientSocket = serverSocket.accept();
                    // handle client connection in a new thread
                    new Thread(() -> {
                        try {
                            log("Client connected from "
                                    + clientSocket.getInetAddress().getHostAddress());

                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()));
                            ObjectOutputStream out = new ObjectOutputStream(
                                    clientSocket.getOutputStream());

                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                log("Received command: " + inputLine);
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
    }

    private static Object processCommand(String command) {
        // only return the violations
        return objectCoverage;
    }

}
