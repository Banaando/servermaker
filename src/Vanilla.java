import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Vanilla extends Server{
    protected String serverJSONURL;
    protected String serverDownloadURL;

    public Vanilla(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        serverJSONURL = getServerJSONURL(serverType);
        serverDownloadURL = getServerDownloadURL(serverJSONURL);
    }

    protected String getServerJSONURL(String serverType) throws MalformedURLException {
        URL versionManifestURL = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        JSONObject versionManifestJSON = getJSONFromURL(versionManifestURL);
        JSONArray versionManifestArray = versionManifestJSON.getJSONArray("versions");
        for(int i = 0; i < versionManifestArray.length(); i++) {
            if(versionManifestArray.getJSONObject(i).getString("id").equals(serverVersion)) {
                return versionManifestArray.getJSONObject(i).getString("url");
            }
        }
        return "Server JSON not found";
    }

    protected String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        JSONObject serverJSON = getJSONFromURL(new URL(serverJSONURL));
        return serverJSON.getJSONObject("downloads").getJSONObject("server").getString("url");
    }

    public void downloadJar() throws IOException {
        System.out.println("Downloading server jar...");
        if(!fileExists(serverType + "-" + serverVersion + ".jar", false)) {
            FileUtils.copyURLToFile(new URL(serverDownloadURL), new File(serverType + "-" + serverVersion + ".jar"));
            System.out.println("Server jar downloaded.");
        } else {
            System.out.println("Server jar already found.");
        }
    }
}
