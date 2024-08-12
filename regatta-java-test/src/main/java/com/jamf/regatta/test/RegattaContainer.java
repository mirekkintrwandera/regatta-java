/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.test;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegattaContainer extends GenericContainer<RegattaContainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegattaContainer.class);

    private final AtomicBoolean configured;
    private final String node;
    private final Set<String> nodes;

    private String clusterName;
    private boolean ssl;
    private Path dataDirectory;
    private Collection<String> additionalArgs;
    private boolean shouldMountDataDirectory = true;
    private String user;

    public RegattaContainer(String image, String node, Collection<String> nodes) {
        super(image);

        this.configured = new AtomicBoolean();
        this.node = node;

        this.nodes = new HashSet<>(nodes);
        this.nodes.add(node);
    }

    public RegattaContainer withSsl(boolean ssl) {
        this.ssl = ssl;
        return self();
    }

    public RegattaContainer withShouldMountDataDirectory(boolean shouldMountDataDiretory) {
        this.shouldMountDataDirectory = shouldMountDataDiretory;
        return self();
    }

    public RegattaContainer withClusterName(String clusterToken) {
        this.clusterName = clusterToken;
        return self();
    }

    public RegattaContainer withAdditionalArgs(Collection<String> additionalArgs) {
        if (additionalArgs != null) {
            this.additionalArgs = Collections.unmodifiableCollection(new ArrayList<>(additionalArgs));
        }

        return self();
    }

    /**
     * Optional values are {@code [ user | user:group | uid | uid:gid | user:gid | uid:group ]}.
     * See <a href="https://docs.docker.com/engine/reference/run/#user">User</a> .
     *
     * @param user Refer to {@link com.github.dockerjava.api.command.CreateContainerCmd#withUser(String)}
     * @return self container.
     */
    public RegattaContainer withUser(String user) {
        this.user = user;
        return self();
    }

    @Override
    protected void configure() {
        if (!configured.compareAndSet(false, true)) {
            return;
        }

        if (shouldMountDataDirectory) {
            dataDirectory = createDataDirectory(node);
            addFileSystemBind(dataDirectory.toString(), Regatta.REGATTA_DATA_DIR, BindMode.READ_WRITE, SelinuxContext.SHARED);
        }

        withExposedPorts(Regatta.REGATTA_PEER_PORT, Regatta.REGATTA_CLIENT_PORT, Regatta.REGATTA_METRICS_PORT);
        withNetworkAliases(node);
        withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix(node).withSeparateOutputStreams());
        withCommand(createCommand());

        String tempUser = this.user;
        if (tempUser == null) {
            tempUser = System.getenv("TC_USER");
        }
        if (tempUser != null) {
            String finalUser = tempUser;
            withCreateContainerCmdModifier(c -> c.withUser(finalUser));
        }

        waitingFor(Wait.forHttp("/healthz").forPort(Regatta.REGATTA_METRICS_PORT).forStatusCode(200));
    }

    private Path createDataDirectory(String name) {
        try {
            final String prefix = "regatta_test_" + name + "_";
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                final FileAttribute<?> attribute = PosixFilePermissions
                        .asFileAttribute(EnumSet.allOf(PosixFilePermission.class));
                return Files.createTempDirectory(prefix, attribute).toRealPath();
            } else {
                return Files.createTempDirectory(prefix).toRealPath();
            }
        } catch (IOException e) {
            throw new ContainerLaunchException("Error creating data directory", e);
        }
    }

    private String[] createCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add("leader");
        cmd.add("--memberlist.cluster-name");
        cmd.add(clusterName);
        cmd.add("--memberlist.node-name");
        cmd.add(node);
        cmd.add("--api.address");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + Regatta.REGATTA_CLIENT_PORT);
        cmd.add("--api.advertise-address");
        cmd.add((ssl ? "https" : "http") + "://0.0.0.0:" + Regatta.REGATTA_CLIENT_PORT);

        cmd.add("--rest.address");
        cmd.add("http://0.0.0.0:" + Regatta.REGATTA_METRICS_PORT);

        if (shouldMountDataDirectory) {
            cmd.add("--raft.state-machine-dir");
            cmd.add(Regatta.REGATTA_DATA_DIR + "/stable");
            cmd.add("--raft.node-host-dir");
            cmd.add(Regatta.REGATTA_DATA_DIR + "/raft");
        } else {
            cmd.add("--raft.state-machine-dir");
            cmd.add("/tmp/stable");
            cmd.add("--raft.node-host-dir");
            cmd.add("/tmp/raft");
        }

        String nodeId = node.substring(node.lastIndexOf("-") + 1);

        cmd.add("--raft.node-id");
        cmd.add(nodeId);
        cmd.add("--raft.address");
        cmd.add(node + ":" + Regatta.REGATTA_PEER_PORT);


        if (ssl) {
            withClasspathResourceMapping(
                    "ssl/cert/" + node + ".pem", "/etc/ssl/regatta/server.pem",
                    BindMode.READ_ONLY,
                    SelinuxContext.SHARED);

            withClasspathResourceMapping(
                    "ssl/cert/" + node + ".key", "/etc/ssl/regatta/server.key",
                    BindMode.READ_ONLY,
                    SelinuxContext.SHARED);

            cmd.add("--cert-file");
            cmd.add("/etc/ssl/regatta/server.pem");
            cmd.add("--key-file");
            cmd.add("/etc/ssl/regatta/server.key");
        }

        if (nodes.size() > 1) {
            var members = nodes.stream()
                    .map(node -> node.substring(node.lastIndexOf("-") + 1) + "=" + node + ":" + Regatta.REGATTA_PEER_PORT)
                    .collect(Collectors.joining(","));
            cmd.add("--raft.initial-members");
            cmd.add(members);
        } else {
            cmd.add("--raft.initial-members");
            cmd.add(nodeId + "=" + node + ":" + Regatta.REGATTA_PEER_PORT);
        }

        if (additionalArgs != null) {
            cmd.addAll(additionalArgs);
        }

        return cmd.toArray(new String[0]);
    }

    private static void deleteDataDirectory(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try {
                try (Stream<Path> stream = Files.walk(dir)) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    LOGGER.error("Error deleting directory {}", dir, e);
                }
            } catch (Exception e) {
                LOGGER.error("Error deleting directory {}", dir, e);
            }
        }
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {

        try {
            super.containerIsStarting(containerInfo);

            if (shouldMountDataDirectory) {
                execInContainer("chmod", "o+rwx", "-R", Regatta.REGATTA_DATA_DIR);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        LOGGER.debug("starting regatta container {} with command: {}", node, String.join(" ", getCommandParts()));
        super.start();
    }

    @Override
    public void close() {
        super.close();
        deleteDataDirectory(dataDirectory);
    }

    public String node() {
        return this.node;
    }

    public InetSocketAddress getClientAddress() {
        return new InetSocketAddress(getHost(), getMappedPort(Regatta.REGATTA_CLIENT_PORT));
    }

    public URI clientEndpoint() {
        return newURI(
                getHost(),
                getMappedPort(Regatta.REGATTA_CLIENT_PORT));
    }

    public InetSocketAddress getPeerAddress() {
        return new InetSocketAddress(getHost(), getMappedPort(Regatta.REGATTA_PEER_PORT));
    }

    public URI peerEndpoint() {
        return newURI(
                getHost(),
                getMappedPort(Regatta.REGATTA_PEER_PORT));
    }

    private URI newURI(final String host, final int port) {
        try {
            return new URI(ssl ? "https" : "http", null, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URISyntaxException should never happen here", e);
        }
    }

    public boolean hasDataDirectoryMounted() {
        return dataDirectory != null;
    }
}
