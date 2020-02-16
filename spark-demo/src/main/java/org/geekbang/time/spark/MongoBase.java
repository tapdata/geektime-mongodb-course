package org.geekbang.time.spark;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public abstract class MongoBase {
    protected MongoClient inputClient;
    protected String inputDatabase;
    protected String inputCollection;
    protected MongoClient outputClient;
    protected String outputDatabase;
    protected String outputCollection;
    protected Config config;

    /**
     * 构造函数
     */
    public MongoBase() {
        // 输入数据库相关
        this.config = new Config();
        String input = this.config.getProperty("input");
        int index = input.lastIndexOf(".");
        String connStr = input.substring(0, index);
        ConnectionString conn = new ConnectionString(connStr);
        this.inputClient = MongoClients.create(conn);
        this.inputDatabase = conn.getDatabase();
        this.inputCollection = input.substring(index + 1);

        // 输出数据库相关
        String output = config.getProperty("output");
        index = output.lastIndexOf(".");
        connStr = output.substring(0, index);
        conn = new ConnectionString(connStr);
        this.outputClient = MongoClients.create(conn);
        this.outputDatabase = conn.getDatabase();
        this.outputCollection = output.substring(index + 1);
    }


    /**
     * 获取输入数据库
     * @return 输入数据库
     */
    protected MongoDatabase getInputDatabase() {
        MongoDatabase db = this.inputClient.getDatabase(this.inputDatabase);
        return db;
    }

    /**
     * 获取输入数据表
     * @return 输入数据表
     */
    protected MongoCollection getInputCollection() {
        MongoCollection coll = this.getInputDatabase().getCollection(this.inputCollection);
        return coll;
    }

    /**
     * 获取输出数据库
     * @return 输出数据库
     */
    protected MongoDatabase getOutputDatabase() {
        MongoDatabase db = this.outputClient.getDatabase(this.outputDatabase);
        return db;
    }

    /**
     * 获取输出数据表
     * @return 输出数据表
     */
    protected MongoCollection getOutputCollection() {
        MongoCollection coll = this.getOutputDatabase().getCollection(this.outputCollection);
        return coll;
    }
}
