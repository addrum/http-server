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

public class RequestHandler1 extends Thread {

    private final Socket conn;
    private final Thread thread;
    private final String serverLocation;
    private InputStream is;
    private OutputStream os;
    private Path path;
    private String uri;
    private String message;
    private String version;
    private int status;

    public RequestHandler1(Socket conn) {
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
                    case "GET":
                        // calls get method which passes in the created uri and output stream
                        GET(uri, os);
                        conn.close();
                        break;
                    case "PUT":
                        // calls put method which passes in the created uri and input stream
                        PUT(uri, is);
                        conn.close();
                        break;
                    default:
                        // returns bad request response if no cases are met
                        //createResponse(400);
                        conn.close();
                        break;
                }
            } catch (MessageFormatException mfe) {
                //createResponse(500);
                System.out.println("MFE - RequestHandler.java");
                conn.close();
            }
        } catch (IOException ioe) {
            //createResponse(500);
            System.out.println("IOE - RequestHandler.java in handleRequests");
            try {
                conn.close();
            } catch (IOException ex) {
                System.out.println("IOE - RequestHandler.java in handleRequests");
            }
        }
        try {
            thread.join();
        } catch (InterruptedException IE) {
            System.out.println("IE - Couldn't join thread.");
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
            } catch (IOException ioe) {
                System.out.println("IOE - RequestHandler.java in GET");
            }
            createResponse(200);
        } else {
            createResponse(404);
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
                ResponseMessage resMsg = new ResponseMessage(201);
                os.write(("\r\n" + resMsg.toString()).getBytes());
                os.write(("\r\nClosing connection in 3 seconds...").getBytes());
                sleepThread(3000);
                fos.close();              
            } catch (IOException ioe) {
                System.out.println("IOE - RequestHandler.java in PUT");
            }
        } else {
            //createResponse(400);
        }
    }

    // creates response based on the status number parameter
    public void createResponse(int code) {
        switch (code) {
            case 1:
                status = 200;
                message = "OK";
            case 2:
                status = 201;
                message = "Created";
            case 3:
                status = 304;
                message = "Not Modified";
            case 4:
                status = 400;
                message = "Bad Request";
            case 5:
                status = 403;
                message = "Forbidden";
            case 6:
                status = 404;
                message = "Not Found";
            case 7:
                status = 500;
                message = "Internal Server Error";
            case 8:
                status = 501;
                message = "Not Implemented";
            case 9:
                status = 505;
                message = "HTTP Version Not Supported";
        }
        ResponseMessage resMsg = new ResponseMessage(status);
        try {
            os.write(("\r\n" + resMsg.toString()).getBytes());
            os.write(("\r\nClosing connection in 3 seconds...").getBytes());
            sleepThread(3000);
        } catch (IOException ex) {
            System.out.println("IOE - Could not write response.");
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
