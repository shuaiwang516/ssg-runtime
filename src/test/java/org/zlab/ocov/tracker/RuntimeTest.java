package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Paths;

public class RuntimeTest {

    private static final String SERVER_HOST = "localhost"; // the server host name or IP address
    private static final int SERVER_PORT = 62000; // the server port

    // @Test
    public void testRuntimeServer() {
    }

    // @Test
    public void testFetchMultiThreads()
            throws IOException, ClassNotFoundException, InterruptedException {
        // fetchInvInfo();
        Thread t1 = new Thread(() -> {
            try {
                fetchInvInfo();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();

        Thread t2 = new Thread(() -> {
            try {
                fetchInvInfo();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        t2.start();

        t1.join();
        t2.join();
    }

    // @Test
    public void testFetch() throws IOException, ClassNotFoundException {
        fetchInvInfo();
    }

    public void fetchInvInfo() throws IOException, ClassNotFoundException {
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("collectInv"); // send a command to the server
        ObjectGraphCoverage response = (ObjectGraphCoverage) in.readObject(); // read the server
        // clean up resources
        out.close();
        in.close();
        socket.close();
    }

    // Run this only when debugging
    // @Test
    public void testFetchLoop() throws IOException, ClassNotFoundException {
        fetchInvInfoLoop();
    }

    public void fetchInvInfoLoop() throws IOException, ClassNotFoundException {
        ObjectGraphCoverage objectGraphCoverage = null;
        Runtime.initWriter(Paths.get("/tmp/testCoverage.log"));
        while (true) {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("collectInv"); // send a command to the server
            ObjectGraphCoverage response = (ObjectGraphCoverage) in.readObject(); // read the server
            // response.objCoverage.get("org.apache.cassandra.db.RowIndexEntry$IndexedEntry").graph
            FormatCoverageStatus newCov;
            if (objectGraphCoverage == null) {
                objectGraphCoverage = response;
                newCov = new FormatCoverageStatus(true, true);
            } else {
                // Add a break point here for checking
                newCov = objectGraphCoverage.merge(response);
            }
            System.out.println("newCov = " + newCov);
            out.close();
            in.close();
            socket.close();
        }
    }

}
