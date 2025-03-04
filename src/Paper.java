import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Paper extends Server{
    private int paperBuild;
    private String jarName;
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
        serverDownloadURL = "https://api.papermc.io/v2/projects/paper/versions/" + serverVersion + "/builds/" + paperBuild + "/downloads/" + jarName;
        if(!fileExists(jarName, false)) {
            FileUtils.copyURLToFile(new URL(serverDownloadURL), new File(jarName));
            System.out.println("Server jar downloaded.");
        } else {
            System.out.println("Server jar already found.");
        }
    }

    private int getBuild(String serverVersion) throws MalformedURLException {
        JSONObject paperBuildsJSON = new JSONObject();
        paperBuildsJSON = getJSONFromURL(new URL("https://api.papermc.io/v2/projects/paper/versions/" + serverVersion + "/builds"));
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
            System.out.println("Paper build not found for this version. Aborting...");
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

    private String getLatestVersion() {
        JSONObject paperVersionsJSON = new JSONObject();
        try {
            paperVersionsJSON = getJSONFromURL(new URL("https://api.papermc.io/v2/projects/paper"));
        } catch (MalformedURLException e) {
            System.out.println("Paper versions JSON not found. Is the PaperMC API down?");
            System.exit(1);
        }
        assert paperVersionsJSON != null;
        JSONArray paperVersionsList = paperVersionsJSON.getJSONArray("versions");
        return paperVersionsList.getString(paperVersionsList.length() - 1);
    }

    public void generateStartScript() {
        if(serverOS.equals("linux") || serverOS.equals("macos") || serverOS.equals("mac")) {
            try {
                FileWriter linuxStartScript = new FileWriter("start.sh");
                if(javaFlags.equals("")) {
                    linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " -jar " + jarName + " nogui");
                    linuxStartScript.close();
                } else {
                    linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " -jar " + jarName + " nogui");
                    linuxStartScript.close();
                }
                File startSH = new File("start.sh");
                if(!startSH.setExecutable(true)) {
                    System.out.println("Unable to make start script executable. Please do this yourself by running chmod +x start.sh in a terminal.");
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } else if(serverOS.equals("windows")) {
            try {
                FileWriter windowsStartScript = new FileWriter("start.bat");
                if(javaFlags.equals("")) {
                    windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " -jar " + jarName + " nogui");
                    windowsStartScript.close();
                } else {
                    windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " -jar " + jarName + " nogui");
                    windowsStartScript.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }
}
