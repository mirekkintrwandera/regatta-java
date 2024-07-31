/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.core.ssl;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;
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

public class PemSSLFactoryRefresher implements SSLFactoryRefresher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PemSSLFactoryRefresher.class);

    private final SSLFactory baseSslFactory;
    private final Path identityCertPath;
    private final Path identityKeyPath;
    private final Path trustStorePath;

    private ZonedDateTime lastModifiedTimeIdentityCert = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
    private ZonedDateTime lastModifiedTimeIdentityKey = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
    private ZonedDateTime lastModifiedTimeTrustStore = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);


    public PemSSLFactoryRefresher(SSLFactory baseSslFactory, Path trustStorePath, Path identityCertPath, Path identityKeyPath) {
        this.baseSslFactory = baseSslFactory;
        this.identityKeyPath = identityKeyPath;
        this.trustStorePath = trustStorePath;
        this.identityCertPath = identityCertPath;
        this.tryRefresh();
    }

    public void tryRefresh() {
        try {
            if (Files.exists(identityCertPath) && Files.exists(identityKeyPath) && Files.exists(trustStorePath)) {
                BasicFileAttributes identityCertAttributes = Files.readAttributes(identityCertPath, BasicFileAttributes.class);
                BasicFileAttributes identityKeyAttributes = Files.readAttributes(identityKeyPath, BasicFileAttributes.class);
                BasicFileAttributes trustStoreAttributes = Files.readAttributes(trustStorePath, BasicFileAttributes.class);

                boolean identityUpdated = lastModifiedTimeIdentityCert.isBefore(ZonedDateTime.ofInstant(identityCertAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));
                boolean identityKeyUpdated = lastModifiedTimeIdentityKey.isBefore(ZonedDateTime.ofInstant(identityKeyAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));
                boolean trustStoreUpdated = lastModifiedTimeTrustStore.isBefore(ZonedDateTime.ofInstant(trustStoreAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));

                if (identityUpdated || identityKeyUpdated || trustStoreUpdated) {
                    LOGGER.info("Keystore files have been changed. Trying to read the file content and preparing to update the ssl material");

                    SSLFactory updatedSslFactory = SSLFactory.builder()
                            .withIdentityMaterial(PemUtils.loadIdentityMaterial(identityCertPath, identityKeyPath))
                            .withTrustMaterial(PemUtils.loadTrustMaterial(trustStorePath))
                            .build();

                    SSLFactoryUtils.reload(baseSslFactory, updatedSslFactory);

                    lastModifiedTimeIdentityCert = ZonedDateTime.ofInstant(identityCertAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                    lastModifiedTimeIdentityKey = ZonedDateTime.ofInstant(identityKeyAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);
                    lastModifiedTimeTrustStore = ZonedDateTime.ofInstant(trustStoreAttributes.lastModifiedTime().toInstant(), ZoneOffset.UTC);

                    LOGGER.info("Updating ssl material finished");
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to refresh ssl material", e);
        }
    }

}
