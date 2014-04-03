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
    private InputStream is;
    private OutputStream os;
    private Path path;
    private String uri;
    private String message;
    private int status;
    private boolean logging;

    public RequestHandler(Socket conn, String rootDir, boolean logging) {
        thread = new Thread();
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
        r();
    }

    //@Override
    public void r() {
        // get the output stream for sending data to the client
        try {
            while (!conn.isClosed()) {
                os = conn.getOutputStream();
                is = conn.getInputStream();
                // creates a request message with parses from the input stream
                try {
                    RequestMessage reqMsg = RequestMessage.parse(is);
                    // gets the uri from the parsed request message
                    uri = reqMsg.getURI();
                    if (reqMsg.getMethod().equals("PUT")) {
                        // calls put method which passes in the created uri and input stream
                        PUT(uri, is);
                    } if (reqMsg.getMethod().equals("GET")) {
                        GET(reqMsg, os);
                    } if (reqMsg.getMethod().equals("HEAD")) {
                        HEAD(uri, os);
                    }
                } catch (MessageFormatException mfe) {
                    createResponse(400);
                    System.out.println("Incorrect message format.");
                }
                conn.close();
            }
        } catch (IOException ioe) {
            createResponse(500);
            System.out.println("ioe");
        }
        try {
            thread.join();
        } catch (InterruptedException IE) {
            createResponse(500);
            System.out.println("Couldn't join thread.");
        }
    }

    public void PUT(String uri, InputStream is) {
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
                        createResponse(400);
                        logSomething("PUT", uri, "400");
                    } else if (b <= 1024 * 1024) {
                        fos.write(b);
                    }
                }
                createResponse(201);
                logSomething("PUT", uri, "201");
                fos.close();
                System.out.println("HTTP/1.1 201 Created");
            } catch (IOException ioe) {
                System.out.println("Couldn't write message body to file.");
                createResponse(400);
                logSomething("PUT", uri, "400");
            }
        } else {
            createResponse(403);
            logSomething("PUT", uri, "403");
        }
    }

    public void POST(String uri, InputStream is) {
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
                createResponse(201);
                logSomething("POST", uri, "201");
                fos.close();
                System.out.println("HTTP/1.1 201 Created");
            } catch (IOException ioe) {
                System.out.println("Couldn't write message body to file.");
                createResponse(400);
                logSomething("POST", uri, "400");
            }
        } else {
            createResponse(403);
        }
    }

    public void GET(RequestMessage rq, OutputStream os) {
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
                    createResponse(200);
                    logSomething("GET", rq.getURI(), "200");
                    while (true) {
                        int b = fis.read();
                        if (b == -1) {
                            break;
                        }
                        os.write(b);
                    }
                } else {
                    long modifiedMili;
                    Date modifiedDate = new SimpleDateFormat("MM/dd/yyyy").parse(rq.getHeaderFieldValue("If-Modified-Since"));
                    modifiedMili = modifiedDate.getTime();
                    if (modifiedMili <= file.lastModified()) {
                        createResponse(200);
                        logSomething("GET", rq.getURI(), "200");
                        while (true) {
                            int b = fis.read();
                            if (b == -1) {
                                break;
                            }
                            os.write(b);
                        }
                    } else {
                        createResponse(304);
                        logSomething("GET", rq.getURI(), "304");
                    }
                }

                String contentType = Files.probeContentType(path);
                int contentLength = is.read();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                os.write(("\r\nContent-Type: " + contentType).getBytes());
                os.write(("\r\nContent-Length: " + contentLength).getBytes());
                os.write(("\r\nLast Modified: " + sdf.format(file.lastModified()).toString()).getBytes());
            } catch (ParseException | IOException ioe) {
                System.out.println("Couldn't write file to output stream.");
                createResponse(400);
                logSomething("GET", rq.getURI(), "400");
            }
        } else if (!file.exists() && file.isDirectory()) {
            file.listFiles();
        } else {
            createResponse(404);
            logSomething("GET", rq.getURI(), "404");
        }
    }

    public void HEAD(String uri, OutputStream os) {
        // creates an absolute path based on the uri relative to the server location
        path = Paths.get(rootDir, uri);
        path.toAbsolutePath();
        System.out.println(path.toString());
        File file = new File(path.toString());
        if (file.exists()) {
            createResponse(200);
            logSomething("HEAD", uri, "200");
        } else {
            createResponse(404);
            logSomething("HEAD", uri, "404");
        }
    }

    // creates response based on the status number parameter
    public void createResponse(int code) {
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

    // makes the thread sleep based on the time parameter to allow user
    // to read the messages
    public void sleepThread(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
            createResponse(500);
            System.out.println("Could not sleep thread properly");
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
