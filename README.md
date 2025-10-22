# 💬 Client-Server-Chat-Application-with-GUI-in-Java
Java client-server communication system featuring a GUI for real-time message exchange. Implements socket programming, multithreading, and message handling for efficient, concurrent data transfer between clients and server.



-----------------
### 🧭 Overview  
This project implements a **multithreaded client-server chat system** in Java, allowing multiple clients to communicate simultaneously through a centralized server. The application features a **graphical user interface (GUI)** built with Swing for real-time messaging and connection management.

---

### ⚙️ Features  
- 💻 **Client-Server Architecture:** Clients connect to a server using TCP sockets.  
- 🔁 **Multithreading:** Each client runs on a dedicated thread to handle concurrent communication.  
- 💬 **Real-Time Messaging:** Clients can send and receive messages instantly.  
- 🪟 **Graphical Interface:** User-friendly GUI built with Java Swing for chat interaction.  
- 🧪 **Unit Testing:** Includes JUnit tests for key components such as client, server, and message handling.  

---

### 🧠 Technical Details  
- **Language:** Java  
- **Core Concepts:**  
  - Socket Programming  
  - Multithreading  
  - GUI Design (Swing)  
  - Object-Oriented Programming  
- **Key Classes:**  
  - `Server.java` – Handles client connections and message broadcasting  
  - `Client.java` – Connects to the server and exchanges messages  
  - `ClientHandler.java` – Manages communication for each connected client  
  - `ClientGUI.java` – Swing interface for message input/output  
  - `ServerMain.java` / `ClientMain.java` – Entry points to run server and client  

---

### 🚀 How to Run  
1. **Compile all Java files:**
   ```bash
   javac *.java
