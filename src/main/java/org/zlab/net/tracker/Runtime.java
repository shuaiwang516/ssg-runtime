package org.zlab.net.tracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Runtime {
    private static final Object lock = new Object();

    private static final String enableEnvName = "ENABLE_NETWORK_TRACE";
    private static final String portEnvName = "NET_TRACE_PORT";
    private static final String logPathEnvName = "NET_TRACE_LOG_PATH";
    private static final String nodeIdEnvName = "NET_TRACE_NODE_ID";
    private static final String nodeRoleEnvName = "NET_TRACE_NODE_ROLE";
    private static final String enableServerEnvName = "NET_TRACE_ENABLE_SERVER";
    private static final String maxAfterBranchesEnvName = "NET_TRACE_MAX_AFTER_BRANCHES";
    private static final String receiveTimeoutEnvName = "NET_TRACE_RECEIVE_TIMEOUT_MS";

    public static BufferedWriter writer;
    public static Path filePath = Paths.get("/tmp/coverage.log");
    public static Set<String> changedClasses;
    public static String nodeId = "unknown";
    public static String nodeRole = null;

    private static final Path modifiedFieldsPath = Paths.get("/tmp/modifiedFields.json");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    private static final int defaultPort = 62000;
    private static final int defaultMaxAfterBranches = 128;
    private static final long defaultReceiveTimeoutMs = 1_000L;
    private static final int K = 128;

    private static int port = defaultPort;
    private static boolean enable = true;
    private static boolean enableServer = true;
    private static int maxAfterBranches = defaultMaxAfterBranches;
    private static long receiveTimeoutMs = defaultReceiveTimeoutMs;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean serverStarted = new AtomicBoolean(false);
    private static final AtomicLong receiveTokenGenerator = new AtomicLong(1L);

    private static Trace trace;

    private static final ThreadLocal<int[]> buf = ThreadLocal.withInitial(() -> new int[K]);
    private static final ThreadLocal<Integer> idx = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Map<Long, ReceiveContext>> activeReceives = ThreadLocal
            .withInitial(HashMap::new);

    public static void init() {
        init(true);
    }

    public static void init(boolean startServer) {
        if (initialized.get()) {
            return;
        }
        synchronized (lock) {
            if (initialized.get()) {
                return;
            }
            loadRuntimeConfig();
            trace = new Trace();

            try {
                writer = new BufferedWriter(new FileWriter(filePath.toFile(), true));
            } catch (IOException e) {
                throw new RuntimeException("Error initializing network trace writer", e);
            }

            if (!enable) {
                log("Network Runtime disabled by environment");
                initialized.set(true);
                return;
            }

            log("Net Runtime initialization started!");
            if (modifiedFieldsPath.toFile().exists()) {
                Map<String, Set<String>> modifiedFields = Utils
                        .loadModifiedFields(modifiedFieldsPath);
                changedClasses = modifiedFields.keySet();
                log("Loaded changed classes, size = " + changedClasses.size());
            } else {
                log("No modified fields file found");
            }
            initialized.set(true);
            log("Net Runtime initialized!");
        }
        if (startServer && enableServer) {
            startTracker();
        }
    }

    public static Trace getTrace() {
        synchronized (lock) {
            if (trace == null) {
                return new Trace();
            }
            return trace.copy();
        }
    }

    /* =========== Branch Recording =========== */

    public static void hit(int branchId) {
        if (!enable) {
            return;
        }

        int[] b = buf.get();
        int i = idx.get();
        b[i & (K - 1)] = branchId;
        idx.set(i + 1);

        Map<Long, ReceiveContext> contexts = activeReceives.get();
        if (contexts.isEmpty()) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        List<ReceiveContext> expired = null;
        for (Iterator<Map.Entry<Long, ReceiveContext>> it = contexts.entrySet().iterator(); it
                .hasNext();) {
            ReceiveContext ctx = it.next().getValue();
            if (nowMs - ctx.beginTimestampMillis > receiveTimeoutMs) {
                it.remove();
                if (expired == null) {
                    expired = new ArrayList<>();
                }
                expired.add(ctx);
                continue;
            }
            ctx.appendAfter(branchId);
        }

        if (expired != null) {
            for (ReceiveContext ctx : expired) {
                finalizeReceiveContext(ctx, true);
            }
        }
    }

    public static int[] snapshot() {
        int[] b = buf.get();
        int i = idx.get();
        int size = Math.min(i, K);
        if (size <= 0) {
            return new int[0];
        }
        int[] snap = new int[size];
        for (int j = 0; j < size; j++) {
            snap[j] = b[(i - size + j) & (K - 1)];
        }
        return snap;
    }

    public static void clear() {
        synchronized (lock) {
            if (trace != null) {
                trace = new Trace();
            }
        }
        buf.set(new int[K]);
        idx.set(0);
        activeReceives.get().clear();
    }

    /* =========== Sender/Receiver APIs =========== */

    // Legacy API kept for compatibility, mapped to sender event.
    public static void record(String name, int id, Object... contextArgs) {
        Object message = firstArg(contextArgs);
        recordSend(name, id, message, null, contextArgs);
    }

    public static void recordSend(String name, int id, Object message, SendMeta sendMeta,
            Object... contextArgs) {
        if (!enable || trace == null) {
            return;
        }
        int[] before = snapshot();
        SendMeta normalized = sendMeta == null
                ? SendMeta.builder().nodeId(nodeId).nodeRole(nodeRole).build()
                : sendMeta.withDefaults(nodeId, nodeRole);
        synchronized (lock) {
            if (trace == null) {
                return;
            }
            trace.recordSend(name, id, before, message, normalized, contextArgs);
        }
        log("NETTRACE SEND name=" + name + " id=" + id + " node=" + normalized.nodeId + " peer="
                + normalized.peerId + " msgType=" + normalized.messageType + " beforeLen="
                + before.length);
    }

    public static long beginReceive(String name, int id, Object message, RecvMeta recvMeta,
            Object... contextArgs) {
        if (!enable || trace == null) {
            return -1L;
        }
        int[] before = snapshot();
        long token = receiveTokenGenerator.getAndIncrement();
        RecvMeta normalized = recvMeta == null
                ? RecvMeta.builder().nodeId(nodeId).nodeRole(nodeRole).build()
                : recvMeta.withDefaults(nodeId, nodeRole);
        ReceiveContext ctx = new ReceiveContext(token, name, id, message, normalized, before,
                contextArgs, maxAfterBranches, System.currentTimeMillis());
        activeReceives.get().put(token, ctx);
        synchronized (lock) {
            if (trace != null) {
                trace.recordReceiveBegin(name, id, before, message, normalized, contextArgs);
            }
        }
        log("NETTRACE RECV_BEGIN name=" + name + " id=" + id + " token=" + token + " node="
                + normalized.nodeId + " peer=" + normalized.peerId + " msgType="
                + normalized.messageType + " beforeLen=" + before.length);
        return token;
    }

    public static void endReceive(long token) {
        if (token <= 0 || trace == null) {
            return;
        }
        ReceiveContext ctx = activeReceives.get().remove(token);
        if (ctx == null) {
            return;
        }
        finalizeReceiveContext(ctx, false);
    }

    private static void finalizeReceiveContext(ReceiveContext ctx, boolean timedOut) {
        synchronized (lock) {
            if (trace == null) {
                return;
            }
            trace.recordReceiveEnd(ctx.name, ctx.id, ctx.beforeExecPath, ctx.afterSnapshot(),
                    ctx.message, ctx.recvMeta, timedOut, ctx.contextArgs);
        }
        log("NETTRACE RECV_END name=" + ctx.name + " id=" + ctx.id + " token=" + ctx.token
                + " timedOut=" + timedOut + " afterLen=" + ctx.afterCount);
    }

    public static void log(String message) {
        if (writer == null) {
            return;
        }
        try {
            String timeStamp = dateFormat.format(new Date());
            writer.write(timeStamp + " - " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startTracker() {
        if (!serverStarted.compareAndSet(false, true)) {
            return;
        }
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                serverStarted.set(false);
                log("Network tracker stopped: " + e.getMessage());
            } catch (RuntimeException e) {
                serverStarted.set(false);
                log("Network tracker stopped unexpectedly: " + e.getMessage());
            }
        }, "net-trace-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    // Backward compatibility with older callers.
    public static void Tracker() {
        startTracker();
    }

    private static void loadRuntimeConfig() {
        String enableEnv = System.getenv(enableEnvName);
        if (enableEnv != null) {
            enable = Boolean.parseBoolean(enableEnv);
        }
        String serverEnv = System.getenv(enableServerEnvName);
        if (serverEnv != null) {
            enableServer = Boolean.parseBoolean(serverEnv);
        }
        nodeId = readStringEnv(nodeIdEnvName, nodeId);
        // System property takes priority over env var for nodeRole
        // (allows per-JVM override when multiple JVMs share a container)
        String sysPropRole = System.getProperty(nodeRoleEnvName);
        if (sysPropRole != null && !sysPropRole.trim().isEmpty()) {
            nodeRole = sysPropRole.trim();
        } else {
            nodeRole = readStringEnv(nodeRoleEnvName, nodeRole);
        }
        filePath = Paths.get(readStringEnv(logPathEnvName, filePath.toString()));
        port = readIntEnv(portEnvName, defaultPort);
        maxAfterBranches = Math.max(1,
                readIntEnv(maxAfterBranchesEnvName, defaultMaxAfterBranches));
        receiveTimeoutMs = Math.max(1L,
                readLongEnv(receiveTimeoutEnvName, defaultReceiveTimeoutMs));
    }

    private static String readStringEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static int readIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long readLongEnv(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Object firstArg(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        private ClientHandler(Socket clientSocket) {
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
                    Trace snapshot = null;
                    synchronized (lock) {
                        if (!"clear".equals(inputLine)) {
                            snapshot = trace == null ? new Trace() : trace.copy();
                        }
                        trace = new Trace();
                    }
                    if (snapshot != null) {
                        out.writeObject(snapshot);
                        out.flush();
                    }
                }
            } catch (IOException e) {
                log("Error in client connection: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ReceiveContext {
        private final long token;
        private final String name;
        private final int id;
        private final Object message;
        private final RecvMeta recvMeta;
        private final int[] beforeExecPath;
        private final Object[] contextArgs;
        private final int[] afterBranchBuffer;
        private int afterCount = 0;
        private final long beginTimestampMillis;

        private ReceiveContext(long token, String name, int id, Object message, RecvMeta recvMeta,
                int[] beforeExecPath, Object[] contextArgs, int maxAfterBranches,
                long beginTimestampMillis) {
            this.token = token;
            this.name = name;
            this.id = id;
            this.message = message;
            this.recvMeta = recvMeta;
            this.beforeExecPath = beforeExecPath;
            this.contextArgs = contextArgs;
            this.afterBranchBuffer = new int[Math.max(1, maxAfterBranches)];
            this.beginTimestampMillis = beginTimestampMillis;
        }

        private void appendAfter(int branchId) {
            if (afterCount >= afterBranchBuffer.length) {
                return;
            }
            afterBranchBuffer[afterCount++] = branchId;
        }

        private int[] afterSnapshot() {
            int[] snapshot = new int[afterCount];
            for (int i = 0; i < afterCount; i++) {
                snapshot[i] = afterBranchBuffer[i];
            }
            return snapshot;
        }
    }
}
