package org.zlab.ocov.tracker;

import org.zlab.ocov.Utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class Runtime {
    public static final boolean debug = false;
    public static final boolean debugTimeUsage = false;

    // Only enable the runtime when the environment variable is set
    public static boolean enable = true;

    // FIXME: This is disabled in BC+FC evaluation
    public static boolean enableCreationContextMonitor = false;

    public static boolean enableUpdate = true;
    public static boolean enableBoundaryCheck = true;
    public static boolean enableEqualityLikelyInvariant = true;
    public static final String enableEnvName = "ENABLE_FORMAT_COVERAGE";
    public static final String enableSampleEnvName = "ENABLE_FORMAT_COVERAGE_SAMPLE";
    public static final String sampleRateEnvName = "FORMAT_COVERAGE_SAMPLE_RATE";

    public static final Random rand = new Random();

    // VD: whether to use prob
    public static final boolean useProbabilityModel = false;
    public static final boolean captureObjectChangedDirectly = true;
    public static final int distanceThreshold = 2;

    public static boolean sample = false;
    public static double sampleRate = 0.2; // default value

    public static Path baseClassPath = Paths.get("/tmp/serializedFields_alg1.json");
    public static Path topObjectsPath = Paths.get("/tmp/topObjects.json");
    public static Path comparableClassesPath = Paths.get("/tmp/comparableClasses.json");

    public static Path modifiedFieldsPath = Paths.get("/tmp/modifiedFields.json");
    public static Path modifiedEnumsPath = Paths.get("/tmp/modifiedEnums.json");
    public static Path modifiedTypeHierarchyPath = Paths.get("/tmp/typeWithModifiedHierarchy.json");

    public static Path branch2CollectionPath = Paths.get("/tmp/branch2Collection.json");
    public static Path specialDumpIdsPath = Paths.get("/tmp/modifiedDumpIds.json");

    public static Path filePath = Paths.get("/tmp/coverage.log");
    // public static Path filePath = Paths.get("/var/log/cassandra/coverage.log");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    public static BufferedWriter writer;
    public static ObjectGraphCoverage objectCoverage;
    private static final Object objectCoverageLock = new Object();

    // Debug
    private static Map<Integer, Double> dumpId2AccumTime = new HashMap<>();

    public static boolean isSampled() {
        return rand.nextDouble() < sampleRate;
    }

    public static final Set<String> preservedStrings = new HashSet<>();
    static {
        preservedStrings.add("system");
    }

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
                    modifiedTypeHierarchyPath, branch2CollectionPath, specialDumpIdsPath);
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

    // contextObject refer to "this"
    public static Object monitorCreationContext(Object obj, int dumpId) {
        return monitorCreationContext(obj, dumpId, null);
    }

    public static Object monitorCreationContext(Object obj, int dumpId, Object contextObject) {
        if (!enable || obj == null || objectCoverage == null)
            return obj;

        if (!enableCreationContextMonitor)
            return obj;

        long time1 = System.currentTimeMillis();

        objectCoverage.monitorCreationContext(obj, contextObject, dumpId);

        long time2 = System.currentTimeMillis();
        if (debug && debugTimeUsage) {
            double totalTime = (time2 - time1) / 1000.;

            if (totalTime > 5)
                log("slow dump id: " + dumpId);
            // update dumpId2AccumTime
            if (dumpId2AccumTime.containsKey(dumpId)) {
                dumpId2AccumTime.put(dumpId, dumpId2AccumTime.get(dumpId) + totalTime);
            } else {
                dumpId2AccumTime.put(dumpId, totalTime);
            }
            log("[debug performance: monitorCreationContext] dumpId = " + dumpId + ", total time = "
                    + totalTime + "s" + ", accum time = " + dumpId2AccumTime.get(dumpId) + "s");
        }
        return obj;
    }

    // id uniquely identify the program location for dumping
    public static Object update(Object obj, int dumpId, Object... contextArgs) {
        if (!enable || obj == null || (sample && !isSampled()) || objectCoverage == null)
            return obj;

        if (!enableUpdate)
            return obj;

        if (debug)
            Runtime.log("[Runtime.update] dumpId: " + dumpId + ", obj type = : "
                    + obj.getClass().getName() + ", hashcode = " + System.identityHashCode(obj));

        long time1 = System.currentTimeMillis();

        synchronized (objectCoverageLock) {
            long time2 = System.currentTimeMillis();

            // Runtime.log("dumpId: " + dumpId + ", obj type = : " +
            // obj.getClass().getName()
            // + ", hashcode = " + System.identityHashCode(obj));

            objectCoverage.update(obj, dumpId, contextArgs);

            long time3 = System.currentTimeMillis();
            if (debug && debugTimeUsage) {
                double processTime = (time3 - time2) / 1000.;
                double totalTime = (time3 - time1) / 1000.;

                if (totalTime > 5)
                    log("slow dump id: " + dumpId);
                // update dumpId2AccumTime
                if (dumpId2AccumTime.containsKey(dumpId)) {
                    dumpId2AccumTime.put(dumpId, dumpId2AccumTime.get(dumpId) + totalTime);
                } else {
                    dumpId2AccumTime.put(dumpId, totalTime);
                }
                log("[debug performance: update] dumpId = " + dumpId + "\t, process time = "
                        + processTime + "s" + ", total time = " + totalTime + "s"
                        + ", accum time = " + dumpId2AccumTime.get(dumpId) + "s");
            }
        }
        return obj;
    }

    public static boolean updateBranch(Object lhsOp, Object rhsOp, String operator, int dumpId) {
        if (enable && enableBoundaryCheck && objectCoverage != null) {
            synchronized (objectCoverageLock) {
                return objectCoverage.updateBranch(lhsOp, rhsOp, operator, dumpId);
            }
        } else {
            return Utils.computeBinaryComparison(Utils.toLong(lhsOp), Utils.toLong(rhsOp),
                    operator);
        }
    }

    // Deprecated
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

    private static final int PORT = 62000; // the port to listen on

    public static void formatCoverageTracker() throws IOException {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    log("[hklog] Invariant Runtime waiting!");
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    log("Received command: " + inputLine);
                    ObjectGraphCoverage response;
                    synchronized (objectCoverageLock) {
                        if (!inputLine.equals("clear")) {
                            response = processCommand(inputLine);
                            out.writeObject(response);
                        }
                        objectCoverage = new ObjectGraphCoverage(baseClassPath, topObjectsPath,
                                comparableClassesPath, modifiedFieldsPath, modifiedEnumsPath,
                                modifiedTypeHierarchyPath, branch2CollectionPath,
                                specialDumpIdsPath);
                    }
                    System.out.println("Coverage has been sent to the client");
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
        }
    }

    private static ObjectGraphCoverage processCommand(String command) {
        objectCoverage.inferInvariant();
        return objectCoverage;
    }
}
