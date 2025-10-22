package Java_courswework;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientTest {
    private ServerSocket testServerSocket;
    private int testPort;
    private Client client;
    private JTextArea chatArea;

 // Create a mock/stub class for testing
    class MockClientGUI extends ClientGUI {
        public MockClientGUI() {
        	 super("testUser", "127.0.0.1", 5000);
        }

        @Override
        public void resetActiveCheckButton() {
            // Do nothing in tests
        }
    }

    // Now use this mock in tests:
    @BeforeEach
    void setUp() throws Exception {
        testServerSocket = new ServerSocket(0);
        testPort = testServerSocket.getLocalPort();
        chatArea = new JTextArea();
        
        ClientGUI mockGui = new MockClientGUI();
        client = new Client("testUser", "127.0.0.1", testPort, chatArea, mockGui); // ✅ Provide mock GUI
    }


    @AfterEach
    void tearDown() throws Exception {
        client.disconnect(true);
        testServerSocket.close();
    }

    @Test
    void testConnectSuccess() throws Exception {
        new Thread(() -> {
            try (Socket serverSideSocket = testServerSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(serverSideSocket.getOutputStream(), true)) {
                
                String receivedId = in.readLine();
                assertEquals("testUser", receivedId);
                out.println("CONNECTED_SUCCESSFULLY testUser");
                out.println("YOU_ARE_COORDINATOR");
            } catch (IOException e) {
                fail("Server-side exception: " + e.getMessage());
            }
        }).start();
        
        assertTrue(client.connect());
        assertTrue(client.isConnected());
    }

    @Test
    void testConnectIdAlreadyInUse() throws Exception {
        new Thread(() -> {
            try (Socket serverSideSocket = testServerSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(serverSideSocket.getOutputStream(), true)) {
                
                in.readLine(); // Read the ID
                out.println("ID_ALREADY_IN_USE");
            } catch (IOException e) {
                fail("Server-side exception: " + e.getMessage());
            }
        }).start();
        
        assertFalse(client.connect());
    }

    @Test
    void testSendMessage() throws Exception {
        new Thread(() -> {
            try (Socket serverSideSocket = testServerSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(serverSideSocket.getOutputStream(), true)) {
                
                in.readLine(); // Read connection message
                out.println("CONNECTED_SUCCESSFULLY testUser");
                out.println("YOU_ARE_COORDINATOR");
                
                assertEquals("BROADCAST: Hello", in.readLine());
            } catch (IOException e) {
                fail("Server-side exception: " + e.getMessage());
            }
        }).start();
        
        client.connect();
        Thread.sleep(100); // Ensure connection setup completes
        client.sendMessage("Hello", "");
    }

    @Test
    void testChangeIdSuccess() throws Exception {
        new Thread(() -> {
            try (Socket serverSideSocket = testServerSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(serverSideSocket.getOutputStream(), true)) {
                
                in.readLine(); // Read initial ID
                out.println("CONNECTED_SUCCESSFULLY testUser");
                out.println("YOU_ARE_COORDINATOR");
                
                String receivedChangeIdCommand = in.readLine();
                if ("CHANGE_ID newTestUser".equals(receivedChangeIdCommand)) {
                    out.println("ID_CHANGED newTestUser");
                } else {
                    out.println("ERROR_ID_NOT_FOUND");
                }
            } catch (IOException e) {
                fail("Server-side exception: " + e.getMessage());
            }
        }).start();
        
        client.connect();
        Thread.sleep(100); // Ensure connection setup completes
        client.changeId("newTestUser");
    }
    
    @Test
    void testDisconnect() throws Exception {
        new Thread(() -> {
            try (Socket serverSideSocket = testServerSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(serverSideSocket.getOutputStream(), true)) {
                
                in.readLine(); // Read connection ID
                out.println("CONNECTED_SUCCESSFULLY testUser");
                out.println("YOU_ARE_COORDINATOR");
                
                assertEquals("EXIT", in.readLine());
            } catch (IOException e) {
                fail("Server-side exception: " + e.getMessage());
            }
        }).start();
        
        client.connect();
        Thread.sleep(100); // Ensure connection setup completes
        client.disconnect(true);
    }
}