import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Server {

    private static BufferedReader inputBufferedReader;

    private static int portnumber = 8095;

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(portnumber)) {
            System.out.println("Waiting for connection on port: " + portnumber);
            while (true){
                try (Socket clientSocket = serverSocket.accept()){
                    System.out.println("Connected to : " + portnumber);
                    handleClient(clientSocket);
                }
            }
        }catch (IOException io){
            io.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        System.out.println("Got New Client on socket" + clientSocket.toString());
        inputBufferedReader = new BufferedReader((new InputStreamReader(clientSocket.getInputStream())));
        StringBuilder requestBuilder = new StringBuilder();
        String line;

        while (!(line = inputBufferedReader.readLine()).isBlank()){
            requestBuilder.append(line + "\r\n"); //reconstruct the entire request
        }
        String request = requestBuilder.toString();
        String[] requestsLines = request.split("\r\n");
        String[] requestLine = requestsLines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine[1];
        String version = requestLine[2];
        String host = requestsLines[1].split(" ")[1];

        List <String> headers = new ArrayList<>();
        for (int i = 2; i < requestsLines.length; i++){
            String header = requestsLines[i];
            headers.add(header);
        }

        String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
        clientSocket.toString(), method, path,version,host,headers.toString());
        System.out.println(accessLog);

        System.out.println(request);

        Path filePath = getFilePath(path);
        System.out.println(Files.exists(filePath));

        File file = new File(String.valueOf(filePath));
        System.out.println(file.getPath());

        if (file.exists()) {
            String contentType = guessContentType(filePath);
            sendResponse(clientSocket, "200 ok", contentType, Files.readAllBytes(filePath));
        } else {
            sendResponse(clientSocket, "404 Not Found", "text/html", Files.readAllBytes(Path.of("src/main/www/404.html")));
        }

    }

    /**
     * Send an HTTP response to a client connected via a socket.
     *
     */

    private static void sendResponse(Socket client, String status,String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 " + status + "\r\n").getBytes(StandardCharsets.UTF_8));
        System.out.println(status);
        if (contentType.equals("text/html")){
            clientOutput.write(("Content-Type: " + contentType + "; charset=UTF-8" + "\r\n").getBytes(StandardCharsets.UTF_8));
        } else {
            clientOutput.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
            clientOutput.write(("Content-Length: " + content.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            clientOutput.write("\r\n".getBytes(StandardCharsets.UTF_8));
            clientOutput.write(content);
            clientOutput.flush();
            client.close();
        }



    /**
     *
     * ensures that the method returns the correct file path
     *
     */

    private static Path getFilePath(String path){
        if ("/".equals(path)){
            path = "www/index.html";
        }

        return Paths.get("src/main/www", path);
    }

    
    /**
     * ensure better accuracy in determining the content type, especially for web server applications
     *
     */
    private static String guessContentType(Path filePath) throws IOException {
        // Default content type
        String contentType = "application/octet-stream"; // Default to binary data

        // Mapping of file extensions to content types
        Map<String, String> extensionToContentType = new HashMap<>();
        extensionToContentType.put(".html", "text/html");
        extensionToContentType.put(".png", "image/png");

        // Get the file extension
        String fileExtension = getFileExtension(filePath);

        // Check if the extension is mapped to a content type
        if (extensionToContentType.containsKey(fileExtension)) {
            contentType = extensionToContentType.get(fileExtension);
        }

        return contentType;
    }

    /**
     *
     *  Help method to extract file extension from the path after the dot
     */

    private static String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex).toLowerCase();
        }
        return "";
    }

}
