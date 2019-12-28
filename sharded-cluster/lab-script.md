# 实验：分片集群搭建及扩容

## 准备工作

### 为服务器设置域名

我们将使用域名来搭建分片集。使用域名也是搭建MongoDB集群的推荐方式，它在以后发生迁移时将带来很多便利。

```bash
echo "54.238.247.149 geekdemo1 member1.example.com member2.example.com" >> /etc/hosts
echo "54.178.197.163 geekdemo2 member3.example.com member4.example.com" >> /etc/hosts
echo "13.230.147.181 geekdemo3 member5.example.com member6.example.com" >> /etc/hosts
```
### 服务器分工

在本例中，我们将使用:

- `member1`/`member3`/`member5`搭建`shard1`和`config`
- `member2`/`member4`/`member6`搭建`shard2`和`mongos`

||member1|member2|member3|member4|member5|member6|
|-----|-----|-----|-----|-----|-----|-----|
|shard1|&#10003;||&#10003;||&#10003;||
|shard2||&#10003;||&#10003;||&#10003;|
|config|&#10003;||&#10003;||&#10003;||
|mongos||&#10003;||&#10003;||&#10003;|

### 准备分片目录

在各服务器上创建数据目录，我们使用`/data`，请按自己需要修改为其他目录：

- 在`member1`/`member3`/`member5`上执行以下命令：

    ```bash
    mkdir -p /data/shard1/
    mkdir -p /data/config/
    ```

- 在`member2`/`member4`/`member6`上执行以下命令：

    ```bash
    mkdir -p /data/shard2/
    mkdir -p /data/mongos/
    ```

## 搭建分片

### 搭建shard1

在`member1`/`member3`/`member5`上执行以下命令。注意以下参数：

- `shardsvr`: 表示这不是一个普通的复制集，而是分片集的一部分；
- `wiredTigerCacheSizeGB`: 该参数表示MongoDB能够使用的缓存大小。默认值为`(RAM - 1GB) / 2`。
    - 不建议配置超过默认值，有OOM的风险；
    - 因为我们当前测试会在一台服务器上运行多个实例，因此配置了较小的值；
- `bind_ip`: 生产环境中强烈建议不要绑定外网IP，此处为了方便演示绑定了所有IP地址。类似的道理，生产环境中应开启认证`--auth`，此处为演示方便并未使用；

```bash
mongod --bind_ip 0.0.0.0 --replSet shard1 --dbpath /data/shard1 --logpath /data/shard1/mongod.log --port 27010 --fork --shardsvr --wiredTigerCacheSizeGB 1
```

用这三个实例搭建shard1复制集：

- 任意连接到一个实例，例如我们连接到`member1.example.com`：

    ```bash
    mongo --host member1.example.com:27010
    ```

- 初始化`shard1`复制集。我们使用如下配置初始化复制集：

    ```javascript
    rs.initiate({
        _id: "shard1",
        "members" : [
            {
                "_id": 0,
                "host" : "member1.example.com:27010"
            },
            {
                "_id": 1,
                "host" : "member3.example.com:27010"
            },
            {
                "_id": 2,
                "host" : "member5.example.com:27010"
            }
        ]
    });
    ```

### 搭建config

与`shard1`类似的方式，我们可以搭建`config`服务器。在`member1`/`member3`/`member5`上执行以下命令：

- 运行`config`实例：

    ```bash
    mongod --bind_ip 0.0.0.0 --replSet config --dbpath /data/config --logpath /data/config/mongod.log --port 27019 --fork --configsvr --wiredTigerCacheSizeGB 1
    ```

- 连接到`member1`：

    ```bash
    mongo --host member1.example.com:27019
    ```

- 初始化`config`复制集：
    ```javascript
    rs.initiate({
        _id: "config",
        "members" : [
            {
                "_id": 0,
                "host" : "member1.example.com:27019"
            },
            {
                "_id": 1,
                "host" : "member3.example.com:27019"
            },
            {
                "_id": 2,
                "host" : "member5.example.com:27019"
            }
        ]
    });
    ```

### 搭建mongos

mongos的搭建比较简单，我们在`member2`/`member4`/`member6`上搭建3个mongos。注意以下参数：

- `configdb`: 表示config使用的集群地址；

开始搭建：

- 运行mongos进程：

    ```bash
    mongos --bind_ip 0.0.0.0 --logpath /data/mongos/mongos.log --port 27017 --configdb config/member1.example.com:27019,member3.example.com:27019,member5.example.com:27019 --fork
    ```

- 连接到任意一个mongos，此处我们使用`member1`：

    ```bash
    mongo --host member1.example.com:27017
    ```

- 将`shard1`加入到集群中：

    ```javascript
    sh.addShard("shard1/member1.example.com:27010,member3.example.com:27010,member5.example.com:27010");
    ```

