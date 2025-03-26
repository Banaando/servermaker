import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;

public class Forge extends Server {
    String forgeVersionHTMLLink;
    String forgeVersion;
    String forgeInstallerName;
    public Forge(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);

        // Get a link to the page that has the installer downloads for the MC version the user wants
        if(serverVersion.equals("latest")) {
            serverVersion = getLatestServerVersion();
        }
        forgeVersionHTMLLink = "https://files.minecraftforge.net/net/minecraftforge/forge/index_" + serverVersion + ".html";
        jarName = serverType + "-" + serverVersion;

        // Get the Forge version the user wants
        forgeVersion = getForgeVersion(serverConfigFile.getJSONObject("(neo)forge-config").getString("loader-version"));
        forgeInstallerName = "forge-" + serverVersion + "-" + forgeVersion + "-installer.jar";

        // Download the installer
        try {
            downloadFile("https://maven.minecraftforge.net/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "/forge-" + serverVersion + "-" + forgeVersion + "-installer.jar", forgeInstallerName);
        } catch (Exception e) {
            try {
                downloadFile("https://maven.minecraftforge.net/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "-" + serverVersion + "/forge-" + serverVersion + "-" + forgeVersion + "-" + serverVersion + "-installer.jar", forgeInstallerName);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getForgeVersion(String forgeVersion) throws IOException {
        Document forgeVersionPage = Jsoup.connect(forgeVersionHTMLLink)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "*")
                .get();
        String versionType = forgeVersion.equalsIgnoreCase("latest") ? "Latest" : "Recommended";
        Element versionElement = forgeVersionPage.selectFirst("meta[property=og:description]");

        if (versionElement != null) {
            String description = versionElement.attr("content");
            for (String part : description.split("\n")) {
                if (part.contains(versionType)) {
                    return part.split(": ")[1]; // Extracts version number
                }
            }
        }
        return forgeVersion;
    }

    void downloadJar() throws IOException, InterruptedException {
        File nullDevice = new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null");
        ProcessBuilder proc = new ProcessBuilder(javaPath, "-jar", forgeInstallerName, "--installServer");
        proc.directory(new File(System.getProperty("user.dir")));
        System.out.println("Installing Forge server... (This may take a while)");
        proc.redirectOutput(ProcessBuilder.Redirect.to(nullDevice));
        proc.redirectError(ProcessBuilder.Redirect.to(nullDevice));
        Process runProc = proc.start();
        runProc.waitFor();
        System.out.println("Forge server installed!");
        FileUtils.delete(new File(forgeInstallerName));
        try {
            FileUtils.delete(new File("run.sh"));
        } catch (Exception ignored) {}
        try {
            FileUtils.delete(new File("run.bat"));
        } catch (Exception ignored) {}
        try {
            FileUtils.delete(new File("user_jvm_args.txt"));
        } catch (Exception ignored) {}
        try {
            FileUtils.delete(new File("forge-" + serverVersion + "-" + forgeVersion + "-installer.jar.log"));
        } catch (Exception ignored) {}
        try {
            FileUtils.delete(new File("README.txt"));
        } catch (Exception ignored) {}

        File currentDir = new File("."); // Get current working directory
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().contains("universal") && file.getName().endsWith(".jar")) {
                    File newFile = new File(currentDir, "forge-" + serverVersion + "-" + forgeVersion + ".jar");
                    if (newFile.exists()) {
                        return;
                    }
                    file.renameTo(newFile);
                }
            }
        } else {
            System.out.println("Failed to list files in the directory.");
        }
    }

    public void generateStartScript() throws IOException {
        if(serverOS.equals("windows")) {
            if(fileExists("libraries\\net\\minecraftforge\\forge\\" + serverVersion + "-" + forgeVersion + "\\win_args.txt", true)) {
                try {
                    FileWriter windowsStartScript = new FileWriter("start.bat");
                    if(javaFlags.isEmpty()) {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " @libraries/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "/win_args.txt nogui");
                        windowsStartScript.close();
                    } else {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " @libraries/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "/win_args.txt nogui");
                        windowsStartScript.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    FileWriter windowsStartScript = new FileWriter("start.bat");
                    if(javaFlags.isEmpty()) {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " -jar forge-" + serverVersion + "-" + forgeVersion + ".jar nogui");
                        windowsStartScript.close();
                    } else {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " -jar forge-" + serverVersion + "-" + forgeVersion + ".jar nogui");
                        windowsStartScript.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if(fileExists("libraries/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "/unix_args.txt", true)) {
                try {
                    FileWriter linuxStartScript = new FileWriter("start.sh");
                    if(javaFlags.isEmpty()) {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " @libraries/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "/unix_args.txt nogui");
                        linuxStartScript.close();
                    } else {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " @libraries/net/minecraftforge/forge/" + serverVersion + "-" + forgeVersion + "/unix_args.txt nogui");
                        linuxStartScript.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    FileWriter linuxStartScript = new FileWriter("start.sh");
                    if(javaFlags.isEmpty()) {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " -jar forge-" + serverVersion + "-" + forgeVersion + ".jar nogui");
                        linuxStartScript.close();
                    } else {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " -jar forge-" + serverVersion + "-" + forgeVersion + ".jar nogui");
                        linuxStartScript.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String getServerJSONURL(String serverType) throws MalformedURLException {
        return "";
    }

    String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        return "";
    }

    String getLatestServerVersion() throws IOException {
        Document ForgePage = Jsoup.connect("https://files.minecraftforge.net/net/minecraftforge/forge/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "*")
                .get();
        Element activeVersionElement = ForgePage.selectFirst(".sidebar-nav ul.section-content ul.nav-collapsible li.elem-active");
        if (activeVersionElement != null) {
            return activeVersionElement.text();
        }
        return "latest-version-not-found";
    }
}
