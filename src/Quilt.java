import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;

public class Quilt extends Server{
    protected String loaderVersion;
    protected String installerJavaPath;
    public Quilt(JSONObject serverConfigFile, String serverType) throws IOException, InterruptedException {
        super(serverConfigFile, serverType);
        downloadInstaller();
        loaderVersion = serverConfigFile.getJSONObject("quilt-config").getString("loader-version");
        if(loaderVersion.equals("recommended") && serverVersion.equals("latest")) {
            getLatestVersion(true, true);
        } else if(loaderVersion.equals("recommended")) {
            getLatestVersion(true, false);
        } else if(serverVersion.equals("latest")) {
            getLatestVersion(false, true);
        }
        jarName = "quilt-server-launch.jar";
    }

    private void getLatestVersion(boolean getLoader, boolean getVersion) throws IOException, InterruptedException {
        try {
            // Run the Java program using ProcessBuilder
            ProcessBuilder builder = new ProcessBuilder(installerJavaPath, "-jar", "quilt-installer.jar", "listVersions"); // Adjust accordingly
            builder.redirectErrorStream(true); // Merge stdout and stderr
            Process process = builder.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // Define regex patterns
            Pattern loaderPattern = Pattern.compile("Latest Loader release: (\\S+)");
            Pattern serverPattern = Pattern.compile("Latest Minecraft release: (\\S+)");

            while ((line = reader.readLine()) != null) {
                Matcher loaderMatcher = loaderPattern.matcher(line);
                Matcher serverMatcher = serverPattern.matcher(line);

                if (loaderMatcher.find() && getLoader) {
                    loaderVersion = loaderMatcher.group(1);
                }
                if (serverMatcher.find() && getVersion) {
                    serverVersion = serverMatcher.group(1);
                }

                // Print for debugging (optional)
                System.out.println(line);
            }

            // Wait for the process to finish
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void downloadInstaller() throws IOException, InterruptedException {
        downloadFile("https://quiltmc.org/api/v1/download-latest-installer/java-universal", "quilt-installer.jar");
        if(Double.parseDouble(System.getProperty("java.specification.version")) < 17) {
            System.out.println("ServerMaker wasn't run using Java 17 or later, but Quilt's installer requires Java 17 or later to run.\nTemporarily downloading Java 17...");
            if(System.getProperty("os.name").toLowerCase().contains("mac")) { // Run on MacOS with Java 8
                String downloadURLString = String.format("https://api.adoptium.net/v3/binary/latest/17/ga/mac/%s/jre/hotspot/normal/eclipse", System.getProperty("os.arch"));
                downloadFile(downloadURLString, "./.temp/OpenJDK17.tar.gz");


                // Extract Java 17
                ProcessBuilder proc = new ProcessBuilder("/usr/bin/tar", "-xzf", System.getProperty("user.dir") + "/.temp/OpenJDK17.tar.gz");
                proc.directory(new File(System.getProperty("user.dir") + "/.temp"));
                Process runProc = proc.start();
                runProc.waitFor();
                proc.inheritIO();
                File dir = new File(System.getProperty("user.dir") + "/.temp");

                // Find the actual folder name
                File[] matchingFiles = dir.listFiles((file, name) -> name.startsWith("jdk-"));
                if (matchingFiles == null || matchingFiles.length == 0) {
                    throw new FileNotFoundException("No matching 'jdk-*' folder found.");
                }

                File oldFolder = matchingFiles[0]; // Take the first match
                File newFolder = new File(dir, "jdk-17");

                // Run the rename command using ProcessBuilder
                proc.command("mv", oldFolder.getName(), newFolder.getName());
                runProc = proc.start();
                runProc.waitFor();
                installerJavaPath = ".temp/jdk-17/Contents/Home/bin/java";
            } else if(System.getProperty("os.name").toLowerCase().contains("window")) { // Run on Windows with Java 8
                String downloadURLString = String.format("https://api.adoptium.net/v3/binary/latest/17/ga/windows/%s/jre/hotspot/normal/eclipse", System.getProperty("os.arch"));
                downloadFile(downloadURLString, ".\\.temp\\OpenJDK17.zip");


                // Extract Java 17
                String zipFileName = ".\\.temp\\OpenJDK17.zip"; // Change this to your actual ZIP file
                String outputFolder = System.getProperty("user.dir") + File.separator + ".temp";

                try {
                    unzip(zipFileName, outputFolder);
                    System.out.println("Unzip completed successfully!");
                } catch (IOException e) {
                    System.err.println("Error unzipping file: " + e.getMessage());
                }

                // Find the actual folder name
                File dir = new File(System.getProperty("user.dir") + "\\.temp");
                File[] matchingFiles = dir.listFiles((file, name) -> name.startsWith("jdk-"));
                if (matchingFiles == null || matchingFiles.length == 0) {
                    throw new FileNotFoundException("No matching 'jdk-*' folder found.");
                }

                File oldFolder = matchingFiles[0]; // Take the first match
                File newFolder = new File(dir, "jdk-17");

                // Run the rename command using ProcessBuilder
                ProcessBuilder proc = new ProcessBuilder();
                proc.directory(new File(System.getProperty("user.dir") + "\\.temp"));
                proc.inheritIO();
                proc.command("ren", oldFolder.getName(), newFolder.getName());
                Process runProc = proc.start();
                runProc.waitFor();
                installerJavaPath = ".temp\\jdk-17\\bin\\java";
            } else { // Run on Linux with Java 8
                String downloadURLString = String.format("https://api.adoptium.net/v3/binary/latest/17/ga/linux/%s/jre/hotspot/normal/eclipse", System.getProperty("os.arch"));
                downloadFile(downloadURLString, "./.temp/OpenJDK17.tar.gz");


                // Extract Java 17
                ProcessBuilder proc = new ProcessBuilder("/usr/bin/tar", "-xzf", System.getProperty("user.dir") + "/.temp/OpenJDK17.tar.gz");
                proc.directory(new File(System.getProperty("user.dir") + "/.temp"));
                Process runProc = proc.start();
                runProc.waitFor();
                proc.inheritIO();
                File dir = new File(System.getProperty("user.dir") + "/.temp");

                // Find the actual folder name
                File[] matchingFiles = dir.listFiles((file, name) -> name.startsWith("jdk-"));
                if (matchingFiles == null || matchingFiles.length == 0) {
                    throw new FileNotFoundException("No matching 'jdk-*' folder found.");
                }

                File oldFolder = matchingFiles[0]; // Take the first match
                File newFolder = new File(dir, "jdk-17");

                // Run the rename command using ProcessBuilder
                proc.command("mv", oldFolder.getName(), newFolder.getName());
                runProc = proc.start();
                runProc.waitFor();
                installerJavaPath = ".temp/jdk-17/bin/java";
            }
        } else {
            if(System.getProperty("os.name").toLowerCase().contains("mac")) { // Run on MacOS with updated Java version
                installerJavaPath = System.getProperty("java.home" + "/bin/java");
            } else if(System.getProperty("os.name").toLowerCase().contains("window")) { // Run on Windows with updated Java version
                installerJavaPath = System.getProperty("java.home" + "\\bin\\java");
            } else { // Run on Linux with updated Java version
                installerJavaPath = System.getProperty("java.home" + "/bin/java");
            }
        }
    }

    private void unzip(String zipFilePath, String destDirectory) throws FileNotFoundException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs(); // Create the .temp folder if it doesn't exist
        }

        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                File extractedFile = new File(destDirectory, entry.getName());

                if (entry.isDirectory()) {
                    extractedFile.mkdirs();
                } else {
                    // Ensure parent directories exist
                    new File(extractedFile.getParent()).mkdirs();

                    // Write file content
                    try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(extractedFile.toPath()))) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipIn.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void downloadJar() throws IOException, InterruptedException {
        ProcessBuilder proc = new ProcessBuilder(installerJavaPath, "-jar", "quilt-installer.jar", "install", "server", serverVersion, loaderVersion, "--install-dir=.", "--download-server");
        proc.directory(new File(System.getProperty("user.dir")));
        proc.inheritIO();
        Process runProc = proc.start();
        runProc.waitFor();
        FileUtils.deleteDirectory(new File(".temp"));
    }

    String getServerJSONURL(String serverType) throws MalformedURLException {
        return "";
    }

    String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        return "";
    }
}
