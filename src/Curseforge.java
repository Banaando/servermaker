import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

public class Curseforge extends Server {
    protected String modpackVersion;
    protected String modpackURL;

    public Curseforge(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
        modpackVersion = serverConfigFile.getJSONObject("curseforge-config").getString("modpack-version");
        modpackURL = serverConfigFile.getJSONObject("curseforge-config").getString("modpack-URL");
        if(!downloadServerPack(modpackURL, modpackVersion)) {
            System.out.println("Server pack not found for this modpack. Unfortunately, ServerMaker doesn't support this yet.");
        }
    }

    private boolean downloadServerPack(String modpackURL, String modpackVersion) throws IOException {
        String modpackFilesURL = modpackURL + "/files/all?page=1&pageSize=20&showAlphaFiles=show";
        String desiredModpackURL = findModpackURL(modpackFilesURL, modpackVersion);
        System.out.println(desiredModpackURL);
        System.exit(0);

        return false;
    }

    /**
     * Returns a link to the first main modpack file (non-server pack) that meets these criteria:
     * - It is of the specified type (latest-release, latest-beta, latest-alpha, latest, or a specific version).
     * - A corresponding server pack (file whose name contains "Server Files" with the same version) is available.
     *
     * @param baseUrl the website URL (including query parameters) to start searching.
     * @param type a string indicating the type. Acceptable values are "latest-release", "latest-beta",
     *             "latest-alpha", "latest", or a specific version (e.g. "9.2.9").
     * @return the full URL of the matching file, or null if no matching file is found.
     * @throws IOException if an error occurs during the HTTP request.
     */
    public static String findModpackURL(String baseUrl, String type) throws IOException {
        int page = 1;
        while (true) {
            // Update the URL for the current page.
            String url = updatePageParam(baseUrl, page);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .header("Accept", "text/html")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .get();
            System.out.println(doc);
            System.out.println(url + " obtained");

            // If the page text indicates no results, return null.
            if (doc.text().contains("No Results")) {
                return null;
            }

            // Build a set of versions that have server pack files.
            Set<String> serverPackVersions = new HashSet<>();
            Elements serverPackCards = doc.select("a.file-card");
            for (Element card : serverPackCards) {
                Element nameDiv = card.selectFirst("div.name");
                if (nameDiv != null && nameDiv.attr("title").toLowerCase().contains("server")) {
                    // Extract version by assuming it is the last token of the title.
                    String title = nameDiv.attr("title").trim();
                    String[] parts = title.split("\\s+");
                    if (parts.length > 0) {
                        String version = parts[parts.length - 1];
                        serverPackVersions.add(version);
                    }
                }
            }

            // Now, iterate over main file cards (skip those that are server pack cards).
            Elements fileCards = doc.select("a.file-card");
            for (Element card : fileCards) {
                Element nameDiv = card.selectFirst("div.name");
                if (nameDiv == null) {
                    continue;
                }
                String title = nameDiv.attr("title").trim();
                // Skip server pack files.
                if (title.contains("Server Files")) {
                    continue;
                }
                // Extract version from the title (assumed to be the last word)
                String[] parts = title.split("\\s+");
                String version = parts[parts.length - 1];
                // Verify that a server pack exists for this version.
                if (!serverPackVersions.contains(version)) {
                    continue;
                }

                // Check file type criteria.
                // Look for the tooltip text that indicates the release type.
                Element tooltip = card.selectFirst("div.tooltip");
                String releaseType = tooltip != null ? tooltip.text().trim() : "";

                if ("latest-release".equalsIgnoreCase(type)) {
                    if (!"Release".equalsIgnoreCase(releaseType)) {
                        continue;
                    }
                } else if ("latest-beta".equalsIgnoreCase(type)) {
                    if (!"Beta".equalsIgnoreCase(releaseType)) {
                        continue;
                    }
                } else if ("latest-alpha".equalsIgnoreCase(type)) {
                    if (!"Alpha".equalsIgnoreCase(releaseType)) {
                        continue;
                    }
                } else if (!"latest".equalsIgnoreCase(type)) {
                    // Assume a specific version is provided.
                    if (!title.contains(type)) {
                        continue;
                    }
                }

                // Found a matching file – extract its link.
                String fileLink = card.attr("href").trim();
                if (!fileLink.startsWith("http")) {
                    fileLink = "https://www.curseforge.com" + fileLink;
                }
                return fileLink;
            }
            // No matching file found on this page; move to the next.
            page++;
        }
    }

    /**
     * Updates the "page" query parameter in the URL.
     *
     * @param url the original URL.
     * @param page the new page number.
     * @return the URL with the updated page parameter.
     */
    private static String updatePageParam(String url, int page) {
        if (url.matches(".*[?&]page=\\d+.*")) {
            return url.replaceAll("([?&]page=)\\d+", "$1" + page);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "page=" + page;
        }
    }

    void downloadJar() throws IOException, InterruptedException {

    }

    String getServerJSONURL(String serverType) throws MalformedURLException {
        return "";
    }

    String getServerDownloadURL(String serverJSONURL) throws MalformedURLException {
        return "";
    }
}
