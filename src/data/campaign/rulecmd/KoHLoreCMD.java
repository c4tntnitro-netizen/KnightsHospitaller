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
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.KoHLoreModPlugin;
import data.scripts.campaign.intel.missions.KoHLoreGreatLabor;

import java.util.List;
import java.util.Map;

public class KoHLoreCMD extends BaseCommandPlugin {

    public static final String PK_BLACK_HOLE_KEY = "$koh_lore_pk_black_hole";
    public static final String FLEET_FOUND_KEY = "$koh_lore_fleet_found";
    public static final String GREAT_LABOR_ACTIVE_KEY = "$hospitaller_great_labor_active";
    public static final String GREAT_LABOR_COMPLETED_KEY = "$completed_great_labor_planetkiller";
    public static final String SUPPLIES_GIVEN_KEY = "$hospitaller_supplies_given_recently";
    public static final String DONATED_CREDITS_KEY = "$hospitaller_donated_credits_recently";
    public static final String DONATED_SUPPLIES_KEY = "$hospitaller_donated_supplies_recently";
    public static final String DONATED_MACHINERY_KEY = "$hospitaller_donated_machinery_recently";
    private static final float GREAT_LABOR_MIN_CHURCH_REP = 0.99f;
    private static final float DONATION_COOLDOWN_DAYS = 30f;
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
        if ("completeGreatLabor".equals(action)) {
            return completeGreatLabor(dialog);
        }
        if ("askForSupplies".equals(action)) {
            return askForSupplies();
        }
        if ("startGreatLabor".equals(action)) {
            return startGreatLabor();
        }
        if ("markDonateCredits".equals(action)) {
            return markDonation(DONATED_CREDITS_KEY);
        }
        if ("markDonateSupplies".equals(action)) {
            return markDonation(DONATED_SUPPLIES_KEY);
        }
        if ("markDonateMachinery".equals(action)) {
            return markDonation(DONATED_MACHINERY_KEY);
        }
        if ("canAskForSupplies".equals(action)) {
            return canAskForSupplies();
        }
        if ("canDonateCredits".equals(action)) {
            return canDonateCredits(10000);
        }
        if ("canDonateSupplies".equals(action)) {
            return canDonateCommodity("supplies", 50);
        }
        if ("canDonateMachinery".equals(action)) {
            return canDonateCommodity("heavy_machinery", 20);
        }
        if ("canDonateAnything".equals(action)) {
            return canDonateAnything();
        }
        if ("canStartGreatLabor".equals(action)) {
            return canStartGreatLabor();
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

        global.set("$koh_lore_lpp_done", lpp);
        global.set("$koh_lore_lke_done", lke);
        global.set("$koh_lore_bffi_done", bffi);
        global.set("$koh_lore_any_luddic_done", lpp || lke || bffi || pkBlackHole);
        global.set("$koh_lore_pk_black_hole", pkBlackHole);
        global.set("$koh_lore_invictus_present", KoHLoreModPlugin.findHospitallerFleet() != null);
        global.set("$koh_lore_great_labor_active", player.getBoolean(GREAT_LABOR_ACTIVE_KEY));
        global.set("$koh_lore_great_labor_completed", player.getBoolean(GREAT_LABOR_COMPLETED_KEY) ||
                global.getBoolean(KoHLoreGreatLabor.COMPLETED_KEY));

        FullName.Gender gender = FullName.Gender.ANY;
        if (sector.getPlayerPerson() != null && sector.getPlayerPerson().getGender() != null) {
            gender = sector.getPlayerPerson().getGender();
        }
        boolean feminine = gender == FullName.Gender.FEMALE;
        global.set("$koh_lore_player_feminine", feminine);
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

    private boolean askForSupplies() {
        SectorAPI sector = Global.getSector();
        if (sector == null || !canAskForSupplies()) return false;

        sector.getPlayerMemoryWithoutUpdate().set(SUPPLIES_GIVEN_KEY, true, DONATION_COOLDOWN_DAYS);
        return true;
    }

    private boolean startGreatLabor() {
        SectorAPI sector = Global.getSector();
        if (sector == null || !canStartGreatLabor()) return false;

        sector.getPlayerMemoryWithoutUpdate().set(GREAT_LABOR_ACTIVE_KEY, true);
        return true;
    }

    private boolean completeGreatLabor(InteractionDialogAPI dialog) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;
        if (!sector.getPlayerMemoryWithoutUpdate().getBoolean(GREAT_LABOR_ACTIVE_KEY)) return false;

        boolean granted = grantInvictus(dialog, false);
        if (!granted) return false;

        sector.getPlayerMemoryWithoutUpdate().set(GREAT_LABOR_ACTIVE_KEY, false);
        sector.getPlayerMemoryWithoutUpdate().set(GREAT_LABOR_COMPLETED_KEY, true);
        sector.getPlayerMemoryWithoutUpdate().set("$hospitaller_rank", "Knight Hospitaller");
        sector.getMemoryWithoutUpdate().set(KoHLoreGreatLabor.COMPLETED_KEY, true);

        return true;
    }

