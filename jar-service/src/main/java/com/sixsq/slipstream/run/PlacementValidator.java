package com.sixsq.slipstream.run;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.ui.UIPlacementResource;
import sixsq.slipstream.prs.core.JavaWrapper;

import java.util.logging.Logger;

/**
 * Validates if a run fullfills placement policy.
 *
 */
public class PlacementValidator {

    private static Logger logger = Logger.getLogger(PlacementValidator.class.getName());


    public static void validate(Run run) throws SlipStreamClientException {

        if(!UIPlacementResource.isPlacementEnabled()) {
            logger.info("Bypassing call to PRS-lib, as Placement is not enabled");
            return;
        }

        logger.info("Calling PRS-lib for module : " + run.getModule().getResourceUri());

        Boolean valid;
        try {
            // TODO provide PRS endpoint to PRS lib
            valid = JavaWrapper.validatePlacement(run);
        } catch (Exception e) {
            throw new SlipStreamClientException("Failed to validate placement for module "
                    + run.getModule().getResourceUri() + ". Cause : " + e.getMessage());
        }
        if (!valid) {
            throw new SlipStreamClientException("No valid placement for module " + run.getModule().getResourceUri());
        }
    }

}
