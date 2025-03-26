import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Modrinth extends Server {
    protected String modpackID;
    protected String modpackVersion;
    protected JSONArray modpackVersions;
    protected String javaVersion;
    public Modrinth(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        modpackID = serverConfigFile.getJSONObject("modrinth-config").getString("modpack-URL");
        modpackVersion = serverConfigFile.getJSONObject("modrinth-config").getString("modpack-version");

        int lastSlash = modpackID.lastIndexOf('/');
        modpackID = modpackID.substring(lastSlash + 1);

        modpackVersion = getModpackVersion(modpackVersion);
        javaVersion = getJavaVersion(javaPath);
        jarName = modpackID + "-" + modpackVersion + "-server.jar";

    }

    public static String getJavaVersion(String javaExePath) {
        try {
            ProcessBuilder builder = new ProcessBuilder(javaExePath, "-version");
            builder.redirectErrorStream(true); // Merge stderr and stdout
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("version")) {
                    return extractMajorVersion(line);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "other";
    }

    private static String extractMajorVersion(String versionLine) {
        // Example line: java version "1.8.0_281" or openjdk version "17.0.1" or "21"
        String[] parts = versionLine.split("\"");
        if (parts.length >= 2) {
            String versionString = parts[1];
            if (versionString.startsWith("1.")) {
                // Java 8 and earlier (1.8 -> 8)
                return versionString.split("\\.")[1];
            } else {
                // Java 9+
                return versionString.split("\\.")[0];
            }
        }
        return "other";
    }

    private String getModpackVersion(String modpackVersion) throws MalformedURLException {
        switch(modpackVersion) {
            case "latest": {
                modpackVersions = getJSONArrayFromURL(new URL("https://api.modrinth.com/v2/project/" + modpackID + "/version"));
                assert modpackVersions != null;
                JSONObject version = modpackVersions.getJSONObject(0);

                try {
                    return version.getString("version_number");
                } catch (JSONException e) {
                    System.out.println("Error: No version found for modpack " + modpackID + ". Are you sure this modpack exists on Modrinth?");
                    System.exit(1);
                }
            }
            case "recommended":
            case "latest-release": {
                modpackVersions = getJSONArrayFromURL(new URL("https://api.modrinth.com/v2/project/" + modpackID + "/version"));
                assert modpackVersions != null;
                for(int i = 0; i < modpackVersions.length(); i++) {
                    JSONObject version = modpackVersions.getJSONObject(i);
                    if(version.getString("version_type").equals("release")) {
                        return version.getString("version_number");
                    }
                }
                System.out.println("Error: No stable version found for modpack " + modpackID + ". Please try a beta or alpha version.");
                System.exit(1);
            }
            case "latest-beta": {
                modpackVersions = getJSONArrayFromURL(new URL("https://api.modrinth.com/v2/project/" + modpackID + "/version"));
                assert modpackVersions != null;
                for(int i = 0; i < modpackVersions.length(); i++) {
                    JSONObject version = modpackVersions.getJSONObject(i);
                    if(version.getString("version_type").equals("beta")) {
                        return version.getString("version_number");
                    }
                }
                System.out.println("Error: No beta version found for modpack " + modpackID + ". Please try a release or alpha version.");
                System.exit(1);
            }
            case "latest-alpha": {
                modpackVersions = getJSONArrayFromURL(new URL("https://api.modrinth.com/v2/project/" + modpackID + "/version"));
                assert modpackVersions != null;
                for(int i = 0; i < modpackVersions.length(); i++) {
                    JSONObject version = modpackVersions.getJSONObject(i);
                    if(version.getString("version_type").equals("alpha")) {
                        return version.getString("version_number");
                    }
                }
                System.out.println("Error: No alpha version found for modpack " + modpackID + ". Please try a release or beta version.");
                System.exit(1);
            }
            default: {
                return modpackVersion;
            }
        }
    }

    void downloadJar() throws IOException, InterruptedException {
        downloadFile("https://mrpack4server.pb4.eu/download/" + modpackID + "/" + modpackVersion + "/jvm" + javaVersion + "/server.jar", jarName);
    }

    String getServerJSONURL(String serverType) throws MalformedURLException {
        return "";
    }

    String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        return "";
    }
}
