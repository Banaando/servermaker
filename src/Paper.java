import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Paper extends Server{
    protected int paperBuild;
    public Paper(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        if(serverVersion.equals("latest")) {
            serverVersion = getLatestVersion();
        }
        paperBuild = getBuild(serverVersion);
        jarName = serverType + "-" + serverVersion + "-" + paperBuild + ".jar";
    }

    public void downloadJar() throws IOException {
        System.out.println("Downloading server jar...");
        serverDownloadURL = "https://api.papermc.io/v2/projects/" + serverType + "/versions/" + serverVersion + "/builds/" + paperBuild + "/downloads/" + jarName;
        if(!fileExists(jarName, false)) {
            FileUtils.copyURLToFile(new URL(serverDownloadURL), new File(jarName));
            System.out.println("Server jar downloaded.");
        } else {
            System.out.println("Server jar already found.");
        }
    }

    int getBuild(String serverVersion) throws MalformedURLException {
        JSONObject paperBuildsJSON;
        paperBuildsJSON = getJSONFromURL(new URL("https://api.papermc.io/v2/projects/" + serverType + "/versions/" + serverVersion + "/builds"));
        assert paperBuildsJSON != null;
        JSONArray paperBuildsArray = new JSONArray(paperBuildsJSON.getJSONArray("builds"));
        for(int i = paperBuildsArray.length() - 1; i > 0; i--) {
            if(paperBuildsArray.getJSONObject(i).getString("channel").equals("default")) {
                return paperBuildsArray.getJSONObject(i).getInt("build");
            }
        }
        System.out.println("Stable build not found for this version. Searching for experimental build...");
        try {
            return paperBuildsArray.getJSONObject(paperBuildsArray.length() - 1).getInt("build");
        } catch (Exception e) {
            System.out.println("Build not found for this version. Aborting...");
            System.exit(1);
        }
        return -1;
    }

    public String getServerJSONURL(String serverType) throws MalformedURLException {
        return "";
    }

    public String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        return "";
    }

    String getLatestVersion() {
        JSONObject paperVersionsJSON = new JSONObject();
        try {
            paperVersionsJSON = getJSONFromURL(new URL("https://api.papermc.io/v2/projects/" + serverType));
        } catch (MalformedURLException e) {
            System.out.println("Versions JSON not found. Is the PaperMC API down?");
            System.exit(1);
        }
        assert paperVersionsJSON != null;
        JSONArray paperVersionsList = paperVersionsJSON.getJSONArray("versions");
        return paperVersionsList.getString(paperVersionsList.length() - 1);
    }

}
