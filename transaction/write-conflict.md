# 写冲突示例

## 准备文档

```javascript
use test
db.writeConflict.insertMany([{
    x: 1
}, {
    x: 2
}])
```

## 开启一个多文档事务

开启两个命令行窗口，在两个窗口中分别执行以下准备工作：

```javascript
use test
var session = db.getMongo().startSession();
session.startTransaction({readConcern: {level: "snapshot"}, writeConcern: {w: "majority"}});
var coll = session.getDatabase('test').getCollection("writeConflict");
```

在一个窗口中执行：

```javascript
coll.updateOne({x: 1}, {$set: {y: 1}}); // 正常结束
```

在另一个窗口中执行：

```javascript
coll.updateOne({x: 1}, {$set: {y: 2}}); // 写冲突
```
