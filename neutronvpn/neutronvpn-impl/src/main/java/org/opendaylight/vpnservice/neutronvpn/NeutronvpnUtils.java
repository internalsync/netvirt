/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vpnservice.neutronvpn;

import com.google.common.base.Optional;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.RouterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.lockmanager.rev150819.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.lockmanager.rev150819.TimeUnits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.lockmanager.rev150819.TryLockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.lockmanager.rev150819.TryLockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.lockmanager.rev150819.UnlockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.lockmanager.rev150819.UnlockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.NeutronPortData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.Subnetmaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.VpnMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.neutron.port.data
        .PortFixedipToPortName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.neutron.port.data
        .PortFixedipToPortNameKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.neutron.port.data
        .PortNameToPortUuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.neutron.port.data
        .PortNameToPortUuidKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.subnetmaps.Subnetmap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.subnetmaps.SubnetmapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.vpnmaps.VpnMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.vpnservice.neutronvpn.rev150602.vpnmaps.VpnMapKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

//import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.port.ext.rev151125.TrunkportExt;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.port.ext.rev151125.TrunkportTypeBase;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.port.ext.rev151125.TrunkportTypeSubport;

public class NeutronvpnUtils {

    private static final Logger logger = LoggerFactory.getLogger(NeutronvpnUtils.class);

    protected static Subnetmap getSubnetmap(DataBroker broker, Uuid subnetId) {
        InstanceIdentifier<Subnetmap> id = InstanceIdentifier.builder(Subnetmaps.class).
                child(Subnetmap.class, new SubnetmapKey(subnetId)).build();
        Optional<Subnetmap> sn = read(broker, LogicalDatastoreType.CONFIGURATION, id);

        if (sn.isPresent()) {
            return sn.get();
        }
        return null;
    }

    protected static VpnMap getVpnMap(DataBroker broker, Uuid id) {
        InstanceIdentifier<VpnMap> vpnMapIdentifier = InstanceIdentifier.builder(VpnMaps.class)
                .child(VpnMap.class, new VpnMapKey(id)).build();
        Optional<VpnMap> optionalVpnMap = read(broker, LogicalDatastoreType.CONFIGURATION,
                vpnMapIdentifier);
        if (optionalVpnMap.isPresent()) {
            return optionalVpnMap.get();
        }
        logger.error("getVpnMap failed, VPN {} not present", id.getValue());
        return null;
    }

    protected static Uuid getVpnForNetwork(DataBroker broker, Uuid network) {
        InstanceIdentifier<VpnMaps> vpnMapsIdentifier = InstanceIdentifier.builder(VpnMaps.class).build();
        Optional<VpnMaps> optionalVpnMaps = read(broker, LogicalDatastoreType.CONFIGURATION,
                vpnMapsIdentifier);
        if (optionalVpnMaps.isPresent()) {
            VpnMaps vpnMaps = optionalVpnMaps.get();
            List<VpnMap> allMaps = vpnMaps.getVpnMap();
            for (VpnMap vpnMap : allMaps) {
                if (vpnMap.getNetworkIds().contains(network)) {
                    return vpnMap.getVpnId();
                }
            }
        }
        return null;
    }

    protected static Uuid getVpnForRouter(DataBroker broker, Uuid router) {
        InstanceIdentifier<VpnMaps> vpnMapsIdentifier = InstanceIdentifier.builder(VpnMaps.class).build();
        Optional<VpnMaps> optionalVpnMaps = read(broker, LogicalDatastoreType.CONFIGURATION,
                vpnMapsIdentifier);
        if (optionalVpnMaps.isPresent()) {
            VpnMaps vpnNets = optionalVpnMaps.get();
            List<VpnMap> allMaps = vpnNets.getVpnMap();
            if (router != null) {
                for (VpnMap vpnMap : allMaps) {
                    if (router.equals(vpnMap.getRouterId())) {
                        return vpnMap.getVpnId();
                    }
                }
            }
        }
        return null;
    }

    protected static Uuid getRouterforVpn(DataBroker broker, Uuid vpnId) {
        InstanceIdentifier<VpnMap> vpnMapIdentifier = InstanceIdentifier.builder(VpnMaps.class)
                .child(VpnMap.class, new VpnMapKey(vpnId)).build();
        Optional<VpnMap> optionalVpnMap = read(broker, LogicalDatastoreType.CONFIGURATION,
                vpnMapIdentifier);
        if (optionalVpnMap.isPresent()) {
            VpnMap vpnMap = optionalVpnMap.get();
            return vpnMap.getRouterId();
        }
        return null;
    }

    protected static Uuid getNeutronPortIdfromPortName(DataBroker broker, String portname) {
        InstanceIdentifier id = buildPortNameToPortUuidIdentifier(portname);
        Optional<PortNameToPortUuid> portNameToPortUuidData = read(broker, LogicalDatastoreType.CONFIGURATION, id);
        if (portNameToPortUuidData.isPresent()) {
            return portNameToPortUuidData.get().getPortId();
        }
        return null;
    }

    protected static String getNeutronPortNamefromPortFixedIp(DataBroker broker, String fixedIp) {
        InstanceIdentifier id = buildFixedIpToPortNameIdentifier(fixedIp);
        Optional<PortFixedipToPortName> portFixedipToPortNameData = read(broker, LogicalDatastoreType.CONFIGURATION,
                id);
        if (portFixedipToPortNameData.isPresent()) {
            return portFixedipToPortNameData.get().getPortName();
        }
        return null;
    }

