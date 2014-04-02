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

    private Socket conn;
    private InputStream is;
    private OutputStream os;
    private Thread thread;
    private Path path;

    public RequestHandler(Socket conn) {
        thread = new Thread();
        this.conn = conn;
        handleRequests();
    }

    public void handleRequests() {
        thread.start();
        // get the output stream for sending data to the client 
        try {
            os = conn.getOutputStream();
            is = conn.getInputStream();
            os.write(("Server is running.\r\n").getBytes());
            try {
                RequestMessage reqMsg = RequestMessage.parse(is);
                if (reqMsg.getMethod().equals("GET")) {
                    GET(reqMsg.getURI(), os);
                    ResponseMessage resMsg = new ResponseMessage(200);
                    os.write(("\r\n" + resMsg.toString()).getBytes());
                } else {
                    ResponseMessage resMsg = new ResponseMessage(400);
                    os.write(("\r\n" + resMsg.toString()).getBytes());
                }
            } catch (MessageFormatException mfe) {
                System.out.println("MFE - RequestHandler.java");
            }
            //conn.close();
        } catch (IOException ioe) {
            System.out.println("IOE - RequestHandler.java in handleREquests");
        }
    }

    public void GET(String uri, OutputStream os) {
        path = Paths.get("./public/", uri);
        path.toAbsolutePath();
        System.out.println(path.toString());
        try {
            InputStream fis = Files.newInputStream(path);
            while (true) {
                int b = fis.read();
                if (b == - 1) {
                    break;
                }
                os.write(b);
            }           
        } catch (IOException ioe) {
            System.out.println("IOE - RequestHandler.java in GET");
        }
    }

}
