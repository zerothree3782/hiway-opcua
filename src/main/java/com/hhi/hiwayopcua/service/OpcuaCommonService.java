package com.hhi.hiwayopcua.service;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpcuaCommonService {

    public List<NodeId> getNodeIdList(int namespace, List<String> tagList){

        List<NodeId> nodeIds = new ArrayList<>();

        for(int i=0; i < tagList.size(); i++){
            nodeIds.add(new NodeId(namespace,tagList.get(i)));
        }

        return nodeIds;
    }
}
