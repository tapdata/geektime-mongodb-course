# Hellow World

## 准备工作

### 创建mongod实例

为了在实验中使用MongoDB，我们先启动一个临时mongod实例：

```bash
mongod --dbpath /data --port 27017
```

以上指令将使用`/data`作为数据目录（如果不存在请创建），在默认端口`27017`启动一个临时mongod实例。

### 安装MongoDB驱动

在使用MongoDB之前必须先安装用于访问数据库的驱动程序。这里我们以Python为例给大家演示：

```bash
pip install pymongo
```

### 检查驱动

在python交互模式下导入pymongo，检查驱动是否已正确安装：

```python
import pymongo
pymongo.version
# 结果：
# '3.7.2'
```

## 使用驱动连接MongoDB

使用驱动连接到MongoDB集群只需要指定MongoDB连接字符串即可。其基本格式可以参考文档: [Connection String URI Format](https://docs.mongodb.com/manual/reference/connection-string/)

连接字符串的大部分参数在不同编程语言之间是通用的。本实验中，我们使用以下连接字符串：

```python
uri = "mongodb://127.0.0.1:27017/?minPoolSize=10&maxPoolSize=100"
```

这里指定了连接池保持连接的最小数量是10，最大连接数100。

要连接到MongoDB，在Python交互模式下执行以下语句即可：

```python
from pymongo import MongoClient

uri = "mongodb://127.0.0.1:27017/?minPoolSize=10&maxPoolSize=100"
client = MongoClient(uri)
print client
# 结果：
# MongoClient(host=['127.0.0.1:27017'], document_class=dict, tz_aware=False, connect=True, minpoolsize=10, maxpoolsize=100)
```

## 执行CRUD操作

在上述过程中创建`MongoClient`后，我们将使用它来完成CRUD操作。假设我们将使用`foo`数据库的`bar`集合来完成测试：

```python
test_db = client["foo"]
bar_coll = test_db["bar"]
result = bar_coll.insert_one({
    "string": "Hello World"
})
print result
# 结果：
# <pymongo.results.InsertOneResult object at 0x1054ebab8>
```

我们将结果查询出来看看是否正确：

```python
result = bar_coll.find_one({
    "string": "Hello World"
})
print result
# 结果：
# {u'_id': ObjectId('5dbeb2290f08fbb017e0e583'), u'string': u'Hello World'}
```

我们可以注意到这个对象上添加了一个`_id`，它是MongoDB对每个对象赋予的唯一主键，如果没有指定则由系统自动分配一个`ObjectId`来填充。

现在我们尝试对刚才的文档进行一点修改，然后再查询它：

```python
result = bar_coll.update_one({
    "string": "Hello World"
}, {
    "$set": {
        "from": "Tom the cat"
    }
})
result = bar_coll.find_one({
    "string": "Hello World"
})
print result
# 结果：
# {u'_id': ObjectId('5dbeb2290f08fbb017e0e583'), u'from': u'Tom the cat', u'string': u'Hello World'}
```

最后我们将它从表中删除：

```python
result = bar_coll.delete_one({
    "string": "Hello World"
})
print result
# 结果：
# <pymongo.results.DeleteResult object at 0x105501440>
```

## 非交互模式下执行

除了在Python交互模式下执行，我们当然也可以将代码放在一个文件中执行。把等价的代码放到文件中：

```python
# hello_world.py
from pymongo import MongoClient

uri = "mongodb://127.0.0.1:27017/?minPoolSize=10&maxPoolSize=100"
client = MongoClient(uri)
print client

test_db = client["foo"]
bar_coll = test_db["bar"]
result = bar_coll.insert_one({
    "string": "Hello World"
})
print result

result = bar_coll.find_one({
    "string": "Hello World"
})
print result

result = bar_coll.update_one({
    "string": "Hello World"
}, {
    "$set": {
        "from": "Tom the cat"
    }
})
result = bar_coll.find_one({
    "string": "Hello World"
})
print result

result = bar_coll.delete_one({
    "string": "Hello World"
})
print result

client.close()
```

执行这个文件：

```bash
python hello_world.py
```

我们将得到跟之前相同的结果。
