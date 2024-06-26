package org.zlab.ocov.tracker;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.util.*;

public class TestUtils {
    static final String system_local_A = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.SystemKeyspace.forceBlockingFlush(SystemKeyspace.java:368)\n"
            + "org.apache.cassandra.db.SystemKeyspace.saveTruncationRecord(SystemKeyspace.java:240)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore$13.run(ColumnFamilyStore.java:2230)\n"
            + "java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.runWithCompactionsDisabled(ColumnFamilyStore.java:2260)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.truncateBlocking(ColumnFamilyStore.java:2235)\n"
            + "org.apache.cassandra.db.SystemKeyspace.discardCompactionsInProgress(SystemKeyspace.java:220)\n"
            + "org.apache.cassandra.service.CassandraDaemon.setup(CassandraDaemon.java:266)\n"
            + "org.apache.cassandra.service.CassandraDaemon.activate(CassandraDaemon.java:616)\n"
            + "org.apache.cassandra.service.CassandraDaemon.main(CassandraDaemon.java:775)\n";
    static final String system_local_B = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.SystemKeyspace.forceBlockingFlush(SystemKeyspace.java:368)\n"
            + "org.apache.cassandra.db.SystemKeyspace.incrementAndGetGeneration(SystemKeyspace.java:514)\n"
            + "org.apache.cassandra.service.StorageService.prepareToJoin(StorageService.java:772)\n"
            + "org.apache.cassandra.service.StorageService.initServer(StorageService.java:677)\n"
            + "org.apache.cassandra.service.StorageService.initServer(StorageService.java:563)\n"
            + "org.apache.cassandra.service.CassandraDaemon.setup(CassandraDaemon.java:346)\n"
            + "org.apache.cassandra.service.CassandraDaemon.activate(CassandraDaemon.java:616)\n"
            + "org.apache.cassandra.service.CassandraDaemon.main(CassandraDaemon.java:775)\n";
    static final String system_local_C = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.SystemKeyspace.forceBlockingFlush(SystemKeyspace.java:368)\n"
            + "org.apache.cassandra.db.SystemKeyspace.setBootstrapState(SystemKeyspace.java:537)\n"
            + "org.apache.cassandra.service.StorageService.finishJoiningRing(StorageService.java:1039)\n"
            + "org.apache.cassandra.service.StorageService.joinTokenRing(StorageService.java:956)\n"
            + "org.apache.cassandra.service.StorageService.initServer(StorageService.java:692)\n"
            + "org.apache.cassandra.service.StorageService.initServer(StorageService.java:563)\n"
            + "org.apache.cassandra.service.CassandraDaemon.setup(CassandraDaemon.java:346)\n"
            + "org.apache.cassandra.service.CassandraDaemon.activate(CassandraDaemon.java:616)\n"
            + "org.apache.cassandra.service.CassandraDaemon.main(CassandraDaemon.java:775)\n";
    static final String system_local_D = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.SystemKeyspace.forceBlockingFlush(SystemKeyspace.java:368)\n"
            + "org.apache.cassandra.db.SystemKeyspace.updateTokens(SystemKeyspace.java:363)\n"
            + "org.apache.cassandra.service.StorageService.setTokens(StorageService.java:216)\n"
            + "org.apache.cassandra.service.StorageService.finishJoiningRing(StorageService.java:1040)\n"
            + "org.apache.cassandra.service.StorageService.joinTokenRing(StorageService.java:956)\n"
            + "org.apache.cassandra.service.StorageService.initServer(StorageService.java:692)\n"
            + "org.apache.cassandra.service.StorageService.initServer(StorageService.java:563)\n"
            + "org.apache.cassandra.service.CassandraDaemon.setup(CassandraDaemon.java:346)\n"
            + "org.apache.cassandra.service.CassandraDaemon.activate(CassandraDaemon.java:616)\n"
            + "org.apache.cassandra.service.CassandraDaemon.main(CassandraDaemon.java:775)\n";
    static final String system_schema = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.SystemKeyspace.forceBlockingFlush(SystemKeyspace.java:368)\n"
            + "org.apache.cassandra.schema.LegacySchemaTables.flushSchemaTables(LegacySchemaTables.java:135)\n"
            + "org.apache.cassandra.schema.LegacySchemaTables.mergeSchema(LegacySchemaTables.java:264)\n"
            + "org.apache.cassandra.schema.LegacySchemaTables.mergeSchema(LegacySchemaTables.java:248)\n"
            + "org.apache.cassandra.service.MigrationManager$2.runMayThrow(MigrationManager.java:378)\n"
            + "org.apache.cassandra.utils.WrappedRunnable.run(WrappedRunnable.java:28)\n"
            + "java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n"
            + "java.util.concurrent.FutureTask.run(FutureTask.java:266)\n"
            + "java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)\n"
            + "java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)\n"
            + "java.lang.Thread.run(Thread.java:750)\n";

