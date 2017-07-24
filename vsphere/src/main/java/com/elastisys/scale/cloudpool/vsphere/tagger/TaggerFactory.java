package com.elastisys.scale.cloudpool.vsphere.tagger;

import com.elastisys.scale.cloudpool.vsphere.tagger.impl.CustomAttributeTagger;

public class TaggerFactory {

    private TaggerFactory() {}

    public static Tagger getTagger(){
        return new CustomAttributeTagger();
    }
}
