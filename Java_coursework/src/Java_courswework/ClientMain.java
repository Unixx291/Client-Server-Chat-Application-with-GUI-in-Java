package Java_courswework;

import javax.swing.*;
import java.util.regex.Pattern;

public class ClientMain {
    public static void main(String[] args) {
        // ✅ Default values
        final String defaultIp = "127.0.0.1";
        final int defaultPort = 5000;

        final String[] id = new String[1];
        final String[] serverIp = new String[1];
        final int[] serverPort = new int[1];
        serverPort[0] = defaultPort; // Default port
        
        // ✅ Keep prompting for a valid ID
        while (true) {
            id[0] = JOptionPane.showInputDialog("Enter your ID:");
            if (id[0] == null) { // User pressed cancel
                JOptionPane.showMessageDialog(null, "ID entry canceled. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            id[0] = id[0].trim();
            if (!id[0].isEmpty()) break; // Exit loop if ID is valid
            JOptionPane.showMessageDialog(null, "ID cannot be empty! Please enter a valid ID.");
        }

        // ✅ Keep prompting for a valid IP address
        while (true) {
            serverIp[0] = JOptionPane.showInputDialog("Enter Server IP Address (Default: " + defaultIp + "):");
            if (serverIp[0] == null) { // User pressed cancel
                JOptionPane.showMessageDialog(null, "Server selection canceled. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            serverIp[0] = serverIp[0].trim();
            if (serverIp[0].isEmpty()) {
                serverIp[0] = defaultIp; // Use default IP if empty
                break;
                
            }
            if (isValidIpAddress(serverIp[0])) break; // Exit loop if IP is valid
            JOptionPane.showMessageDialog(null, "Invalid IP address! Please enter a valid IP.");
        }

        // ✅ Keep prompting for a valid port
        while (true) {
            String portInput = JOptionPane.showInputDialog("Enter Server Port (Default: " + defaultPort + "):");
            if (portInput == null) { // User pressed cancel
                JOptionPane.showMessageDialog(null, "Port selection canceled. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            portInput = portInput.trim();
            if (portInput.isEmpty()) {
                serverPort[0] = defaultPort;
                break;
            }
            try {
                int parsedPort = Integer.parseInt(portInput);
                if (parsedPort >= 1024 && parsedPort <= 65535) {
                    serverPort[0] = parsedPort;

                    break;
                }
            } catch (NumberFormatException ignored) {}
            JOptionPane.showMessageDialog(null, "Invalid port! Please enter a valid port (1024-65535).");
        }

        // ✅ Start Client GUI with selected values
        JOptionPane.showMessageDialog(null, "🚀 Connecting to Server: " + serverIp[0] + ":" + serverPort[0]);
        SwingUtilities.invokeLater(() -> new ClientGUI(id[0], serverIp[0], serverPort[0]));
    }

    // ✅ Validate IP address using regex
    private static boolean isValidIpAddress(String ip) {
        String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return Pattern.matches(ipRegex, ip);
    }
}
