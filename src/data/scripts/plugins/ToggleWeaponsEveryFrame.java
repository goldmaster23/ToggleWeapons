package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponGroupType;
import java.io.IOException;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class ToggleWeaponsEveryFrame extends BaseEveryFrameCombatPlugin {

    private static final String SUCCESS_SOUND = "ui_button_pressed";
    private static final String FAILURE_SOUND = "ui_button_disabled_pressed";
    private static final String CONFIG_FILE = "TOGGLE_WEAPONS_HOTKEYS.ini";
    private static int TOGGLE_SELECTED_GROUP_KEY;
    private static int QUICK_HOLD_FIRE_KEY;
    private static int DESLECT_ALL_WEAPONS;
    private static int NUM_OF_WEAPON_GROUPS;
    private static boolean IS_HOLD_A_TOGGLE;
    private static int pressedCount = 0;//^used if this is true

    private static boolean QUICK_HOLD_GROUP[];
    private static boolean loadedIni = false;

    private CombatEngineAPI engine;

    static void readSettingsFile() throws IOException, JSONException {

        JSONObject settingsFile = Global.getSettings().loadJSON(CONFIG_FILE);
        TOGGLE_SELECTED_GROUP_KEY = settingsFile.getInt("toggleSelectedGroupMode");
        QUICK_HOLD_FIRE_KEY = settingsFile.getInt("quickHoldFire");
        DESLECT_ALL_WEAPONS = settingsFile.getInt("deselectAllWeapons");
        NUM_OF_WEAPON_GROUPS = settingsFile.getInt("numberOfWeaponGroups");
        IS_HOLD_A_TOGGLE = settingsFile.getBoolean("isHoldFireAToggle");

        QUICK_HOLD_GROUP = new boolean[NUM_OF_WEAPON_GROUPS];
        loadedIni = true;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.getCombatUI() == null || !loadedIni) {
            return;
        }

        final ShipAPI player = engine.getPlayerShip();
        if (player == null || !engine.isEntityInPlay(player) || player.isHulk()) {
            return;
        }

        for (InputEventAPI event : events) {
            if (!event.isConsumed() && event.isKeyDownEvent()) {
                if (event.getEventValue() == TOGGLE_SELECTED_GROUP_KEY) {
                    WeaponGroupAPI selectedGroup = player.getSelectedGroupAPI();

                    if (selectedGroup != null) {
                        if (selectedGroup.getType() == WeaponGroupType.ALTERNATING) {
                            selectedGroup.setType(WeaponGroupType.LINKED);
                        } else {
                            selectedGroup.setType(WeaponGroupType.ALTERNATING);
                        }
                        Global.getSoundPlayer().playUISound(SUCCESS_SOUND, 1f, 0.5f);
                    } else {
                        Global.getSoundPlayer().playUISound(FAILURE_SOUND, 1f, 0.5f);
                    }
                    event.consume();
                } else if (event.getEventValue() == QUICK_HOLD_FIRE_KEY) {
                    List<WeaponGroupAPI> groups = player.getWeaponGroupsCopy();
                    boolean hasIPDAI = player.getVariant().getHullMods().contains("pointdefenseai");
                    for (int i = 0; i < groups.size(); ++i) {
                        WeaponGroupAPI group = groups.get(i);
                        if(pressedCount == 0) QUICK_HOLD_GROUP[i] = group.isAutofiring();//to not reset toggle

                        if (QUICK_HOLD_GROUP[i]) {
                            // only affects groups enabled at time of key press
                            for (WeaponAPI weapon : group.getWeaponsCopy()) {
                                if (weapon.hasAIHint(WeaponAPI.AIHints.PD) || weapon.hasAIHint(WeaponAPI.AIHints.PD_ONLY)
                                        || weapon.hasAIHint(WeaponAPI.AIHints.PD_ALSO)
                                        || (hasIPDAI && weapon.getSize() == WeaponSize.SMALL
                                        && weapon.getType() != WeaponAPI.WeaponType.MISSILE)) {
                                    // do not turn off PD or IPDAI weapon groups
                                    if(pressedCount == 0) QUICK_HOLD_GROUP[i] = false;//to not reset toggle
                                    break;
                                }
                            }

                            if (QUICK_HOLD_GROUP[i]) {
                                group.toggleOff();
                            }
                        }
                    }
                    if (IS_HOLD_A_TOGGLE) pressedCount++;
                    //groups.get(pressedCount).setType(WeaponGroupType.ALTERNATING);//used to test current pressedCount
                    event.consume();
                } else if (event.getEventValue() == DESLECT_ALL_WEAPONS) {
                    // According to Alex:
                    // I think this is just a case of a working failsafe for a non-existent group being selected.
                    // http://fractalsoftworks.com/forum/index.php?topic=13330.msg224721#msg224721
                    player.giveCommand(ShipCommand.SELECT_GROUP, null, player.getWeaponGroupsCopy().size());
                    event.consume();
                }
            }

            if (!event.isConsumed() && event.isKeyUpEvent()) {
                if (event.getEventValue() == QUICK_HOLD_FIRE_KEY) {
                    if (IS_HOLD_A_TOGGLE) {
                        if (pressedCount > 1) {
                            List<WeaponGroupAPI> groups = player.getWeaponGroupsCopy();
                            for (int i = 0; i < groups.size(); ++i) {
                                WeaponGroupAPI group = groups.get(i);

                                if (QUICK_HOLD_GROUP[i]) {
                                    group.toggleOn();
                                }
                            }

                            event.consume();
                            pressedCount = 0;
                        }
                    } else {
                        List<WeaponGroupAPI> groups = player.getWeaponGroupsCopy();
                        for (int i = 0; i < groups.size(); ++i) {
                            WeaponGroupAPI group = groups.get(i);

                            if (QUICK_HOLD_GROUP[i]) {
                                group.toggleOn();
                            }
                        }
                        event.consume();
                    }
                }


            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }
}
