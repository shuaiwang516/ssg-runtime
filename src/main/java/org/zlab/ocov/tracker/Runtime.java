package org.zlab.ocov.tracker;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Runtime {
    public static final boolean debug = false;
    // Only enable the runtime when the environment variable is set
    public static boolean enable = true;
    public static final String enableEnvName = "ENABLE_FORMAT_COVERAGE";
    public static final String enableSampleEnvName = "ENABLE_FORMAT_COVERAGE_SAMPLE";
    public static final String sampleRateEnvName = "FORMAT_COVERAGE_SAMPLE_RATE";

    public static final Random rand = new Random();

    public static boolean sample = false;
    public static double sampleRate = 0.2; // default value

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

    public static boolean isSampled() {
        return rand.nextDouble() < sampleRate;
    }

    // private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    // Invoked by main of target program
    public static void init() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));

            // Only enable when this environment variable is set to true
            String envVar = System.getenv(enableEnvName);
            if (!Boolean.parseBoolean(envVar)) {
                enable = false;
                log("Invariant Runtime is disabled by environment variable");
                return;
            }

            // Whether to enable sampling
            String enableSampleStr = System.getenv(enableSampleEnvName);
            if (Boolean.parseBoolean(enableSampleStr)) {
                sample = true;
                String sampleRateStr = System.getenv(sampleRateEnvName);
                if (sampleRateStr != null) {
                    try {
                        // Convert the environment variable to double
                        sampleRate = Double.parseDouble(sampleRateStr);
                    } catch (NumberFormatException e) {
                        log("Error: SAMPLING_RATE is not a valid double: " + sampleRateStr);
                    }
                }
                log("Sampling is enabled: rate = " + sampleRate);
            } else {
                log("Sampling is disabled!");
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

    // id uniquely identify the program location for dumping
    public static Object update(Object obj, int dumpId) {
        if (!enable)
            return obj;

        if (objectCoverage != null) {
            if (!sample || isSampled()) {
                long time1 = System.currentTimeMillis();
                synchronized (objectCoverageLock) {

                    long time2 = System.currentTimeMillis();
                    if (memorizeAllObjectGraph)
                        objectCoverage.dump(obj, dumpId);
                    else
                        objectCoverage.update(obj, dumpId);
                    long time3 = System.currentTimeMillis();

                    if (debug) {
                        if ((time3 - time1) / 1000. > 1)
                            log("slow dump id: " + dumpId);
                        log("[debug performance problem] dumpId = " + dumpId + "\t, process time = "
                                + (time3 - time2) / 1000. + "s" + ", total time = "
                                + (time3 - time1) / 1000. + "s");
                    }
                }
            }
        } else {
            log("objectCoverage is null, Invariant Runtime is not initialized properly!");
        }
        return obj;
    }

    public static boolean updateBranch(boolean status, int dumpId) {
        if (enable) {
            if (objectCoverage != null) {
                synchronized (objectCoverageLock) {
                    objectCoverage.updateBranch(status, dumpId);
                }
            } else {
                log("objectCoverage is null, Invariant Runtime is not initialized properly!");
            }
        }
        return status;
    }

    // Deprecated
    public static boolean updateBranchWithCollection(boolean status, int dumpId) {
        if (enable) {
            if (objectCoverage != null) {
                synchronized (objectCoverageLock) {
                    objectCoverage.updateBranchWithCollection(status, dumpId);
                }
            } else {
                log("objectCoverage is null, Invariant Runtime is not initialized properly!");
            }
        }
        return status;
    }

    // Deprecated
    public static Object updateCollection(Object obj, int dumpId) {
        if (enable) {
            if (objectCoverage != null) {
                synchronized (objectCoverageLock) {
                    objectCoverage.updateCollection(obj, dumpId);
                }
            } else {
                log("objectCoverage is null, Invariant Runtime is not initialized properly!");
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
