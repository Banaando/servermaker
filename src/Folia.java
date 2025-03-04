import org.json.JSONObject;

import java.io.IOException;

public class Folia extends Paper {
    public Folia(JSONObject serverConfigFile, String serverType) throws IOException {
        super(serverConfigFile, serverType);
    }
}
