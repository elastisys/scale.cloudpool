package com.elastisys.scale.cloudpool.commons.testutils.sshserver;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public key authenticator for {@link SshServer}s that accepts <i>any</i>
 * password login attempt. For test purposes only.
 * 
 * @see PermissiveSshServer
 * 
 * 
 * 
 */
public class PermissivePasswordAuthenticator implements PasswordAuthenticator {

	static final Logger LOG = LoggerFactory
			.getLogger(PermissivePasswordAuthenticator.class);

	@Override
	public boolean authenticate(String username, String password,
			ServerSession session) {
		LOG.info("password login attempt by '{}'", username);
		return true;
	}
}
