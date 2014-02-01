package com.sixsq.slipstream.persistence;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hibernate.LazyInitializationException;
import org.junit.Test;

import com.sixsq.slipstream.exceptions.AbortException;
import com.sixsq.slipstream.exceptions.NotFoundException;
import com.sixsq.slipstream.exceptions.ValidationException;

public class RunTest {

	@Test
	public void loadWithRuntimeParameters() throws ValidationException, NotFoundException, AbortException {
		Module image = new ImageModule();

		Run run = new Run(image, RunType.Run, "test", new User("user"));
		
		run.assignRuntimeParameter("ss:key", "value", "description");
		
		run.store();
		
		run = Run.loadFromUuid(run.getUuid());
		
		try {
			run.getRuntimeParameterValue("ss:key");
			fail();
		} catch (LazyInitializationException ex) {
		}
		
		run = Run.loadRunWithRuntimeParameters(run.getUuid());

		assertThat(run.getRuntimeParameterValue("ss:key"), is("value"));
	}
}
