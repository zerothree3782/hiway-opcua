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
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReadBase extends ClientBase {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HiwayOpcuaListner hiwayOpcuaListner = new HiwayOpcuaListner() {
        @Override
        public void onReadData(List<DataValue> dataValues) {
            for(int i = 0; i < dataValues.size(); i++){
                logger.info("Tag {} :: {}",i,dataValues.get(i).getValue().getValue());
            }
        }
    };

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        long readInterval = this.getOpcuaProperties().getReadInterval();
        List<String> tagList = this.getOpcuaProperties().getTagList();
        int namespace=this.getOpcuaProperties().getNamespace();
        OpcuaCommonService opcuaCommonService = new OpcuaCommonService();
        List<NodeId> nodeIds = opcuaCommonService.getNodeIdList(namespace,tagList);

        // synchronous connect
        client.connect().get();

        while(true){
            // asynchronous read request
            client.readValues(0.0, TimestampsToReturn.Both, nodeIds).
                    thenAccept(values -> hiwayOpcuaListner.onReadData(values));

            Thread.sleep(readInterval);
        }
    }

    public void addHiwayOpcuaListner(HiwayOpcuaListner hiwayOpcuaListner){
        this.hiwayOpcuaListner = hiwayOpcuaListner;
    }

}
