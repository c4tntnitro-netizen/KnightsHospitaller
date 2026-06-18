package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.KoHLoreModPlugin;

import java.util.List;
import java.util.Map;

public class KoHLoreCMD extends BaseCommandPlugin {

    public static final String PK_BLACK_HOLE_KEY = "$koh_lore_pk_black_hole";
    public static final String FLEET_FOUND_KEY = "$koh_lore_fleet_found";
    private static final SpecialItemData PLANETKILLER = new SpecialItemData("planetkiller", null);

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params == null || params.isEmpty()) return false;

        String action = params.get(0).getString(memoryMap);
        if ("updateData".equals(action)) {
            updateData();
            return true;
        }
        if ("isBlackHole".equals(action)) {
            return isBlackHole(dialog == null ? null : dialog.getInteractionTarget());
        }
        if ("disposePlanetkiller".equals(action)) {
            return disposePlanetkiller(dialog);
        }
        if ("grantInvictus".equals(action)) {
            return grantInvictus(dialog);
        }
        if ("dismissDialog".equals(action)) {
            if (dialog != null) {
                dialog.dismiss();
            }
            return true;
        }
        return false;
    }

    private void updateData() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;

        MemoryAPI global = sector.getMemoryWithoutUpdate();
        MemoryAPI player = sector.getPlayerMemoryWithoutUpdate();

        boolean lpp = global.getBoolean("$lpp_missionCompleted") || global.getBoolean("$lpp_completed");
        boolean lke = global.getBoolean("$lke_missionCompleted") || global.getBoolean("$lke_completed");
        boolean bffi = global.getBoolean("$bffi_missionCompleted") || global.getBoolean("$bffi_completed");
        boolean pkBlackHole = player.getBoolean(PK_BLACK_HOLE_KEY);
        boolean pkKnights = player.getBoolean("$turnedInPlanetkillerToKnights");

        global.set("$koh_lore_lpp_done", lpp);
        global.set("$koh_lore_lke_done", lke);
        global.set("$koh_lore_bffi_done", bffi);
        global.set("$koh_lore_any_luddic_done", lpp || lke || bffi || pkKnights);
        global.set("$koh_lore_pk_black_hole", pkBlackHole);
        global.set("$koh_lore_pk_knights", pkKnights);
        global.set("$koh_lore_invictus_present", KoHLoreModPlugin.findHospitallerFleet() != null);
    }

    private boolean isBlackHole(SectorEntityToken entity) {
        if (entity == null) return false;
        if (entity instanceof PlanetAPI && ((PlanetAPI) entity).isBlackHole()) return true;
        if (entity.getStarSystem() != null && entity.getStarSystem().hasBlackHole() && entity == entity.getStarSystem().getStar()) {
            return true;
        }

        String type = entity.getCustomEntityType();
        if (type != null && type.toLowerCase().contains("black_hole")) return true;

        String name = entity.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            return lower.contains("black hole") || lower.contains("scythe of orion");
        }
        return false;
    }

    private boolean disposePlanetkiller(InteractionDialogAPI dialog) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;

        CargoAPI cargo = sector.getPlayerFleet().getCargo();
        if (cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, PLANETKILLER) < 1f) {
            if (dialog != null) {
                dialog.getTextPanel().addPara("The Planetkiller is no longer aboard your fleet.");
            }
            return false;
        }

        cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, PLANETKILLER, 1f);
        sector.getPlayerMemoryWithoutUpdate().set(PK_BLACK_HOLE_KEY, true);
        sector.getPlayerMemoryWithoutUpdate().set("$turnedInPlanetkiller", true);
        sector.getMemoryWithoutUpdate().set("$koh_lore_pk_black_hole", true);

        if (dialog != null) {
            dialog.getTextPanel().addPara("The Planetkiller falls away into the dark. There is no flash, no triumph, no witness but your bridge crew and the patient gravity of the abyss.");
            dialog.getTextPanel().addPara("A forbidden weapon is gone from the Sector.");
        }
        return true;
    }

    private boolean grantInvictus(InteractionDialogAPI dialog) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;
        if (!sector.getPlayerMemoryWithoutUpdate().getBoolean(PK_BLACK_HOLE_KEY)) return false;
        if (sector.getMemoryWithoutUpdate().getBoolean(KoHLoreModPlugin.INVICTUS_GRANTED_KEY)) return false;

        CampaignFleetAPI mercy = KoHLoreModPlugin.findHospitallerFleet();
        if (mercy == null || mercy.getFleetData().getNumMembers() <= 0) {
            if (dialog != null) {
                dialog.getTextPanel().addPara("The Mercy is not answering your hails.");
            }
            return false;
        }

        FleetMemberAPI invictus = mercy.getFlagship();
        if (invictus == null) {
            invictus = mercy.getFleetData().getMembersListCopy().get(0);
        }

        mercy.getFleetData().removeFleetMember(invictus);
        invictus.setCaptain(null);
        invictus.setFlagship(false);
        invictus.getVariant().removeTag(Tags.VARIANT_UNBOARDABLE);
        invictus.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        invictus.getRepairTracker().setCR(invictus.getRepairTracker().getMaxCR());

        FleetDataAPI playerFleet = sector.getPlayerFleet().getFleetData();
        playerFleet.addFleetMember(invictus);
        playerFleet.sort();
        sector.getPlayerFleet().forceSync();

        sector.getMemoryWithoutUpdate().set(KoHLoreModPlugin.INVICTUS_GRANTED_KEY, true);
        sector.getMemoryWithoutUpdate().set(FLEET_FOUND_KEY, true);
        sector.getPlayerMemoryWithoutUpdate().set("$koh_lore_invictus_granted", true);

        Misc.makeUnimportant(mercy, "koh_lore");
        mercy.despawn();

        if (dialog != null) {
            dialog.getTextPanel().addPara("The commander signs the Mercy over without ceremony. \"You cast away the Devil's bargain,\" they say. \"Then take this ship, and let mercy travel farther than we could carry it.\"");
            dialog.getTextPanel().addPara("The Mercy of Gilead has joined your fleet.");
        }
        return true;
    }
}
