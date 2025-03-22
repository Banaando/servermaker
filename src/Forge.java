import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class Forge extends Server {
    String forgeVersionHTMLLink;
    String forgeVersion;
    public Forge(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);

        // Get a link to the page that has the installer downloads for the MC version the user wants
        if(serverVersion.equals("latest")) {
            serverVersion = getLatestServerVersion();
        }
        forgeVersionHTMLLink = "https://files.minecraftforge.net/net/minecraftforge/forge/index_" + serverVersion + ".html";
        jarName = serverType + "-" + serverVersion;

        // Get the Forge version the user wants
        forgeVersion = getForgeVersion(serverConfigFile.getJSONObject("forge-config").getString("loader-version"));
        System.out.println(forgeVersion);

        // Download either the installer or the universal jar
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
