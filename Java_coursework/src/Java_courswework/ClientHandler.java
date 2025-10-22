package Java_courswework;

import java.io.*;
import java.net.Socket;


class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private BufferedReader in;
    private PrintWriter out;
    private String id;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
    
            String initialRequest = in.readLine();
            if (initialRequest == null) {
                closeConnection();
                return;
            }
    
            if (initialRequest.startsWith("CHECK_ID ")) {
                String idToCheck = initialRequest.substring("CHECK_ID ".length()).trim();
                synchronized (server) {
                    out.println(server.isIdAvailable(idToCheck) ? "ID_AVAILABLE" : "ID_TAKEN");
                    out.flush();
                }
                closeConnection();
                return;
            }
    
            String requestedId = initialRequest.trim();
    
            synchronized (server) {
                if (!server.isIdAvailable(requestedId)) {
                    out.println("ID_ALREADY_IN_USE");
                    out.flush();
                    closeConnection();
                    return;
                }
                id = requestedId; // ✅ ONLY assign after validation
                server.addClient(id, socket, out);
            }
    
    
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("EXIT") || message.equals("EXIT_WINDOW_CLOSE")) { // ✅ Handle manual disconnect
                    System.out.println("Client " + id + " disconnected gracefully.");
                    break; // ✅ Stop reading messages
                }
                handleClientMessage(message);
            }

    
        } catch (IOException e) {
        	System.out.println("Client " + id + " disconnected unexpectedly.");
        } finally {
            if (id != null) {server.notifyMemberLeft(id, false);
            }
            closeConnection();
        }
    }

    private void handleClientMessage(String message) {
        if (message.equals("EXIT")) {  
            server.notifyMemberLeft(id, false);  // ❌ No prompt for server shutdown
            closeConnection();
            return;
        } 
        
        if (message.equals("EXIT_WINDOW_CLOSE")) {  
            server.notifyMemberLeft(id, true);  // ✅ Prompt for server shutdown if last client
            closeConnection();
            return;
        }
    
        if (message.equals("REQUEST_MEMBER_DETAILS")) {
            server.sendMemberDetails(out);
        } else if (message.equals("COORDINATOR_CHECK_ACTIVE")) {
            if (id.equals(server.coordinatorId)) {
                server.handleCoordinatorCheck(id);
            } else {
            }
        } else if (message.startsWith("CHECK_ID ")) { 
            String newId = message.split(" ", 2)[1].trim();
            out.println(server.isIdAvailable(newId) ? "ID_AVAILABLE" : "ID_TAKEN");
        } else if (message.startsWith("CHANGE_ID ")) { 
            String[] parts = message.split(" ", 2);
            if (parts.length != 2) {
                out.println("ERROR_INVALID_REQUEST");
                return;
            }

            String newId = parts[1].trim();
            System.out.println("Received CHANGE_ID request: " + id + " -> " + newId);
    
            synchronized (server) {
                if (server.changeClientId(id, newId, out)) {
                    id = newId;
                    out.println("ID change successful: " + newId);
                } else {
                    out.println("The ID " + newId + " is already taken!" );
                }
            }
        } else {
            server.handleMessage(id, message);
        }
    }
    
    

    private void closeConnection() {
        try {
            if (out != null) {
                out.println("EXIT"); // ✅ Notify the server before closing
                out.flush();
                out.close(); // ✅ Close PrintWriter
            }
            
            if (in != null) {
                in.close(); // ✅ Close BufferedReader
            }
    
            if (socket != null && !socket.isClosed()) {
                socket.close(); // ✅ Close the socket properly
            }
    
        } catch (IOException e) {
            System.out.println("ERROR: Failed to close connection for client " + id + ": " + e.getMessage());
        }
    }
}    
