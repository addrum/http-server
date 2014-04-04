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
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class RequestHandler extends Thread {

    private final Socket conn;
    private Thread thread;
    private final String rootDir;
    private final String version;
    private static Logger logger;
    private Path path;
    private String uri;
    private String message;
    private int status;
    private boolean logging;

    public RequestHandler(Socket conn, String rootDir, boolean logging) {
        this.conn = conn;
        this.rootDir = "." + File.separator + rootDir + File.separator;
        this.logging = logging;

        logger = Logger.getLogger(RequestHandler.class.getName());
        if (logging) {
            File fileLog = new File(this.rootDir + "webserverIN2011.log");
            try {
                if (!fileLog.exists()) {
                    fileLog.createNewFile();
                }
                else{
                    fileLog.renameTo(new File(this.rootDir+"webserverIN2011-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()).toString()+".log.backup"));
                    fileLog = new File(this.rootDir+"webserverIN2011.log");   
                    fileLog.createNewFile();
                }
                FileHandler logFile = new FileHandler(fileLog.toString(), true);
                logFile.setFormatter(new Formatter() {
                    public String format(LogRecord rec) {
                        StringBuffer buf = new StringBuffer(1000);
                        buf.append(rec.getSequenceNumber());
                        buf.append(":");
                        buf.append(rec.getMessage());
                        buf.append("\r\n");
                        return buf.toString();
                    }
                });
                logger.addHandler(logFile);
            } catch (IOException | SecurityException ex) {
                Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        message = "";
        version = "HTTP/1.1 ";

    }

    @Override
    public void run() {
        // get the output stream for sending data to the client
        try {
            while (!conn.isClosed()) {
                OutputStream os = conn.getOutputStream();
                InputStream is = conn.getInputStream();
                // creates a request message with parses from the input stream
                try {
                    RequestMessage reqMsg = RequestMessage.parse(is);
                    // gets the uri from the parsed request message
                    uri = reqMsg.getURI();
                    if (reqMsg.getMethod().equals("PUT")) {
                        PUT(uri, is, os);
                    }
                    if (reqMsg.getMethod().equals("GET")) {
                        GET(reqMsg, os, is);
                    }
                    if (reqMsg.getMethod().equals("HEAD")) {
                        HEAD(uri, os, is);
                    }
                } catch (MessageFormatException mfe) {
                    createResponse(400, os);
                    System.out.println("Incorrect message format.");
                }
                conn.close();
            }
        } catch (IOException ioe) {
            //createResponse(500, os);
            System.out.println("ioe");
        }
        try {
            thread.join();
        } catch (InterruptedException IE) {
            //createResponse(500, os);
            System.out.println("Couldn't join thread.");
        }
    }

    public void PUT(String uri, InputStream is, OutputStream os) {
        path = Paths.get(rootDir, uri);
        path.toAbsolutePath();
        File file = new File(path.toString());
        if (!file.exists() && !file.isDirectory()) {
            try {
                file.getParentFile().mkdirs();
                // creates a file and writes message body to the file4
                Files.createFile(path);
                OutputStream fos = Files.newOutputStream(path);
                while (true) {
                    int b = is.read();
                    if (b == -1) {
                        break;
                    }
                    if (b > 1024 * 1024) {
                        createResponse(400, os);
                        logSomething("PUT", uri, "400");
                    } else if (b <= 1024 * 1024) {
                        fos.write(b);
                    }
                }
                createResponse(201, os);
                logSomething("PUT", uri, "201");
                fos.close();
                System.out.println("HTTP/1.1 201 Created");
            } catch (IOException ioe) {
                System.out.println("Couldn't write message body to file.");
                createResponse(400, os);
                logSomething("PUT", uri, "400");
            }
        } else {
            createResponse(403, os);
            logSomething("PUT", uri, "403");
        }
    }

    public void POST(String uri, InputStream is, OutputStream os) {
        path = Paths.get(rootDir, uri);
        path.toAbsolutePath();
        File file = new File(path.toString());
        if (!file.isDirectory()) {
            try {
                if (file.exists()) {
                    file.delete();
                }
                file.getParentFile().mkdirs();
                // creates a file and writes message body to the file4
                Files.createFile(path);
                OutputStream fos = Files.newOutputStream(path);
                while (true) {
                    int b = is.read();
                    if (b == -1) {
                        break;
                    }
                    fos.write(b);
                }
                createResponse(201, os);
                logSomething("POST", uri, "201");
                fos.close();
                System.out.println("HTTP/1.1 201 Created");
            } catch (IOException ioe) {
                System.out.println("Couldn't write message body to file.");
                createResponse(400, os);
                logSomething("POST", uri, "400");
            }
        } else {
            createResponse(403, os);
        }
    }

    public void GET(RequestMessage rq, OutputStream os, InputStream is) {
        // creates an absolute path based on the uri relative to the server location
        path = Paths.get(rootDir, rq.getURI());
        path.toAbsolutePath();
        System.out.println(path.toString());
        File file = new File(path.toString());
        if (file.exists() && !file.isDirectory()) {
            try {
                // writes the file body to the output stream
                InputStream fis = Files.newInputStream(path);
                if (rq.getHeaderFieldValue("If-Modified-Since") == null) {
                    createResponse(200, os);
                    writeGetResponse(path, is, os, file);
                    logSomething("GET", rq.getURI(), "200");
                    while (true) {
                        int b = fis.read();
                        if (b == -1) {
                            break;
                        }
                        os.write(b);
                    }
                } else {
                    // compares header value if-modified-since to the file's
                    // last modified date and returns appropriate response
                    long modifiedMili;
                    Date modifiedDate = new SimpleDateFormat("MM/dd/yyyy").parse(rq.getHeaderFieldValue("If-Modified-Since"));
                    modifiedMili = modifiedDate.getTime();
                    if (modifiedMili <= file.lastModified()) {
                        createResponse(200, os);
                        writeGetResponse(path, is, os, file);
                        logSomething("GET", rq.getURI(), "200");
                        while (true) {
                            int b = fis.read();
                            if (b == -1) {
                                break;
                            }
                            os.write(b);
                        }
                    } else {
                        createResponse(304, os);
                        writeGetResponse(path, is, os, file);
                        logSomething("GET", rq.getURI(), "304");
                    }
                }              
            } catch (IOException ioe) {
                createResponse(500, os);
                logSomething("GET", rq.getURI(), "500");
                System.out.println("Couldn't write file to output stream.");
            } catch (ParseException ex) {
                createResponse(500, os);
                logSomething("GET", rq.getURI(), "500");
                System.out.println("Couldn't parse date.");
            }
        } else {
            createResponse(404, os);
            logSomething("GET", rq.getURI(), "404");
        }
    }

    public void writeGetResponse(Path path, InputStream is, OutputStream os, File file) {
        try {
            String contentType = Files.probeContentType(path);
            long contentLength = file.length();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            os.write(("\r\nContent-Type: " + contentType).getBytes());
            os.write(("\r\nContent-Length: " + contentLength).getBytes());
            os.write(("\r\nLast Modified: " + sdf.format(file.lastModified()).toString() + "\r\n").getBytes());
        } catch (IOException ex) {
            createResponse(500, os);
            System.out.println("Couldn't write get response.");
        }
    }

    public void HEAD(String uri, OutputStream os, InputStream is) {
        // creates an absolute path based on the uri relative to the server location
        path = Paths.get(rootDir, uri);
        path.toAbsolutePath();
        System.out.println(path.toString());
        File file = new File(path.toString());
        if (file.exists()) {
            createResponse(200, os);
            writeGetResponse(path, is, os, file);
            logSomething("HEAD", uri, "200");
        } else {
            createResponse(404, os);            
            logSomething("HEAD", uri, "404");
        }
    }

    // creates response based on the status number parameter
    public void createResponse(int code, OutputStream os) {
        status = code;
        try {
            ResponseMessage resMsg = new ResponseMessage(status);
            os.write(("\r\n" + resMsg.toString()).getBytes());
        } catch (IOException ex) {
            try {
                ResponseMessage resMsg = new ResponseMessage(500);
                os.write(("\r\n" + resMsg.toString()).getBytes());
                System.out.println("Could not write response.");
            } catch (IOException ex1) {
                Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    public void logSomething(String logMethod, String logUri, String logResponse) {
        if (logging) {
            String logRecord = logMethod + ":" + logUri + ":" + logResponse;
            logger.log(new LogRecord(Level.INFO, logRecord));
            for (int i = 0; i < logger.getHandlers().length; i++) {
                logger.getHandlers()[i].close();
            }
        }
    }

    @Override
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
            System.out.println(thread.getId());
        }
    }

}
