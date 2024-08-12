/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.resolver;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;
import java.util.Objects;

public abstract class AbstractResolverProvider extends NameResolverProvider {
    private final String scheme;
    private final int priority;

    public AbstractResolverProvider(String scheme, int priority) {
        this.scheme = scheme;
        this.priority = priority;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return priority;
    }

    @Override
    public String getDefaultScheme() {
        return scheme;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return Objects.equals(scheme, targetUri.getScheme())
                ? createResolver(targetUri, args)
                : null;
    }

    protected abstract NameResolver createResolver(URI targetUri, NameResolver.Args args);
}
