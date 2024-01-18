package org.zlab.ocov.tracker;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
// import java.util.concurrent.locks.ReadWriteLock;
// import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Runtime {
    /**
     * Collect & update coverage information, dump coverage when program finishes.
     * TODO: These paths need to be configured with input arguments
     * /Users/hanke/Desktop/Project/vasco/system/cassandra/apache-cassandra-2.2.8/serializedFields_alg1.json
     * /Users/hanke/Desktop/Project/ssg-runtime/input/topObjects_cass.json
     */
    public static Path baseClassPath = Paths.get("/tmp/serializedFields_alg1.json");
    public static Path topObjectsPath = Paths.get("/tmp/topObjects.json");
    public static Path comparableClassesPath = Paths.get("/tmp/comparableClasses.json");
    public static Path modifiedFieldsPath = Paths.get("/tmp/modifiedFields.json");
    public static Path modifiedEnumsPath = Paths.get("/tmp/modifiedEnums.json");

    public static Path filePath = Paths.get("/tmp/coverage.log");
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static BufferedWriter writer;
    public static ObjectCoverage objectCoverage;
    private static final Object objectCoverageLock = new Object();

    // private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public static void init() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
            objectCoverage = new ObjectCoverage(baseClassPath, topObjectsPath,
                    comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath);
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
        // Debug
        // if (dumpId != 474) {
        // return false;
        // }
        synchronized (objectCoverageLock) {
            // Ensure that objectCoverage is not being serialized while it's being updated
            return objectCoverage.update(obj, dumpId);
        }

        // rwLock.readLock().lock();
        // try {
        // boolean val = objectCoverage.update(obj, dumpId);
        // // Runtime.log("Update coverage ret = " + val);
        // return val;
        // } finally {
        // rwLock.readLock().unlock();
        // }
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
                                ObjectCoverage response;

                                synchronized (objectCoverageLock) {
                                    response = processCommand(inputLine);
                                    // Serialize and send the response within the synchronized block
                                    out.writeObject(response);
                                    response.clear();
                                }
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

    private static ObjectCoverage processCommand(String command) {
        // only return the violations
        // visited objects are cleared
        return objectCoverage;
    }

}
