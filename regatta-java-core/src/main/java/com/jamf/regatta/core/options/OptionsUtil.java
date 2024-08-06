/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.options;

import com.jamf.regatta.core.api.ByteSequence;

import java.util.Arrays;

public class OptionsUtil {
    private static final byte[] NO_PREFIX_END = {0};

    private OptionsUtil() {
    }

    /**
     * Gets the range end of the given prefix.
     *
     * <p>
     * The range end is the key plus one (e.g., "aa"+1 == "ab", "a\xff"+1 == "b").
     *
     * @param prefix the given prefix
     * @return the range end of the given prefix
     */
    public static ByteSequence prefixEndOf(ByteSequence prefix) {
        byte[] endKey = prefix.getBytes().clone();
        for (int i = endKey.length - 1; i >= 0; i--) {
            if (endKey[i] != (byte) 0xff) {
                endKey[i] = (byte) (endKey[i] + 1);
                return ByteSequence.from(Arrays.copyOf(endKey, i + 1));
            }
        }

        return ByteSequence.from(NO_PREFIX_END);
    }
}