    static final String user_table1 = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceBlockingFlush(ColumnFamilyStore.java:797)\n"
            + "org.apache.cassandra.service.StorageService.forceKeyspaceFlush(StorageService.java:2977)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n"
            + "sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
            + "java.lang.reflect.Method.invoke(Method.java:498)\n"
            + "sun.reflect.misc.Trampoline.invoke(MethodUtil.java:72)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n"
            + "sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
            + "java.lang.reflect.Method.invoke(Method.java:498)\n"
            + "sun.reflect.misc.MethodUtil.invoke(MethodUtil.java:276)\n"
            + "com.sun.jmx.mbeanserver.StandardMBeanIntrospector.invokeM2(StandardMBeanIntrospector.java:112)\n"
            + "com.sun.jmx.mbeanserver.StandardMBeanIntrospector.invokeM2(StandardMBeanIntrospector.java:46)\n"
            + "com.sun.jmx.mbeanserver.MBeanIntrospector.invokeM(MBeanIntrospector.java:237)\n"
            + "com.sun.jmx.mbeanserver.PerInterface.invoke(PerInterface.java:138)\n"
            + "com.sun.jmx.mbeanserver.MBeanSupport.invoke(MBeanSupport.java:252)\n"
            + "com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.invoke(DefaultMBeanServerInterceptor.java:819)\n"
            + "com.sun.jmx.mbeanserver.JmxMBeanServer.invoke(JmxMBeanServer.java:801)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.doOperation(RMIConnectionImpl.java:1468)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.access$300(RMIConnectionImpl.java:76)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl$PrivilegedOperation.run(RMIConnectionImpl.java:1309)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.doPrivilegedOperation(RMIConnectionImpl.java:1401)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.invoke(RMIConnectionImpl.java:829)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n"
            + "sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
            + "java.lang.reflect.Method.invoke(Method.java:498)\n"
            + "sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:357)\n"
            + "sun.rmi.transport.Transport$1.run(Transport.java:200)\n"
            + "sun.rmi.transport.Transport$1.run(Transport.java:197)\n"
            + "java.security.AccessController.doPrivileged(Native Method)\n"
            + "sun.rmi.transport.Transport.serviceCall(Transport.java:196)\n"
            + "sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:573)\n"
            + "sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:834)\n"
            + "sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:688)\n"
            + "java.security.AccessController.doPrivileged(Native Method)\n"
            + "sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:687)\n"
            + "java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)\n"
            + "java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)\n"
            + "java.lang.Thread.run(Thread.java:750)\n";

    static final String user_table2 = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.reload(ColumnFamilyStore.java:203)\n"
            + "org.apache.cassandra.config.Schema.updateTable(Schema.java:565)\n"
            + "org.apache.cassandra.schema.LegacySchemaTables.mergeTables(LegacySchemaTables.java:344)\n"
            + "org.apache.cassandra.schema.LegacySchemaTables.mergeSchema(LegacySchemaTables.java:272)\n"
            + "org.apache.cassandra.schema.LegacySchemaTables.mergeSchema(LegacySchemaTables.java:248)\n"
            + "org.apache.cassandra.service.MigrationManager$2.runMayThrow(MigrationManager.java:378)\n"
            + "org.apache.cassandra.utils.WrappedRunnable.run(WrappedRunnable.java:28)\n"
            + "java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n"
            + "java.util.concurrent.FutureTask.run(FutureTask.java:266)\n"
            + "java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)\n"
            + "java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)\n"
            + "java.lang.Thread.run(Thread.java:750)\n";

