package org.ballerinalang.stdlib.cache.nativeimpl;

import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BString;

/**
 *
 */
public class Constant {
    static final BString VALUE = BStringUtils.fromString("value");
    static final BString PREV = BStringUtils.fromString("prev");
    static final BString NEXT = BStringUtils.fromString("next");
    static final BString KEY = BStringUtils.fromString("key");
    static final BString EXP_TIME = BStringUtils.fromString("expTime");
    static final BString DATA = BStringUtils.fromString("data");
    static final String HEAD = "head";
    static final String TAIL = "tail";
}
