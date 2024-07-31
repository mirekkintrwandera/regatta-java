/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.ssl;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.SSLFactoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class JksSSLFactoryRefresher implements SSLFactoryRefresher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JksSSLFactoryRefresher.class);

    private final Path identityStorePath;
    private final Path trustStorePath;
    private final char[] identityStorePassword;
    private final char[] trustStorePassword;

    private ZonedDateTime lastModifiedTimeIdentityStore = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
    private ZonedDateTime lastModifiedTimeTrustStore = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);

    private final SSLFactory baseSslFactory;

    public JksSSLFactoryRefresher(SSLFactory baseSslFactory, Path trustStorePath, Path identityStorePath, char[] trustStorePassword, char[] identityStorePassword) {
        this.baseSslFactory = baseSslFactory;
        this.identityStorePath = identityStorePath;
        this.trustStorePath = trustStorePath;
        this.identityStorePassword = identityStorePassword;
        this.trustStorePassword = trustStorePassword;
        this.tryRefresh();
    }

    public void tryRefresh() {
        try {
            if (Files.exists(identityStorePath) && Files.exists(trustStorePath)) {
                BasicFileAttributes identityAttributes = Files.readAttributes(identityStorePath, BasicFileAttributes.class);
                BasicFileAttributes trustStoreAttributes = Files.readAttributes(trustStorePath, BasicFileAttributes.class);

                boolean identityUpdated = lastModifiedTimeIdentityStore.isBefore(ZonedDateTime.ofInstant(identityAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));
                boolean trustStoreUpdated = lastModifiedTimeTrustStore.isBefore(ZonedDateTime.ofInstant(trustStoreAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));

                if (identityUpdated || trustStoreUpdated) {
                    LOGGER.info("Keystore files have been changed. Trying to read the file content and preparing to update the ssl material");

                    SSLFactory updatedSslFactory = SSLFactory.builder()
                            .withIdentityMaterial(identityStorePath, identityStorePassword)
                            .withTrustMaterial(trustStorePath, trustStorePassword)
                            .build();

                    SSLFactoryUtils.reload(baseSslFactory, updatedSslFactory);

                    lastModifiedTimeIdentityStore = ZonedDateTime.ofInstant(identityAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                    lastModifiedTimeTrustStore = ZonedDateTime.ofInstant(trustStoreAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);

                    LOGGER.info("Updating ssl material finished");
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to refresh ssl material", e);
        }
    }

}
