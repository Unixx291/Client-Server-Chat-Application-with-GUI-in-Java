package Java_courswework;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerMain {
    public static void main(String[] args) {
        int defaultPort = 5000; // ✅ Default port
        int port;
        ServerSocket testSocket;
       
        
        // ✅ Keep prompting until a valid port is entered or "Cancel" is pressed
        while (true) {
            String input = JOptionPane.showInputDialog(
                null,
                "Enter port or leave blank for default (" + defaultPort + "):",
                "Server Port Selection",
                JOptionPane.QUESTION_MESSAGE
            );

            // ✅ Handle "Cancel" properly - Exit program
            if (input == null) { 
                JOptionPane.showMessageDialog(null, "Server start canceled. Exiting...");
                System.exit(0);
            }

            // ✅ If input is empty, use the default port
            if (input.trim().isEmpty()) {
                port = defaultPort;
                break;
            }

            // ✅ Validate the port number
            try {
                int parsedPort = Integer.parseInt(input.trim());
                if (parsedPort < 1024 || parsedPort > 65535) {
                    JOptionPane.showMessageDialog(null, "Port must be between 1024 and 65535. Please try again.", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                // ✅ Check if the port is available
                try {
                    testSocket = new ServerSocket(parsedPort);
                    testSocket.close();
                    port = parsedPort; // Assign the validated port
                    break; // ✅ Exit loop with valid port
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Port already in use. Please choose another port.", "Port In Use", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid input! Please enter a numeric port number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }

        // ✅ Start the server with the chosen port
        JOptionPane.showMessageDialog(null, "🚀 Starting server on port: " + port);
        Server server = new Server(port);
        server.start();
    }
}
