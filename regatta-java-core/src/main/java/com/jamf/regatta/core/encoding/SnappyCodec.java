/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.encoding;

import io.grpc.Compressor;
import io.grpc.Decompressor;
import org.xerial.snappy.SnappyFramedInputStream;
import org.xerial.snappy.SnappyFramedOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class SnappyCodec implements Compressor, Decompressor {

    public static final SnappyCodec INSTANCE = new SnappyCodec();

    public static final String NAME = "snappy";

    @Override
    public String getMessageEncoding() {
        return NAME;
    }

    @Override
    public InputStream decompress(InputStream is) throws IOException {
        return new SnappyFramedInputStream(is);
    }

    @Override
    public OutputStream compress(OutputStream os) throws IOException {
        return new SnappyFramedOutputStream(os);
    }
}
