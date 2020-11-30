# Cluster Management
In many product environments, backend need to support high throughputs and provide HA to keep robustness,
so you should need cluster management always in product env.
 
Backend provides several ways to do cluster management. Choose the one you need/want.

- [Zookeeper coordinator](#zookeeper-coordinator). Use Zookeeper to let backend instance detects and communicates
with each other.
- [Kubernetes](#kubernetes). When backend cluster are deployed inside kubernetes, you could choose this
by using k8s native APIs to manage cluster.
- [Consul](#consul). Use Consul as backend cluster management implementor, to coordinate backend instances.
- [Nacos](#nacos). Use Nacos to coordinate backend instances.
- [Etcd](#etcd). Use Etcd to coordinate backend instances.

## Zookeeper coordinator
Zookeeper is a very common and wide used cluster coordinator. Set the **cluster** module's implementor
to **zookeeper** in the yml to active. 

Required Zookeeper version, 3.4+

```yaml
cluster:
  zookeeper:
    nameSpace: ${SW_NAMESPACE:""}
    hostPort: ${SW_CLUSTER_ZK_HOST_PORT:localhost:2181}
    # Retry Policy
    baseSleepTimeMs: 1000 # initial amount of time to wait between retries
    maxRetries: 3 # max number of times to retry
    # Enable ACL
    enableACL: ${SW_ZK_ENABLE_ACL:false} # disable ACL in default
    schema: ${SW_ZK_SCHEMA:digest} # only support digest schema
    expression: ${SW_ZK_EXPRESSION:skywalking:skywalking}
```

- `hostPort` is the list of zookeeper servers. Format is `IP1:PORT1,IP2:PORT2,...,IPn:PORTn`
- `enableACL` enable [Zookeeper ACL](https://zookeeper.apache.org/doc/r3.4.1/zookeeperProgrammers.html#sc_ZooKeeperAccessControl) to control access to its znode.
- `schema` is Zookeeper ACL schemas.
- `expression` is a expression of ACL. The format of the expression is specific to the [schema](https://zookeeper.apache.org/doc/r3.4.1/zookeeperProgrammers.html#sc_BuiltinACLSchemes). 
- `hostPort`, `baseSleepTimeMs` and `maxRetries` are settings of Zookeeper curator client.

Note: 
- If `Zookeeper ACL` is enabled and `/skywalking` existed, must be sure `SkyWalking` has `CREATE`, `READ` and `WRITE` permissions. If `/skywalking` is not exists, it will be created by SkyWalking and grant all permissions to the specified user. Simultaneously, znode is granted READ to anyone.
- If set `schema` as `digest`, the password of expression is set in **clear text**. 

In some cases, oap default gRPC host and port in core are not suitable for internal communication among the oap nodes.
The following setting are provided to set the host and port manually, based on your own LAN env.
- internalComHost, the host registered and other oap node use this to communicate with current node.
- internalComPort, the port registered and other oap node use this to communicate with current node.

```yaml
zookeeper:
  nameSpace: ${SW_NAMESPACE:""}
  hostPort: ${SW_CLUSTER_ZK_HOST_PORT:localhost:2181}
  #Retry Policy
  baseSleepTimeMs: ${SW_CLUSTER_ZK_SLEEP_TIME:1000} # initial amount of time to wait between retries
  maxRetries: ${SW_CLUSTER_ZK_MAX_RETRIES:3} # max number of times to retry
  internalComHost: 172.10.4.10
  internalComPort: 11800
  # Enable ACL
  enableACL: ${SW_ZK_ENABLE_ACL:false} # disable ACL in default
  schema: ${SW_ZK_SCHEMA:digest} # only support digest schema
  expression: ${SW_ZK_EXPRESSION:skywalking:skywalking}
``` 


## Kubernetes
Require backend cluster are deployed inside kubernetes, guides are in [Deploy in kubernetes](backend-k8s.md).
Set implementor to `kubernetes`.

```yaml
cluster:
  kubernetes:
    watchTimeoutSeconds: 60
    namespace: default
    labelSelector: app=collector,release=skywalking
    uidEnvName: SKYWALKING_COLLECTOR_UID
```

## Consul
Now, consul is becoming a famous system, many of companies and developers using consul to be 
their service discovery solution. Set the **cluster** module's implementor to **consul** in 
the yml to active. 

```yaml
cluster:
  consul:
    serviceName: ${SW_SERVICE_NAME:"SkyWalking_OAP_Cluster"}
    # Consul cluster agents, example, 1. client agent, 127.0.0.1:8500 2. server agent, 10.0.0.1:8500,10.0.0.2:8500,10.0.0.3:8500
    hostPort: ${SW_CLUSTER_CONSUL_HOST_PORT:localhost:8500}
```

Same as Zookeeper coordinator,
in some cases, oap default gRPC host and port in core are not suitable for internal communication among the oap nodes.
The following setting are provided to set the host and port manually, based on your own LAN env.
- internalComHost, the host registered and other oap node use this to communicate with current node.
- internalComPort, the port registered and other oap node use this to communicate with current node.


## Nacos
Set the **cluster** module's implementor to **nacos** in 
the yml to active. 

```yaml
cluster:
  nacos:
    serviceName: ${SW_SERVICE_NAME:"SkyWalking_OAP_Cluster"}
    # Nacos cluster nodes, example: 10.0.0.1:8848,10.0.0.2:8848,10.0.0.3:8848
    hostPort: ${SW_CLUSTER_NACOS_HOST_PORT:localhost:8848}
    # Nacos Configuration namespace
    namespace: ${SW_CLUSTER_NACOS_NAMESPACE:"public"}
```

## Etcd
Set the **cluster** module's implementor to **etcd** in
the yml to active.

```yaml
cluster:
  etcd:
    serviceName: ${SW_SERVICE_NAME:"SkyWalking_OAP_Cluster"}
    #etcd cluster nodes, example: 10.0.0.1:2379,10.0.0.2:2379,10.0.0.3:2379
    hostPort: ${SW_CLUSTER_ETCD_HOST_PORT:localhost:2379}
```
