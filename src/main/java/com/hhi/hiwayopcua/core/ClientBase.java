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
import lombok.Getter;
import lombok.Setter;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Getter
@Setter
public abstract class ClientBase {
    String endpointUrl = "opc.tcp://127.0.0.1:12686/milo";
    Predicate<EndpointDescription> endpointFilter = e -> true;
    SecurityPolicy securityPolicy = SecurityPolicy.None;
    IdentityProvider identityProvider = new AnonymousProvider();
    OpcuaProperties opcuaProperties;
    public abstract void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception;

}
