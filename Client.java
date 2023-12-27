import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
    	// checks for the correct command line arguments
        if (args.length != 2) {
            System.err.println("Usage: java Client <host name> <port number>");
            System.exit(1);
        }

        // gets host name and port number
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
        		// creates socket connection to server
                Socket echoSocket = new Socket(hostName, portNumber);
        		// output stream to send messages to server 
                PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
        		// receive messages from server
                BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
               // read user input from the console
        		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
        ) {
            Thread receiveThread = new Thread(() -> {
                try {
                    String serverMsg;
                    // read messages from server and display them
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println("Server: " + serverMsg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            String userInput;
            // read input from user and send it to the console
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}
