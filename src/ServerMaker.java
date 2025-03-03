import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerMaker {
    private static Server server;
    public static void main(String[] args) throws IOException {
        makeServer();
        server.downloadJar();
        server.generateStartScript();
        server.generateServerProperties();
        server.generateEula();
    }

    private static void makeServer() throws IOException {
        String serverConfigContent = "";
        try {
            serverConfigContent = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + File.separator + "ServerMakerConfig.json")));
        } catch (Exception e) {
            System.out.println("Server config not found. Please include a server config in the same folder as this file called ServerMakerConfig.json");
            System.exit(1);
        }
        JSONObject serverConfigFile = new JSONObject(serverConfigContent);
        String serverType = serverConfigFile.getString("server-type");
        switch(serverType) {
            case "vanilla": {
                server = new Vanilla(serverConfigFile, serverType);
            }
            case "paper": {

            }
        }
    }
}
