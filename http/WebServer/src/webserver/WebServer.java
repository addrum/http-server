package webserver;

import in2011.http.RequestMessage;
import in2011.http.ResponseMessage;
import in2011.http.StatusCodes;
import in2011.http.EmptyMessageException;
import in2011.http.MessageFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.*;
import java.util.Date;
import org.apache.http.client.utils.DateUtils;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        System.out.println("Server socket created.");
        while (true) {
            // listen for a new connection on the server socket 
            Socket conn = serverSock.accept();
            System.out.println("Connection accepted.");
            // get the output stream for sending data to the client 
            OutputStream os = conn.getOutputStream();
            // send a response 
            ResponseMessage respMsg = new ResponseMessage(200);
            respMsg.write(os);
            os.write("Server is running.".getBytes());
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while (!(inputLine = in.readLine()).equals("")) {
                    System.out.println(inputLine);
                    if (inputLine.contains("PUT")) {
                        System.out.println("HTTP/1.1 200 OK");
                        try {
                            RequestMessage reqMsg = RequestMessage.parse(conn.getInputStream());
                        } catch (MessageFormatException ex) {
                            System.out.println("HTTP/1.1 400 Bad Request");
                        }
                    } else if (inputLine.contains("GET")) {
                        System.out.println("GET request received!");
                    }
                }
            }
            //conn.close();
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
