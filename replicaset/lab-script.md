# 实验：搭建复制集

## 准备工作

安装步骤请参考“1.4 实验：安装Mongo”。

- Windows系统请事先配置好环境变量以方便启动`mongod`进程；
- Linux和Mac系统请配置`PATH`变量；

## 搭建复制集

### 创建数据目录

MongoDB启动时将使用一个数据目录存放所有数据文件。我们将为3个复制集节点创建各自的数据目录。

Linux/MacOS:

```bash
mkdir -p /data{1,2,3}
```

Windows:

```cmd
md c:\data1
md c:\data2
md c:\data3
```

### 准备配置文件

正常情况下复制集的每个`mongod`进程应该位于不同的服务器。我们现在在一台机器上运行3个进程，因此要为它们各自配置：

- 不同的端口。示例中将使用28017/28018/28019.
- 不同的数据目录。示例中将使用：
  - `/data1`或`c:\data1`
  - `/data2`或`c:\data2`
  - `/data3`或`c:\data3`
- 不同日志文件路径。示例中将使用：
  - `/data1/mongod.log`或`c:\data1\mongod.log`
  - `/data2/mongod.log`或`c:\data2\mongod.log`
  - `/data3/mongod.log`或`c:\data3\mongod.log`

这些配置文件标准格式如下，请修改必要的参数完成3个实例各自的配置文件：

Linux/Mac:

```yaml
# /data1/mongod.conf
systemLog:
  destination: file
  path: /data1/mongod.log   # 日志文件路径
  logAppend: true
storage:
  dbPath: /data1    # 数据目录
net:
  bindIp: 0.0.0.0
  port: 28017   # 端口
replication:
  replSetName: rs0
processManagement:
  fork: true
```

Windows:

```yaml
# c:\data1\mongod.conf
systemLog:
  destination: file
  path: c:\data1\mongod.log   # 日志文件路径
  logAppend: true
storage:
  dbPath: c:\data1    # 数据目录
net:
  bindIp: 0.0.0.0
  port: 28017   # 端口
replication:
  replSetName: rs0
```

### 执行进程

Linux/Mac:

```bash
mongod -f /data1/mongod.conf
mongod -f /data2/mongod.conf
mongod -f /data3/mongod.conf
```

注意：如果启用了SELinux，可能阻止上述进程启动。简单起见请关闭SELinux。

Windows:

```cmd
mongod -f c:\data1\mongod.conf
mongod -f c:\data2\mongod.conf
mongod -f c:\data3\mongod.conf
```

因为Windows不支持fork，以上命令需要在3个不同的窗口执行，执行后不可关闭窗口否则进程将直接结束。

### 配置复制集

进入mongo shell:

```bash
mongo --port 28017
```

创建复制集：

```javascript
rs.initiate({
    _id: "rs0",
    members: [{
        _id: 0,
        host: "localhost:28017"
    },{
        _id: 1,
        host: "localhost:28018"
    },{
        _id: 2,
        host: "localhost:28019"
    }]
})
```

查看复制集状态：

```javascript
rs.status()
// 输出信息
{
    "set" : "rs0",
    "date" : ISODate("2019-11-03T09:27:49.555Z"),
    "myState" : 1,
    "term" : NumberLong(1),
    "syncingTo" : "",
    "syncSourceHost" : "",
    "syncSourceId" : -1,
    "heartbeatIntervalMillis" : NumberLong(2000),
    "optimes" : {
        "lastCommittedOpTime" : {
            "ts" : Timestamp(1572773266, 1),
            "t" : NumberLong(1)
        },
        "appliedOpTime" : {
            "ts" : Timestamp(1572773266, 1),
            "t" : NumberLong(1)
        },
        "durableOpTime" : {
            "ts" : Timestamp(1572773266, 1),
            "t" : NumberLong(1)
        }
    },
    "members" : [
        {
            "_id" : 0,
            "name" : "localhost:28017",
            "health" : 1,
            "state" : 1,
            "stateStr" : "PRIMARY",
            "uptime" : 208,
            "optime" : {
                "ts" : Timestamp(1572773266, 1),
                "t" : NumberLong(1)
            },
            "optimeDate" : ISODate("2019-11-03T09:27:46Z"),
            "syncingTo" : "",
            "syncSourceHost" : "",
            "syncSourceId" : -1,
            "infoMessage" : "could not find member to sync from",
            "electionTime" : Timestamp(1572773214, 1),
            "electionDate" : ISODate("2019-11-03T09:26:54Z"),
            "configVersion" : 1,
            "self" : true,
            "lastHeartbeatMessage" : ""
        },
        {
            "_id" : 1,
            "name" : "localhost:28018",
            "health" : 1,
            "state" : 2,
            "stateStr" : "SECONDARY",
            "uptime" : 66,
            "optime" : {
                "ts" : Timestamp(1572773266, 1),
                "t" : NumberLong(1)
            },
            "optimeDurable" : {
                "ts" : Timestamp(1572773266, 1),
                "t" : NumberLong(1)
            },
            "optimeDate" : ISODate("2019-11-03T09:27:46Z"),
            "optimeDurableDate" : ISODate("2019-11-03T09:27:46Z"),
            "lastHeartbeat" : ISODate("2019-11-03T09:27:48.410Z"),
            "lastHeartbeatRecv" : ISODate("2019-11-03T09:27:47.875Z"),
            "pingMs" : NumberLong(0),
            "lastHeartbeatMessage" : "",
            "syncingTo" : "localhost:28017",
            "syncSourceHost" : "localhost:28017",
            "syncSourceId" : 0,
            "infoMessage" : "",
            "configVersion" : 1
        },
        {
            "_id" : 2,
            "name" : "localhost:28019",
            "health" : 1,
            "state" : 2,
            "stateStr" : "SECONDARY",
            "uptime" : 66,
            "optime" : {
                "ts" : Timestamp(1572773266, 1),
                "t" : NumberLong(1)
            },
            "optimeDurable" : {
                "ts" : Timestamp(1572773266, 1),
                "t" : NumberLong(1)
            },
            "optimeDate" : ISODate("2019-11-03T09:27:46Z"),
            "optimeDurableDate" : ISODate("2019-11-03T09:27:46Z"),
            "lastHeartbeat" : ISODate("2019-11-03T09:27:48.410Z"),
            "lastHeartbeatRecv" : ISODate("2019-11-03T09:27:47.929Z"),
            "pingMs" : NumberLong(0),
            "lastHeartbeatMessage" : "",
            "syncingTo" : "localhost:28018",
            "syncSourceHost" : "localhost:28018",
            "syncSourceId" : 1,
            "infoMessage" : "",
            "configVersion" : 1
        }
    ],
    "ok" : 1
}
```

简单使用：

```javascript
show dbs
// 结果
admin  0.000GB
local  0.000GB

use local
show tables
// 结果
me
oplog.rs
replset.election
replset.minvalid
startup_log
system.replset
```

### 调整复制集配置

```javascript
var conf = rs.conf()
// 将0号节点的优先级调整为10
conf.members[0].priority = 10;
// 将1号节点调整为hidden节点
conf.members[1].hidden = true;
// hidden节点必须配置{priority: 0}
conf.members[1].priority = 0;
// 应用以上调整
rs.reconfig(conf);
```
