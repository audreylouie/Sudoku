import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
  public static List<PrintWriter> clientOutputStreams = new ArrayList<>();
  private static Map<Socket, String> clientBoards = new HashMap<>();

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: java Server <port number>");
      System.exit(1);
    }

    int portNumber = Integer.parseInt(args[0]); // Initialize port number

    try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
      System.out.println("Server started on port " + portNumber); // output once port number is declared

      // start a new Sudoku board
      Sudoku sudoku = new Sudoku(); 
      sudoku.fillValues();

      while (true) {
    	// connect client
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket);

        // to send outputs to client
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
        clientOutputStreams.add(writer);

        ClientHandler clientHandler = new ClientHandler(clientSocket, writer, sudoku);
        Thread clientThread = new Thread(clientHandler); // create thread for client handler
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println("Exception caught when trying to listen on port " + portNumber);
      System.out.println(e.getMessage());
    }
  }
  
  // send message to all clients
  public static void broadcastMessage(String message) {
    for (PrintWriter writer : clientOutputStreams) {
      writer.println(message);
      writer.flush();
    }
  }

  // update board and show it to all clients
  public static void updateClientBoard(Socket clientSocket, String updatedBoard) {
    clientBoards.put(clientSocket, updatedBoard);
    broadcastMessage("Board Updated");
    broadcastMessage(updatedBoard);
  }
  
  public static void showClientBoard(Socket clientSocket, String updatedBoard) {
	  try {
	        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
	        writer.println(updatedBoard); // send the updated board to the specific client
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
  }

}

class ClientHandler implements Runnable {
  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;
  private Sudoku sudoku;
  private String currentBoard;

  public ClientHandler(Socket socket, PrintWriter writer, Sudoku sudoku) {
    this.clientSocket = socket;
    this.out = writer;
    this.sudoku = sudoku;

    try {
      this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      this.currentBoard = sudoku.getSudokuString();
      out.println("enter: ");
      out.println("1. \"show\"");
      out.println("2. \"update <row> <col> <num>\"");
      out.println("3. \"exit\"");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run() {
    try {
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
    	 
        if (inputLine.equals("show")) {  // when client enters "show"
          out.println("Current Board:");
          String updatedBoard = sudoku.getSudokuString();
          Server.showClientBoard(clientSocket, updatedBoard); // send the current board to the client
          
        } else if (inputLine.startsWith("update")) { // when client wants to update
          String[] updateParams = inputLine.split(" ");
          if (updateParams.length == 4) {
            try {
              // getting values for row, col, num
              int i = Integer.parseInt(updateParams[1]);
              int j = Integer.parseInt(updateParams[2]);
              int num = Integer.parseInt(updateParams[3]);

              boolean success = sudoku.enterNumber(i, j, num); // checking validity of update
              if (success) {
                String updatedBoard = sudoku.getSudokuString();
                Server.updateClientBoard(clientSocket, updatedBoard);
                currentBoard = updatedBoard; // update the currentBoard
              } else {
                out.println("Update failed. Try again."); // when update is invalid
              }

              if (sudoku.isBoardFull()) { // when the game is complete
                Server.broadcastMessage("Game Over: Board is Full");
                Server.broadcastMessage(sudoku.getSudokuString());
                Server.broadcastMessage("Thanks For Playing!");
                disconnectClient();  // disconnects all clients when game ends
              }
  
            } catch (NumberFormatException e) {
              out.println("Invalid numeric input. Usage: update row col num");
            }
          } else {
            out.println("Invalid update format. Usage: update row col num");
          }   
        } 
        else if (inputLine.equals("exit")) { // when the player wants to exit during the middle of the game
            out.println("Exiting game. Goodbye!");
            in.close();
            out.close();
            clientSocket.close();
            Server.clientOutputStreams.remove(out);
            System.out.println("Client disconnected: " + clientSocket);
            return;
        }else {
          out.println("Invalid command. Use 'show' or 'update row col num' or 'exit'.");
        }
      }
    } catch (IOException e) {
        e.printStackTrace();
    } 
}

 // removes all clients from server
private void disconnectClient() {
    try {
        in.close();
        out.close();
        clientSocket.close();
        Server.clientOutputStreams.remove(out);
        System.out.println("Client disconnected: " + clientSocket);
        System.exit(0);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}