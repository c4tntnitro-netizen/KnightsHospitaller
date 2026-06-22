package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class GethsemaneRescueBay extends BaseHullMod {

    public static final String RECOVERY_SHUTTLES_ID = "recovery_shuttles";
    public static final float FIGHTER_CREW_LOSS_MULT = 0.2f; // 80% reduction

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getStat(Stats.FIGHTER_CREW_LOSS_MULT).modifyMult(id, FIGHTER_CREW_LOSS_MULT);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship == null
                || !ship.getVariant().getHullMods().contains(RECOVERY_SHUTTLES_ID);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship != null && ship.getVariant().getHullMods().contains(RECOVERY_SHUTTLES_ID)) {
            return "Incompatible with Recovery Shuttles";
        }
        return null;
    }
}
