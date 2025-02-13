package org.zlab.net.tracker;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Runtime {

    private static final Object lock = new Object();

    public static BufferedWriter writer;
    public static final Path filePath = Paths.get("/tmp/coverage.log");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");

    private static final int PORT = 62000; // the port to listen on

    private static Trace trace;

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
        log("Net Runtime initialized!");
    }

    public static void record(String name, int id, Object... contextArgs) {
        // record this information
        if (trace == null)
            return;

        synchronized (lock) {
            log("[Runtime] Recording trace entry: " + id);
            trace.record(name, id, contextArgs);
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
