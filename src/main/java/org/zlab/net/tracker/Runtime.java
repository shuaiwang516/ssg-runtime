package org.zlab.net.tracker;

import org.zlab.ocov.Utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Runtime {
    private static final Object lock = new Object();

    public static BufferedWriter writer;
    public static final Path filePath = Paths.get("/tmp/coverage.log");
    public static Set<String> changedClasses;

    // Hardcoded path
    private static final Path modifiedFieldsPath = Paths.get("/tmp/modifiedFields.json");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    private static final int PORT = 62000; // the port to listen on

    // Execute trace
    private static Trace trace;

    // Per-thread ring buffer
    private static final int K = 128; // number of recent blocks you want
    private static final ThreadLocal<int[]> buf = ThreadLocal.withInitial(() -> new int[K]);
    private static final ThreadLocal<Integer> idx = ThreadLocal.withInitial(() -> 0);

    public static void init() {
        trace = new Trace();
        try {
            writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
            Tracker();
        } catch (IOException e) {
            log("Error initializing Tracker: " + e);
            // rt exception
            throw new RuntimeException(e);
        }

        log("Net Runtime initialization started!");
        // Load the changed classes if it exists
        if (modifiedFieldsPath.toFile().exists()) {
            Map<String, Set<String>> modifiedFields = Utils.loadModifiedFields(modifiedFieldsPath);
            changedClasses = modifiedFields.keySet();
            log("Loaded changed classes, size = " + changedClasses.size());
        } else {
            log("No modified fields file found");
        }
        log("Net Runtime initialized!");
    }

    // Testing
    public static Trace getTrace() {
        // Return a copy of the trace to avoid concurrent modification issues
        return trace;
    }

    /* =========== Recent Branch Recording =========== */

    // Record one hit
    public static void hit(int id) {
        int[] b = buf.get();
        int i = idx.get();
        b[i & (K - 1)] = id; // write into ring
        idx.set(i + 1);
    }

    // Snapshot for this thread (e.g., at message send)
    public static int[] snapshot() {
        int[] b = buf.get();
        int i = idx.get();
        int[] snap = new int[K];
        // copy recent K entries, starting from most recent
        for (int j = 0; j < K; j++) {
            snap[j] = b[(i - K + j) & (K - 1)];
        }
        return snap;
    }

    // Reset after message send (optional)
    public static void clear() {
        // clear trace
        synchronized (lock) {
            log("[Runtime] Clearing trace");
            trace = new Trace();
        }

        // reset the ring buffer
        buf.set(new int[K]);
        idx.set(0);
    }

    /* =========== Message Recording =========== */
    public static void record(String name, int id, Object... contextArgs) {
        // record this information
        if (trace == null)
            return;

        int[] recentExecPath = snapshot();

        synchronized (lock) {
            log("[Runtime] Recording trace entry: " + id);
            trace.record(name, id, recentExecPath, contextArgs);
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

    public static void Tracker() throws IOException {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    log("[hklog] Net Runtime waiting!");
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
                    synchronized (lock) {
                        if (!inputLine.equals("clear")) {
                            out.writeObject(trace);
                        }
                        trace = new Trace();
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
}