    static final String user_table3 = "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.index.AbstractSimplePerColumnSecondaryIndex.forceBlockingFlush(AbstractSimplePerColumnSecondaryIndex.java:121)\n"
            + "org.apache.cassandra.db.index.SecondaryIndex.buildIndexBlocking(SecondaryIndex.java:235)\n"
            + "org.apache.cassandra.db.index.SecondaryIndex$1.run(SecondaryIndex.java:281)\n"
            + "java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\n"
            + "java.util.concurrent.FutureTask.run(FutureTask.java:266)\n"
            + "java.lang.Thread.run(Thread.java:750)\n"
            + "[hklog] table = system.schema_keyspaces\n";
    static final String user_table4 = "[hklog] table = myks.monkey_species\n"
            + "java.lang.Thread.getStackTrace(Thread.java:1564)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtable(ColumnFamilyStore.java:700)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.switchMemtableIfCurrent(ColumnFamilyStore.java:681)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceFlush(ColumnFamilyStore.java:756)\n"
            + "org.apache.cassandra.db.ColumnFamilyStore.forceBlockingFlush(ColumnFamilyStore.java:797)\n"
            + "org.apache.cassandra.service.StorageService.forceKeyspaceFlush(StorageService.java:2977)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n"
            + "sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
            + "java.lang.reflect.Method.invoke(Method.java:498)\n"
            + "sun.reflect.misc.Trampoline.invoke(MethodUtil.java:72)\n"
            + "sun.reflect.GeneratedMethodAccessor5.invoke(Unknown Source)\n"
            + "sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
            + "java.lang.reflect.Method.invoke(Method.java:498)\n"
            + "sun.reflect.misc.MethodUtil.invoke(MethodUtil.java:276)\n"
            + "com.sun.jmx.mbeanserver.StandardMBeanIntrospector.invokeM2(StandardMBeanIntrospector.java:112)\n"
            + "com.sun.jmx.mbeanserver.StandardMBeanIntrospector.invokeM2(StandardMBeanIntrospector.java:46)\n"
            + "com.sun.jmx.mbeanserver.MBeanIntrospector.invokeM(MBeanIntrospector.java:237)\n"
            + "com.sun.jmx.mbeanserver.PerInterface.invoke(PerInterface.java:138)\n"
            + "com.sun.jmx.mbeanserver.MBeanSupport.invoke(MBeanSupport.java:252)\n"
            + "com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.invoke(DefaultMBeanServerInterceptor.java:819)\n"
            + "com.sun.jmx.mbeanserver.JmxMBeanServer.invoke(JmxMBeanServer.java:801)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.doOperation(RMIConnectionImpl.java:1468)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.access$300(RMIConnectionImpl.java:76)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl$PrivilegedOperation.run(RMIConnectionImpl.java:1309)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.doPrivilegedOperation(RMIConnectionImpl.java:1401)\n"
            + "javax.management.remote.rmi.RMIConnectionImpl.invoke(RMIConnectionImpl.java:829)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
            + "sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n"
            + "sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
            + "java.lang.reflect.Method.invoke(Method.java:498)\n"
            + "sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:357)\n"
            + "sun.rmi.transport.Transport$1.run(Transport.java:200)\n"
            + "sun.rmi.transport.Transport$1.run(Transport.java:197)\n"
            + "java.security.AccessController.doPrivileged(Native Method)\n"
            + "sun.rmi.transport.Transport.serviceCall(Transport.java:196)\n"
            + "sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:573)\n"
            + "sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:834)\n"
            + "sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:688)\n"
            + "java.security.AccessController.doPrivileged(Native Method)\n"
            + "sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:687)\n"
            + "java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)\n"
            + "java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)\n"
            + "java.lang.Thread.run(Thread.java:750)\n";

