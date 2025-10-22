package Java_courswework;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


class Server {
    private int port;
    private ServerSocket serverSocket; // Store the server socket reference
    private ConcurrentHashMap<String, PrintWriter> clients;
    private ConcurrentHashMap<String, String> clientDetails;
    public String coordinatorId;
    private ExecutorService pool;
    private volatile boolean running = true;
    


    public Server(int port) {
        this.port = port;
        clients = new ConcurrentHashMap<>();
        clientDetails = new ConcurrentHashMap<>();
    }

    public void start() {
        try {
            running = true; // ✅ Set running to true when the server starts
            serverSocket = new ServerSocket(port);
            System.out.println("🚀 Server started on port " + port);
            pool = Executors.newCachedThreadPool();
    
            // ✅ Start listening for manual shutdown commands
            new Thread(this::listenForCommands).start();
    
            while (running) { // ✅ Stop when running is set to false
                try {
                    Socket clientSocket = serverSocket.accept();
                    pool.submit(new ClientHandler(clientSocket, this));
                } catch (IOException e) {
                    if (!running) { // ✅ Stop loop if running is false
                        System.out.println("🔴 Server socket closed, stopping server.");
                        break;
                    }
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("🔴 Server stopped.");
        }
    }
    
    

    public synchronized boolean changeClientId(String oldId, String newId, PrintWriter out) {
        if (!clients.containsKey(oldId)) {
            out.println("ERROR_ID_NOT_FOUND");
            return false;
        }
    
        if (clients.containsKey(newId)) {
            out.println("ERROR_ID_TAKEN");
            return false;
        }
    
        // 🚀 Fix: Reserve new ID immediately before removing the old one
        clients.put(newId, clients.get(oldId));
        clientDetails.put(newId, clientDetails.get(oldId));
    
        // Remove old ID after reserving new one
        clients.remove(oldId);
        clientDetails.remove(oldId);
    
        // ✅ If the changing user is the Coordinator, update the coordinatorId
        boolean wasCoordinator = oldId.equals(coordinatorId);
        if (wasCoordinator) {
        	coordinatorId = newId;  // ✅ Ensure the new ID remains the coordinator
            for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                String clientId = entry.getKey();
                PrintWriter clientOut = entry.getValue();
                
                if (!clientId.equals(newId)) { // ✅ Skip sending message to the Coordinator themselves
                    clientOut.println("The Coordinator has changed their ID to " + newId);
                    clientOut.flush();
                }
            }
        } else {
            broadcastMessage("User " + oldId + " changed ID to " + newId);
        }
        
    
        return true;
    }
    

    public synchronized void handleCoordinatorCheck(String senderId) {
        if (clients.isEmpty()) {
            return;
        }
    
        if (!coordinatorId.equals(senderId)) {
            return;
        }
    
        for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            PrintWriter clientOut = entry.getValue();
    
            if (clientId.equals(coordinatorId)) continue; // ✅ Skip sending to the coordinator
            

    
            // ✅ Send "Are you still active?" message to all non-coordinators
            clientOut.println("Are you still active?");
            clientOut.flush();
        }
    }

    public synchronized void handleMessage(String sender, String message) {
        if (message.trim().isEmpty()) return; // Ignore empty messages
    
        if (message.equals("REQUEST_MEMBER_DETAILS")) {
            sendMemberDetails(clients.get(sender)); // Send member details to the client
        } else if (message.equals("COORDINATOR_CHECK_ACTIVE")) {
            if (sender.equals(coordinatorId)) { // ✅ Ensure only the Coordinator can send the request
                handleCoordinatorCheck(sender); // Process coordinator check
            } else {
                System.out.println("Coordinator check request rejected from non-coordinator: " + sender);
            }
        } else if (message.startsWith("BROADCAST: ")) {
            broadcastMessage(sender + ": " + message.substring(11));
        } else if (message.startsWith("PRIVATE: ")) {
            String[] parts = message.split(" ", 3);
            if (parts.length >= 3) {
                sendPrivateMessage(parts[1], sender + " (private): " + parts[2]);
            }
        }
    }
    

