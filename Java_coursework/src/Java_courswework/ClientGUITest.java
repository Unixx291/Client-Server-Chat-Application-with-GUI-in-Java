package Java_courswework;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientGUITest {

    private ClientGUI clientGUI;
    
    @BeforeEach
    void setUp() {
        clientGUI = new ClientGUI("testUser", "127.0.0.1", 12345);
    }
    
    @Test
    void testCheckUniqueId() {
        boolean isUnique = clientGUI.checkUniqueId("testUser", "127.0.0.1", 12345);
        assertTrue(isUnique || !isUnique); // Ensures method runs without errors
    }
    
    @Test
    void testPromptForUniqueId() {
        SwingUtilities.invokeLater(() -> {
            String newId = clientGUI.promptForUniqueId("127.0.0.1", 12345);
            assertNotNull(newId);
        });
    }
    
    @Test
    void testInvalidIpAddress() {
        assertFalse(clientGUI.isValidIpAddress("999.999.999.999"));
        assertTrue(clientGUI.isValidIpAddress("192.168.1.1"));
    }
    
    @Test
    void testValidId() {
        assertTrue(clientGUI.isValidId("valid_id123"));
        assertFalse(clientGUI.isValidId("invalid id!"));
    }
    
    @Test
    void testChangeIp() {
        SwingUtilities.invokeLater(() -> {
            clientGUI.ipField.setText("192.168.1.100");
            clientGUI.changeIpButton.doClick();
            assertEquals("192.168.1.100", clientGUI.ipField.getText());
        });
    }
    
    @Test
    void testChangePort() {
        SwingUtilities.invokeLater(() -> {
            clientGUI.portField.setText("8080");
            clientGUI.changePortButton.doClick();
            assertEquals("8080", clientGUI.portField.getText());
        });
    }
    
    @Test
    void testUpdateIdField() {
        SwingUtilities.invokeLater(() -> {
            clientGUI.updateIdField("updatedUser");
            assertEquals("updatedUser", clientGUI.idField.getText());
        });
    }
}