    public Map<String, String> constructStackTraceMap(boolean removeJvmLib) {

        // copy to local variables
        String system_local_A = TestUtils.system_local_A;
        String system_local_B = TestUtils.system_local_B;
        String system_local_C = TestUtils.system_local_C;
        String system_local_D = TestUtils.system_local_D;
        String system_schema = TestUtils.system_schema;
        String user_table1 = TestUtils.user_table1;
        String user_table2 = TestUtils.user_table2;
        String user_table3 = TestUtils.user_table3;
        String user_table4 = TestUtils.user_table4;

        // for each stack trace, eliminate frame starts with sun., java., javax.,
        // com.sun.
        if (removeJvmLib) {
            Set<String> blackList = new HashSet<>();
            blackList.add("sun\\..*\\n");
            blackList.add("java\\..*\\n");
            blackList.add("javax\\..*\\n");
            blackList.add("com.sun\\..*\\n");
            for (String blackListPattern : blackList) {
                system_local_A = system_local_A.replaceAll(blackListPattern, "");
                system_local_B = system_local_B.replaceAll(blackListPattern, "");
                system_local_C = system_local_C.replaceAll(blackListPattern, "");
                system_local_D = system_local_D.replaceAll(blackListPattern, "");
                system_schema = system_schema.replaceAll(blackListPattern, "");
                user_table1 = user_table1.replaceAll(blackListPattern, "");
                user_table2 = user_table2.replaceAll(blackListPattern, "");
                user_table3 = user_table3.replaceAll(blackListPattern, "");
                user_table4 = user_table4.replaceAll(blackListPattern, "");
            }
        }

        Map<String, String> stackTraces = new HashMap<>();
        // add all
        stackTraces.put("system_local_A", system_local_A);
        stackTraces.put("system_local_B", system_local_B);
        stackTraces.put("system_local_C", system_local_C);
        stackTraces.put("system_local_D", system_local_D);
        stackTraces.put("system_schema", system_schema);
        stackTraces.put("user_table1", user_table1);
        stackTraces.put("user_table2", user_table2);
        stackTraces.put("user_table3", user_table3);
        stackTraces.put("user_table4", user_table4);

        // print all stack traces
        // for (Map.Entry<String, String> entry : stackTraces.entrySet()) {
        // System.out.println(entry.getKey() + " : " + entry.getValue());
        // }
        return stackTraces;
    }

