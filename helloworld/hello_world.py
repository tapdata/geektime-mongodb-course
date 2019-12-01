from pymongo import MongoClient
from pprint import pprint

uri = "mongodb://127.0.0.1:27017/"
client = MongoClient(uri)
pprint(client)


db = client["eshop"]
user_coll = db["users"]

new_user = {
	"username": "nina",
	"password": "xxxx",
	"email": "123456@qq.com"
}
result = user_coll.insert_one(new_user)
pprint(result)

result = user_coll.find_one()
pprint(result)

result = user_coll.update_one({
    "username": "nina"
}, {
    "$set": {
        "phone": "123456789"
    }
})

result = user_coll.find_one({ "username": "nina" })
pprint(result)


client.close()
