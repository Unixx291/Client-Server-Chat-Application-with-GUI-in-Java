package Java_courswework;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

class ClientGUI {
    private JFrame frame;
    JTextField idField;
	JTextField ipField;
	JTextField portField;
	private JTextField messageField;
	private JTextField recipientField;
    private JButton connectButton, sendButton, checkActiveButton, requestMembersButton;
    private JButton changeIdButton;
	JButton changeIpButton;
	JButton changePortButton;
    private JTextArea chatArea;
    private Client client;
    private boolean isConnected = false;
    private volatile boolean connectingInProgress = false;
    
    public ClientGUI(String id, String serverIp, int serverPort) {
        // ✅ If ID is taken, ask for a new one
        if (!checkUniqueId(id, serverIp, serverPort)) {
            id = promptForUniqueId(serverIp, serverPort);
        }
        
        frame = new JFrame("Client Connection");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Prevent default close action
        frame.setLayout(new GridBagLayout());
    
        // Add Window Listener to detect when the "X" button is clicked
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (client != null && client.isConnected()) {
                    client.disconnect(false); // ✅ window-close scenario (prompt)
                }
                frame.dispose(); // close GUI
            }
        });
        

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; frame.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; idField = new JTextField(id, 15); idField.setEditable(true); frame.add(idField, gbc);
        gbc.gridx = 2; changeIdButton = new JButton("Change ID"); frame.add(changeIdButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; frame.add(new JLabel("Server IP:"), gbc);
        gbc.gridx = 1; ipField = new JTextField(serverIp, 15); ipField.setEditable(true); frame.add(ipField, gbc);
        gbc.gridx = 2; changeIpButton = new JButton("Change IP"); frame.add(changeIpButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2; frame.add(new JLabel("Server Port:"), gbc);
        gbc.gridx = 1; portField = new JTextField(String.valueOf(serverPort), 10); portField.setEditable(true); frame.add(portField, gbc);
        gbc.gridx = 2; changePortButton = new JButton("Change Port"); frame.add(changePortButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3; connectButton = new JButton("Connect"); frame.add(connectButton, gbc);

        // Chat Area
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        chatArea = new JTextArea(15, 40);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), gbc);
        gbc.gridwidth = 1; gbc.weighty = 0;

        gbc.gridx = 0; gbc.gridy = 5; frame.add(new JLabel("Specify member for Private Message"), gbc);
        gbc.gridx = 1; recipientField = new JTextField(20); frame.add(recipientField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; frame.add(new JLabel("Message:"), gbc);
        gbc.gridx = 1; messageField = new JTextField(30); frame.add(messageField, gbc);
        gbc.gridx = 2; sendButton = new JButton("Send"); frame.add(sendButton, gbc);
        sendButton.setEnabled(false);

        gbc.gridx = 0; gbc.gridy = 7; checkActiveButton = new JButton("Manual Active Check"); frame.add(checkActiveButton, gbc);
        checkActiveButton.setEnabled(false);

        gbc.gridx = 1; requestMembersButton = new JButton("Member Details"); frame.add(requestMembersButton, gbc);
        requestMembersButton.setEnabled(false);

        frame.setVisible(true);

        connectToServer(); // ✅ Auto-connect after obtaining a valid ID
        
        connectButton.addActionListener(e -> {
            if (isConnected) {
                client.disconnect(true);  // explicit manual disconnect, no prompt
                isConnected = false;
                connectButton.setText("Connect");
                sendButton.setEnabled(false);
                requestMembersButton.setEnabled(false);
                checkActiveButton.setEnabled(false);
            } else {
                connectToServer();
            }
        });
        
        sendButton.addActionListener(e -> {
            String message = messageField.getText().trim();
            String recipient = recipientField.getText().trim();
        
            if (message.isEmpty()) {
                chatArea.append("Provide a message!\n");
                return;
            }
        
            // Disable button temporarily to prevent spam
            sendButton.setEnabled(false);
        
            new Thread(() -> {
                client.sendMessage(message, recipient);
        
                SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(true); // Re-enable button after sending
                    messageField.setText(""); // Clear input field
                });
            }).start();
        });

        
        checkActiveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            private boolean longPressTriggered = false;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                longPressTriggered = false;

                // ✅ Start a background thread to check if held for 2 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(2000); // ✅ Wait 2 seconds
                        if (checkActiveButton.getModel().isPressed()) { // ✅ If still holding after 5 sec
                            longPressTriggered = true; // ✅ Mark as long press
                            SwingUtilities.invokeLater(() -> {
                                client.toggleAutoActiveCheck();
                                if (client.isAutoActiveCheckEnabled()) {
                                    checkActiveButton.setText("Auto Active Check");
                                } else {
                                    checkActiveButton.setText("Manual Active Check");
                                }
                            });
                        }
                    } catch (InterruptedException ignored) {}
                }).start();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (!longPressTriggered) { // ✅ If released before 3 sec → Send manual Active Check
                    client.checkActiveMembers();
                }
            }
        });



        
        
        requestMembersButton.addActionListener(e -> {
            if (!isConnected) {
                chatArea.append("Please, connect first!\n");
                return;
            }
            client.requestMemberDetails(); // Ensures the request is sent
        });
        

        changeIdButton.addActionListener(e -> {
            if (!isConnected) {
                chatArea.append("Please, connect first!\n");
                return;
            }
        
            String newId = idField.getText().trim();
            if (newId.isEmpty()) {
                chatArea.append("Error: ID cannot be empty!\n");
                return;
            }
        
            // Prevent duplicate requests by disabling button while checking
            changeIdButton.setEnabled(false);
        
            new Thread(() -> {
                client.changeId(newId);
        
                // Wait briefly to ensure the server processes the change
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
        
                SwingUtilities.invokeLater(() -> changeIdButton.setEnabled(true)); // Re-enable button
            }).start();
        });
        

     // Change IP Button Action (FIXED)
        changeIpButton.addActionListener(e -> {
            if (!isConnected) {
                chatArea.append("Please, connect first!\n");
                return;
            }

            String newIp = ipField.getText().trim();
            if (newIp.isEmpty() || !isValidIpAddress(newIp)) {
                chatArea.append("Error: Invalid IP Address!\n");
                return;
            }

            boolean changed = client.changeIpAddress(newIp); // ✅ Only print if change happened
            if (changed) {
                chatArea.append("IP changed successfully! Reconnecting...\n");
            }
        });

        // Change Port Button Action (FIXED)
        changePortButton.addActionListener(e -> {
            if (!isConnected) {
                chatArea.append("Please, connect first!\n");
                return;
            }

            try {
                int newPort = Integer.parseInt(portField.getText().trim());
                if (newPort >= 1024 && newPort <= 65535) {
                    boolean changed = client.changePort(newPort); // ✅ Only print if change happened
                    if (changed) {
                        chatArea.append("Port changed successfully! Reconnecting...\n");
                    }
                } else {
                    chatArea.append("Error: Port must be between 1024 and 65535!\n");
                }
            } catch (NumberFormatException ex) {
                chatArea.append("Error: Invalid port number!\n");
            }
        });


        frame.setVisible(true);       
    }

    public String promptForUniqueId(String serverIp, int serverPort) {
        while (true) {
            String newId = JOptionPane.showInputDialog(frame, "ID already in use, please enter another:", "Duplicate ID", JOptionPane.WARNING_MESSAGE);
            if (newId == null) {
                return null;  // User canceled
            }
            newId = newId.trim();
    
            if (newId.isEmpty() || !isValidId(newId)) {
                JOptionPane.showMessageDialog(frame, "Invalid ID format!", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }
    
            // Clearly use the new static method
            if (Client.isIdAvailable(newId, serverIp, serverPort)) {
                return newId;
            } else {
                JOptionPane.showMessageDialog(frame, "This ID is already taken, try another.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void connectToServer() {
        if (isConnected || connectingInProgress) {
            chatArea.append("Connection already established or in progress...\n");
            return;
        }

        connectingInProgress = true;

        String id = idField.getText().trim();
        String serverIp = ipField.getText().trim();
        int serverPort;

        try {
            serverPort = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            chatArea.append("Error: Invalid port number!\n");
            connectingInProgress = false;
            return;
        }

        client = new Client(id, serverIp, serverPort, chatArea, this);

        new Thread(() -> {
            boolean connected = client.connect();

            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(true);
                    requestMembersButton.setEnabled(true);
                    checkActiveButton.setEnabled(true);
                    isConnected = true;
                    connectButton.setText("Disconnect");
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Connection failed: ID already in use or network issue.\n");
                    String newId = promptForUniqueId(serverIp, serverPort);
                    if (newId != null) {
                        idField.setText(newId);
                        reconnectWithNewId(newId, serverIp, serverPort);
                    } else {
                        chatArea.append("User canceled connection attempt.\n");
                    }
                });
            }
            connectingInProgress = false;  // Reset flag after the connection attempt finishes
        }).start();
    }

    private void reconnectWithNewId(String newId, String serverIp, int serverPort) {
        if (client != null && client.isConnected()) {
            client.disconnect(true); // disconnect existing connection first
        }
        idField.setText(newId);
        connectToServer();
    }
    

    boolean checkUniqueId(String id, String serverIp, int serverPort) {
        try {
            Client tempClient = new Client(id, serverIp, serverPort, new JTextArea(), null);
            return tempClient.checkUniqueId(id);
        } catch (Exception e) {
            System.out.println(" Error: Failed to check ID availability!");
            return false;
        }
    }

    boolean isValidId(String id) {
        return Pattern.matches("^[a-zA-Z0-9_]+$", id); // Allows only letters, numbers, and underscores
    }

    boolean isValidIpAddress(String ip) {
        String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return Pattern.matches(ipRegex, ip);
    }

    public void updateIdField(String newId) {
        SwingUtilities.invokeLater(() -> idField.setText(newId));
    }
    
    public void resetActiveCheckButton() {
        checkActiveButton.setText("Manual Active Check"); // ✅ Reset button text
    }
    
    public void resetConnectButton() {
        connectButton.setText("Connect"); // ✅ Change button text to "Connect"
        connectButton.setEnabled(true); // ✅ Ensure the button is enabled
    }


}
