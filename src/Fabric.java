import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Fabric extends Server{
    protected String loaderVersion;
    protected String installerVersion;
    public Fabric(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        getFabricConfig();
        jarName = serverType + "-" + serverVersion + "-" + loaderVersion + ".jar";
    }

    private void getFabricConfig() {
        loaderVersion = serverConfigFile.getJSONObject("fabric-config").getString("loader-version");
        installerVersion = serverConfigFile.getJSONObject("fabric-config").getString("installer-version");
        if(serverVersion.equals("latest") || serverVersion.equals("latest-snapshot")) {
            serverVersion = getLatestVersion();
        }
        if(loaderVersion.equals("recommended") || loaderVersion.equals("latest")) {
            loaderVersion = getRecommendedLoaderVersion();
        }
        if(installerVersion.equals("latest") || installerVersion.equals("recommended")) {
            installerVersion = getLatestInstaller();
        }
    }

    private String getRecommendedLoaderVersion() {
        JSONArray fabricLoadersJSONArray = new JSONArray();
        try {
            fabricLoadersJSONArray = getJSONArrayFromURL(new URL("https://meta.fabricmc.net/v2/versions/loader/" + serverVersion));
        } catch (MalformedURLException e) {
            System.out.println("Versions JSON not found. Is the Fabric meta API down?");
            System.exit(1);
        }
        assert fabricLoadersJSONArray != null;
        if(!fabricLoadersJSONArray.getJSONObject(0).getJSONObject("loader").getBoolean("stable")) {
            System.out.println("Warning: Fabric loader version unstable. If problems arise, you may want to manually set the loader version in ServerMaker's config.");
        }
        return fabricLoadersJSONArray.getJSONObject(0).getJSONObject("loader").getString("version");
    }

    private String getLatestInstaller() {
        JSONArray fabricInstallersJSONArray = new JSONArray();
        try {
            fabricInstallersJSONArray = getJSONArrayFromURL(new URL("https://meta.fabricmc.net/v2/versions/installer"));
        } catch (MalformedURLException e) {
            System.out.println("Versions JSON not found. Is the Fabric meta API down?");
            System.exit(1);
        }
        assert fabricInstallersJSONArray != null;
        return fabricInstallersJSONArray.getJSONObject(0).getString("version");
    }

    private String getLatestVersion() {
        JSONArray fabricVersionsJSONArray = new JSONArray();
        try {
            fabricVersionsJSONArray = getJSONArrayFromURL(new URL("https://meta.fabricmc.net/v2/versions/game"));
        } catch (MalformedURLException e) {
            System.out.println("Versions JSON not found. Is the FabricMC meta API down?");
            System.exit(1);
        }
        assert fabricVersionsJSONArray != null;
        if(serverVersion.equals("latest")) {
            for(int i = 0; i < fabricVersionsJSONArray.length(); i++) {
                if(fabricVersionsJSONArray.getJSONObject(i).getBoolean("stable")) {
                    return fabricVersionsJSONArray.getJSONObject(i).getString("version");
                }
            }
            System.out.println("Stable fabric build not found for this version. Searching for unstable build...");
            return fabricVersionsJSONArray.getJSONObject(0).getString("version");
        } else if(serverVersion.equals("latest-snapshot")) {
            return fabricVersionsJSONArray.getJSONObject(0).getString("version");
        }
        return "";
    }

    public void downloadJar() throws IOException {
        System.out.println("Downloading server jar...");
        serverDownloadURL = "https://meta.fabricmc.net/v2/versions/loader/" + serverVersion + "/" + loaderVersion + "/" + installerVersion + "/server/jar";

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
