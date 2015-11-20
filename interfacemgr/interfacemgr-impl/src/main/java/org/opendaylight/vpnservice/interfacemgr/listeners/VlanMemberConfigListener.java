/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.vpnservice.interfacemgr.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.idmanager.IdManager;
import org.opendaylight.vpnservice.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.vpnservice.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.vpnservice.interfacemgr.renderer.ovs.confighelpers.OvsVlanMemberConfigAddHelper;
import org.opendaylight.vpnservice.interfacemgr.renderer.ovs.confighelpers.OvsVlanMemberConfigRemoveHelper;
import org.opendaylight.vpnservice.interfacemgr.renderer.ovs.confighelpers.OvsVlanMemberConfigUpdateHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.interfacemgr.rev150331.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.interfacemgr.rev150331.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class VlanMemberConfigListener extends AsyncDataTreeChangeListenerBase<Interface, VlanMemberConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(VlanMemberConfigListener.class);
    private DataBroker dataBroker;
    private IdManager idManager;

    public VlanMemberConfigListener(final DataBroker dataBroker, final IdManager idManager) {
        super(Interface.class, VlanMemberConfigListener.class);
        this.dataBroker = dataBroker;
        this.idManager = idManager;
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceOld) {
        IfL2vlan ifL2vlan = interfaceOld.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null) {
            return;
        }

        if (ifL2vlan.getL2vlanMode() != IfL2vlan.L2vlanMode.TrunkMember) {
            return;
        }

        ParentRefs parentRefs = interfaceOld.getAugmentation(ParentRefs.class);
        if (parentRefs == null) {
            LOG.error("Attempt to remove Vlan Trunk-Member {} without a parent interface", interfaceOld);
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (lowerLayerIf.equals(interfaceOld.getName())) {
            LOG.error("Attempt to remove Vlan Trunk-Member {} with same parent interface name.", interfaceOld);
            return;
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigRemoveWorker removeWorker = new RendererConfigRemoveWorker(key, interfaceOld, parentRefs, ifL2vlan);
        coordinator.enqueueJob(lowerLayerIf, removeWorker);
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceOld, Interface interfaceNew) {
        IfL2vlan ifL2vlanNew = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlanNew == null) {
            return;
        }
        if (ifL2vlanNew.getL2vlanMode() != IfL2vlan.L2vlanMode.TrunkMember) {
            LOG.error("Configuration Error. Attempt to modify Vlan Mode of interface: {} " +
                    "to interface: {}", interfaceOld, interfaceNew);
            return;
        }

        ParentRefs parentRefsNew = interfaceNew.getAugmentation(ParentRefs.class);
        if (parentRefsNew == null) {
            LOG.error("Configuration Error. Attempt to update Vlan Trunk-Member {} without a " +
                    "parent interface", interfaceNew);
            return;
        }

        String lowerLayerIf = parentRefsNew.getParentInterface();
        if (lowerLayerIf.equals(interfaceNew.getName())) {
            LOG.error("Configuration Error. Attempt to update Vlan Trunk-Member {} with same parent " +
                    "interface name.", interfaceNew);
            return;
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigUpdateWorker updateWorker = new RendererConfigUpdateWorker(key, interfaceNew, interfaceOld,
                parentRefsNew, ifL2vlanNew);
        coordinator.enqueueJob(lowerLayerIf, updateWorker);
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceNew) {
        IfL2vlan ifL2vlan = interfaceNew.getAugmentation(IfL2vlan.class);
        if (ifL2vlan == null) {
            return;
        }

        if (ifL2vlan.getL2vlanMode() != IfL2vlan.L2vlanMode.TrunkMember) {
            return;
        }

        ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
        if (parentRefs == null) {
            LOG.error("Attempt to add Vlan Trunk-Member {} without a parent interface", interfaceNew);
            return;
        }

        String lowerLayerIf = parentRefs.getParentInterface();
        if (lowerLayerIf.equals(interfaceNew.getName())) {
            LOG.error("Attempt to add Vlan Trunk-Member {} with same parent interface name.", interfaceNew);
            return;
        }

        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        RendererConfigAddWorker configWorker = new RendererConfigAddWorker(key, interfaceNew, parentRefs, ifL2vlan);
        coordinator.enqueueJob(lowerLayerIf, configWorker);
    }

    @Override
    protected VlanMemberConfigListener getDataTreeChangeListener() {
        return VlanMemberConfigListener.this;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        IfL2vlan ifL2vlan;
        ParentRefs parentRefs;

        public RendererConfigAddWorker(InstanceIdentifier<Interface> key, Interface interfaceNew,
                                       ParentRefs parentRefs, IfL2vlan ifL2vlan) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.ifL2vlan = ifL2vlan;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsVlanMemberConfigAddHelper.addConfiguration(dataBroker, parentRefs, interfaceNew,
                    ifL2vlan, idManager);
        }
    }

    private class RendererConfigUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        Interface interfaceOld;
        IfL2vlan ifL2vlanNew;
        ParentRefs parentRefsNew;

        public RendererConfigUpdateWorker(InstanceIdentifier<Interface> key, Interface interfaceNew,
                                       Interface interfaceOld, ParentRefs parentRefsNew, IfL2vlan ifL2vlanNew) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.interfaceOld = interfaceOld;
            this.ifL2vlanNew = ifL2vlanNew;
            this.parentRefsNew = parentRefsNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsVlanMemberConfigUpdateHelper.updateConfiguration(dataBroker, parentRefsNew, interfaceOld,
                    ifL2vlanNew, interfaceNew, idManager);
        }
    }

    private class RendererConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        IfL2vlan ifL2vlan;
        ParentRefs parentRefs;

        public RendererConfigRemoveWorker(InstanceIdentifier<Interface> key, Interface interfaceOld,
                                          ParentRefs parentRefs, IfL2vlan ifL2vlan) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.ifL2vlan = ifL2vlan;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsVlanMemberConfigRemoveHelper.removeConfiguration(dataBroker, parentRefs, interfaceOld,
                    ifL2vlan, idManager);
        }
    }
}