# 实验：聚合框架

## 简介

测试数据中模拟了2019年1月1日~2019年10月31日之间的订单和订单行数据，总计`100000`条。这些数据中包括以下主要字段：

- `userId`: 下单人ID；
- `name`: 订单人联系姓名；
- `orderDate`: 下单日期；
- `shippingFee`: 运费；
- `total`: 订单物品总金额（不包括运费）；
- `status`: 订单状态，取值包括`["created", "cancelled", "shipping", "fulfilled", "completed"]`
- `orderLines`: 订单包含的物品；
  - `price`: 物品售价；
  - `cost`: 物品成本；
  - `qty`: 购买件数；
  - `sku`: 产品唯一编号；

## 准备工作

### 导入实验数据

```bash
# 解压实验数据文件
tar -zxvf dump.tar.gz
# 将实验数据导入到MongoDB
mongorestore -h 127.0.0.1:27017 dump
```

### 验证导入结果

```javascript
use mock
db.orders.count()
// 执行结果：100000
db.orders.findOne()
// 执行结果：单条数据示例
```

## 实验内容

### 实验一：总销量

计算到目前为止的总销售额

- 无论订单状态
- 不限制时间范围
- 不算运费

```javascript
db.orders.aggregate([
    {
        $group: {
            _id: null,
            total: {
                $sum: "$total"
            }
        }
    }
])
// 结果：
// { "_id" : null, "total" : NumberDecimal("44019609") }
```

### 实验二：订单金额汇总

查询2019年第一季度（1月1日~3月31日）订单中已完成（completed）状态的总金额和总数量：

```javascript
db.orders.aggregate([
    {
        // 步骤1：匹配条件
        $match: {
            status: "completed",
            orderDate: {
                $gte: ISODate("2019-01-01"),
                $lt: ISODate("2019-04-01")
            }
        }
    }, {
        $group: {
            // 步骤二：聚合订单总金额、总运费、总数量
            _id: null,
            total: {
                $sum: "$total"
            },
            shippingFee: {
                $sum: "$shippingFee"
            },
            count: {
                $sum: 1
            }
        }
    }, {
        $project: {
            // 计算总金额
            grandTotal: {
                $add: ["$total", "$shippingFee"]
            },
            count: 1,
            _id: 0
        }
    }
])
// 结果：
// { "count" : 5875, "grandTotal" : NumberDecimal("2636376.00") }
```

### 实验三：计算月销量

计算前半年每个月的销售额和总订单数。

- 不算运费
- 不算取消（cancelled）状态的订单

```javascript
db.orders.aggregate([
    {
        // 步骤1：匹配条件
        $match: {
            status: {
                $ne: "cancelled"
            },
            orderDate: {
                $gte: ISODate("2019-01-01"),
                $lt: ISODate("2019-07-01")
            }
        }
    }, {
        // 步骤2：取出年月
        $project: {
            month: {
                $dateToString: {
                    date: "$orderDate",
                    format: "%G年%m月"
                }
            },
            total: 1
        }
    }, {
        // 步骤3：按年月分组汇总
        $group: {
            _id: "$month",
            total: {
                $sum: "$total"
            },
            count: {
                $sum: 1
            }
        }
    }
])
// 结果：
// { "_id" : "2019年01月", "total" : NumberDecimal("3620936"), "count" : 8249 }
// { "_id" : "2019年04月", "total" : NumberDecimal("3551291"), "count" : 8038 }
// { "_id" : "2019年06月", "total" : NumberDecimal("3496645"), "count" : 7942 }
// { "_id" : "2019年05月", "total" : NumberDecimal("3590503"), "count" : 8163 }
// { "_id" : "2019年02月", "total" : NumberDecimal("3258201"), "count" : 7387 }
// { "_id" : "2019年03月", "total" : NumberDecimal("3574185"), "count" : 8167 }
```

### 实验四：地区销量top1

计算第一季度每个州（state）销量最多的`sku`第一名。

- 只算`complete`订单；

```javascript
db.orders.aggregate([
    {
        // 步骤1：匹配条件
        $match: {
            status: "completed",
            orderDate: {
                $gte: ISODate("2019-01-01"),
                $lt: ISODate("2019-04-01")
            }
        }
    }, {
        // 步骤2：按订单行展开
        $unwind: "$orderLines"
    }, {
        // 步骤3：按sku汇总
        $group: {
            _id: {
                state: "$state",
                sku: "$orderLines.sku"
            },
            count: {
                $sum: "$orderLines.qty"
            }
        }
    }, {
        // 步骤4：按州和销量排序
        $sort: {
            "_id.state": 1,
            "count": -1
        }
    }, {
        // 步骤4：取每个州top1
        $group: {
            _id: "$_id.state",
            sku: {
                $first: "$_id.sku"
            },
            count: {
                $first: "$count"
            }
        }
    }
])
// 结果：
// { "_id" : "Wyoming", "sku" : "8181", "count" : 183 }
// { "_id" : "Wisconsin", "sku" : "9684", "count" : 195 }
// { "_id" : "West Virginia", "sku" : "9376", "count" : 170 }
// { "_id" : "North Dakota", "sku" : "2411", "count" : 243 }
// ...
```

### 实验五：统计SKU销售件数

统计每个`sku`在第一季度销售的次数。

- 不算取消（cancelled）状态的订单；
- 按销售数量降序排列；

```javascript
db.orders.aggregate([
    {
        // 步骤1：匹配条件
        $match: {
            status: {
                $ne: "cancelled"
            },
            orderDate: {
                $gte: ISODate("2019-01-01"),
                $lt: ISODate("2019-04-01")
            }
        }
    }, {
        // 步骤2：按订单行展开
        $unwind: "$orderLines"
    }, {
        // 步骤3：按sku汇总
        $group: {
            _id: "$orderLines.sku",
            count: {
                $sum: "$orderLines.qty"
            }
        }
    }, {
        $sort: {
            count: -1
        }
    }
])
// 结果：
// { "_id" : "4751", "count" : 2115 }
// { "_id" : "798", "count" : 1945 }
// { "_id" : "3863", "count" : 1913 }
// { "_id" : "2558", "count" : 1896 }
// ...
```
