package com.elastisys.scale.cloudpool.openstack.requests.lab;

import static java.lang.String.format;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.openstack.driver.OpenStackPoolDriverConfig;
import com.google.common.io.Closeables;

public class AbstractClient {

	static Logger logger = LoggerFactory.getLogger(AbstractClient.class);

	/**
	 * TODO: set to a Java properties file containing the OpenStack credentials.
	 * The property names are assumed to be the same ones as those used by the
	 * python-novaclient (see: https://github.com/openstack/python-novaclient)
	 */
	private static final File CREDENTIALS_FILE = new File(
			System.getProperty("user.home")
					+ "/.elastisys/credentials.properties");

	protected static String getKeystoneEndpoint() {
		return getCredentialProperty("OS_AUTH_URL");
	}

	protected static String getRegionName() {
		return getCredentialProperty("OS_REGION_NAME");
	}

	protected static String getTenantName() {
		return getCredentialProperty("OS_TENANT_NAME");
	}

	protected static String getUserName() {
		return getCredentialProperty("OS_USERNAME");
	}

	protected static String getPassword() {
		return getCredentialProperty("OS_PASSWORD");
	}

	protected static OpenStackPoolDriverConfig getAccountConfig() {
		return new OpenStackPoolDriverConfig(getKeystoneEndpoint(),
				getRegionName(), getTenantName(), getUserName(), getPassword());
	}

	/**
	 * Loads a property from the {@link #CREDENTIALS_FILE}.
	 * 
	 * @param property
	 * @return
	 */
	private static String getCredentialProperty(String property) {
		if (!CREDENTIALS_FILE.isFile()) {
			throw new RuntimeException(format("credentials property file %s "
					+ "does not exist", CREDENTIALS_FILE));
		}
		Properties properties = new Properties();
		FileReader reader = null;
		try {
			reader = new FileReader(CREDENTIALS_FILE);
			properties.load(reader);
			String value = properties.getProperty(property);
			if (value == null) {
				throw new RuntimeException(format(
						"credentials property file %s "
								+ "does not contain property '%s'",
						CREDENTIALS_FILE, property));
			}
			return value;
		} catch (Exception e) {
			throw new RuntimeException(format("failed to load property '%s' "
					+ "from credentials property file %s: %s", property,
					CREDENTIALS_FILE, e.getMessage()), e);
		} finally {
			if (reader != null) {
				Closeables.closeQuietly(reader);
			}
		}
	}

}
