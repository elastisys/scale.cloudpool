package com.elastisys.scale.cloudpool.vsphere.tagger;

import com.elastisys.scale.cloudpool.vsphere.tagger.impl.CustomAttributeTagger;

/**
 * This class makes it possible to get a Tagger without specifying which implementation to use.
 */
public class TaggerFactory {

    // Make sure no one can instantiate the TaggerFactory.
    private TaggerFactory() {}

    /**
     * Get a Tagger. Note that some implementations may require initialization of the tagger.
     * @return the Tagger
     */
    public static Tagger getTagger(){
        return new CustomAttributeTagger();
    }
}
