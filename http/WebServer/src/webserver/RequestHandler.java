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
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler extends Thread {

    private Socket conn;
    private InputStream is;
    private OutputStream os;
    private Thread thread;

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
            os.write("Server is running.".getBytes());
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while (!(inputLine = in.readLine()).equals("")) {
                    System.out.println(inputLine);
                    if (inputLine.contains("PUT")) {
                    // get uri
                        // convert to path
                        // does path to file exist?
                        // yes - does file exist?
                        // no - create file at location
                        // no - response message
                    } else if (inputLine.contains("GET")) {
                        String[] requestParam = inputLine.split(" ");
                        String path = requestParam[1];
                        try (PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {
                            File file = new File(path);
                            if (!file.exists()) {
                                ResponseMessage respMsg = new ResponseMessage(404);
                                respMsg.write(os);
                            }
                            FileReader fr = new FileReader(file);
                            try (BufferedReader bfr = new BufferedReader(fr)) {
                                String line;
                                while ((line = bfr.readLine()) != null) {
                                    out.write(line);
                                }
                            }
                            is.close();
                        } catch (FileNotFoundException fnfe) {
                            ResponseMessage respMsg = new ResponseMessage(404);
                            respMsg.write(os);
                        }
                    }
                }
            }
            conn.close();
        } catch (IOException ioe) {
            System.out.println("IOE - RequestHandler.java");
        }
    }

}
