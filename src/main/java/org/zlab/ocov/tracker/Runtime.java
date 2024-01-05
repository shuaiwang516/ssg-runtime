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
     * TODO: These paths need to be configured with input arguments
     * /Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/serializedFields_alg1.json
     * /Users/hanke/Desktop/Project/ssg-runtime/input/topObjects_cass.json
     */
    public static Path baseClassPath = Paths.get("/tmp/serializedFields_alg1.json");
    public static Path topObjectsPath = Paths.get("/tmp/topObjects.json");
    public static Path filePath = Paths.get("/tmp/coverage.log");
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static BufferedWriter writer;
    public static ObjectCoverage objectCoverage;

    public static void init() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
            objectCoverage = new ObjectCoverage(baseClassPath, topObjectsPath);
            formatCoverageTracker();
            log("Invariant Runtime initialized!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init(Path baseClassPath, Path topObjectsPath) {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
            objectCoverage = new ObjectCoverage(baseClassPath, topObjectsPath);
            formatCoverageTracker();
            log("Invariant Runtime initialized!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Only init writer
    public static void initWriter() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initWriter(Path filePath) {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        // writer needs to be initialized for logging!
        if (writer == null)
            return;
        try {
            String timeStamp = dateFormat.format(new Date());
            writer.write(timeStamp + " - " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // id uniquely identify the program location for dumping
    public static boolean update(Object obj, int dumpId) {
        boolean val = objectCoverage.update(obj, dumpId);
        // Runtime.log("Update coverage ret = " + val);
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
