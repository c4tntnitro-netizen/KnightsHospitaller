package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class GethsemaneListener extends BaseCampaignEventListener implements EveryFrameScript {

    public static final String HULLMOD_ID = "gethsemane_rescue_bay";
    public static final float CASUALTY_REDUCTION = 0.8f;

    private int crewSnapshot = -1;

    public GethsemaneListener() {
        super(false);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        crewSnapshot = fleet.getCargo().getCrew();
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return;
        if (!fleetHasHullmod(fleet, HULLMOD_ID)) return;
        if (crewSnapshot < 0) return;

        int currentCrew = fleet.getCargo().getCrew();
        int lost = crewSnapshot - currentCrew;
        if (lost <= 0) {
            crewSnapshot = currentCrew;
            return;
        }

        int refund = Math.round(lost * CASUALTY_REDUCTION);
        if (refund > 0) {
            fleet.getCargo().addCrew(refund);
        }
        crewSnapshot = fleet.getCargo().getCrew();
    }

    private boolean fleetHasHullmod(CampaignFleetAPI fleet, String id) {
        for (FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            if (m.getVariant() != null && m.getVariant().hasHullMod(id)) return true;
        }
        return false;
    }
}
