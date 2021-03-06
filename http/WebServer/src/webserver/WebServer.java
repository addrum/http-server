package webserver;

import java.io.IOException;
import java.net.*;

public class WebServer {

    private int port;
    private String rootDir;
    private boolean logging;

    public WebServer(int port, String rootDir, boolean logging) {
        this.port = port;
        this.rootDir = rootDir;
        this.logging = logging;
    }

    public void start() throws IOException {
        // create a server socket 
        ServerSocket serverSock = new ServerSocket(port);
        System.out.println("Server socket created. Port open: " + port);
        while (true) {
            // listen for a new connection on the server socket 
            Socket conn = serverSock.accept();  
            // create a new client with a new thread
            RequestHandler client = new RequestHandler(conn, rootDir, logging);
            client.start();
            System.out.println("Connection accepted."); 
        }
    }

    public static void main(String[] args) throws IOException {
        String usage = "Usage: java webserver.WebServer <port-number> <root-dir> (\"0\" | \"1\")";
        if (args.length != 3) {
            throw new Error(usage);
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new Error(usage + "\n" + "<port-number> must be an integer");
        }
        String rootDir = args[1];
        boolean logging;
        if (args[2].equals("0")) {
            logging = false;
        } else if (args[2].equals("1")) {
            logging = true;
        } else {
            throw new Error(usage);
        }
        WebServer server = new WebServer(port, rootDir, logging);
        server.start();
    }
}
