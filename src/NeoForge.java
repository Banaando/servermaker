import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoForge extends Server {
    String neoForgeVersion;
    List<String> neoForgeVersions;
    String neoForgeInstallerName;
    public NeoForge(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        neoForgeVersion = serverConfigFile.getJSONObject("(neo)forge-config").getString("loader-version");

        if(serverVersion.equals("1.20.1")) {
            if(neoForgeVersion.equals("latest") || neoForgeVersion.equals("recommended")) {
                getVersionsList("https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml");
                alphabetizeList(neoForgeVersions);
                neoForgeVersions.remove(neoForgeVersions.size() - 1);
                neoForgeVersion = neoForgeVersions.get(neoForgeVersions.size() - 1);
            } else if(!neoForgeVersion.contains("1.20.1")) {
                neoForgeVersion = "1.20.1-" + neoForgeVersion;
            }
        } else if(neoForgeVersion.equals("latest") || neoForgeVersion.equals("recommended")) {
            getVersionsList("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml");
            alphabetizeList(neoForgeVersions);
            if(serverVersion.equals("latest")) {
                neoForgeVersion = neoForgeVersions.get(neoForgeVersions.size() - 1);
            } else {
                neoForgeVersion = getLastMatchingVersion(neoForgeVersions, serverVersion);
            }
            assert neoForgeVersion != null;
            if(neoForgeVersion.contains("beta")) {
                System.out.println("Warning: Latest NeoForge version available for this Minecraft version is in beta, if you don't want a beta version. Go to https://projects.neoforged.net/neoforged/neoforge and set loader-version equal to the latest loader available that isn't in beta.");
            }
        }

        if(neoForgeVersion.contains("1.20.1")) {
            neoForgeInstallerName = "forge-" + neoForgeVersion + "-installer.jar";
        } else {
            neoForgeInstallerName = "neoforge-" + neoForgeVersion + "-installer.jar";
        }

        downloadInstaller(neoForgeVersion);
    }

    public static String getLastMatchingVersion(List<String> versions, String version) {
        // Extract substring after the first dot (.)
        int firstDotIndex = version.indexOf(".");
        if (firstDotIndex == -1 || firstDotIndex == version.length() - 1) {
            return null; // Invalid version format
        }
        String searchPrefix = version.substring(firstDotIndex + 1); // Get substring after first dot

        // Find the last occurrence of an element that starts with the extracted prefix
        String lastMatch = null;
        for (String v : versions) {
            if (v.startsWith(searchPrefix)) {
                lastMatch = v; // Keep updating to get the last match
            }
        }

        return lastMatch; // Returns the last matching version or null if none found
    }

    private void alphabetizeList(List<String> versions) {
        // Sort using natural ordering (handling numbers correctly)
        versions.sort(NeoForge::compareVersions);
    }

    private static int compareVersions(String v1, String v2) {
        List<Integer> parts1 = extractVersionNumbers(v1);
        List<Integer> parts2 = extractVersionNumbers(v2);

        for (int i = 0; i < Math.max(parts1.size(), parts2.size()); i++) {
            int num1 = (i < parts1.size()) ? parts1.get(i) : 0;
            int num2 = (i < parts2.size()) ? parts2.get(i) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return v1.compareTo(v2); // Final lexicographic comparison if all numbers match
    }

    private static List<Integer> extractVersionNumbers(String version) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+").matcher(version);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }

    private void getVersionsList(String url) {
        try {
            InputStream inputStream = fetchXmlFromUrl(url);

            if (inputStream != null) {
                neoForgeVersions = parseXmlForVersions(inputStream);
            } else {
                System.out.println("Failed to retrieve XML.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static InputStream fetchXmlFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Avoid blocking by servers

        if (connection.getResponseCode() == 200) {
            return connection.getInputStream();
        } else {
            System.out.println("HTTP request failed with response code: " + connection.getResponseCode());
            return null;
        }
    }

    private static List<String> parseXmlForVersions(InputStream xmlStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlStream);

        document.getDocumentElement().normalize();

        NodeList versionNodes = document.getElementsByTagName("version");
        List<String> versions = new ArrayList<>();

        for (int i = 0; i < versionNodes.getLength(); i++) {
            versions.add(versionNodes.item(i).getTextContent());
        }

        return versions;
    }

    private void downloadInstaller(String neoForgeVersion) throws IOException {
        if(neoForgeVersion.contains("1.20.1")) {
            downloadFile("https://maven.neoforged.net/releases/net/neoforged/forge/" + neoForgeVersion + "/" + neoForgeInstallerName, neoForgeInstallerName);
        } else {
            downloadFile("https://maven.neoforged.net/releases/net/neoforged/neoforge/" + neoForgeVersion + "/" + neoForgeInstallerName, neoForgeInstallerName);
        }
    }

    void downloadJar() throws IOException, InterruptedException {
        File nullDevice = new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null");
        ProcessBuilder proc = new ProcessBuilder(javaPath, "-jar", neoForgeInstallerName, "--installServer");
        proc.directory(new File(System.getProperty("user.dir")));
        System.out.println("Installing NeoForge server... (This may take a while)");
        proc.redirectOutput(ProcessBuilder.Redirect.to(nullDevice));
        proc.redirectError(ProcessBuilder.Redirect.to(nullDevice));
        Process runProc = proc.start();
        runProc.waitFor();
        System.out.println("NeoForge server installed!");

        FileUtils.delete(new File(neoForgeInstallerName));
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
            FileUtils.delete(new File(neoForgeInstallerName + ".log"));
        } catch (Exception ignored) {}
    }

    public void generateStartScript() throws IOException {
        if(serverVersion.equals("1.20.1")) {
            if(serverOS.equals("windows")) {
                try {
                    FileWriter windowsStartScript = new FileWriter("start.bat");
                    if(javaFlags.isEmpty()) {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " @libraries/net/neoforged/forge/" + neoForgeVersion + "/win_args.txt nogui");
                        windowsStartScript.close();
                    } else {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " @libraries/net/neoforged/forge/" + neoForgeVersion + "/win_args.txt nogui");
                        windowsStartScript.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    FileWriter linuxStartScript = new FileWriter("start.sh");
                    if(javaFlags.isEmpty()) {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " @libraries/net/neoforged/forge/" + neoForgeVersion + "/unix_args.txt nogui");
                        linuxStartScript.close();
                    } else {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " @libraries/net/neoforged/forge/" + neoForgeVersion + "/unix_args.txt nogui");
                        linuxStartScript.close();
                    }
                    File startSH = new File("start.sh");
                    if(!startSH.setExecutable(true)) {
                        System.out.println("Unable to make start script executable. Please do this yourself by running chmod +x start.sh in a terminal.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if(serverOS.equals("windows")) {
                try {
                    FileWriter windowsStartScript = new FileWriter("start.bat");
                    if(javaFlags.isEmpty()) {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " @libraries/net/neoforged/neoforge/" + neoForgeVersion + "/win_args.txt nogui");
                        windowsStartScript.close();
                    } else {
                        windowsStartScript.write("@echo off\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " @libraries/net/neoforged/neoforge/" + neoForgeVersion + "/win_args.txt nogui");
                        windowsStartScript.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    FileWriter linuxStartScript = new FileWriter("start.sh");
                    if(javaFlags.isEmpty()) {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " @libraries/net/neoforged/neoforge/" + neoForgeVersion + "/unix_args.txt nogui");
                        linuxStartScript.close();
                    } else {
                        linuxStartScript.write("#!/bin/bash\n" + javaPath + " -Xms" + serverMemory + " -Xmx" + serverMemory + " " + javaFlags + " @libraries/net/neoforged/neoforge/" + neoForgeVersion + "/unix_args.txt nogui");
                        linuxStartScript.close();
                    }
                    File startSH = new File("start.sh");
                    if(!startSH.setExecutable(true)) {
                        System.out.println("Unable to make start script executable. Please do this yourself by running chmod +x start.sh in a terminal.");
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
}
