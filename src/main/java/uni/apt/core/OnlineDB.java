package uni.apt.core;

import com.mongodb.client.*;
import org.bson.Document;
import uni.apt.Defaults;

import java.util.HashMap;

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

        IndexerDB           = base.getCollection(Defaults.INDEXER_MONGO_NAME);
        RankerSuggestionsDB = base.getCollection(Defaults.RANKER_SUGGESTIONS_DB);
        RankerPopularityDB  = base.getCollection(Defaults.RANKER_POPULARITY_DB);
        ParagraphsDB        = base.getCollection(Defaults.INDEXER_INDEXED_PARAGRAPHS);
        TitlesDB            = base.getCollection(Defaults.INDEXER_INDEXED_TITLES);
        MetaDB              = base.getCollection(Defaults.META_DB);
        CrawlerLogDB        = base.getCollection(Defaults.CRAWLER_VISITED_LOG);
        CrawlerSeedsDB      = base.getCollection(Defaults.CRAWLER_SEEDS);
        CrawlerCrawledDB    = base.getCollection(Defaults.CRAWLER_CRAWLED);

        _ready = true;
    }

    public static boolean ready(){
        return _ready;
    }

    public static MongoCollection<Document> MetaDB;

    public static MongoCollection<Document> CrawlerLogDB;
    public static MongoCollection<Document> CrawlerSeedsDB;
    public static MongoCollection<Document> CrawlerCrawledDB;

    public static MongoCollection<Document> IndexerDB;
    public static MongoCollection<Document> ParagraphsDB;
    public static MongoCollection<Document> TitlesDB;

    public static MongoCollection<Document> RankerPopularityDB;
    public static MongoCollection<Document> RankerSuggestionsDB;


    private static HashMap<String , Integer> Popularises;
    private static final Object getPopularityLock = new Object();
    public static int getPopularity(String link){
        synchronized (getPopularityLock) {
            if (Popularises == null) {
                Popularises = new HashMap<>();
                MongoIterable<Document> docs = RankerPopularityDB.find();
                for (Document d : docs){
                    Popularises.put(d.getString("link") , d.getInteger("mentions" , -2));
                }
            }
        }

        Integer i = Popularises.get(link);
        if (i == null)
            return 0;
        return i;
    }

}