    public void broadcastMessage(String message) {
        if (message.trim().isEmpty()) return;
    
        String cleanMessage = message.replaceAll("[^\\x20-\\x7E]", ""); // ✅ Removes non-printable characters
    
        for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
            entry.getValue().println(cleanMessage);
            entry.getValue().flush();
        }
    }
    
    
    
    

    private void sendPrivateMessage(String recipient, String message) {
        if (clients.containsKey(recipient)) {
            clients.get(recipient).println(message);
            clients.get(recipient).flush();
        } else {
            System.out.println("Error: Private message recipient not found: " + recipient);
        }
    }

    public synchronized void addClient(String id, Socket socket, PrintWriter out) {
        clients.put(id, out);
        String clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        clientDetails.put(id, clientAddress);
    
        if (coordinatorId == null) {
            coordinatorId = id;
            out.println("CONNECTED_SUCCESSFULLY " + id);
            out.println("You are coordinator.");
        } else {
            out.println("CONNECTED_SUCCESSFULLY " + id);
            out.println("The coordinator is: " + coordinatorId);
        }
        out.flush();
    
        // 🚀 If no more clients, close the server and release the port
        if (clients.isEmpty()) {
            System.out.println("All clients have disconnected. Stopping server...");
            stopServer();
        }
    }

    
    public synchronized String getCoordinatorId() {
        return coordinatorId;
    }
    

    public synchronized boolean isIdAvailable(String newId) {
        boolean available = !clients.containsKey(newId);
        return available;
    }


    public synchronized void sendMemberDetails(PrintWriter requesterOut) {
        if (clients.isEmpty()) return; // Prevent sending if no clients exist
    
        StringBuilder memberList = new StringBuilder("\n---- Group Members ----\n");
        
        for (Map.Entry<String, String> entry : clientDetails.entrySet()) {
            String id = entry.getKey();
            String address = entry.getValue();
            boolean isCoordinator = id.equals(coordinatorId);
    
            memberList.append("ID: ").append(id)
                      .append(", IP:Port: ").append(address)
                      .append(isCoordinator ? " (Coordinator)" : "")
                      .append("\n");
        }
        memberList.append("------------------------");
    
        // Send only once per request to avoid duplicates
        requesterOut.println(memberList.toString());
        requesterOut.flush();
    }
    

    public synchronized void notifyMemberLeft(String id, boolean promptOnLastClientExit) {
        if (!clients.containsKey(id)) return;
        
    
        clients.remove(id);
        clientDetails.remove(id);
        broadcastMessage("The user " + id + " has left the chat.");
        
        if (id.equals(coordinatorId)) {
            if (!clients.isEmpty()) {
                coordinatorId = clients.keySet().iterator().next();
                PrintWriter newCoordinator = clients.get(coordinatorId);
                
                if (newCoordinator != null) {
                    newCoordinator.println("You are coordinator."); // ✅ Notify only the new coordinator
                    newCoordinator.flush();
                }
                
                // ✅ Notify everyone except the new coordinator
                for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                    String clientId = entry.getKey();
                    PrintWriter clientOut = entry.getValue();
                    
                    if (!clientId.equals(coordinatorId)) { // ✅ Skip sending message to the new Coordinator
                        clientOut.println("New Coordinator assigned: " + coordinatorId);
                        clientOut.flush();
                    }
                }
            } else {
                coordinatorId = null;
            }
        }
        if (clients.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(
                    null,
                    "All clients have disconnected. Do you want to stop the server?",
                    "Shutdown Server?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (choice == JOptionPane.YES_OPTION) {
                    stopServer();
                } else {
                    System.out.println("Server remains active waiting for new connections...");
                }
            });
        }
    }
     

    private void listenForCommands() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String command = reader.readLine();
                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Shutting down server...");
                    notifyClientsShutdown();
                    stopServer();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error listening for commands: " + e.getMessage());
        }
    }

    private void notifyClientsShutdown() {
        for (PrintWriter client : clients.values()) {
            client.println("Server is shutting down. You will be disconnected.");
        }
        try {
            Thread.sleep(2000); // Give clients time to receive the message
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void stopServer() {
        try {
            running = false;
            notifyClientsShutdown();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            if (pool != null && !pool.isShutdown()) {
                pool.shutdownNow(); // ✅ Stop all active threads
            }

            System.out.println("🚀 Server has stopped.");
            System.exit(0); // ✅ Ensures the Java process exits completely
        } catch (IOException e) {
            System.out.println("Error while stopping server: " + e.getMessage());
        }
    }


    public synchronized boolean isCoordinator(String id) {
        return id != null && id.equals(coordinatorId);
    } 
    
}
