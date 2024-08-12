/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.resolver;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import io.grpc.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class HttpNameResolver extends AbstractNameResolver {
    public static final String SCHEME = "http";

    private final URI address;

    public HttpNameResolver(URI targetUri) {
        super(targetUri);

        this.address = targetUri;
    }

    @Override
    protected List<EquivalentAddressGroup> computeAddressGroups() {
        if (address == null) {
            throw Status.INVALID_ARGUMENT.withDescription("Unable to resolve endpoint " + getTargetUri()).asRuntimeException();
        }

        return Collections.singletonList(
                new EquivalentAddressGroup(
                        new InetSocketAddress(
                                address.getHost(),
                                address.getPort() != -1 ? address.getPort() : REGATTA_CLIENT_PORT),
                        Strings.isNullOrEmpty(getServiceAuthority())
                                ? Attributes.newBuilder()
                                .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, address.toString())
                                .build()
                                : Attributes.EMPTY));
    }

    @AutoService(NameResolverProvider.class)
    public static class HttpResolverProvider extends AbstractResolverProvider {
        public HttpResolverProvider() {
            super(HttpNameResolver.SCHEME, 5);
        }

        @Override
        protected NameResolver createResolver(URI targetUri, NameResolver.Args args) {
            return new HttpNameResolver(targetUri);
        }
    }
}


