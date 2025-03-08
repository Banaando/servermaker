import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class Server {
    // Config variables retrieved by getServerConfig
    protected boolean agreedToEula;
    protected String serverType;
    protected String serverVersion;
    protected String javaPath;
    protected String serverMemory;
    protected String javaFlags;
    protected String serverOS;
    protected String jarName;
    protected JSONObject serverProperties;
    protected JSONObject serverConfigFile;

    // Retrieved by getServerJSONURL
    protected String serverJSONURL;
    protected String serverDownloadURL;

    public Server(JSONObject serverConfigFile, String serverType) throws IOException {
        this.serverConfigFile = serverConfigFile;
        this.serverType = serverType;
        getServerConfig();
    }

    public Server() throws IOException {
        getServerConfig();
    }

    abstract void downloadJar() throws IOException;

    public static JSONObject getJSONFromURL(URL url) {
        try {
            HttpURLConnection JSONURLConnection = (HttpURLConnection) url.openConnection();
            JSONURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            String json = IOUtils.toString(JSONURLConnection.getInputStream(), StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception e) {
            System.out.println("Link to JSON not found. Perhaps the server doesn't have a build available for the version you selected?");
            e.printStackTrace();
            return null;
        }
    }

    public static JSONArray getJSONArrayFromURL(URL url) {
        try {
            HttpURLConnection JSONURLConnection = (HttpURLConnection) url.openConnection();
            JSONURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            String json = IOUtils.toString(JSONURLConnection.getInputStream(), StandardCharsets.UTF_8);
            return new JSONArray(json);
        } catch (Exception e) {
            System.out.println("Link to JSON not found. Perhaps the server doesn't have a build available for the version you selected?");
            e.printStackTrace();
            return null;
        }
    }

    public void generateStartScript() {
        if(serverOS.equals("linux") || serverOS.equals("macos") || serverOS.equals("mac")) {
            try {
                FileWriter linuxStartScript = new FileWriter("start.sh");
                if(javaFlags.isEmpty()) {
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
                if(javaFlags.isEmpty()) {
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

    private void getServerConfig() throws IOException {
        agreedToEula = serverConfigFile.getBoolean("agreed-to-eula");
        serverVersion = getServerVersion(serverType);
        javaPath = serverConfigFile.getString("java-path");
        serverMemory = serverConfigFile.getString("memory");
        javaFlags = serverConfigFile.getString("java-flags");
        serverOS = serverConfigFile.getString("server-os");
        if(serverOS.equals("auto")) {
            if(System.getProperty("os.name").contains("Window")) {
                serverOS = "windows";
            } else {
                serverOS = "linux";
            }
        }
        serverProperties = serverConfigFile.getJSONObject("server-properties");
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

    abstract String getServerJSONURL(String serverType) throws MalformedURLException;

    abstract String getServerDownloadURL(String serverJSONURL) throws MalformedURLException;

    public void generateEula() {
        try {
            FileWriter eula = new FileWriter("eula.txt");
            eula.write("eula=" + agreedToEula);
            eula.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void generateServerProperties() throws IOException {
        if(fileExists("server.properties", true)) { // If server.properties already exists, replace properties with the ones in config.
            HashMap<String, Object> propertiesHashMap = (HashMap<String, Object>) serverProperties.toMap();
            ArrayList<String> propertyNames = new ArrayList<String>(propertiesHashMap.keySet());
            ArrayList<Object> propertyValues = new ArrayList<Object>(propertiesHashMap.values());
            PrintWriter propertyWriter = new PrintWriter(new FileWriter("server.properties", true));
            for(int i = 0; i < serverProperties.length(); i++) {
                String regexExpression = propertyNames.get(i) + "=.*";
                String propertyValue = propertyValues.get(i).toString();
                if(fileContainsRegex("server.properties", regexExpression)) {
                    replaceText("server.properties", regexExpression, propertyNames.get(i) + "=" + propertyValue);
                } else {
                    propertyWriter.print("\n" + propertyNames.get(i) + "=" + propertyValue);
                }
            }
            propertyWriter.close();
        } else { // If it doesn't exist, make a new server.properties.
            try {
                FileWriter propertyWriter = new FileWriter("server.properties");
                HashMap<String, Object> propertiesHashMap = (HashMap<String, Object>) serverProperties.toMap();
                ArrayList<String> propertyNames = new ArrayList<String>(propertiesHashMap.keySet());
                ArrayList<Object> propertyValues = new ArrayList<Object>(propertiesHashMap.values());
                for(int i = 0; i < serverProperties.length(); i++) {
                    String propertyValue = propertyValues.get(i).toString();
                    propertyWriter.write(propertyNames.get(i) + "=" + propertyValue + "\n");
                }
                propertyWriter.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }


    }

    public static void replaceText(String file, String regexExpression, String replacement) throws IOException {
        Path path = Paths.get(file);
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(regexExpression, replacement);
        Files.write(path, content.getBytes(charset));
    }

    public static boolean fileExists(String testFileName, boolean isTextFile) throws IOException {
        if(Files.exists(FileSystems.getDefault().getPath(testFileName))) {
            if(!isTextFile) {
                return true;
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(testFileName));
                int lines = 0;
                while (reader.readLine() != null) {
                    lines++;
                }
                reader.close();
                return lines > 0;
            }
        } else {
            return false;
        }
    }

    public static boolean fileContainsRegex(String filePath, String regex) {
        Pattern pattern = Pattern.compile(regex);
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            return lines.anyMatch(line -> pattern.matcher(line).find());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