    //TODO
    //Will be done once integrated with TrunkPort Extensions
    protected static int getVlanFromNeutronPort(Port port) {
        int vlanId = 0;
        /*
        TrunkportExt trunkportExt = port.getAugmentation(TrunkportExt.class);
        if (trunkportExt != null) {
            Class<? extends TrunkportTypeBase> trunkportType = trunkportExt.getType();
            if (trunkportType != null && trunkportType.isAssignableFrom(TrunkportTypeSubport.class)) {
                vlanId = trunkportExt.getVid();
            }
        }
        */
        return vlanId;
    }

    protected static Router getNeutronRouter(DataBroker broker, Uuid routerId) {

        InstanceIdentifier<Router> inst = InstanceIdentifier.create(Neutron.class).
                child(Routers.class).child(Router.class, new RouterKey(routerId));

        Optional<Router> rtr = read(broker, LogicalDatastoreType.CONFIGURATION, inst);
        if (rtr.isPresent()) {
            return rtr.get();
        }
        return null;
    }

    protected static Network getNeutronNetwork(DataBroker broker, Uuid networkId) {
        logger.debug("getNeutronNetwork for {}", networkId.getValue());
        InstanceIdentifier<Network> inst = InstanceIdentifier.create(Neutron.class).
                child(Networks.class).child(Network.class, new NetworkKey(networkId));

        Optional<Network> net = read(broker, LogicalDatastoreType.CONFIGURATION, inst);
        if (net.isPresent()) {
            return net.get();
        }
        return null;
    }

    protected static List<Uuid> getNeutronNetworkSubnetIds(DataBroker broker, Uuid networkId) {

        logger.debug("getNeutronNetworkSubnetIds for {}", networkId.getValue());
        Network network = getNeutronNetwork(broker, networkId);
        if (network != null) {
            //TODO
            //return network.getSubnets();
        }
        logger.debug("returning from getNeutronNetworkSubnetIds for {}", networkId.getValue());

        return null;
    }

    protected static List<Uuid> getNeutronRouterSubnetIds(DataBroker broker, Uuid routerId) {
        logger.info("getNeutronRouterSubnetIds for {}", routerId.getValue());

        List<Uuid> subnetNames = new ArrayList<Uuid>();
        Router router = getNeutronRouter(broker, routerId);
        if (router != null) {
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.router
                    .Interfaces> ifs = router.getInterfaces();
            if (!ifs.isEmpty()) {
                for (org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers
                        .router.Interfaces iff : ifs) {
                    subnetNames.add(iff.getSubnetId());
                }
            }
        }
        logger.info("returning from getNeutronRouterSubnetIds for {}", routerId.getValue());
        return subnetNames;
    }

    protected static String uuidToTapPortName(Uuid id) {
        String tapId = id.getValue().substring(0, 11);
        return new StringBuilder().append("tap").append(tapId).toString();
    }

    protected static void lockVpnInterface(LockManagerService lockManager, String vpnInterfaceName) {
        TryLockInput input = new TryLockInputBuilder().setLockName(vpnInterfaceName).setTime(5L).setTimeUnit
                (TimeUnits.Milliseconds).build();
        Future<RpcResult<Void>> result = lockManager.tryLock(input);
        try {
            if ((result != null) && (result.get().isSuccessful())) {
                logger.debug("Acquired lock for vpninterface {}", vpnInterfaceName);
            } else {
                throw new RuntimeException(String.format("Unable to getLock for vpninterface %s", vpnInterfaceName));
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unable to getLock for vpninterface {}", vpnInterfaceName);
            throw new RuntimeException(String.format("Unable to getLock for vpninterface %s", vpnInterfaceName), e
                    .getCause());
        }
    }

    protected static void unlockVpnInterface(LockManagerService lockManager, String vpnInterfaceName) {
        UnlockInput input = new UnlockInputBuilder().setLockName(vpnInterfaceName).build();
        Future<RpcResult<Void>> result = lockManager.unlock(input);
        try {
            if ((result != null) && (result.get().isSuccessful())) {
                logger.debug("Unlocked vpninterface{}", vpnInterfaceName);
            } else {
                logger.debug("Unable to unlock vpninterface {}", vpnInterfaceName);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unable to unlock vpninterface {}", vpnInterfaceName);
            throw new RuntimeException(String.format("Unable to unlock vpninterface %s", vpnInterfaceName), e
                    .getCause());
        }
    }

    static InstanceIdentifier<PortNameToPortUuid> buildPortNameToPortUuidIdentifier(String portname) {
        InstanceIdentifier<PortNameToPortUuid> id =
                InstanceIdentifier.builder(NeutronPortData.class).child(PortNameToPortUuid.class, new
                        PortNameToPortUuidKey(portname)).build();
        return id;
    }

    static InstanceIdentifier<PortFixedipToPortName> buildFixedIpToPortNameIdentifier(String fixedIp) {
        InstanceIdentifier<PortFixedipToPortName> id =
                InstanceIdentifier.builder(NeutronPortData.class).child(PortFixedipToPortName.class, new
                        PortFixedipToPortNameKey(fixedIp)).build();
        return id;
    }

    static InstanceIdentifier<Interface> buildVlanInterfaceIdentifier(String interfaceName) {
        InstanceIdentifier<Interface> id = InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new
                InterfaceKey(interfaceName)).build();
        return id;
    }

    static <T extends DataObject> Optional<T> read(DataBroker broker, LogicalDatastoreType datastoreType,
                                                   InstanceIdentifier<T> path) {

        ReadOnlyTransaction tx = broker.newReadOnlyTransaction();

        Optional<T> result = Optional.absent();
        try {
            result = tx.read(datastoreType, path).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

}
