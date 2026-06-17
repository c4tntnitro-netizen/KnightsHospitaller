package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
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
    public static final String ATTACKED_MERCY_KEY = "$koh_lore_attacked_mercy";
    public static final String INVICTUS_GRANTED_KEY = "$koh_lore_invictus_granted";
    public static final String FLEET_NAME = "Knights Hospitaller Mercy";
    public static final float MERCY_ATTACK_REP_DELTA = -0.5f;

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
            enableBlackHoleInteractions();
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean(INVICTUS_GRANTED_KEY) ||
                    Global.getSector().getMemoryWithoutUpdate().getBoolean(ATTACKED_MERCY_KEY)) {
                return;
            }
            if (findHospitallerFleet() != null) {
                Global.getSector().getMemoryWithoutUpdate().set(SPAWNED_KEY, true);
                ensureMercyListener(findHospitallerFleet());
                return;
            }
            spawnFleet();
        } catch (Exception e) {
            log.error("KoHLore: failed to spawn Hospitaller Invictus", e);
        }
    }

    private void enableBlackHoleInteractions() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;

        for (StarSystemAPI system : sector.getStarSystems()) {
            for (PlanetAPI planet : system.getPlanets()) {
                if (!planet.isBlackHole()) continue;
                planet.addTag(Tags.HAS_INTERACTION_DIALOG);
                planet.removeTag(Tags.NON_CLICKABLE);
            }
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
        ensureMercyListener(fleet);

        fleet.getFlagship().getRepairTracker().setCR(fleet.getFlagship().getRepairTracker().getMaxCR());
        fleet.getFleetData().sort();
        fleet.forceSync();

        canaan.addEntity(fleet);
        fleet.setLocation(gilead.getLocation().x + 300f, gilead.getLocation().y);
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, gilead, 999999f, "standing relief watch");

        sector.getMemoryWithoutUpdate().set(SPAWNED_KEY, true);
        log.info("KoHLore: spawned Hospitaller Invictus around Gilead");
    }

    public static void ensureMercyListener(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof MercyFleetListener) return;
        }
        fleet.addEventListener(new MercyFleetListener());
    }

    public static void applyMercyAttackPenalty() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        if (sector.getMemoryWithoutUpdate().getBoolean(ATTACKED_MERCY_KEY)) return;

        sector.getMemoryWithoutUpdate().set(ATTACKED_MERCY_KEY, true);
        int atrocities = sector.getPlayerMemoryWithoutUpdate().getInt("$atrocities");
        sector.getPlayerMemoryWithoutUpdate().set("$atrocities", atrocities + 2);

        for (FactionAPI faction : sector.getAllFactions()) {
            if (faction == null) continue;
            if (faction.isPlayerFaction() || faction.isNeutralFaction()) continue;
            if (!faction.isShowInIntelTab()) continue;
            if (Factions.PLAYER.equals(faction.getId()) || Factions.NEUTRAL.equals(faction.getId())) continue;

            faction.adjustRelationship(Factions.PLAYER, MERCY_ATTACK_REP_DELTA);
            faction.getMemoryWithoutUpdate().set(MemFlags.FACTION_SATURATION_BOMBARED_BY_PLAYER, true);
        }

        if (sector.getCampaignUI() != null) {
            sector.getCampaignUI().addMessage(
                    "Your attack on the Knights Hospitaller Mercy is condemned as an atrocity across the Sector.",
                    Misc.getNegativeHighlightColor());
        }
    }

    public static class MercyFleetListener implements FleetEventListener {
        @Override
        public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
            if (fleet == null || battle == null) return;
            if (!fleet.getMemoryWithoutUpdate().getBoolean(FLEET_MEMORY_KEY)) return;
            if (!battle.isPlayerInvolved() || !battle.isInvolved(fleet)) return;
            if (battle.onPlayerSide(fleet)) return;

            applyMercyAttackPenalty();
        }

        @Override
        public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
            if (fleet != null) {
                fleet.removeEventListener(this);
            }
        }
    }
}
