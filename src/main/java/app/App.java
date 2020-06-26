package app;

import mindustry.plugin.Plugin;
import mindustry.entities.type.Player;

import org.takes.Response;
import org.takes.http.Exit;
import org.takes.http.FtBasic;
import org.takes.facets.fork.RqRegex;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.rs.RsWithType.Json;
import org.takes.rs.RsText;

import arc.util.*;
import org.json.simple.JSONObject;

import java.util.*;

import static mindustry.Vars.*;

public class App extends Plugin {
    public App() {
        Thread serverThread = new Thread(() -> {
            try {
                new FtBasic(new TkFork(

                    new FkRegex("/",
                      new TkRegex() {
                         @Override
                         public Response act(final RqRegex req) {
                           List<JSONObject> players = new ArrayList<JSONObject>();

                           JSONObject r = new JSONObject();

                           for(Player p : playerGroup.all()){
                               JSONObject obj = new JSONObject();
                               obj.put("name", p.name);
                               obj.put("id", p.id);
                               obj.put("color", p.color);
                               obj.put("uuid", p.uuid);
                               obj.put("usid", p.usid);
                               obj.put("address", p.con.address);
                               obj.put("isAdmin", p.isAdmin);
                               obj.put("isMobile", p.isMobile);

                               players.add(obj);
                           }

                           r.put("players", players);

                           JSONObject status = new JSONObject();

                          status.put("map", world.getMap());
                          status.put("wave", state.wave);

                          r.put("status", status);

                           return new Json(new RsText(r.toString()));
                         }
                       }
                    )

                  ), 5678).start(Exit.NEVER);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        serverThread.start();
    }
}
