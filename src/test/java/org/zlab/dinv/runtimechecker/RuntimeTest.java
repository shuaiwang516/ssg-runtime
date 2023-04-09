package org.zlab.dinv.runtimechecker;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;

import static org.zlab.dinv.runtimechecker.Runtime.dumpViolationServer;

public class RuntimeTest {

    private static final String SERVER_HOST = "localhost"; // the server host name or IP address
    private static final int SERVER_PORT = 8080; // the server port

    @Test
    public void testRuntimeServer() {
        try {
            dumpViolationServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRuntimeClient() throws IOException, ClassNotFoundException {
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT); // create a socket connection to the server

        // BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // create a reader for the server response
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // create a writer for the client input

        out.println("collectInv"); // send a command to the server
        System.out.println("Sent command: Hello");

        Runtime.ViolationInfo response = (Runtime.ViolationInfo) in.readObject(); // read the server response

        System.out.println("Received response: " + response.getMap());

        // clean up resources
        out.close();
        in.close();
        socket.close();
    }

}
