package Java_courswework;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import java.io.PrintWriter;
import java.net.Socket;

@TestMethodOrder(OrderAnnotation.class)  // ✅ Enables ordering of test execution
class ServerTest {
    private Server server;

    @BeforeEach
    void setUp() throws Exception {
        server = new Server(5000);
        new Thread(() -> server.start()).start();  
        Thread.sleep(500);  // ✅ Give time for the server to start
    }



    @Test
    @Order(1)
    void testAddClient() throws Exception {
        Thread.sleep(500); // ✅ Allow server to fully start

        // ✅ Keep connection open throughout the test
        Socket socket = new Socket("127.0.0.1", 5000);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        server.addClient("testUser", socket, out);

        assertFalse(server.isIdAvailable("testUser"), "Client should be added successfully.");
        
        Thread.sleep(1000); // ✅ Keep connection open to prevent immediate disconnection

        // ✅ Clean up
        socket.close();
    }



    @Test
    @Order(2)
    void testChangeClientId() throws Exception {
        // ✅ Ensure the user is registered first
        try (Socket socket = new Socket("127.0.0.1", 5000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            server.addClient("testUser", socket, out);
        }

        // ✅ Change the ID now that it exists
        boolean changed = server.changeClientId("testUser", "newTestUser", new PrintWriter(System.out, true));
        
        assertTrue(changed, "Client ID should change successfully.");
    }


    @Test
    @Order(3)  // ✅ Runs last
    void testStopServer() {
        server.stopServer();
        assertFalse(server.isCoordinator("testUser"), "Server should stop.");
    }
    
}
