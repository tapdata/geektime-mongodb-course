package org.geekbang.time.spark;

import com.github.javafaker.Faker;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Field;
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.config.ReadConfig;
import com.mongodb.spark.config.WriteConfig;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Aggregates.addFields;

public class SparkDemo extends MongoBase {
    public static final int BATCH_SIZE = 100;
    public static final int TOTAL_COUNT = 100000;

    /**
     * 生成`TOTAL_COUNT`条模拟数据供稍后Spark计算使用
     */
    public void mock() {
        MongoCollection coll = this.getInputCollection();

        Faker faker = new Faker();
        List<Document> data = new ArrayList<Document>();
        for(int i = 0; i < TOTAL_COUNT; i++) {
            Document user = new Document();
            user.put("name", faker.name().fullName());
            user.put("address", faker.address().fullAddress());
            user.put("birthday", faker.date().birthday());
            user.put("favouriteColor", faker.color().name());
            data.add(user);
            // 使用批量方式插入以提高效率
            if (i % BATCH_SIZE == 0) {
                coll.insertMany(data);
                data.clear();
            }
        }
        if (data.size() > 0) {
            coll.insertMany(data);
        }
        System.out.println(String.format("%d documents generated!", TOTAL_COUNT));
    }

    /**
     * 基于生成的数据在Spark中进行统计计算。
     */
    public void spartCompute() {
        SparkSession spark = SparkSession.builder()
                .master("local")
                .appName("SparkDemo")
                .config("spark.mongodb.input.uri", this.config.getProperty("input"))
                .config("spark.mongodb.output.uri", this.config.getProperty("output"))
                .getOrCreate();
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        ReadConfig rc = ReadConfig.create(jsc)
                .withOption("readPreference.name", "secondaryPreferred")
                .withOption("collection", "User");  // 与input uri中一致，可省略
        // 加载数据时通过Aggregation获取了生日月份
        List pipeline = Arrays.asList(addFields(new Field("month", new Document("$month", "$birthday"))));
        JavaMongoRDD<Document> rdd = MongoSpark.load(jsc, rc).withPipeline(pipeline);
        Dataset<Row> ds = rdd.toDF();
        ds.createOrReplaceTempView("User");
        // 按月份计算每个月出生的人最喜欢的颜色是什么
        Dataset<Row> result = spark.sql("SELECT b.month AS _id, b.favouriteColor, b.qty\n" +
                "FROM (\n" +
                "    SELECT a.*, \n" +
                "        ROW_NUMBER() OVER (PARTITION BY month ORDER BY qty desc) AS seq\n" +
                "    FROM (\n" +
                "        SELECT month, favouriteColor, COUNT(1) AS qty\n" +
                "        FROM User\n" +
                "        GROUP BY month, favouriteColor\n" +
                "    ) AS a\n" +
                ") AS b\n" +
                "WHERE b.seq = 1\n" +
                "ORDER BY _id ASC");

        WriteConfig wc = WriteConfig.create(jsc).withOption("writeConcern.w", "majority");
        MongoSpark.save(result, wc);
        jsc.close();
    }

    public void display() {
        MongoCollection coll = this.getOutputCollection();
        FindIterable<Document> result = coll.find();
        for(MongoCursor<Document> iter = result.iterator(); iter.hasNext();) {
            Document doc = iter.next();
            System.out.println(doc);
        }
    }

    public static void main(String[] args) throws IOException {
        SparkDemo demo = new SparkDemo();
        demo.mock();
        demo.spartCompute();
        demo.display();
    }
}
