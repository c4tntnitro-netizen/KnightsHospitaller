package data.scripts.campaign.intel.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.KoHLoreModPlugin;

import java.awt.Color;

public class KoHLoreGreatLabor extends HubMissionWithSearch {

    public static final String REF_KEY = "$koh_great_labor_ref";
    public static final String COMPLETED_KEY = "$koh_great_labor_completed";

    public static enum Stage {
        GREAT_DEED,
        COMPLETED,
    }

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        if (!setGlobalReference(REF_KEY)) return false;

        setStartingStage(Stage.GREAT_DEED);
        setSuccessStage(Stage.COMPLETED);
        setStageOnGlobalFlag(Stage.COMPLETED, COMPLETED_KEY);
        setNoAbandon();
        setStoryMission();
        setRepFactionChangesNone();
        setRepPersonChangesNone();

        return true;
    }

    @Override
    protected void updateInteractionDataImpl() {
    }

    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        info.addPara("The Knights Hospitaller have asked you to accomplish a Great Labor. They offered no contract, no bounty marker, and no coordinates. When the hour comes, you will know whether the deed is yours.", 10f);
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        if (currentStage == Stage.GREAT_DEED) {
            info.addPara("Accomplish a great deed", tc, pad);
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
    public String getIcon() {
        return "graphics/icons/missions/pilgrims_path.png";
    }

    @Override
    public String getBaseName() {
        return "The Great Labor";
    }

    @Override
    public String getPostfixForState() {
        return "";
    }
}
