import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Purpur extends Server{
    protected String purpurBuild;
    public Purpur(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        if(serverVersion.equals("latest")) {
            serverVersion = getLatestVersion();
        }
        purpurBuild = getBuild(serverVersion);
        jarName = serverType + "-" + serverVersion + "-" + purpurBuild + ".jar";
    }

    private String getBuild(String serverVersion) throws MalformedURLException {
        JSONObject paperBuildsJSON = new JSONObject();
        paperBuildsJSON = getJSONFromURL(new URL("https://api.purpurmc.org/v2/purpur/" + serverVersion));
        assert paperBuildsJSON != null;
        JSONArray paperBuildsArray = new JSONArray(paperBuildsJSON.getJSONObject("builds").getJSONArray("all"));
        try {
            return paperBuildsArray.getString(paperBuildsArray.length() - 1);
        } catch (Exception e) {
            System.out.println("Build not found for this version. Aborting...");
            System.exit(1);
        }
        return "oops";
    }

    private String getLatestVersion() {
        JSONObject purpurVersionsJSON = new JSONObject();
        try {
            purpurVersionsJSON = getJSONFromURL(new URL("https://api.purpurmc.org/v2/purpur/"));
        } catch (MalformedURLException e) {
            System.out.println("Versions JSON not found. Is the PurpurMC API down?");
            System.exit(1);
        }
        assert purpurVersionsJSON != null;
        JSONArray purpurVersionsList = purpurVersionsJSON.getJSONArray("versions");
        return purpurVersionsList.getString(purpurVersionsList.length() - 1);
    }

    public void downloadJar() throws IOException {
        System.out.println("Downloading server jar...");
        serverDownloadURL = "https://api.purpurmc.org/v2/purpur/" + serverVersion + "/" + purpurBuild + "/download";

        if (!fileExists(jarName, false)) {
            URL url = new URL(serverDownloadURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Add User-Agent to bypass 403

            try (InputStream in = connection.getInputStream()) {
                FileUtils.copyInputStreamToFile(in, new File(jarName));
                System.out.println("Server jar downloaded.");
            } catch (IOException e) {
                System.out.println("Failed to download server jar. Check if the URL is correct or if authentication is needed.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Server jar already found.");
        }
    }

    String getServerJSONURL(String serverType) throws MalformedURLException {
        return "";
    }

    String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        return "";
    }
}
