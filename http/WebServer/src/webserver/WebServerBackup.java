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

public class WebServerBackup {

    private int port;
    private String rootDir;
    private boolean logging;

    public WebServerBackup(int port, String rootDir, boolean logging) {
        this.port = port;
        this.rootDir = rootDir;
        this.logging = logging;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("port " + port + " opened");
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Someone has connected");
                try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
                    String s = in.readLine();
                    while (true) {
                        String misc = in.readLine();
                        if (misc.isEmpty()) {
                            break;
                        }
                    }
                    if (!s.startsWith("GET") || s.length() < 14 || !(s.endsWith("HTTP/1.0") || s.endsWith("HTTP/1.1"))) {
                        // bad request
                        System.out.println(construct_http_header(400) + "Your browser sent a request that "
                                + "this server could not understand.");
                    }
                    out.write("HTTP/1.0" + construct_http_header(200));
                    out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
                    out.write("Server: Apache/0.8.4\r\n");
                    out.write("Content-Type: text/html\r\n");
                    out.write("Content-Length: 59\r\n");
                    out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
                    out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
                    out.write("\r\n");

                    System.err.println("Your connection has been terminated.");
                }
            }
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
        WebServerBackup server = new WebServerBackup(port, rootDir, logging);
        server.start();
    }

    private String construct_http_header(int return_code) {
        String s = "HTTP/1.0 ";
        switch (return_code) {
            case 200:
                s = s + "200 OK";
                break;
            case 400:
                s = s + "400 Bad Request";
                break;
            case 403:
                s = s + "403 Forbidden";
                break;
            case 404:
                s = s + "404 Not Found";
                break;
            case 500:
                s = s + "500 Internal Server Error";
                break;
            case 501:
                s = s + "501 Not Implemented";
                break;
        }

        s = s + "\r\n"; //other header fields,
        s = s + "Connection: close\r\n"; //we can't handle persistent connections
        s = s + "Server: SimpleHTTPtutorial v0\r\n"; //server name

        return s;
    }

    public void putRequest() throws Exception {
        URL url = new URL("http://www.example.com/resource");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        OutputStreamWriter out = new OutputStreamWriter(
                httpCon.getOutputStream());
        out.write("Resource content");
        out.close();
        httpCon.getInputStream();
    }

}
