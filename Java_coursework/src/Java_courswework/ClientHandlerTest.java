package Java_courswework;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;

class ClientHandlerTest {
    private static Server testServer;
    private static Thread serverThread;
    private ByteArrayOutputStream outputStream;
    private PrintWriter testPrintWriter;
    private Socket testSocket;
    private ClientHandler clientHandler;

    @BeforeAll
    static void setUpServer() throws IOException, InterruptedException {
        testServer = new Server(5000);
        serverThread = new Thread(() -> testServer.start());
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(1500); // Ensure server has time to start properly
    }

    @AfterAll
    static void tearDownServer() throws IOException, InterruptedException {
        testServer.stopServer();
        Thread.sleep(1000); // Ensure server stops properly
    }

    void setUpClient() throws IOException {
        testSocket = new Socket("localhost", 5000);
        outputStream = new ByteArrayOutputStream();
        testPrintWriter = new PrintWriter(outputStream, true);
        clientHandler = new ClientHandler(testSocket, testServer);
        clientHandler.start();
    }

    @Test
    void testClientHandler_CheckIdAvailable() throws IOException {
        setUpClient();
        assertNotNull(testServer);
        assertTrue(testServer.isIdAvailable("test123"));
    }

    @Test
    void testClientHandler_CheckIdTaken() throws IOException {
        setUpClient();
        testServer.addClient("test123", testSocket, testPrintWriter);
        assertFalse(testServer.isIdAvailable("test123"));
    }

    @Test
    void testClientHandler_RegisterIdSuccess() throws IOException {
        setUpClient();
        assertTrue(testServer.isIdAvailable("user1"));
        testServer.addClient("user1", testSocket, testPrintWriter);
        assertFalse(testServer.isIdAvailable("user1"));
    }

    @Test
    void testClientHandler_RegisterIdFailure() throws IOException {
        setUpClient();
        testServer.addClient("user1", testSocket, testPrintWriter);
        boolean result = testServer.isIdAvailable("user1");
        assertFalse(result);
    }

    @Test
    void testClientHandler_ChangeIdSuccess() throws IOException {
        setUpClient();
        testServer.addClient("oldUser", testSocket, testPrintWriter);
        boolean changed = testServer.changeClientId("oldUser", "newUser", testPrintWriter);
        assertTrue(changed);
        assertTrue(testServer.isIdAvailable("oldUser"));
        assertFalse(testServer.isIdAvailable("newUser"));
    }

    @Test
    void testClientHandler_ChangeIdFailure() throws IOException {
        setUpClient();
        testServer.addClient("takenUser", testSocket, testPrintWriter);
        boolean changed = testServer.changeClientId("newUser", "takenUser", testPrintWriter);
        assertFalse(changed);
    }

    @Test
    void testClientHandler_ExitCommand() throws IOException {
        setUpClient();
        testServer.addClient("user1", testSocket, testPrintWriter);
        testServer.notifyMemberLeft("user1", false);
        assertTrue(testServer.isIdAvailable("user1"));
    }
}
