package com.sixsq.slipstream.run;

import com.sixsq.slipstream.exceptions.SlipStreamClientException;
import com.sixsq.slipstream.persistence.Run;
import sixsq.slipstream.prs.core.JavaWrapper;

import java.util.logging.Logger;

/**
 * Validates if a run fullfills placement policy.
 *
 */
public class PlacementValidator {

    private static Logger logger = Logger.getLogger(PlacementValidator.class.getName());

    public static void validate(Run run) throws SlipStreamClientException {
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
