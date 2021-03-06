/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netvirt.aclservice.shell;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.netvirt.aclservice.api.AclInterfaceCache;
import org.opendaylight.netvirt.aclservice.api.utils.AclDataCache;
import org.opendaylight.netvirt.aclservice.api.utils.AclInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Command(scope = "aclservice", name = "display-acl-data-cache", description = " ")
public class DisplayAclDataCaches extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayAclDataCaches.class);
    private AclDataCache aclDataCache;
    private AclInterfaceCache aclInterfaceCache;
    private static final String KEY_TAB = "   %-8s";
    private static final String ACL_INT_TAB = "   %-4s  %-4s  %-4s  %-4s %-4s  %-4s  %-6s  %-20s  %-20s %-4s";
    private static final String ACL_INT_TAB_FOR = KEY_TAB + ACL_INT_TAB;
    private static final String ACL_INT_HEAD = String.format(ACL_INT_TAB_FOR, "UUID", "PortSecurityEnabled",
            "InterfaceId", "LPortTag", "DpId", "ElanId", "VpnId", "SecurityGroups", "AllowedAddressPairs",
            "SubnetIpPrefixes", "MarkedForDelete")
            + "\n   -------------------------------------------------------------------------------------------------";
    private static final String REM_ID_TAB = "   %-20s  ";
    private static final String REM_ID_TAB_FOR = KEY_TAB + REM_ID_TAB;
    private static final String REM_ID_HEAD = String.format(REM_ID_TAB_FOR, "UUID", "Values")
            + "\n   -------------------------------------------------------------------------";
    private static final String ACL_DATA_TAB_FOR = "   %-8s %-8s  ";
    private static final String ACL_DATA_HEAD = String.format(ACL_DATA_TAB_FOR, "Key", "Value")
            + "\n   -------------------------------------------------------------------------";
    private final String exeCmdStr = "exec display-acl-data-cache -op ";
    private final String opSelections = "[ aclInterface | remoteAclId | aclFlowPriority | aclInterfaceCache ]";
    private final String opSelStr = exeCmdStr + opSelections;


    @Option(name = "-op", aliases = { "--option",
            "--op" }, description = opSelections, required = false, multiValued = false)
    private String op;

    @Option(name = "--uuid", description = "uuid for aclInterface/remoteAclId", required = false, multiValued = false)
    private String uuidStr;


    @Option(name = "--all", description = "display the complete selected map", required = false, multiValued = false)
    private String all ;

    @Option(name = "--key", description = "key for aclFlowPriority/aclInterfaceCache", required = false,
            multiValued = false)
    private String key;

    public void setAclDataCache(AclDataCache aclDataCache) {
        this.aclDataCache = aclDataCache;
    }

    public void setAclInterfaceCache(AclInterfaceCache aclInterfaceCache) {
        this.aclInterfaceCache = aclInterfaceCache;
    }

    @Override
    protected Object doExecute() throws Exception {
        if (aclDataCache == null) {
            session.getConsole().println("Failed to handle the command, AclData reference is null at this point");
            return null;
        }

        if (op == null) {
            session.getConsole().println("Please provide valid option");
            usage();
            session.getConsole().println(opSelStr);
            return null;
        }
        switch (op) {
            case "aclInterface":
                getAclInterfaceMap();
                break;
            case "remoteAclId":
                getRemoteAclIdMap();
                break;
            case "aclFlowPriority":
                getAclFlowPriorityMap();
                break;
            case "aclInterfaceCache":
                getAclInterfaceCache();
                break;
            default:
                session.getConsole().println("invalid operation");
                usage();
                session.getConsole().println(opSelStr);
        }
        return null;
    }

    void usage() {
        session.getConsole().println("usage:");
    }

    void printAclInterfaceMapHelp() {
        session.getConsole().println("invalid input");
        usage();
        session.getConsole().println(
                exeCmdStr + "aclInterface --all show | --uuid <uuid>");
    }

    void printRemoteAclIdMapHelp() {
        session.getConsole().println("invalid input");
        usage();
        session.getConsole().println(
                exeCmdStr + "remoteAclId --all show | --uuid <uuid>");
    }

    void printAclFlowPriorityMapHelp() {
        session.getConsole().println("invalid input");
        usage();
        session.getConsole().println(
                exeCmdStr + "aclFlowPriority --all show | --key <key>");
    }

    void printAclInterfaceCacheHelp() {
        session.getConsole().println("invalid input");
        usage();
        session.getConsole().println(
                exeCmdStr + "aclInterfaceCache --all show | --key <key>");
    }

    private boolean validateAll(String all) {
        if (all.equalsIgnoreCase("show")) {
            return true;
        }
        return false;
    }

    protected void getAclInterfaceMap() throws Exception {
        if (all == null && uuidStr == null) {
            printAclInterfaceMapHelp();
            return;
        }
        if (all == null && uuidStr != null) {
            Uuid uuid;
            try {
                uuid = Uuid.getDefaultInstance(uuidStr);
            } catch (IllegalArgumentException e) {
                session.getConsole().println("Invalid uuid" + e.getMessage());
                log.error("Invalid uuid" + e);
                return;
            }
            Collection<AclInterface> aclInterfaceList = aclDataCache.getInterfaceList(uuid);
            if (aclInterfaceList == null | aclInterfaceList.isEmpty()) {
                session.getConsole().println("UUID not matched");
                return;
            } else {
                session.getConsole().println(String.format(ACL_INT_HEAD));
                session.getConsole().print(String.format(KEY_TAB, uuid.toString()));
                for (AclInterface aclInterface : aclInterfaceList) {
                    session.getConsole().println(String.format(ACL_INT_TAB,
                            aclInterface.isPortSecurityEnabled(), aclInterface.getInterfaceId(),
                            aclInterface.getLPortTag(), aclInterface.getDpId(), aclInterface.getElanId(),
                            aclInterface.getVpnId(), aclInterface.getSecurityGroups(),
                            aclInterface.getAllowedAddressPairs(), aclInterface.getSubnetIpPrefixes(),
                            aclInterface.isMarkedForDelete()));
                }
                return;
            }
        }
        if (all != null && uuidStr == null) {
            if (!validateAll(all)) {
                printAclInterfaceMapHelp();
                return;
            }
            Map<Uuid, Collection<AclInterface>> map = aclDataCache.getAclInterfaceMap();

            if (map.isEmpty()) {
                session.getConsole().println("No data found");
                return;
            } else {
                session.getConsole().println(String.format(ACL_INT_HEAD));
                for (Entry<Uuid, Collection<AclInterface>> entry: map.entrySet()) {
                    Uuid key = entry.getKey();
                    session.getConsole().print(String.format(KEY_TAB, key.toString()));
                    for (AclInterface aclInterface: entry.getValue()) {
                        session.getConsole().println(String.format(ACL_INT_TAB,
                                aclInterface.isPortSecurityEnabled(), aclInterface.getInterfaceId(),
                                aclInterface.getLPortTag(), aclInterface.getDpId(), aclInterface.getElanId(),
                                aclInterface.getVpnId(), aclInterface.getSecurityGroups(),
                                aclInterface.getAllowedAddressPairs(), aclInterface.getSubnetIpPrefixes(),
                                aclInterface.isMarkedForDelete()));
                    }
                }
                return;
            }
        }
    }

    protected void getRemoteAclIdMap() throws Exception {
        if (all == null && uuidStr == null) {
            printRemoteAclIdMapHelp();
            return;
        }
        if (all == null && uuidStr != null) {
            Uuid uuidRef;
            try {
                uuidRef = Uuid.getDefaultInstance(uuidStr);
            } catch (IllegalArgumentException e) {
                session.getConsole().println("Invalid uuid" + e.getMessage());
                log.error("Invalid uuid" + e);
                return;
            }
            Collection<Uuid> remoteUuidLst = aclDataCache.getRemoteAcl(uuidRef);
            if (remoteUuidLst == null | remoteUuidLst.isEmpty()) {
                session.getConsole().println("UUID not matched");
                return;
            } else {
                session.getConsole().println(String.format(REM_ID_HEAD));
                session.getConsole().print(String.format(KEY_TAB, uuidRef.toString()));
                for (Uuid uuid : remoteUuidLst) {
                    session.getConsole().println(String.format(REM_ID_TAB, uuid.getValue()));
                }
                return;
            }
        }
        if (all != null && uuidStr == null) {
            if (!validateAll(all)) {
                printRemoteAclIdMapHelp();
                return;
            }

            Map<Uuid, Collection<Uuid>> map = aclDataCache.getRemoteAclIdMap();
            if (map.isEmpty()) {
                session.getConsole().println("No data found");
                return;
            } else {
                session.getConsole().println(String.format(REM_ID_HEAD));
                for (Entry<Uuid, Collection<Uuid>> entry: map.entrySet()) {
                    Uuid key = entry .getKey();
                    session.getConsole().print(String.format(KEY_TAB, key.toString()));
                    for (Uuid uuid: entry.getValue()) {
                        session.getConsole().println(String.format(REM_ID_TAB, uuid.getValue()));
                    }
                }
                return;
            }
        }
    }

    protected void getAclFlowPriorityMap() throws Exception {
        if (all == null && key == null) {
            printAclFlowPriorityMapHelp();
            return;
        }
        if (all == null && key != null) {
            Integer val = aclDataCache.getAclFlowPriority(key);
            if (val == null) {
                session.getConsole().println("No data found");
                return;
            }
            session.getConsole().println(String.format(ACL_DATA_HEAD));
            session.getConsole().println(String.format(ACL_DATA_TAB_FOR, key, val));

            return;
        }

        if (all != null && key == null) {
            if (!validateAll(all)) {
                printAclFlowPriorityMapHelp();
                return;
            }
            Map<String, Integer> map = aclDataCache.getAclFlowPriorityMap();
            if (map == null || map.isEmpty()) {
                session.getConsole().println("No data found");
                return;
            } else {
                session.getConsole().println(String.format(ACL_DATA_HEAD));
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    session.getConsole().println(String.format(ACL_DATA_TAB_FOR, entry.getKey(), entry.getValue()));
                }
                return;
            }
        }
    }

    protected void getAclInterfaceCache() throws Exception {
        if (all == null && key == null) {
            printAclInterfaceCacheHelp();
            return;
        }
        if (all == null && key != null) {
            AclInterface aclInterface = aclInterfaceCache.get(key);
            if (aclInterface == null) {
                session.getConsole().println("No data found");
                return;
            }
            session.getConsole().println(String.format(ACL_INT_HEAD));
            session.getConsole().println(String.format(ACL_INT_TAB_FOR, key,
                    aclInterface.isPortSecurityEnabled(), aclInterface.getInterfaceId(),
                    aclInterface.getLPortTag(), aclInterface.getDpId(), aclInterface.getElanId(),
                    aclInterface.getVpnId(), aclInterface.getSecurityGroups(),
                    aclInterface.getAllowedAddressPairs(), aclInterface.getSubnetIpPrefixes(),
                    aclInterface.isMarkedForDelete()));

            return;
        }

        if (all != null && key == null) {
            if (!validateAll(all)) {
                printAclInterfaceCacheHelp();
                return;
            }
            Collection<Entry<String, AclInterface>> entries = aclInterfaceCache.entries();
            if (entries.isEmpty()) {
                session.getConsole().println("No data found");
                return;
            } else {
                session.getConsole().println(String.format(ACL_INT_HEAD));
                for (Map.Entry<String, AclInterface> entry : entries) {
                    AclInterface aclInterface = entry.getValue();
                    session.getConsole().println(String.format(ACL_INT_TAB_FOR, entry.getKey(),
                            aclInterface.isPortSecurityEnabled(), aclInterface.getInterfaceId(),
                            aclInterface.getLPortTag(), aclInterface.getDpId(), aclInterface.getElanId(),
                            aclInterface.getVpnId(), aclInterface.getSecurityGroups(),
                            aclInterface.getAllowedAddressPairs(), aclInterface.getSubnetIpPrefixes(),
                            aclInterface.isMarkedForDelete()));
                }
            }
            return;
        }
    }
}



