
package app;

import arc.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.*;
import mindustry.core.GameState.State;
import mindustry.entities.type.Player;
import mindustry.net.Packets.KickReason;
import org.json.simple.JSONObject;

import static mindustry.Vars.*;
/*
TODO:

*/

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
          if(!state.is(State.playing)){
              return newFixedLengthResponse("Not hosting a game yet. Calm down.");
          }

          Map<String, List<String>> params = session.getParameters();

          String query = params.get("q").get(0);

          Player target;

          switch(params.get("procedure").get(0)) {

              case "kick":
                target = playerGroup.find(p -> p.id == Integer.parseInt(query));
                if (target == null) {return newFixedLengthResponse("Error: target is null");}
                target.con.kick(KickReason.kick);
                return newFixedLengthResponse("Kicked " + target.name + " successfully!");

              case "banID":
                target = playerGroup.find(p -> p.uuid.equals(query.replace("-", "+").replace("_", "/")));
                if (target == null) {return newFixedLengthResponse("Error: target is null");}
                netServer.admins.banPlayer(target.uuid);
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
          return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "400 Bad Request"); // if a procedure is not specified, too bad!
      }
      return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");  // if no uri match, then it doesn't exist!
    }
}
