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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class KoHLoreModPlugin extends BaseModPlugin {

    public static final String FLEET_MEMORY_KEY = "$koh_lore_invictus_fleet";
    public static final String SPAWNED_KEY = "$koh_lore_invictus_spawned";
    public static final String ATTACKED_MERCY_KEY = "$koh_lore_attacked_mercy";
    public static final String INVICTUS_GRANTED_KEY = "$koh_lore_invictus_granted";
    public static final String PEACEFUL_HANDOFF_KEY = "$koh_lore_peaceful_invictus_handoff";
    public static final String GETHSEMANE_LISTENER_KEY = "$koh_lore_gethsemane_listener_installed";
    public static final String COMPOSITION_KEY = "$koh_lore_hospitaller_composition_v2";
    public static final String KH_FACTION_ID = "knights_hospitaller";
    public static final String FLEET_NAME = "Knights Hospitaller Mercy";
    public static final float MERCY_ATTACK_REP_DELTA = -0.5f;

    private static final Logger log = Global.getLogger(KoHLoreModPlugin.class);

    private static final Set<String> VANILLA_FLOOR_FACTIONS = new HashSet<String>(Arrays.asList(
            "hegemony", "tritachyon", "sindrian_diktat", "persean_league",
            "luddic_church", "luddic_path", "independent", "pirates",
            "knights_of_ludd", "lions_guard"
    ));

    private static final Set<String> NO_DIPLOMACY_FACTIONS = new HashSet<String>(Arrays.asList(
            "derelict", "remnants", "omega", "threat", "sleeper",
            "dweller", "neutral", KH_FACTION_ID, "player"
    ));

    @Override
    public void onNewGameAfterProcGen() {
        setupRelations(true);
        installGethsemaneListener();
        spawnFleetIfNeeded();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        setupRelations(newGame);
        installGethsemaneListener();
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
            CampaignFleetAPI existing = findHospitallerFleet();
            if (existing != null) {
                Global.getSector().getMemoryWithoutUpdate().set(SPAWNED_KEY, true);
                ensureHospitallerFleetComposition(existing);
                ensureMercyListener(existing);
                return;
            }
            spawnFleet();
        } catch (Exception e) {
            log.error("KoHLore: failed to spawn Hospitaller Invictus", e);
        }
    }

    private void setupRelations(boolean firstTime) {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;

        FactionAPI kh = sector.getFaction(KH_FACTION_ID);
        if (kh == null) {
            log.warn("KoHLore: faction " + KH_FACTION_ID + " not loaded; skipping relations setup");
            return;
        }

        kh.setRelationship(Factions.LUDDIC_CHURCH, com.fs.starfarer.api.campaign.RepLevel.FRIENDLY);

        for (FactionAPI other : sector.getAllFactions()) {
            if (other == null || other.getId() == null) continue;
            String id = other.getId();
            if (id.equals(KH_FACTION_ID) || NO_DIPLOMACY_FACTIONS.contains(id)) continue;

            if (Factions.LUDDIC_CHURCH.equals(id)) {
                continue;
            }
            if (Factions.PIRATES.equals(id) || Factions.LUDDIC_PATH.equals(id)) {
                kh.setRelationship(id, com.fs.starfarer.api.campaign.RepLevel.NEUTRAL);
                continue;
            }
            if (firstTime) {
                kh.setRelationship(id, com.fs.starfarer.api.campaign.RepLevel.FRIENDLY);
            } else if (VANILLA_FLOOR_FACTIONS.contains(id) && kh.getRelationship(id) < 0f) {
                kh.setRelationship(id, com.fs.starfarer.api.campaign.RepLevel.NEUTRAL);
            }
        }
    }

    private void installGethsemaneListener() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        if (sector.getMemoryWithoutUpdate().getBoolean(GETHSEMANE_LISTENER_KEY)) return;

        GethsemaneListener listener = new GethsemaneListener();
        sector.addScript(listener);
        sector.getListenerManager().addListener(listener);
        sector.getMemoryWithoutUpdate().set(GETHSEMANE_LISTENER_KEY, true);
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

        FactionAPI faction = sector.getFaction(KH_FACTION_ID);
        if (faction == null) {
            log.warn("KoHLore: faction " + KH_FACTION_ID + " not loaded; cannot spawn Hospitaller Invictus");
            return;
        }

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(KH_FACTION_ID, FLEET_NAME, true);
        populateHospitallerFleet(fleet, faction, new Random());

        canaan.addEntity(fleet);
        fleet.setLocation(gilead.getLocation().x + 300f, gilead.getLocation().y);
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, gilead, 999999f, "standing relief watch");

        sector.getMemoryWithoutUpdate().set(SPAWNED_KEY, true);
        log.info("KoHLore: spawned Hospitaller Invictus around Gilead");
    }

    private void ensureHospitallerFleetComposition(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        if (fleet.getMemoryWithoutUpdate().getBoolean(COMPOSITION_KEY)) return;

        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        FactionAPI faction = sector.getFaction(KH_FACTION_ID);
        if (faction == null) {
            log.warn("KoHLore: faction " + KH_FACTION_ID + " not loaded; cannot rebuild existing Mercy fleet");
            return;
        }

        populateHospitallerFleet(fleet, faction, new Random());
        log.info("KoHLore: rebuilt existing Mercy fleet with Hospitaller Invictus composition");
    }

    private void populateHospitallerFleet(CampaignFleetAPI fleet, FactionAPI faction, Random random) {
        fleet.setFaction(KH_FACTION_ID, true);

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            fleet.getFleetData().removeFleetMember(member);
        }

        PersonAPI commander = OfficerManagerEvent.createOfficer(
                faction, 7,
                OfficerManagerEvent.SkillPickPreference.ANY,
                false, null, true, true, 1, random);
        commander.setRankId(Ranks.SPACE_ADMIRAL);
        commander.setPostId(Ranks.POST_FLEET_COMMANDER);
        commander.setPersonality(Personalities.STEADY);
        fleet.setCommander(commander);

        FleetMemberAPI invictus = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "invictus_kh_Strike");
        fleet.getFleetData().addFleetMember(invictus);
        fleet.getFleetData().ensureHasFlagship();
        fleet.getFlagship().setCaptain(commander);
        fleet.getFlagship().setVariant(fleet.getFlagship().getVariant(), false, true);
        fleet.getFlagship().getVariant().setSource(VariantSource.REFIT);
        fleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        fleet.getFlagship().setShipName("Abundant Mercy");

        for (int i = 0; i < 3; i++) {
            addShip(fleet, "legion_Strike");
        }
        for (int i = 0; i < 6; i++) {
            addShip(fleet, "mora_Assault");
        }
        for (int i = 0; i < 10; i++) {
            addShip(fleet, "condor_Support");
        }
        assignHospitallerOfficers(fleet, faction, random);

        fleet.getMemoryWithoutUpdate().set(FLEET_MEMORY_KEY, true);
        fleet.getMemoryWithoutUpdate().set(COMPOSITION_KEY, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleet.setTransponderOn(true);
        Misc.makeImportant(fleet, "koh_lore");
        ensureMercyListener(fleet);

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }
        fleet.getFleetData().sort();
        fleet.forceSync();
    }

    private void addShip(CampaignFleetAPI fleet, String variantId) {
        fleet.getFleetData().addFleetMember(
                Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId));
    }

    private void assignHospitallerOfficers(CampaignFleetAPI fleet, FactionAPI faction, Random random) {
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getCaptain() != null && !member.getCaptain().isDefault()) continue;
            int level = 3 + random.nextInt(3);
            PersonAPI officer = OfficerManagerEvent.createOfficer(
                    faction, level,
                    OfficerManagerEvent.SkillPickPreference.ANY,
                    false, null, true, true, 1, random);
            officer.setPersonality(Personalities.STEADY);
            member.setCaptain(officer);
            fleet.getFleetData().addOfficer(officer);
        }
    }

    public static void ensureMercyListener(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof MercyFleetListener) return;
        }
        fleet.addEventListener(new MercyFleetListener());
    }

    public static void markPeacefulInvictusHandoff(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        fleet.getMemoryWithoutUpdate().set(PEACEFUL_HANDOFF_KEY, true);
        fleet.getMemoryWithoutUpdate().unset(FLEET_MEMORY_KEY);
        Misc.makeUnimportant(fleet, "koh_lore");
        FleetEventListener toRemove = null;
        for (FleetEventListener listener : fleet.getEventListeners()) {
            if (listener instanceof MercyFleetListener) {
                toRemove = listener;
                break;
            }
        }
        if (toRemove != null) {
            fleet.removeEventListener(toRemove);
        }
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
            if (fleet.getMemoryWithoutUpdate().getBoolean(PEACEFUL_HANDOFF_KEY)) return;
            if (Global.getSector().getMemoryWithoutUpdate().getBoolean(INVICTUS_GRANTED_KEY)) return;
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
