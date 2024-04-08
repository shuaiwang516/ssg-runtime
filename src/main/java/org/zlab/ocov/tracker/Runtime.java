package org.zlab.ocov.tracker;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Runtime {
    // Only enable the runtime when the environment variable is set
    public static boolean enable = true;
    public static final String envVarName = "ENABLE_FORMAT_COVERAGE";

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
    public static Path branch2CollectionPath = Paths.get("/tmp/branch2Collection.json");

    public static Path filePath = Paths.get("/tmp/coverage.log");
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static BufferedWriter writer;
    public static ObjectGraphCoverage objectCoverage;
    private static final Object objectCoverageLock = new Object();

    public static long totalTime1 = 0;
    public static int count = 0;

    public static boolean memorizeAllObjectGraph = false;

    // private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    // Invoked by main of target program
    public static void init() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));

            // Only enable when this environment variable is set to true
            String envVar = System.getenv(envVarName);
            if (!Boolean.parseBoolean(envVar)) {
                enable = false;
                log("Invariant Runtime is disabled by environment variable");
                return;
            }

            objectCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                    comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath,
                    branch2CollectionPath);
            formatCoverageTracker();
            log("Invariant Runtime initialized!");
        } catch (Exception e) {
            log("Invariant Runtime failed to initialize!, e = " + e);
            for (StackTraceElement ste : e.getStackTrace()) {
                log(ste.toString());
            }
            throw new RuntimeException("Invariant Runtime failed to initialize!");
        }
    }

    // Used by tests
    public static void init(Path baseClassPath, Path topObjectsPath) {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
            objectCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath);
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

    public static boolean update1(int dumpId) {
        System.out.println("hello ke from ssg-runtime, dumpId = " + dumpId);
        return true;
    }

    public static boolean update2(Object obj) {
        System.out.println("hello ke from ssg-runtime, obj = " + obj);
        return true;
    }

    // id uniquely identify the program location for dumping
    public static Object update(Object obj, int dumpId) {
        // Debug
        // if (dumpId != 474) {
        // return false;
        // }
        // long time1 = System.currentTimeMillis();
        if (enable) {
            if (objectCoverage != null) {
                synchronized (objectCoverageLock) {
                    // Ensure that objectCoverage is not being serialized while it's being updated
                    boolean ret;
                    if (memorizeAllObjectGraph)
                        ret = objectCoverage.dump(obj, dumpId);
                    else
                        ret = objectCoverage.update(obj, dumpId);
                    // long time2 = System.currentTimeMillis();
                    //
                    // count++;
                    // totalTime1 += time2 - time1;
                    // if (count % 1000 == 0)
                    // log(String.format("Time1: %d ms", totalTime1));
                }
            } else {
                log("objectCoverage is null, Invariant Runtime is not initialized properly!");
            }
        }
        return obj;
    }

    public static boolean updateBranch(boolean status, int dumpId) {
        if (enable) {
            synchronized (objectCoverageLock) {
                objectCoverage.updateBranch(status, dumpId);
            }
        }
        return status;
    }

    // Deprecated
    public static boolean updateBranchWithCollection(boolean status, int dumpId) {
        if (enable) {
            synchronized (objectCoverageLock) {
                objectCoverage.updateBranchWithCollection(status, dumpId);
            }
        }
        return status;
    }

    // Deprecated
    public static Object updateCollection(Object obj, int dumpId) {
        if (enable) {
            synchronized (objectCoverageLock) {
                objectCoverage.updateCollection(obj, dumpId);
            }
        }
        return obj;
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
                                ObjectGraphCoverage response;

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

    private static ObjectGraphCoverage processCommand(String command) {
        // only return the violations
        // visited objects are cleared
        if (memorizeAllObjectGraph)
            objectCoverage.inferInvariantFromAllObjectGraphs();
        objectCoverage.inferInvariant();
        return objectCoverage;
    }
}
