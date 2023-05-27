package data.scripts.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import java.io.IOException;
import org.json.JSONException;

public class ToggleWeaponsModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        try {
            ToggleWeaponsEveryFrame.readSettingsFile();
        } catch (IOException | JSONException e) {
            System.console().printf("Error while loading ToggleWeapons: " + e.getMessage());
        }
    }
}