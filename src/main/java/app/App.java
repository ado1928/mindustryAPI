package app;

import static app.Main.cookies;
import static app.Main.logfile;
import static app.Main.password;
import static app.Main.port;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

import arc.files.Fi;
import arc.struct.Array;
import fi.iki.elonen.NanoHTTPD;
import mindustry.entities.type.Player;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

public class App extends NanoHTTPD {
  ArrayList<String> tokens = new ArrayList<>(Arrays.asList(cookies.readString().split("\n")));
  Logger log = Logger.getLogger(App.class.getName());

  public App() throws IOException {
    super(port);
    log.addHandler(new ConsoleHandler());
    log.addHandler(new FileHandler);
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    log.info("Mindustry API running on port " + port);
  }
  public List<String> arrayToList(final Array<String> a) {
    final List<String> l = new ArrayList<String>();
    for (int i = 0; i < a.size; i++) {
      l.add(a.get(i).toString());
    }
    return l;
  }

  @Override
  public Response serve(final IHTTPSession session) {
    String ip = session.getRemoteIpAddress();
    JSONObject r;
    Player target;
    PlayerInfo pi;
    String query;
    List<JSONObject> idbans;
    List<JSONObject> players;
    JSONObject pj;

    final Map<String, List<String>> params = session.getParameters();
    final Map<String, String> headers = session.getHeaders();
    final CookieHandler ch = new CookieHandler(headers);
    final String uri = session.getUri();
    final Response res;

    try {

      if (uri.equals("/auth") && params.containsKey("password")) {
        if (params.get("password").get(0).equals(password)) {
          byte[] b = new byte[63];
          new Random().nextBytes(b);
          String str = new String(Base64.getEncoder().encode(b));
          ch.set("token", str, 7776000);
          tokens.add(str);
          cookies.writeString(String.join("\n", tokens));
          log.info(ip + " has been assigned cookie " + str);
          res = newFixedLengthResponse(Response.Status.OK, "application/json", "OK");
          ch.unloadQueue(res);
          return res;
        }
      }

      if (tokens.contains(ch.read("token"))) {
        switch (uri) {
          case "/":
            log.info(ip + " has requested index.html");
            return newFixedLengthResponse(Response.Status.OK, "text/html", new Fi("web/index.html").readString());
          case "/players":
            r = new JSONObject();
            players = new ArrayList<JSONObject>(); // Player json list

            for (final Player p : playerGroup.all()) { // add stuff to the player list
              pj = new JSONObject();
              pj.put("name", p.name);
              pj.put("id", p.id);
              pj.put("color", p.color.toString());
              pj.put("uuid", p.uuid);
              pj.put("address", p.con.address);
              players.add(pj);
            }

            r.put("players", players); // add players to final responmse

            res = newFixedLengthResponse(Response.Status.OK, "application/json", r.toString());
            res.addHeader("Cache-Control", "no-cache");
            return res;

          case "/call":

            query = params.get("q").get(0);
            log.info(ip + " has called procedure " + params.get("procedure").get(0) + " with query " + query);

            switch (params.get("procedure").get(0)) {

              case "kick":
                target = playerGroup.find(p -> p.id == Integer.parseInt(query));
                if (target == null) {
                  return newFixedLengthResponse("Error: target is null");
                }
                target.con.kick(KickReason.kick);
                return newFixedLengthResponse("Kicked " + target.name + " successfully!");

              case "banID":
                final String uuid = query.replace("-", "+").replace("_", "/");
                target = playerGroup.find(p -> p.uuid.equals(uuid));
                if (target == null) {
                  netServer.admins.banPlayer(uuid);
                  return newFixedLengthResponse("Target was null. Banned offline UUID " + uuid);
                }
                netServer.admins.banPlayer(target.uuid);
                target.con.kick(KickReason.banned);
                return newFixedLengthResponse("Banned " + target.name + " successfully!");

              case "banIP":
                netServer.admins.banPlayerIP(query);
                return newFixedLengthResponse("Banned IP " + query + " successfully!");

              case "unbanIP":
                netServer.admins.unbanPlayerIP(query);
                return newFixedLengthResponse("Unbanned IP " + query + " successfully!");

              case "unbanID":
                netServer.admins.unbanPlayerID(query.replace("-", "+").replace("_", "/"));
                return newFixedLengthResponse("Unbanned UUID " + query + " successfully!");

            }
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "400 Bad Request"); // if a
                                                                                                         // procedure is
                                                                                                         // not
                                                                                                         // specified,
                                                                                                         // too bad!
          case "/bans":
            r = new JSONObject();
            idbans = new ArrayList<JSONObject>();

            for (final PlayerInfo p : netServer.admins.getBanned()) {
              pj = new JSONObject(); // player json object
              pj.put("names", arrayToList(p.names));
              pj.put("ips", arrayToList(p.ips));
              pj.put("lastName", p.lastName);
              pj.put("id", p.id);
              idbans.add(pj);
            }

            r.put("id", idbans);

            r.put("ip", arrayToList(netServer.admins.getBannedIPs()));

            res = newFixedLengthResponse(Response.Status.OK, "application/json", r.toString());
            res.addHeader("Cache-Control", "no-cache");
            return res;

          case "/info":
            query = params.get("q").get(0).replace("-", "+").replace("_", "/");
            r = new JSONObject();
            target = playerGroup.find(p -> p.uuid.equals(query));
            if (target == null) {
              pi = netServer.admins.getBanned().find(p -> p.id.equals(query));
              if (pi == null) {
                return newFixedLengthResponse("Error: target is null");
              }
            } else {
              pi = target.getInfo();
            }
            r.put("id", pi.id);
            r.put("lastName", pi.lastName);
            r.put("lastIP", pi.lastIP);
            r.put("ips", arrayToList(pi.ips));
            r.put("names", arrayToList(pi.names));
            r.put("timesKicked", pi.timesKicked);
            r.put("timesJoined", pi.timesJoined);
            r.put("banned", pi.banned);
            r.put("admin", pi.admin);
            r.put("lastKicked", pi.lastKicked);

            return newFixedLengthResponse(Response.Status.OK, "application/json", r.toString());
        }
        if (Arrays.asList(new File("web").list()).contains(uri.substring(uri.indexOf("/") + 1))) {
          String mime;
          String cc;
          switch (uri.substring(uri.lastIndexOf(".") + 1)) {
            case "html":
              mime = "text/html";
              cc = "no-cache";
              break;
            case "js":
              mime = "text/javascript";
              cc = "no-cache";
              break;
            case "woff2":
              mime = "font/woff2";
              cc = "public, max-age=604800, immutable";
              break;
            case "png":
              mime = "image/png";
              cc = "public, max-age=86400, immutable";
              break;
            case "ico":
              mime = "image/vnd.microsoft.icon";
              cc = "public, max-age=86400, immutable";
              break;
            default:
              mime = "application";
              cc = "no-store";
          }
          res = newChunkedResponse(Response.Status.OK, mime, new FileInputStream(new File("web" + uri)));
          res.addHeader("Cache-Control", cc);
          return res;

        } else {
          return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        }
      } else {
        log.warning(ip + " has attempted to enter the site without a valid cookie");
        return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "NO");
      }
    } catch (

    final Exception e) {
      log.severe(ip + " has encountered " + e + " with URI " + uri);
      log.severe(e.printStackTrace());
      return newFixedLengthResponse("An error has occurred while processing your request: " + e);
    }

  }
}
