package Java_courswework;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Client Component


class Client {
    private String id;
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JTextArea chatArea;
    private ClientGUI gui;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isCoordinator = false; // ✅ Track if this client is the coordinator
	private boolean isAutoActiveCheckEnabled = false;
	private boolean disconnecting = false;
	private boolean isChangingConnection = false;

    public Client(String id, String serverIp, int serverPort, JTextArea chatArea, ClientGUI gui) {
        this.id = id;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.chatArea = chatArea;
        this.gui = gui;
    }

    // Return true if successfully connected, false if ID already in use or failure.
    public boolean connect() {
        if (socket != null && !socket.isClosed()) {
            SwingUtilities.invokeLater(() -> chatArea.append("Already connected.\n"));
            return true;
        }

        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(id);
            out.flush();

            String response;
            boolean connectedSuccessfully = false;

            // Read initial server responses
            while ((response = in.readLine()) != null) {
                final String serverResponse = response.trim();

                if ("ID_ALREADY_IN_USE".equals(serverResponse)) {
                    SwingUtilities.invokeLater(() -> chatArea.append("ID already in use.\n"));
                    disconnect(true);
                    return false;  // Indicate clearly to the GUI: ID is taken
                } else if (serverResponse.startsWith("CONNECTED_SUCCESSFULLY")) {
                    SwingUtilities.invokeLater(() -> chatArea.append("Connected successfully as: " + id + "\n"));
                    connectedSuccessfully = true;
                } else if (serverResponse.equals("You are coordinator.")) {
                    if (!isCoordinator) { // ✅ Ensure this is set only once
                        isCoordinator = true;
                        SwingUtilities.invokeLater(() -> chatArea.append("You are now the Coordinator.\n"));
                    }
                    
                } else if (serverResponse.startsWith("The coordinator is: ")) {
                    SwingUtilities.invokeLater(() -> chatArea.append("🔹 " + serverResponse + "\n"));
                }

                // Exit once we've processed initial connection responses
                if (connectedSuccessfully) {
                    break;
                }
            }

            if (connectedSuccessfully) {
                Thread listenerThread = new Thread(this::listenForMessages);
                listenerThread.setDaemon(true);
                listenerThread.start();
                return true;  // Successful connection
            }

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> chatArea.append("Error: Failed to connect to server!\n"));
            disconnect(true);
        }

        return false;  // Connection failed
    }

    
    
    
    
    

    private void listenForMessages() {
        try {
            String receivedMessage;
            while ((receivedMessage = in.readLine()) != null) {
                final String finalMessage = receivedMessage.trim();                

                SwingUtilities.invokeLater(() -> {
                    String cleanMessage = finalMessage.replaceAll("[^\\x20-\\x7E]", ""); // ✅ Remove non-printable characters
                    chatArea.append(cleanMessage + "\n");


                    if (cleanMessage.equals("You are coordinator.")) {
                        // ✅ Ensure "You are now the Coordinator." is printed only once
                        if (!isCoordinator) {
                            isCoordinator = true;
                        }
                       
                    } else if (cleanMessage.startsWith("The coordinator is: ")) {
                        String[] parts = cleanMessage.split(" ");
                        if (parts.length > 1) { // Ensures valid format
                            String newCoordinator = parts[parts.length - 1]; // Extract the new coordinator ID
                            boolean wasCoordinator = isCoordinator; // ✅ Track previous state
                            isCoordinator = newCoordinator.equals(id);

                            // ✅ Print only if the role changed to coordinator
                            if (!wasCoordinator && isCoordinator) {
                                chatArea.append("You are now the Coordinator!\n");
                            }
                        }
                    }
                });
            }
        } catch (IOException e) {
        	if (!disconnecting && !isChangingConnection) { // ✅ Only show error if NOT a manual disconnect
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Error: Connection lost!\n");
                    gui.resetConnectButton(); // ✅ Reset the "Connect" button when disconnected unexpectedly
                });
            }
        }
    }
    
    
    
    public static boolean isIdAvailable(String idToCheck, String serverIp, int serverPort) {
        try (Socket tempSocket = new Socket(serverIp, serverPort);
             PrintWriter tempOut = new PrintWriter(tempSocket.getOutputStream(), true);
             BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()))) {
    
            // Send special CHECK_ID command clearly:
            tempOut.println("CHECK_ID " + idToCheck);
            tempOut.flush();
    
            String response = tempIn.readLine();
            return "ID_AVAILABLE".equals(response);
        } catch (IOException e) {
            return false; // Assume unavailable on network error
        }
    }
    
    

    public void sendMessage(String message, String recipient) {
        if (out == null) {
            chatArea.append("Error: Not connected to the server!\n");
            return;
        }

        if (recipient.isEmpty()) {
            out.println("BROADCAST: " + message);
        } else {
            out.println("PRIVATE: " + recipient + " " + message);
        }
    }

    public void checkActiveMembers() {
        if (out == null || socket.isClosed()) {
            chatArea.append("Error: Not connected to the server!\n");
            return;
        }
        
        if (!isCoordinator) { 
            chatArea.append("Error: Only the Coordinator can send an active check!\n");
            // ✅ Confirm the last received message from the server
            return;
        } else {
            // ✅ If the client is the Coordinator, send the active check request
            out.println("COORDINATOR_CHECK_ACTIVE");
            out.flush();
            chatArea.append("Active member check sent!\n");
        }
    }
    
    
    public void requestMemberDetails() {
        if (out == null) {
            chatArea.append("Error: Not connected to the server!\n");
            return;
        }
        out.println("REQUEST_MEMBER_DETAILS");
        out.flush();  // Ensure the command is sent immediately
    }
    


    public void changeId(String newId) {
        if (newId.equals(id)) {
            SwingUtilities.invokeLater(() -> chatArea.append("Error: New ID must be different!\n"));
            return;
        }
    
        new Thread(() -> {
            try {
    
                // Send request to server
                out.println("CHANGE_ID " + newId);
                out.flush();
    
                // Read response asynchronously
                String response = in.readLine();
    
                SwingUtilities.invokeLater(() -> {
                    if (response == null) {
                        chatArea.append("Error: No response from server!\n");
                        return;
                    }
    
                    if (response.startsWith("ID_CHANGED")) {
                        id = newId;
                        chatArea.append("✅ ID changed successfully to: " + newId + "\n");
                        gui.updateIdField(newId); // ✅ Update GUI
                        // ✅ Prevent duplicate connections
                        reconnect();  

                    } else if (response.equals("ERROR_ID_TAKEN")) {
                        chatArea.append("Error: ID is already taken!\n");
                    } else {
                        chatArea.append( response + "\n");
                    }
                });
    
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatArea.append("Error: Failed to change ID!\n"));
            }
        }).start();
    }

    
    public boolean changeIpAddress(String newIp) {
        if (newIp.equals(serverIp)) {
            SwingUtilities.invokeLater(() -> chatArea.append("⚠️ Error: New IP must be different!\n"));
            return false;
        }

        System.out.println("🔄 Changing IP from " + serverIp + " to " + newIp);

        isChangingConnection = true; // ✅ Prevent server shutdown prompt
        serverIp = newIp;

        new Thread(() -> {
            try {
                if (!reconnectWithTimeout(5000)) {
                    SwingUtilities.invokeLater(() -> chatArea.append("❌ Error: Could not connect to " + newIp + "\n"));
                    serverIp = ""; // ✅ Reset IP since connection failed
                } else {
                    SwingUtilities.invokeLater(() -> chatArea.append("✅ Connected successfully to " + newIp + "\n"));
                }
            } finally {
                isChangingConnection = false; // ✅ Reset after reconnect attempt
            }
        }).start();

        return true;
    }


    public boolean changePort(int newPort) {
        if (newPort == serverPort) {
            SwingUtilities.invokeLater(() -> chatArea.append("⚠️ Error: New port must be different!\n"));
            return false;
        }

        System.out.println("🔄 Changing port from " + serverPort + " to " + newPort);

        isChangingConnection = true; // ✅ Prevent server shutdown prompt
        serverPort = newPort;

        new Thread(() -> {
            try {
                if (!reconnectWithTimeout(5000)) {
                    SwingUtilities.invokeLater(() -> chatArea.append("❌ Error: Could not connect to port " + newPort + "\n"));
                    serverPort = -1; // ✅ Reset port since connection failed
                } else {
                    SwingUtilities.invokeLater(() -> chatArea.append("✅ Connected successfully to port " + newPort + "\n"));
                }
            } finally {
                isChangingConnection = false; // ✅ Reset after reconnect attempt
            }
        }).start();

        return true;
    }



    public boolean checkUniqueId(String newId) {
        if (newId.equals(id)) {
            SwingUtilities.invokeLater(() -> chatArea.append("This is your current ID.\n"));
            return true;
        }
    
       
        try {
            out.println("CHECK_ID " + newId);
            out.flush();
            String response = in.readLine();
            return "ID_AVAILABLE".equals(response);
    
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> chatArea.append("Error: Failed to check ID availability!\n"));
            return false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    private void reconnect() {
        disconnect(true); // Ensure previous connection is closed
        SwingUtilities.invokeLater(() -> chatArea.append("Reconnecting with new ID: " + id + "\n"));
        connect();
    }

    public void disconnect(boolean userInitiated) {
        try {
            if (out != null) {
                if (isChangingConnection) { // ✅ If disconnecting for IP/Port change, send a temporary disconnect message
                    out.println("TEMP_DISCONNECT");
                } else {
                    out.println(userInitiated ? "EXIT" : "EXIT_WINDOW_CLOSE");
                }
                out.flush();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            // ✅ Reset Active Check Mode to Manual on Disconnect
            stopAutoActiveCheck();
            isAutoActiveCheckEnabled = false;

            // ✅ Update GUI Button (Ensure it resets to Manual)
            SwingUtilities.invokeLater(() -> {
                chatArea.append("Disconnected from server.\n");
                gui.resetActiveCheckButton();
                gui.resetConnectButton();
            });

            if (in != null) in.close();
            if (out != null) out.close();

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> chatArea.append("⚠️ Error: Failed to disconnect properly!\n"));
        } finally {
            if (!isChangingConnection) { // ✅ Reset `isChangingConnection` only if NOT changing IP/Port
                isChangingConnection = false;
            }
        }
    }

    
    public void startAutoActiveCheck() {
        if (!isCoordinator) return; // ✅ Only the coordinator should send active checks

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow(); // ✅ Stop existing timer before starting a new one
        }

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (isConnected() && isCoordinator) { // ✅ Ensure still connected and still coordinator
                checkActiveMembers();
            }
        }, 5, 5, TimeUnit.MINUTES); // ✅ Restart the timer to 5 minutes
    }

    public void stopAutoActiveCheck() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    
    public boolean isCoordinator() {
        return isCoordinator;
    }
    
    public boolean isAutoActiveCheckEnabled() {
        return isAutoActiveCheckEnabled;
    }
    
    public void toggleAutoActiveCheck() {
        if (!isCoordinator) {
            SwingUtilities.invokeLater(() -> chatArea.append("Only the Coordinator can use Active Check mode!\n"));
            return;
        }

        isAutoActiveCheckEnabled = !isAutoActiveCheckEnabled; // ✅ Toggle the state

        if (isAutoActiveCheckEnabled) {
            startAutoActiveCheck(); // ✅ Restart automatic Active Check
            SwingUtilities.invokeLater(() -> chatArea.append("Auto Active Check enebled, repeats every 5 minutes.\n"));
        } else {
            stopAutoActiveCheck(); // ✅ Stop automatic Active Check
            SwingUtilities.invokeLater(() -> chatArea.append("Auto Active Check disabled.\n"));
        }
    }
    
    private boolean reconnectWithTimeout(int timeoutMillis) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                disconnect(true); // ✅ Disconnect before attempting to reconnect
                Thread.sleep(500); // ✅ Small delay before retrying
                return connect(); // ✅ Try reconnecting
            } catch (Exception e) {
                System.out.println("🔄 Retrying connection...");
            }
        }

        return false; // ❌ If timeout reached, return failure
    }
    
    public void someMethodThatUsesGUI() {
        if (gui != null) {  // ✅ Prevent NullPointerException
            gui.resetActiveCheckButton();
        }
    }

}