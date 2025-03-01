import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {
    // Config variables retrieved by getServerConfig
    private boolean agreedToEula;
    private String serverType;
    private String serverVersion;
    private String javaPath;
    private String serverMemory;
    private int serverPort;
    private String serverMOTD;
    private String javaFlags;
    private String serverOS;
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
        if(!fileExists(serverType + "-" + serverVersion + ".jar", false)) {
            FileUtils.copyURLToFile(new URL(serverDownloadURL), new File(serverType + "-" + serverVersion + ".jar"));
            System.out.println("Server jar downloaded.");
        } else {
            System.out.println("Server jar already found.");
        }
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
        if(serverOS.equals("linux") || serverOS.equals("macos") || serverOS.equals("mac")) {
            try {
                FileWriter linuxStartScript = new FileWriter("start.sh");
                if(javaFlags.equals("")) {
                    linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " -jar " + serverType + "-" + serverVersion + ".jar nogui");
                    linuxStartScript.close();
                } else {
                    linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " -jar " + serverType + "-" + serverVersion + ".jar nogui");
                    linuxStartScript.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } else if(serverOS.equals("windows")) {
            try {
                FileWriter windowsStartScript = new FileWriter("start.bat");
                if(javaFlags.equals("")) {
                    windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " -jar " + serverType + "-" + serverVersion + ".jar nogui");
                    windowsStartScript.close();
                } else {
                    windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " -jar " + serverType + "-" + serverVersion + ".jar nogui");
                    windowsStartScript.close();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }


    }

    private void getServerConfig() throws IOException {
        String serverConfigContent = new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + File.separator + "src" + File.separator + "ServerMakerConfig.json")));
        serverConfigFile = new JSONObject(serverConfigContent);
        agreedToEula = serverConfigFile.getBoolean("agreed-to-eula");
        serverType = serverConfigFile.getString("server-type");
        serverVersion = getServerVersion(serverType);
        javaPath = serverConfigFile.getString("java-path");
        serverMemory = serverConfigFile.getString("memory");
        serverPort = serverConfigFile.getInt("port");
        serverMOTD = serverConfigFile.getString("motd");
        javaFlags = serverConfigFile.getString("java-flags");
        serverOS = serverConfigFile.getString("server-os");
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
        if(fileExists("server.properties", true)) {
            replaceText("server.properties", "server-port=.*", "server-port=" + serverPort);
            replaceText("server.properties", "motd=.*", "motd=" + serverMOTD);
        } else {
            try {
                FileWriter eula = new FileWriter("server.properties");
                eula.write("server-port=" + serverPort + "\n" + "motd=" + serverMOTD);
                eula.close();
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
}
