package uni.apt.core;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
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




}
