package com.sixsq.slipstream.run;

import com.sixsq.slipstream.configuration.Configuration;
import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.exceptions.ValidationException;
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

    private static boolean isPlacementEnabled = false;
    static {
        try {
            isPlacementEnabled = Configuration.isEnabled(UIPlacementResource.PRS_ENABLED_PROPERTY_KEY);
            logger.info("Placement Server enabled ? " + isPlacementEnabled);
        } catch (ValidationException ve) {
            logger.severe("Unable to access configuration to determine if Placement is enabled. Cause: " + ve.getMessage());
        }
    }

    public static void validate(Run run) throws SlipStreamClientException {

        if(!isPlacementEnabled) {
            logger.info("Bypassing call to PRS-lib, as Placement is not enabled");
            return;
        }

        logger.info("Calling PRS-lib with run : " + run);

        Boolean valid;
        try {
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
