package uni.apt.core;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import uni.apt.Defaults;

public class OnlineDB {
    public static MongoClient client;
    public static MongoDatabase base;

    private static boolean _ready = false;



    public static void init(){
        client = MongoClients.create(Defaults.CONNECTION_STR);

        base = client.getDatabase(Defaults.BASE_NAME);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close(); //close the connection on shutdown
        }));

        _ready = true;
    }

    public static boolean ready(){
        return _ready;
    }



}
