import java.io.IOException;

public class ServerMaker {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.downloadJar();
        server.generateStartScript();
        server.generateServerProperties();
        server.generateEula();
    }
}
