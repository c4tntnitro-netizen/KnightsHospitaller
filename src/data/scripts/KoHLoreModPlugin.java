package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

import org.apache.log4j.Logger;

import java.util.Random;

public class KoHLoreModPlugin extends BaseModPlugin {

    public static final String FLEET_MEMORY_KEY = "$koh_lore_invictus_fleet";
    public static final String SPAWNED_KEY = "$koh_lore_invictus_spawned";
    public static final String FLEET_NAME = "Knights Hospitaller Mercy";

    private static final Logger log = Global.getLogger(KoHLoreModPlugin.class);

    @Override
    public void onNewGameAfterProcGen() {
        spawnFleetIfNeeded();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        spawnFleetIfNeeded();
    }

    public static CampaignFleetAPI findHospitallerFleet() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return null;

        for (StarSystemAPI system : sector.getStarSystems()) {
            for (CampaignFleetAPI fleet : system.getFleets()) {
                if (fleet.getMemoryWithoutUpdate().getBoolean(FLEET_MEMORY_KEY)) {
                    return fleet;
                }
            }
        }
        return null;
    }

    private void spawnFleetIfNeeded() {
        try {
            if (findHospitallerFleet() != null) {
                Global.getSector().getMemoryWithoutUpdate().set(SPAWNED_KEY, true);
                return;
            }
            spawnFleet();
        } catch (Exception e) {
            log.error("KoHLore: failed to spawn Hospitaller Invictus", e);
        }
    }

    private void spawnFleet() {
        SectorAPI sector = Global.getSector();
        StarSystemAPI canaan = sector.getStarSystem("Canaan");
        if (canaan == null) {
            log.warn("KoHLore: Canaan not found; cannot spawn Hospitaller Invictus");
            return;
        }

        SectorEntityToken gilead = canaan.getEntityById("gilead");
        if (gilead == null) {
            for (SectorEntityToken planet : canaan.getPlanets()) {
                if ("Gilead".equals(planet.getName())) {
                    gilead = planet;
                    break;
                }
            }
        }
        if (gilead == null) {
            log.warn("KoHLore: Gilead not found; cannot spawn Hospitaller Invictus");
            return;
        }

        Random random = new Random();
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(Factions.LUDDIC_CHURCH, FLEET_NAME, true);
        fleet.setFaction(Factions.LUDDIC_CHURCH, true);

        PersonAPI commander = OfficerManagerEvent.createOfficer(
                sector.getFaction(Factions.LUDDIC_CHURCH), 6,
                OfficerManagerEvent.SkillPickPreference.ANY,
                false, null, true, true, 1, random);
        commander.setRankId(Ranks.SPACE_COMMANDER);
        commander.setPostId(Ranks.POST_FLEET_COMMANDER);
        commander.setPersonality(Personalities.STEADY);
        fleet.setCommander(commander);

        FleetMemberAPI invictus = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "invictus_Standard");
        fleet.getFleetData().addFleetMember(invictus);
        fleet.getFleetData().ensureHasFlagship();
        fleet.getFlagship().setCaptain(commander);
        fleet.getFlagship().setVariant(fleet.getFlagship().getVariant(), false, true);
        fleet.getFlagship().getVariant().setSource(VariantSource.REFIT);
        fleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        fleet.getFlagship().setShipName("Mercy of Gilead");

        fleet.getMemoryWithoutUpdate().set(FLEET_MEMORY_KEY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleet.setTransponderOn(true);
        Misc.makeImportant(fleet, "koh_lore");

        fleet.getFlagship().getRepairTracker().setCR(fleet.getFlagship().getRepairTracker().getMaxCR());
        fleet.getFleetData().sort();
        fleet.forceSync();

        canaan.addEntity(fleet);
        fleet.setLocation(gilead.getLocation().x + 300f, gilead.getLocation().y);
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, gilead, 999999f, "standing relief watch");

        sector.getMemoryWithoutUpdate().set(SPAWNED_KEY, true);
        log.info("KoHLore: spawned Hospitaller Invictus around Gilead");
    }
}
