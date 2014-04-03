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
import java.nio.file.*;
import java.util.Date;
import org.apache.http.client.utils.DateUtils;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler extends Thread {

    private final Socket conn;
    private final Thread thread;
    private final String serverLocation;
    private final String version;
    private InputStream is;
    private OutputStream os;
    private Path path;
    private String uri;
    private String message;
    private int status;

    public RequestHandler(Socket conn) {
        thread = new Thread();
        this.conn = conn;
        serverLocation = "." + File.separator + "data" + File.separator + "public" + File.separator;
        message = "";
        version = "HTTP/1.1 ";
        handleRequests();
    }

    private void handleRequests() {
        thread.start();
        // get the output stream for sending data to the client 
        try {
            os = conn.getOutputStream();
            is = conn.getInputStream();
            try {
                // creates a request message with parses from the input stream
                RequestMessage reqMsg = RequestMessage.parse(is);
                // gets the uri from the parsed request message
                uri = reqMsg.getURI();
                switch (reqMsg.getMethod()) {
                    case "PUT":
                        // calls put method which passes in the created uri and input stream
                        PUT(uri, is);
                        conn.close();
                        break;
                    case "GET":
                        // calls get method which passes in the created uri and output stream
                        GET(uri, os);
                        conn.close();
                        break;                    
                    case "HEAD":
                        // calls head method which passies in the creatrd uri and output stream
                        HEAD(uri, os);
                        conn.close();
                    default:
                        // returns bad request response if no cases are met
                        createResponse(400);
                        conn.close();
                        break;
                }
            } catch (MessageFormatException mfe) {
                createResponse(400);
                System.out.println("Incorrect message format.");
                conn.close();
            }
        } catch (IOException ioe) {
            createResponse(500);
            System.out.println("Couldn't get streams.");
            try {
                conn.close();
            } catch (IOException ex) {
                createResponse(500);
                System.out.println("Couldn't close connection.");
            }
        }
        try {
            thread.join();
        } catch (InterruptedException IE) {
            System.out.println("Couldn't join thread.");
        }
    }

    public void PUT(String uri, InputStream is) {
        path = Paths.get(serverLocation, uri);
        path.toAbsolutePath();
        File file = new File(path.toString());
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                // creates a file and writes message body to the file
                Files.createFile(path);
                OutputStream fos = Files.newOutputStream(path);
                while (true) {
                    int b = is.read();
                    if (b == -1) {
                        break;
                    }
                    fos.write(b);
                }
                fos.close();
                createResponse(201);
                System.out.println("HTTP/1.1 201 Created");
            } catch (IOException ioe) {
                System.out.println("Couldn't write message body to file.");
                createResponse(400);
            }
        } else {
            createResponse(403);
        }
    }

    public void GET(String uri, OutputStream os) {
        // creates an absolute path based on the uri relative to the server location
        path = Paths.get(serverLocation, uri);
        path.toAbsolutePath();
        System.out.println(path.toString());
        File file = new File(path.toString());
        if (file.exists() && !file.isDirectory()) {
            try {
                // writes the file body to the output stream
                InputStream fis = Files.newInputStream(path);
                while (true) {
                    int b = fis.read();
                    if (b == -1) {
                        break;
                    }
                    os.write(b);
                }
                createResponse(200);
            } catch (IOException ioe) {
                System.out.println("Couldn't write file to output stream.");
                createResponse(400);
            }
        } else {
            createResponse(404);
        }
    }

    public void HEAD(String uri, OutputStream os) {
        // creates an absolute path based on the uri relative to the server location
        path = Paths.get(serverLocation, uri);
        path.toAbsolutePath();
        System.out.println(path.toString());
        File file = new File(path.toString());
        if (file.exists() && !file.isDirectory()) {
            createResponse(200);
        } else {
            createResponse(404);
        }
    }

    // creates response based on the status number parameter
    public void createResponse(int code) {
        status = code;
        try {
            ResponseMessage resMsg = new ResponseMessage(status);
            os.write(("\r\n" + resMsg.toString()).getBytes());
            sleepThread(1000);
        } catch (IOException ex) {
            System.out.println("Could not write response.");
        }
    }

    // makes the thread sleep based on the time parameter to allow user
    // to read the messages
    public void sleepThread(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
            System.out.println("IE - Could not sleep thread properly");
        }
    }

}
