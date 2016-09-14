package com.elastisys.scale.cloudpool.aws.spot.driver;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.amazonaws.ClientConfiguration;

public class IsClientConfigMatcher extends BaseMatcher<ClientConfiguration> {

    private final Integer expectedConnectionTimeout;
    private final Integer expectedSocketTimeout;

    public IsClientConfigMatcher(Integer execptedConnectionTimeout, Integer execptedSocketTimeout) {
        this.expectedConnectionTimeout = execptedConnectionTimeout;
        this.expectedSocketTimeout = execptedSocketTimeout;
    }

    @Override
    public boolean matches(Object item) {
        if (!ClientConfiguration.class.isAssignableFrom(item.getClass())) {
            return false;
        }
        ClientConfiguration actual = ClientConfiguration.class.cast(item);
        return actual.getConnectionTimeout() == this.expectedConnectionTimeout
                && actual.getSocketTimeout() == this.expectedSocketTimeout;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("ClientConfiguration with connectionTimeout '%d' and socketTimeout '%d'",
                this.expectedConnectionTimeout, this.expectedSocketTimeout));
    }

    public static Matcher<ClientConfiguration> isClientConfig(int expectedConnectionTimeout,
            int expectedSocketTimeout) {
        return new IsClientConfigMatcher(expectedConnectionTimeout, expectedSocketTimeout);
    }

}
