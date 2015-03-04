package com.elastisys.scale.cloudpool.commons.testutils.sshserver;

import java.security.PublicKey;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public key authenticator for {@link SshServer}s that accepts <i>any</i>
 * public key login attempt. For test purposes only.
 * 
 * @see PermissiveSshServer
 * 
 * 
 * 
 */
public class PermissivePublicKeyAuthenticator implements PublickeyAuthenticator {

	static final Logger LOG = LoggerFactory
			.getLogger(PermissivePublicKeyAuthenticator.class);

	@Override
	public boolean authenticate(String username, PublicKey key,
			ServerSession session) {
		LOG.info("public key login attempt by '{}'", username);
		return true;
	}
}
