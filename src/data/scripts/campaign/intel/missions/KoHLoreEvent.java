package data.scripts.campaign.intel.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.KoHLoreModPlugin;

import java.awt.Color;

public class KoHLoreEvent extends HubMissionWithBarEvent {

    public static final String MARKET_ID = "kantas_den";
    public static final String REF_KEY = "$koh_lore_ref";
    public static final String STARTED_KEY = "$koh_lore_kantas_den_started";
    public static final String DONE_KEY = "$koh_lore_fleet_found";

    public static enum Stage {
        FIND_FLEET,
        COMPLETED,
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        if (market == null) return false;
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(STARTED_KEY)) return false;
        if (Global.getSector().getMemoryWithoutUpdate().getBoolean(DONE_KEY)) return false;
        return MARKET_ID.equals(market.getId());
    }

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        if (!barEvent) return false;
        if (!shouldShowAtMarket(createdAt)) return false;

        setGiverRank(Ranks.SPACE_COMMANDER);
        setGiverPost(Ranks.POST_MEDICAL_SUPPLIER);
        setGiverImportance(PersonImportance.MEDIUM);
        setGiverFaction(Factions.LUDDIC_CHURCH);
        setGiverTags(Tags.CONTACT_MILITARY);
        setGiverVoice(Voices.SOLDIER);
        findOrCreateGiver(createdAt, false, false);

        PersonAPI person = getPerson();
        if (person == null) return false;
        if (!setPersonMissionRef(person, REF_KEY)) return false;
        if (!setGlobalReference(REF_KEY)) return false;

        setStartingStage(Stage.FIND_FLEET);
        setSuccessStage(Stage.COMPLETED);
        setStageOnGlobalFlag(Stage.COMPLETED, DONE_KEY);
        setNoAbandon();
        setRepFactionChangesNone();
        setRepPersonChangesNone();

        return true;
    }

    @Override
    protected void updateInteractionDataImpl() {
        set("$koh_lore_manOrWoman", getPerson().getManOrWoman());
        set("$koh_lore_heOrShe", getPerson().getHeOrShe());
        set("$koh_lore_hisOrHer", getPerson().getHisOrHer());
    }

    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        info.addPara("The Hospitaller at Kanta's Den spoke of a relief ship standing watch above Gilead. Find the white-cloaked order's Invictus in the Canaan system.", 10f);
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        if (currentStage == Stage.FIND_FLEET) {
            info.addPara("Find the Hospitaller Invictus at Gilead", tc, pad);
            return true;
        }
        return false;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        CampaignFleetAPI fleet = KoHLoreModPlugin.findHospitallerFleet();
        if (fleet != null) return fleet;
        MarketAPI gilead = Global.getSector().getEconomy().getMarket("gilead");
        if (gilead != null) return gilead.getPrimaryEntity();
        return null;
    }

    @Override
    public String getBaseName() {
        return "Knights Hospitaller";
    }
}
