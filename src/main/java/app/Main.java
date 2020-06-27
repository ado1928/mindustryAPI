package app;

import mindustry.plugin.Plugin;
import app.App;
import arc.util.*;

public class Main extends Plugin {
    public Main() {
          try {
              new App();
          } catch (Exception e) {
              Log.err("Couldn't start server: " + e);
          }
    }
}
