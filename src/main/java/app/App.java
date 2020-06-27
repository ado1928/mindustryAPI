package app;

import java.io.IOException;
import fi.iki.elonen.NanoHTTPD;
import org.json.simple.JSONObject;
import java.util.*;
import arc.util.*;
import mindustry.entities.type.Player;
import static mindustry.Vars.*;

public class App extends NanoHTTPD {

    public App() throws IOException {
        super(80);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        Log.info("Mindustry API running!");
        }

    @Override
    public Response serve(IHTTPSession session) {

      switch (session.getUri()) {
        case "/":
          List<JSONObject> players = new ArrayList<JSONObject>(); // Player json list
          JSONObject r = new JSONObject(); // response json

          for(Player p : playerGroup.all()){ // add stuff to the player list
            JSONObject obj = new JSONObject();
            obj.put("name", p.name);
            obj.put("id", p.id);
            obj.put("color", p.color.toString());
            obj.put("uuid", p.uuid);
            obj.put("usid", p.usid);
            obj.put("address", p.con.address);
            obj.put("isAdmin", p.isAdmin);
            obj.put("isMobile", p.isMobile);
            players.add(obj);
          }

          r.put("players", players);  // add players to final responmse

          JSONObject status = new JSONObject();

           status.put("map", world.getMap() == null ? null : world.getMap().name());
           status.put("wave", state.wave);

           r.put("status", status); // add status to final response

          return newFixedLengthResponse(Response.Status.OK, "application/json", r.toString()); // respond with the json

        case "/call":
          Map<String, List<String>> params = session.getParameters();
          Map<String, String> headers = session.getHeaders();

          switch(params.get("procedure").get(0)) {

              case "kick":
                int id = Integer.parseInt(params.get("id").get(0));
                playerGroup.find(p -> p.id == id).con.kick("[scarlet]You have been kicked by the server!");
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Kicked ID " + id + " successfully!");

              case "banID":
                String uuid = params.get("id").get(0);
                netServer.admins.banPlayer(uuid);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Banned UUID " + uuid + " successfully!");

              case "banIP":
                String ip = params.get("ip").get(0);
                netServer.admins.banPlayerIP(ip);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Banned IP " + ip + " successfully!");
            }
          return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "400 Bad Request"); // if a procedure is not specified, too bad!
      }
      return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");  // if no uri match, then it doesn't exist!
    }
}
