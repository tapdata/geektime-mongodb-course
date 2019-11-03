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
