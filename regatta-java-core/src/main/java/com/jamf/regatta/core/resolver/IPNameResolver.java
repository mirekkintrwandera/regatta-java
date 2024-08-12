/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.resolver;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.grpc.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IPNameResolver extends AbstractNameResolver {
    public static final String SCHEME = "ip";

    private final List<HostAndPort> addresses;

    public IPNameResolver(URI targetUri) {
        super(targetUri);

        this.addresses = Stream.of(targetUri.getPath().split(","))
                .map(address -> address.startsWith("/") ? address.substring(1) : address)
                .map(HostAndPort::fromString)
                .collect(Collectors.toList());
    }

    @Override
    protected List<EquivalentAddressGroup> computeAddressGroups() {
        if (addresses.isEmpty()) {
            throw Status.INVALID_ARGUMENT.withDescription("Unable to resolve endpoint " + getTargetUri()).asRuntimeException();
        }

        return addresses.stream()
                .map(address -> new EquivalentAddressGroup(
                        new InetSocketAddress(
                                address.getHost(),
                                address.getPortOrDefault(REGATTA_CLIENT_PORT)),
                        Strings.isNullOrEmpty(getServiceAuthority())
                                ? Attributes.newBuilder()
                                .set(EquivalentAddressGroup.ATTR_AUTHORITY_OVERRIDE, address.toString())
                                .build()
                                : Attributes.EMPTY))
                .collect(Collectors.toList());
    }

    @AutoService(NameResolverProvider.class)
    public static class IPResolverProvider extends AbstractResolverProvider {
        public IPResolverProvider() {
            super(IPNameResolver.SCHEME, 5);
        }

        @Override
        protected NameResolver createResolver(URI targetUri, NameResolver.Args args) {
            return new IPNameResolver(targetUri);
        }
    }
}
