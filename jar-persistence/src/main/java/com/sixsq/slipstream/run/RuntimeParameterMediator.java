package com.sixsq.slipstream.run;

import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.persistence.VmRuntimeParameterMapping;
import com.sixsq.slipstream.persistence.RuntimeParameter.SpecialValues;

public class RuntimeParameterMediator {

	public static void processSpecialValue(RuntimeParameter rp) {
		String name = rp.getName();
		SpecialValues sp;
		try {
			sp = SpecialValues.valueOf(name);
		} catch(IllegalArgumentException ex) {
			// no there
			return;
		}
		
		switch(sp) {
			case instanceid:
				setVmInstanceMapping(rp);
				break;
		}
	}

	private static void setVmInstanceMapping(RuntimeParameter rp) {
		VmRuntimeParameterMapping.insertVmInstanceMapping(rp);
	}

}
