package app;

import java.util.Base64;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import arc.files.Fi;
import arc.util.Log;
import mindustry.plugin.Plugin;

public class Main extends Plugin {
    public static Fi cookies;
    public static Fi cfg;
    public static Fi logfile;
    public static JSONObject config;
    public static String password;
    public static int port;

    public Main() {
        try {
            cookies = new Fi("api/trustedCookies");
            cfg = new Fi("api/config.json");
            logfile = new Fi("api/latest.xml");

            if (!cookies.exists()) {
                cookies.write();
            }
            if (!cfg.exists()) {
                Log.err("No config.json exists! Creating one");
                byte[] b = new byte[63];
                new Random().nextBytes(b);
                cfg.writeString(
                        "{\"password\": \"" + new String(Base64.getUrlEncoder().encode(b)) + "\",\"port\": 80}");
            }
            config = (JSONObject) new JSONParser().parse(cfg.reader());
            password = (String) config.get("password");
            port = (int) (long) config.get("port");
            new App();
        } catch (final Exception e) {
            Log.err("Could not start server: " + e);
        }
    }
}
