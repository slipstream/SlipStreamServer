package com.sixsq.slipstream.run;

import org.restlet.data.Cookie;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.sixsq.slipstream.cookie.CookieUtils;
import com.sixsq.slipstream.persistence.Run;
import com.sixsq.slipstream.persistence.RuntimeParameter;
import com.sixsq.slipstream.resource.BaseResource;

public abstract class RunBaseResource extends BaseResource {

	private String uuid;

	private boolean ignoreAbort;

	@Override
	public final void initialize() throws ResourceException {
		extractAndSetRunUuid();
		initializeSubResource();
	}

	protected abstract void initializeSubResource();

	@Override
	protected String getPageRepresentation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isMachineAllowedToAccessThisResource() {
		Cookie cookie = CookieUtils.extractAuthnCookie(getRequest());
		return uuid.equals(CookieUtils.getRunId(cookie));
	}

	protected void extractAndSetRunUuid() {
		uuid = getAttribute("uuid");
	}

	protected String getUuid() {
		return uuid;
	}

	protected void extractAndSetIgnoreAbort() {
		String ignoreAbortVal = (String) getRequest().getAttributes().get(
				RunListResource.IGNORE_ABORT_QUERY);
		ignoreAbort = Boolean.parseBoolean(ignoreAbortVal);
	}

	protected boolean getIgnoreAbort() {
		return ignoreAbort;
	}

	protected boolean isAbortSet() {
		return getGlobalAbort().isSet();
	}

	private RuntimeParameter getGlobalAbort() {
		return loadRuntimeParameter(RuntimeParameter.GLOBAL_ABORT_KEY);
	}

	protected RuntimeParameter loadRuntimeParameter(String key) {
		RuntimeParameter rp = RuntimeParameter.loadFromUuidAndKey(getUuid(), key);
		if (rp == null) {
			Run run = Run.loadFromUuid(getUuid());
			if (run == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"Unknown run id " + getUuid());
			} else {
				String error = "Unknown key " + key;
				String nodename = RuntimeParameter.extractNodeNamePart(key);
				Run.abortOrReset(error, nodename, getUuid());
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						error);
			}
		}
		return rp;
	}

}
