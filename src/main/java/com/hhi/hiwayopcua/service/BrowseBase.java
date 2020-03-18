/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.hhi.hiwayopcua.service;

import com.hhi.hiwayopcua.core.ClientBase;
import com.hhi.hiwayopcua.core.HiwayOpcuaListner;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

public class BrowseBase extends ClientBase {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HiwayOpcuaListner hiwayOpcuaListner = new HiwayOpcuaListner() {
        @Override
        public void onReadTag(List<ReferenceDescription> referenceDescriptionList,List<String> identList) {
            for(int i = 0; i < referenceDescriptionList.size(); i++) {
                logger.info("{} Node={}", identList.get(i), referenceDescriptionList.get(i).getBrowseName().getName());
            }
        }
    };

    private List<ReferenceDescription> referenceDescriptionList = new ArrayList<ReferenceDescription>();
    private List<String> identList = new ArrayList<String>();

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();
        NodeId rootNodeId = new NodeId(this.getOpcuaProperties().getNamespace(),this.getOpcuaProperties().getRootNode());

        // start browsing at root folder
        browseNode("", client, rootNodeId);

        hiwayOpcuaListner.onReadTag(referenceDescriptionList,identList);
        future.complete(client);
    }

    private void browseNode(String indent, OpcUaClient client, NodeId browseRoot) {
        BrowseDescription browse = new BrowseDescription(
            browseRoot,
            BrowseDirection.Forward,
            Identifiers.References,
            true,
            uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
            uint(BrowseResultMask.All.getValue())
        );

        try {
            BrowseResult browseResult = client.browse(browse).get();

            List<ReferenceDescription> references = toList(browseResult.getReferences());

            for (ReferenceDescription rd : references) {
                //logger.info("{} Node={}", indent, rd.getBrowseName().getName());
                identList.add(indent);
                referenceDescriptionList.add(rd);
                // recursively browse to children
                rd.getNodeId().local().ifPresent(nodeId -> browseNode(indent + "  ", client, nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Browsing nodeId={} failed: {}", browseRoot, e.getMessage(), e);
        }
    }

    public void addHiwayOpcuaListner(HiwayOpcuaListner hiwayOpcuaListner){
        this.hiwayOpcuaListner = hiwayOpcuaListner;
    }

}

