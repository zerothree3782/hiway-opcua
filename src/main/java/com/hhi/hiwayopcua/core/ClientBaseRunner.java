/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.hhi.hiwayopcua.core;

import com.hhi.hiwayopcua.properties.OpcuaProperties;
import com.hhi.hiwayopcua.service.ReadBase;
import com.hhi.hiwayopcua.service.SubscriptionBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class ClientBaseRunner {

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CompletableFuture<OpcUaClient> future = new CompletableFuture<>();

    private final ClientBase clientBase;
    private final boolean serverRequired;

    public ClientBaseRunner(ClientBase clientBase) throws Exception {
        this(clientBase, true);
    }

    public ClientBaseRunner(ClientBase clientBase, boolean serverRequired) throws Exception {
        this.clientBase = clientBase;
        this.serverRequired = serverRequired;
    }

    private OpcUaClient createClient() throws Exception {
        OpcuaProperties opcuaProperties = clientBase.getOpcuaProperties();
        String ip = opcuaProperties.getIp();
        int port = opcuaProperties.getPort();
        String user = opcuaProperties.getUser();
        String password = opcuaProperties.getPassword();
        clientBase.setEndpointUrl(String.format("opc.tcp://%s:%d", ip, port));

        if(user != null && password != null && user.length() != 0 && password.length() != 0){
            clientBase.setIdentityProvider(new UsernameProvider(user,password));
        }

        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }
        LoggerFactory.getLogger(getClass())
            .info("security temp dir: {}", securityTempDir.toAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        SecurityPolicy securityPolicy = clientBase.getSecurityPolicy();

        List<EndpointDescription> endpoints;

        try {
            endpoints = DiscoveryClient.getEndpoints(clientBase.getEndpointUrl()).get();
        } catch (Throwable ex) {
            // try the explicit discovery endpoint as well
            String discoveryUrl = clientBase.getEndpointUrl();

            if (!discoveryUrl.endsWith("/")) {
                discoveryUrl += "/";
            }
            discoveryUrl += "discovery";

            logger.info("Trying explicit discovery URL: {}", discoveryUrl);
            endpoints = DiscoveryClient.getEndpoints(discoveryUrl).get();
        }

        EndpointDescription endpoint = endpoints.stream()
            .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getUri()))
            .filter(clientBase.getEndpointFilter())
            .findFirst()
            .orElseThrow(() -> new Exception("no desired endpoints returned"));

        ExecutorService executor = createExecutor(clientBase.getOpcuaProperties().getServerName());

        logger.info("Using endpoint: {} [{}/{}]",
            endpoint.getEndpointUrl(), securityPolicy, endpoint.getSecurityMode());

        OpcUaClientConfig config = OpcUaClientConfig.builder()
            .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
            .setApplicationUri("urn:eclipse:milo:examples:client")
            .setCertificate(loader.getClientCertificate())
            .setKeyPair(loader.getClientKeyPair())
            .setEndpoint(endpoint)
            .setExecutor(executor)
            .setIdentityProvider(clientBase.getIdentityProvider())
            .setRequestTimeout(uint(5000))
            .build();

        return OpcUaClient.create(config);
    }

    private synchronized ExecutorService createExecutor(String serverName) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(0L);

            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(r, serverName + "-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler(
                        (t, e) ->
                                LoggerFactory.getLogger(Stack.class)
                                        .warn("Uncaught Exception on shared stack ExecutorService thread!", e)
                );
                return thread;
            }
        };
        return Executors.newCachedThreadPool(threadFactory);
    }

    public void run() {
        try {
            OpcUaClient client = createClient();

//            future.whenCompleteAsync((c, ex) -> {
//                if (ex != null) {
//                    logger.error("Error running example: {}", ex.getMessage(), ex);
//                }
//
//                try {
//                    client.disconnect().get();
//                    Stack.releaseSharedResources();
//                } catch (InterruptedException | ExecutionException e) {
//                    logger.error("Error disconnecting:", e.getMessage(), e);
//                }
//
//                try {
//                    Thread.sleep(1000);
//                    System.exit(0);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            });

            try {
                ExecutorService es = Executors.newCachedThreadPool();
                if(clientBase.getClass() == SubscriptionBase.class || clientBase.getClass() == ReadBase.class) {
                    es.execute(() -> {
                        try {
                            clientBase.run(client, future);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }else{
                    clientBase.run(client,future);
                }
                //future.get(15, TimeUnit.SECONDS);
            } catch (Throwable t) {
                logger.error("Error running client example: {}", t.getMessage(), t);
                future.completeExceptionally(t);
            }
        } catch (Throwable t) {
            logger.error("Error getting client: {}", t.getMessage(), t);

            future.completeExceptionally(t);

            try {
                Thread.sleep(1000);
                //System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("closing......");

//        try {
//            Thread.sleep(999_999_999);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

}
