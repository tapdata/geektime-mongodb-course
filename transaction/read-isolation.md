# 写冲突示例

## 准备文档

```javascript
use test
db.readIsolation.drop();
db.readIsolation.insertMany([{
    x: 1
}, {
    x: 2
}]);
```

## 读隔离

开启一个命令行窗口，在窗口中执行以下准备工作：

```javascript
use test
var session = db.getMongo().startSession();
session.startTransaction({readConcern: {level: "snapshot"}, writeConcern: {w: "majority"}});
var coll = session.getDatabase('test').getCollection("readIsolation");
```

在同一个窗口中执行：

```javascript
coll.updateOne({x: 1}, {$set: {y: 1}});
coll.findOne({x: 1});   // 返回：{x: 1, y: 1}
db.readIsolation.findOne({x: 1});   // 返回：{x: 1}
session.abortTransaction();
```

## 事务的快照读(可重复读)

开启一个命令行窗口，在窗口中执行以下准备工作：

```javascript
use test
var session = db.getMongo().startSession();
session.startTransaction({readConcern: {level: "snapshot"}, writeConcern: {w: "majority"}});
var coll = session.getDatabase('test').getCollection("readIsolation");
```

在同一个窗口中执行：

```javascript
coll.findOne({x: 1});   // 返回：{x: 1}
db.readIsolation.updateOne({x: 1}, {$set: {y: 1}});
db.readIsolation.findOne({x: 1});   // 返回：{x: 1, y: 1}
coll.findOne({x: 1});   // 返回：{x: 1}
session.abortTransaction();
```
