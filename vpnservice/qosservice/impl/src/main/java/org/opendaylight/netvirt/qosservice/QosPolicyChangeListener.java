/*
 * Copyright (c) 2017 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netvirt.qosservice;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.netvirt.neutronvpn.api.utils.ChangeUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev150712.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.QosPolicies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.QosRuleTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.QosRuleTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.qos.policies.QosPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.qos.policies.qos.policy.BandwidthLimitRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.qos.policies.qos.policy.BandwidthLimitRulesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.qos.policies.qos.policy.DscpmarkingRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.qos.rule.types.RuleTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.qos.rev160613.qos.attributes.qos.rule.types.RuleTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class QosPolicyChangeListener extends AsyncClusteredDataTreeChangeListenerBase<QosPolicy,
                                                QosPolicyChangeListener> {
    private static final Logger LOG = LoggerFactory.getLogger(QosPolicyChangeListener.class);
    private final DataBroker dataBroker;
    private final QosAlertManager qosAlertManager;
    private final QosNeutronUtils qosNeutronUtils;
    private final JobCoordinator jobCoordinator;

    @Inject
    public QosPolicyChangeListener(final DataBroker dataBroker, final QosAlertManager qosAlertManager,
            final QosNeutronUtils qosNeutronUtils, final JobCoordinator jobCoordinator) {
        super(QosPolicy.class, QosPolicyChangeListener.class);
        this.dataBroker = dataBroker;
        this.qosAlertManager = qosAlertManager;
        this.qosNeutronUtils = qosNeutronUtils;
        this.jobCoordinator = jobCoordinator;
        LOG.debug("{} created",  getClass().getSimpleName());
    }

    @PostConstruct
    public void init() {
        registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
        supportedQoSRuleTypes();
        LOG.debug("{} init and registerListener done", getClass().getSimpleName());
    }

    @Override
    protected InstanceIdentifier<QosPolicy> getWildCardPath() {
        return InstanceIdentifier.create(Neutron.class).child(QosPolicies.class).child(QosPolicy.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<QosPolicy>> changes) {
        handleQosPolicyChanges(changes);
        handleBandwidthLimitRulesChanges(changes);
        handleDscpMarkingRulesChanges(changes);
    }

    @Override
    protected QosPolicyChangeListener getDataTreeChangeListener() {
        return QosPolicyChangeListener.this;
    }

    private void handleQosPolicyChanges(Collection<DataTreeModification<QosPolicy>> changes) {
        Map<InstanceIdentifier<QosPolicy>, QosPolicy> qosPolicyOriginalMap =
                ChangeUtils.extractOriginal(changes, QosPolicy.class);

        for (Entry<InstanceIdentifier<QosPolicy>, QosPolicy> qosPolicyMapEntry :
                ChangeUtils.extractCreated(changes, QosPolicy.class).entrySet()) {
            add(qosPolicyMapEntry.getKey(), qosPolicyMapEntry.getValue());
        }
        for (Entry<InstanceIdentifier<QosPolicy>, QosPolicy> qosPolicyMapEntry :
                ChangeUtils.extractUpdated(changes, QosPolicy.class).entrySet()) {
            update(qosPolicyMapEntry.getKey(), qosPolicyOriginalMap.get(qosPolicyMapEntry.getKey()),
                    qosPolicyMapEntry.getValue());
        }
        for (InstanceIdentifier<QosPolicy> qosPolicyIid : ChangeUtils.extractRemoved(changes, QosPolicy.class)) {
            remove(qosPolicyIid, qosPolicyOriginalMap.get(qosPolicyIid));
        }
    }

    private void handleBandwidthLimitRulesChanges(Collection<DataTreeModification<QosPolicy>> changes) {
        Map<InstanceIdentifier<BandwidthLimitRules>, BandwidthLimitRules> bwLimitOriginalMap =
                ChangeUtils.extractOriginal(changes, BandwidthLimitRules.class);

        for (Entry<InstanceIdentifier<BandwidthLimitRules>, BandwidthLimitRules> bwLimitMapEntry :
                ChangeUtils.extractCreated(changes, BandwidthLimitRules.class).entrySet()) {
            add(bwLimitMapEntry.getKey(), bwLimitMapEntry.getValue());
        }
        for (Entry<InstanceIdentifier<BandwidthLimitRules>, BandwidthLimitRules> bwLimitMapEntry :
                ChangeUtils.extractUpdated(changes, BandwidthLimitRules.class).entrySet()) {
            update(bwLimitMapEntry.getKey(), bwLimitOriginalMap.get(bwLimitMapEntry.getKey()),
                    bwLimitMapEntry.getValue());
        }
        for (InstanceIdentifier<BandwidthLimitRules> bwLimitIid :
                ChangeUtils.extractRemoved(changes, BandwidthLimitRules.class)) {
            remove(bwLimitIid, bwLimitOriginalMap.get(bwLimitIid));
        }
    }

    private void handleDscpMarkingRulesChanges(Collection<DataTreeModification<QosPolicy>> changes) {
        Map<InstanceIdentifier<DscpmarkingRules>, DscpmarkingRules> dscpMarkOriginalMap =
                ChangeUtils.extractOriginal(changes, DscpmarkingRules.class);

        for (Entry<InstanceIdentifier<DscpmarkingRules>, DscpmarkingRules> dscpMarkMapEntry :
                ChangeUtils.extractCreated(changes, DscpmarkingRules.class).entrySet()) {
            add(dscpMarkMapEntry.getKey(), dscpMarkMapEntry.getValue());
        }
        for (Entry<InstanceIdentifier<DscpmarkingRules>, DscpmarkingRules> dscpMarkMapEntry :
                ChangeUtils.extractUpdated(changes, DscpmarkingRules.class).entrySet()) {
            update(dscpMarkMapEntry.getKey(), dscpMarkOriginalMap.get(dscpMarkMapEntry.getKey()),
                    dscpMarkMapEntry.getValue());
        }
        for (InstanceIdentifier<DscpmarkingRules> dscpMarkIid :
                ChangeUtils.extractRemoved(changes, DscpmarkingRules.class)) {
            remove(dscpMarkIid, dscpMarkOriginalMap.get(dscpMarkIid));
        }
    }

    @Override
    protected void add(InstanceIdentifier<QosPolicy> identifier, QosPolicy input) {
        LOG.trace("Adding  QosPolicy : key: {}, value={}", identifier, input);
        qosNeutronUtils.addToQosPolicyCache(input);
    }

    protected void add(InstanceIdentifier<BandwidthLimitRules> identifier, BandwidthLimitRules input) {
        LOG.trace("Adding BandwidthlimitRules : key: {}, value={}", identifier, input);

        Uuid qosUuid = identifier.firstKeyOf(QosPolicy.class).getUuid();
        for (Network network : qosNeutronUtils.getQosNetworks(qosUuid)) {
            qosNeutronUtils.handleNeutronNetworkQosUpdate(network, qosUuid);
            qosAlertManager.addToQosAlertCache(network);
        }

        for (Port port : qosNeutronUtils.getQosPorts(qosUuid)) {
            qosAlertManager.addToQosAlertCache(port);
            jobCoordinator.enqueueJob("QosPort-" + port.getUuid().getValue(), () -> {
                WriteTransaction wrtConfigTxn = dataBroker.newWriteOnlyTransaction();
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                qosNeutronUtils.setPortBandwidthLimits(port, input, wrtConfigTxn);
                futures.add(wrtConfigTxn.submit());
                return futures;
            });
        }
    }

    private void add(InstanceIdentifier<DscpmarkingRules> identifier, DscpmarkingRules input) {
        LOG.trace("Adding DscpMarkingRules : key: {}, value={}", identifier, input);

        Uuid qosUuid = identifier.firstKeyOf(QosPolicy.class).getUuid();

        for (Network network : qosNeutronUtils.getQosNetworks(qosUuid)) {
            qosNeutronUtils.handleNeutronNetworkQosUpdate(network, qosUuid);
        }

        for (Port port : qosNeutronUtils.getQosPorts(qosUuid)) {
            jobCoordinator.enqueueJob("QosPort-" + port.getUuid().getValue(), () -> {
                WriteTransaction wrtConfigTxn = dataBroker.newWriteOnlyTransaction();
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                qosNeutronUtils.setPortDscpMarking(port, input);
                futures.add(wrtConfigTxn.submit());
                return futures;
            });
        }
    }

    @Override
    protected void remove(InstanceIdentifier<QosPolicy> identifier, QosPolicy input) {
        LOG.trace("Removing QosPolicy : key: {}, value={}", identifier, input);
        qosNeutronUtils.removeFromQosPolicyCache(input);
    }

    private void remove(InstanceIdentifier<BandwidthLimitRules> identifier, BandwidthLimitRules input) {
        LOG.trace("Removing BandwidthLimitRules : key: {}, value={}", identifier, input);

        Uuid qosUuid = identifier.firstKeyOf(QosPolicy.class).getUuid();
        BandwidthLimitRulesBuilder bwLimitBuilder = new BandwidthLimitRulesBuilder();
        BandwidthLimitRules zeroBwLimitRule =
                bwLimitBuilder.setMaxBurstKbps(BigInteger.ZERO).setMaxKbps(BigInteger.ZERO).build();

        for (Network network : qosNeutronUtils.getQosNetworks(qosUuid)) {
            qosAlertManager.removeFromQosAlertCache(network);
            qosNeutronUtils.handleNeutronNetworkQosBwRuleRemove(network, zeroBwLimitRule);
        }

        for (Port port : qosNeutronUtils.getQosPorts(qosUuid)) {
            qosAlertManager.removeFromQosAlertCache(port);
            jobCoordinator.enqueueJob("QosPort-" + port.getUuid().getValue(), () -> {
                WriteTransaction wrtConfigTxn = dataBroker.newWriteOnlyTransaction();
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                qosNeutronUtils.setPortBandwidthLimits(port, zeroBwLimitRule, wrtConfigTxn);
                futures.add(wrtConfigTxn.submit());
                return futures;
            });
        }
    }

    private void remove(InstanceIdentifier<DscpmarkingRules> identifier,DscpmarkingRules input) {
        LOG.trace("Removing DscpMarkingRules : key: {}, value={}", identifier, input);

        Uuid qosUuid = identifier.firstKeyOf(QosPolicy.class).getUuid();

        for (Network network : qosNeutronUtils.getQosNetworks(qosUuid)) {
            qosNeutronUtils.handleNeutronNetworkQosDscpRuleRemove(network);
        }

        for (Port port : qosNeutronUtils.getQosPorts(qosUuid)) {
            jobCoordinator.enqueueJob("QosPort-" + port.getUuid().getValue(), () -> {
                WriteTransaction wrtConfigTxn = dataBroker.newWriteOnlyTransaction();
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                qosNeutronUtils.unsetPortDscpMark(port);
                futures.add(wrtConfigTxn.submit());
                return futures;
            });
        }
    }

    @Override
    protected void update(InstanceIdentifier<QosPolicy> identifier, QosPolicy original, QosPolicy update) {
        LOG.trace("Updating QosPolicy : key: {}, original value={}, update value={}", identifier, original, update);
        qosNeutronUtils.addToQosPolicyCache(update);
    }

    private void update(InstanceIdentifier<BandwidthLimitRules> identifier, BandwidthLimitRules original,
                        BandwidthLimitRules update) {
        LOG.trace("Updating BandwidthLimitRules : key: {}, original value={}, update value={}", identifier, original,
                update);
        Uuid qosUuid = identifier.firstKeyOf(QosPolicy.class).getUuid();
        for (Network network : qosNeutronUtils.getQosNetworks(qosUuid)) {
            qosNeutronUtils.handleNeutronNetworkQosUpdate(network, qosUuid);
        }

        for (Port port : qosNeutronUtils.getQosPorts(qosUuid)) {
            jobCoordinator.enqueueJob("QosPort-" + port.getUuid().getValue(), () -> {
                WriteTransaction wrtConfigTxn = dataBroker.newWriteOnlyTransaction();
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                qosNeutronUtils.setPortBandwidthLimits(port, update, wrtConfigTxn);
                futures.add(wrtConfigTxn.submit());
                return futures;
            });
        }
    }

    private void update(InstanceIdentifier<DscpmarkingRules> identifier, DscpmarkingRules original,
                        DscpmarkingRules update) {
        LOG.trace("Updating DscpMarkingRules : key: {}, original value={}, update value={}", identifier, original,
                update);
        Uuid qosUuid = identifier.firstKeyOf(QosPolicy.class).getUuid();

        for (Network network : qosNeutronUtils.getQosNetworks(qosUuid)) {
            qosNeutronUtils.handleNeutronNetworkQosUpdate(network, qosUuid);
        }

        for (Port port : qosNeutronUtils.getQosPorts(qosUuid)) {
            jobCoordinator.enqueueJob("QosPort-" + port.getUuid().getValue(), () -> {
                WriteTransaction wrtConfigTxn = dataBroker.newWriteOnlyTransaction();
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                qosNeutronUtils.setPortDscpMarking(port, update);
                futures.add(wrtConfigTxn.submit());
                return futures;
            });
        }
    }

    private void supportedQoSRuleTypes() {
        QosRuleTypesBuilder qrtBuilder = new QosRuleTypesBuilder();
        List<RuleTypes> value = new ArrayList<>();

        value.add(getRuleTypes("bandwidth_limit_rules"));
        value.add(getRuleTypes("dscp_marking_rules"));

        qrtBuilder.setRuleTypes(value);
        final WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier instanceIdentifier = InstanceIdentifier.create(Neutron.class).child(QosRuleTypes.class);

        writeTx.merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, qrtBuilder.build());
        writeTx.submit();
    }

    private RuleTypes getRuleTypes(String ruleType) {
        RuleTypesBuilder rtBuilder = new RuleTypesBuilder();
        rtBuilder.setRuleType(ruleType);
        return rtBuilder.build();
    }
}
