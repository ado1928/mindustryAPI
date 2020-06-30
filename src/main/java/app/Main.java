package app;

import mindustry.plugin.Plugin;
import arc.util.*;

public class Main extends Plugin {
    public Main() {
        try {
            new App();
        } catch (final Exception e) {
            Log.err("Couldn't start server: " + e);
        }
    }
}
