import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerMaker {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = makeServer();
        server.downloadJar();
        server.generateStartScript();
        server.generateServerProperties();
        server.generateEula();
    }

    private static Server makeServer() throws IOException, InterruptedException {
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
                return new Vanilla(serverConfigFile, serverType);
            }
            case "paper": {
                return new Paper(serverConfigFile, serverType);
            }
            case "folia": {
                return new Folia(serverConfigFile, serverType);
            }
            case "purpur": {
                return new Purpur(serverConfigFile, serverType);
            }
            case "fabric": {
                return new Fabric(serverConfigFile, serverType);
            }
            case "quilt": {
                return new Quilt(serverConfigFile, serverType);
            }
        }
        return null;
    }
}
