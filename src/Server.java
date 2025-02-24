import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {
    String downloadURL;
    String version;
    String type;
    URL versionManifestURL;
    JSONObject versionManifestJSON;
    String latest;
    String latestSnapshot;
    JSONArray versions;
    JSONObject config;
    JSONObject versionJSON;
    URL versionURL;
    JSONObject versionJSON2;

    public Server() throws IOException {
        versionManifestURL = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        versionManifestJSON = getJson(versionManifestURL);
        versions = versionManifestJSON.getJSONArray("versions");
        String configContent = new String(Files.readAllBytes(Paths.get("/Users/eric/IdeaProjects/servermaker/ServerMakerConfig.json")));
        config = new JSONObject(configContent);
        type = config.getString("server-type");
        version = config.getString("server-version");
        latest = versionManifestJSON.getJSONObject("latest").getString("release");
        latestSnapshot = versionManifestJSON.getJSONObject("latest").getString("snapshot");

        if(version.equals("latest")) {
            version = latest;
        } else if(version.equals("latest-snapshot")) {
            version = latestSnapshot;
        }

        for(int i = 0; i < versions.length(); i++) {
            versionJSON = versions.getJSONObject(i);
            if(versionJSON.getString("id").equals(version)) {
                versionURL = new URL(versionJSON.getString("url"));
                break;
            }
        }

        versionJSON2 = getJson(versionURL);
        downloadURL = versionJSON2.getJSONObject("downloads").getJSONObject("server").getString("url");

    }

    public void downloadJar() throws IOException {
        FileUtils.copyURLToFile(new URL(downloadURL), new File(version + ".json"));
    }

    public static JSONObject getJson(URL url) {
        try {
            String json = IOUtils.toString(url, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
