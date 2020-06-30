
package app;

import arc.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.*;
import java.util.*;
import mindustry.core.GameState.State;
import mindustry.entities.type.Player;
import mindustry.net.Packets.KickReason;
import mindustry.net.Administration.PlayerInfo;
import org.json.simple.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;

import static mindustry.Vars.*;

public class App extends NanoHTTPD {

  public App() throws IOException {
    super(80);
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    Log.info("Mindustry API running!");
  }

  @Override
  public Response serve(final IHTTPSession session) {
    try {
      JSONObject r;
      Player target;
      PlayerInfo pi;
      final Map<String, List<String>> params = session.getParameters();
      String query;
      List<JSONObject> idbans;
      List<JSONObject> players;
      JSONObject pj;
      final String uri = session.getUri();

      switch (uri) {
        case "/":
          return newFixedLengthResponse(Response.Status.OK, "text/html",
              new String(Files.readAllBytes(Paths.get("web/index.html"))));
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

          return newFixedLengthResponse(Response.Status.OK, "application/json", r.toString());

        case "/call":
          if (!state.is(State.playing)) {
            return newFixedLengthResponse("Not hosting a game yet. Calm down.");
          }

          query = params.get("q").get(0);

          switch (params.get("procedure").get(0)) {

            case "kick":
              target = playerGroup.find(p -> p.id == Integer.parseInt(query));
              if (target == null) {
                return newFixedLengthResponse("Error: target is null");
              }
              target.con.kick(KickReason.kick);
              return newFixedLengthResponse("Kicked " + target.name + " successfully!");

            case "banID":
              target = playerGroup.find(p -> p.uuid.equals(query.replace("-", "+").replace("_", "/")));
              if (target == null) {
                return newFixedLengthResponse("Error: target is null");
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
            pj.put("names", p.names);
            pj.put("ips", p.ips);
            pj.put("id", p.id);
            idbans.add(pj);
          }

          r.put("id", idbans);

          r.put("ip", netServer.admins.getBannedIPs());

          return newFixedLengthResponse(Response.Status.OK, "application/json", r.toString());

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
          r.put("ips", pi.ips);
          r.put("names", pi.names);
          r.put("timesKicked", pi.timesKicked);
          r.put("timesJoined", pi.timesJoined);
          r.put("banned", pi.banned);
          r.put("admin", pi.admin);
          r.put("lastKicked", pi.lastKicked);

          return newFixedLengthResponse(Response.Status.OK, "application/json", r.toString());
      }
      if (Arrays.asList(new File("web").list()).contains(uri.substring(uri.indexOf("/") + 1))) {
        return newFixedLengthResponse(Response.Status.OK, "application",
            new String(Files.readAllBytes(Paths.get("web" + uri))));
      } else {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
      }

    } catch (final Exception e) {
      return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
          "An error has occurred while processing your request: " + e);
    }
  }
}
