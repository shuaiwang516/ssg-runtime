package org.zlab.dinv.runtimechecker;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;

import static org.zlab.dinv.runtimechecker.Runtime.dumpViolationServer;

public class RuntimeTest {

    private static final String SERVER_HOST = "localhost"; // the server host name or IP address
    private static final int SERVER_PORT = 62000; // the server port

    @Test
    public void testRuntimeServer() {
        try {
            dumpViolationServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRuntimeClient() throws IOException, ClassNotFoundException, InterruptedException {
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

    public void fetchInvInfo() throws IOException, ClassNotFoundException {
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT); // create a socket connection to the server

        // BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // create a reader for the server response
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // create a writer for the client input

        out.println("collectInv"); // send a command to the server
        System.out.println("Sent command: Hello");

        Runtime.ViolationInfo response = (Runtime.ViolationInfo) in.readObject(); // read the server response

        System.out.println("Received response length " + response.getViolations().length);

        // clean up resources
        out.close();
        in.close();
        socket.close();
    }

    public void tmp() {
        System.out.println("hh");
    }

    @Test
    public void test() {

    }

}