    private boolean canAskForSupplies() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;

        MemoryAPI player = sector.getPlayerMemoryWithoutUpdate();
        return !player.getBoolean(SUPPLIES_GIVEN_KEY) || player.getExpire(SUPPLIES_GIVEN_KEY) <= 0f;
    }

    private boolean canDonateCredits(int amount) {
        SectorAPI sector = Global.getSector();
        return sector != null &&
                !sector.getPlayerMemoryWithoutUpdate().getBoolean(DONATED_CREDITS_KEY) &&
                sector.getPlayerFleet().getCargo().getCredits().get() >= amount;
    }

    private boolean canDonateCommodity(String commodityId, int amount) {
        SectorAPI sector = Global.getSector();
        return sector != null &&
                !sector.getPlayerMemoryWithoutUpdate().getBoolean(getDonationCooldownKey(commodityId)) &&
                sector.getPlayerFleet().getCargo().getCommodityQuantity(commodityId) >= amount;
    }

    private boolean canDonateAnything() {
        return canDonateCredits(10000) ||
                canDonateCommodity("supplies", 50) ||
                canDonateCommodity("heavy_machinery", 20);
    }

    private boolean markDonation(String key) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;

        sector.getPlayerMemoryWithoutUpdate().set(key, true, DONATION_COOLDOWN_DAYS);
        return true;
    }

    private String getDonationCooldownKey(String commodityId) {
        if ("supplies".equals(commodityId)) return DONATED_SUPPLIES_KEY;
        if ("heavy_machinery".equals(commodityId)) return DONATED_MACHINERY_KEY;
        return "$hospitaller_donated_" + commodityId + "_recently";
    }

    private boolean canStartGreatLabor() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return false;

        MemoryAPI player = sector.getPlayerMemoryWithoutUpdate();
        if (player.getBoolean(GREAT_LABOR_ACTIVE_KEY) || player.getBoolean(GREAT_LABOR_COMPLETED_KEY)) return false;
        if (sector.getMemoryWithoutUpdate().getBoolean(KoHLoreModPlugin.INVICTUS_GRANTED_KEY)) return false;
        return sector.getFaction(Factions.LUDDIC_CHURCH).getRelToPlayer().getRel() >= GREAT_LABOR_MIN_CHURCH_REP;
    }

    private boolean grantInvictus(InteractionDialogAPI dialog) {
        return grantInvictus(dialog, true);
    }

    private boolean grantInvictus(InteractionDialogAPI dialog, boolean addText) {
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

        KoHLoreModPlugin.markPeacefulInvictusHandoff(mercy);
        mercy.getFleetData().removeFleetMember(invictus);
        invictus.setCaptain(null);
        invictus.setFlagship(false);
        invictus.getVariant().removeTag(Tags.VARIANT_UNBOARDABLE);
        invictus.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        invictus.getRepairTracker().setCR(invictus.getRepairTracker().getMaxCR());
        invictus.setShipName("Abundant Mercy");

        FleetDataAPI playerFleet = sector.getPlayerFleet().getFleetData();
        playerFleet.addFleetMember(invictus);
        playerFleet.sort();
        sector.getPlayerFleet().forceSync();

        sector.getMemoryWithoutUpdate().set(KoHLoreModPlugin.INVICTUS_GRANTED_KEY, true);
        sector.getMemoryWithoutUpdate().set(FLEET_FOUND_KEY, true);
        sector.getPlayerMemoryWithoutUpdate().set("$koh_lore_invictus_granted", true);

        Misc.fadeAndExpire(mercy);

        if (dialog != null && addText) {
            dialog.getTextPanel().addPara("The commander signs the Mercy over without ceremony. \"You cast away the Devil's bargain,\" they say. \"Then take this ship, and let mercy travel farther than we could carry it.\"");
            dialog.getTextPanel().addPara("The Abundant Mercy has joined your fleet.");
        }
        return true;
    }
}
