# 写等待示例

## 准备文档

```javascript
use test
db.writeWait.drop();
db.writeWait.insertMany([{
    x: 1
}, {
    x: 2
}]);
```

## 写等待

开启一个命令行窗口，在窗口中执行以下准备工作：

```javascript
use test
var session = db.getMongo().startSession();
session.startTransaction({readConcern: {level: "snapshot"}, writeConcern: {w: "majority"}});
var coll = session.getDatabase('test').getCollection("writeWait");
coll.updateOne({x: 1}, {$set: {y: 1}}); // 正常结束
```

在另个窗口中执行：

```javascript
db.writeWait.updateOne({x: 1}, {$set: {y: 2}}); // 阻塞等待
```

在原窗口中执行：

```javascript
session.commitTransaction();
```