    @Test
    public void test1() {
        Map<String, String> stackTraces = constructStackTraceMap(false);
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        // compute distance between all paris

        int distanceThreshold = 500;

        // collect pairs with larger than threshold and also smaller than threshold in a
        // map
        Map<String, Map<String, Integer>> distanceMap1 = new HashMap<>();
        Map<String, Map<String, Integer>> distanceMap2 = new HashMap<>();

        for (Map.Entry<String, String> entry1 : stackTraces.entrySet()) {
            for (Map.Entry<String, String> entry2 : stackTraces.entrySet()) {
                if (entry1.getKey().equals(entry2.getKey())) {
                    continue;
                }
                int distance = levenshteinDistance.apply(entry1.getValue(), entry2.getValue());
                if (distance < distanceThreshold) {
                    distanceMap1.putIfAbsent(entry1.getKey(), new HashMap<>());
                    distanceMap1.get(entry1.getKey()).put(entry2.getKey(), distance);
                } else {
                    distanceMap2.putIfAbsent(entry1.getKey(), new HashMap<>());
                    distanceMap2.get(entry1.getKey()).put(entry2.getKey(), distance);
                }
            }
        }
        // print pairs with distance smaller than threshold
        System.out.println("Pairs with distance smaller than threshold");
        for (Map.Entry<String, Map<String, Integer>> entry : distanceMap1.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println();

        // print pairs with distance larger than threshold
        System.out.println("Pairs with distance larger than threshold");
        for (Map.Entry<String, Map<String, Integer>> entry : distanceMap2.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    @Test
    public void grouping() {
        Map<String, String> stackTraces = constructStackTraceMap(false);
        // compute a reverse map
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : stackTraces.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }

        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        // compute distance between all paris

        int distanceThreshold = 600;

        Map<String, Integer> stacktrace2groupId = new HashMap<>();
        int groupId = 0;

        for (String stacktrace : stackTraces.values()) {
            if (stacktrace2groupId.containsKey(stacktrace)) {
                continue;
            }
            // find whether a similar one exists
            int smallestDistance = Integer.MAX_VALUE;
            int closestGroupId = -1;
            for (Map.Entry<String, Integer> entry : stacktrace2groupId.entrySet()) {
                String existingStacktrace = entry.getKey();
                int distance = levenshteinDistance.apply(stacktrace, existingStacktrace);
                if (distance < distanceThreshold) {
                    smallestDistance = distance;
                    closestGroupId = entry.getValue();
                    break;
                }
            }
            if (smallestDistance < distanceThreshold) {
                stacktrace2groupId.put(stacktrace, closestGroupId);
            } else {
                stacktrace2groupId.put(stacktrace, groupId);
                groupId++;
            }
        }

        // print number of groups
        System.out.println("Number of groups: " + groupId + "\n");

        // print group id for each stack trace
        System.out.println("Group id for each stack trace");
        for (Map.Entry<String, Integer> entry : stacktrace2groupId.entrySet()) {
            // print group id first, then stack trace
            System.out.println(entry.getValue() + " : " + reverseMap.get(entry.getKey()));
        }
    }

    @Test
    public void groupByMappingFrameToSymbol() {
        // Hash each frame into a symbol to compute the distance
        List<String> system_local_A_list = Utils.mapStackTraceToSymbol(system_local_A);
        List<String> system_local_B_list = Utils.mapStackTraceToSymbol(system_local_B);
        List<String> system_local_C_list = Utils.mapStackTraceToSymbol(system_local_C);
        List<String> system_local_D_list = Utils.mapStackTraceToSymbol(system_local_D);
        List<String> system_schema_list = Utils.mapStackTraceToSymbol(system_schema);
        List<String> user_table1_list = Utils.mapStackTraceToSymbol(user_table1);
        List<String> user_table2_list = Utils.mapStackTraceToSymbol(user_table2);
        List<String> user_table3_list = Utils.mapStackTraceToSymbol(user_table3);
        List<String> user_table4_list = Utils.mapStackTraceToSymbol(user_table4);

        // store them in map
        Map<String, List<String>> stackTraces = new HashMap<>();
        stackTraces.put("system_local_A", system_local_A_list);
        stackTraces.put("system_local_B", system_local_B_list);
        stackTraces.put("system_local_C", system_local_C_list);
        stackTraces.put("system_local_D", system_local_D_list);
        stackTraces.put("system_schema", system_schema_list);
        stackTraces.put("user_table1", user_table1_list);
        stackTraces.put("user_table2", user_table2_list);
        stackTraces.put("user_table3", user_table3_list);
        stackTraces.put("user_table4", user_table4_list);

        // reverse
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : stackTraces.entrySet()) {
            reverseMap.put(entry.getValue().toString(), entry.getKey());
        }

        // Compute the distance between each pair of stack traces
        // Utils.computeEditDistance(system_local_A_list, system_local_B_list);
        for (Map.Entry<String, List<String>> entry1 : stackTraces.entrySet()) {
            for (Map.Entry<String, List<String>> entry2 : stackTraces.entrySet()) {
                if (entry1.getKey().equals(entry2.getKey())) {
                    continue;
                }
                int distance = Utils.computeEditDistance(entry1.getValue(), entry2.getValue());
                System.out.println(entry1.getKey() + " : " + entry2.getKey() + " : " + distance);
            }
        }

        // compute distance between all paris
        int distanceThreshold = 10;

        // string format => list format => group id
        Map<String, Integer> stacktrace2groupId = new HashMap<>();
        Map<String, List<String>> stacktrace2ListForm = new HashMap<>();

        int groupId = 0;

        for (List<String> stacktrace : stackTraces.values()) {
            // store the string form
            if (stacktrace2ListForm.containsKey(stacktrace.toString())) {
                continue;
            }
            // find whether a similar one exists
            int smallestDistance = Integer.MAX_VALUE;
            int closestGroupId = -1;
            for (Map.Entry<String, List<String>> entry : stacktrace2ListForm.entrySet()) {
                List<String> existingStacktrace = entry.getValue();
                int distance = Utils.computeEditDistance(stacktrace, existingStacktrace);
                if (distance < distanceThreshold) {
                    smallestDistance = distance;
                    assert stacktrace2groupId.containsKey(entry.getKey());
                    closestGroupId = stacktrace2groupId.get(entry.getKey());
                    break;
                }
            }
            if (smallestDistance < distanceThreshold) {
                stacktrace2groupId.put(stacktrace.toString(), closestGroupId);
                stacktrace2ListForm.put(stacktrace.toString(), stacktrace);
            } else {
                stacktrace2groupId.put(stacktrace.toString(), groupId);
                stacktrace2ListForm.put(stacktrace.toString(), stacktrace);
                groupId++;
            }
        }

        // print number of groups
        System.out.println("Number of groups: " + groupId + "\n");

        // print group id for each stack trace
        System.out.println("Group id for each stack trace");
        for (Map.Entry<String, Integer> entry : stacktrace2groupId.entrySet()) {
            // print group id first, then stack trace
            System.out.println(entry.getValue() + " : " + reverseMap.get(entry.getKey()));
        }
    }
}