### 测试分片集

上述示例中我们搭建了一个只有1个分片的分片集。在继续之前我们先来测试一下这个分片集。

- 连接到分片集：

    ```bash
    mongo --host member1.example.com:27017
    ```
    ```javascript
    sh.status();
    ```
    ```txt
    mongos> sh.status()
    --- Sharding Status ---
      sharding version: {
        "_id" : 1,
        "minCompatibleVersion" : 5,
        "currentVersion" : 6,
        "clusterId" : ObjectId("5e06b2509264c9792e19ea0f")
      }
      shards:
            {  "_id" : "shard1",  "host" : "shard1/member1.example.com:27010,member3.example.com:27010,member5.example.com:27010",  "state" : 1 }
      active mongoses:
            "4.2.2" : 2
      autosplit:
            Currently enabled: yes
      balancer:
            Currently enabled:  yes
            Currently running:  no
            Failed balancer rounds in last 5 attempts:  0
            Migration Results for the last 24 hours:
                    No recent migrations
      databases:
            {  "_id" : "config",  "primary" : "config",  "partitioned" : true }
    ```

- 创建一个分片表：

    ```javascript
    sh.enableSharding("foo");
    sh.shardCollection("foo.bar", {_id: 'hashed'});
    sh.status();
    ```
    ```txt
    ...
            {  "_id" : "foo",  "primary" : "shard1",  "partitioned" : true,  "version" : {  "uuid" : UUID("838293c5-f083-4a3b-b75e-548cfe2f6087"),  "lastMod" : 1 } }
                    foo.bar
                            shard key: { "_id" : "hashed" }
                            unique: false
                            balancing: true
                            chunks:
                                    shard1  2
                            { "_id" : { "$minKey" : 1 } } -->> { "_id" : NumberLong(0) } on : shard1 Timestamp(1, 0)
                            { "_id" : NumberLong(0) } -->> { "_id" : { "$maxKey" : 1 } } on : shard1 Timestamp(1, 1)
    ```

- 任意写入若干数据：

    ```javascript
    use foo
    for (var i = 0; i < 10000; i++) {
        db.bar.insert({i: i});
    }
    ```

### 向分片集加入新的分片

下面我们搭建`shard2`并将其加入分片集中，观察发生的效果。

使用类似`shard1`的方式搭建`shard2`。在`member2`/`member4`/`member6`上执行以下命令：

```bash
mongod --bind_ip 0.0.0.0 --replSet shard2 --dbpath /data/shard2 --logpath /data/shard2/mongod.log --port 27011 --fork --shardsvr --wiredTigerCacheSizeGB 1
```

用这三个实例搭建`shard2`复制集：

- 任意连接到一个实例，例如我们连接到`member2.example.com`：

    ```bash
    mongo --host member2.example.com:27011
    ```

- 初始化`shard2`复制集。我们使用如下配置初始化复制集：

    ```javascript
    rs.initiate({
        _id: "shard2",
        "members" : [
            {
                "_id": 0,
                "host" : "member2.example.com:27011"
            },
            {
                "_id": 1,
                "host" : "member4.example.com:27011"
            },
            {
                "_id": 2,
                "host" : "member6.example.com:27011"
            }
        ]
    });
    ```

- 连接到任意一个mongos。此处使用`member1`：

    ```bash
    mongo --host member1.example.com
    ```

- 将`shard2`加入到集群中：
    ```javascript
    sh.addShard("shard2/member2.example.com:27011,member4.example.com:27011,member6.example.com:27011");
    ```
    ```txt
    {
        "shardAdded" : "shard2",
        "ok" : 1,
        "operationTime" : Timestamp(1577498687, 6),
        "$clusterTime" : {
            "clusterTime" : Timestamp(1577498687, 6),
            "signature" : {
                "hash" : BinData(0,"AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
                "keyId" : NumberLong(0)
            }
        }
    }
    ```

- 观察`sh.status()`：
    ```javascript
    sh.status();
    ```
    ```txt
    ...
    {  "_id" : "foo",  "primary" : "shard1",  "partitioned" : true,  "version" : {  "uuid" : UUID("838293c5-f083-4a3b-b75e-548cfe2f6087"),  "lastMod" : 1 } }
        foo.bar
                shard key: { "_id" : "hashed" }
                unique: false
                balancing: true
                chunks:
                        shard1  1
                        shard2  1
                { "_id" : { "$minKey" : 1 } } -->> { "_id" : NumberLong(0) } on : shard2 Timestamp(2, 0)
                { "_id" : NumberLong(0) } -->> { "_id" : { "$maxKey" : 1 } } on : shard1 Timestamp(2, 1)
    ```

可以发现原本`shard1`上的两个chunk被均衡到了`shard2`上，这就是MongoDB的自动均衡机制。




