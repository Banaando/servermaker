import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {
    // Config variables retrieved by getServerConfig
    private boolean agreedToEula;
    private String serverType;
    private String serverVersion;
    private int javaVersion;
    private String serverMemory;
    private int serverPort;
    private String serverMOTD;
    private String javaFlags;
    private JSONObject serverConfigFile;

    // Retrieved by getServerJSONURL
    private String serverJSONURL;
    private String serverDownloadURL;

    public Server() throws IOException {
        getServerConfig();
        serverJSONURL = getServerJSONURL(serverType);
        serverDownloadURL = getServerDownloadURL(serverJSONURL);
    }

    public void downloadJar() throws IOException {
        System.out.println("Downloading server jar...");
        FileUtils.copyURLToFile(new URL(serverDownloadURL), new File(serverVersion + ".jar"));
    }

    public static JSONObject getJSONFromURL(URL url) {
        try {
            String json = IOUtils.toString(url, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void generateStartScript() {
        try {
            FileWriter linuxStartScript = new FileWriter("start.sh");
            linuxStartScript.write("you still have laundry on our bed.");
            linuxStartScript.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getServerConfig() throws IOException {
        String serverConfigContent = new String(Files.readAllBytes(Paths.get("/Users/eric/IdeaProjects/servermaker/src/ServerMakerConfig.json")));
        serverConfigFile = new JSONObject(serverConfigContent);
        agreedToEula = serverConfigFile.getBoolean("agreed-to-eula");
        serverType = serverConfigFile.getString("server-type");
        serverVersion = getServerVersion(serverType);
        javaVersion = serverConfigFile.getInt("java-version");
        serverMemory = serverConfigFile.getString("memory");
        serverPort = serverConfigFile.getInt("port");
        serverMOTD = serverConfigFile.getString("motd");
        javaFlags = serverConfigFile.getString("java-flags");
    }

    private String getServerVersion(String serverType) throws MalformedURLException {
        if(serverConfigFile.getString("server-version").equals("latest")) {
            switch(serverType) {
                case "vanilla": {
                    URL versionManifestURL = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
                    JSONObject versionManifestJSON = getJSONFromURL(versionManifestURL);
                    return versionManifestJSON.getJSONObject("latest").getString("release");
                }
                case "paper": {

                }
            }
        } else if(serverConfigFile.getString("server-version").equals("latest-snapshot")) {
            switch(serverConfigFile.getString("server-type")) {
                case "vanilla": {
                    URL versionManifestURL = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
                    JSONObject versionManifestJSON = getJSONFromURL(versionManifestURL);
                    return versionManifestJSON.getJSONObject("latest").getString("snapshot");
                }
                case "paper": {

                }
            }
        }
        return serverConfigFile.getString("server-version");
    }

    private String getServerJSONURL(String serverType) throws MalformedURLException {
        switch(serverType) {
            case "vanilla": {
                URL versionManifestURL = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
                JSONObject versionManifestJSON = getJSONFromURL(versionManifestURL);
                JSONArray versionManifestArray = versionManifestJSON.getJSONArray("versions");
                for(int i = 0; i < versionManifestArray.length(); i++) {
                    if(versionManifestArray.getJSONObject(i).getString("id").equals(serverVersion)) {
                        return versionManifestArray.getJSONObject(i).getString("url");
                    }
                }
            }
            case "paper": {

            }
        }
        return "Server JSON not found";
    }

    private String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        JSONObject serverJSON = getJSONFromURL(new URL(serverJSONURL));
        return serverJSON.getJSONObject("downloads").getJSONObject("server").getString("url");

    }
}
