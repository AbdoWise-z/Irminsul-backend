package uni.apt.core;

import ch.qos.logback.classic.Logger;
import com.mongodb.Block;
import com.mongodb.client.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;

import org.bson.Document;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpServerErrorException;
import uni.apt.Defaults;
import java.util.ArrayList;
import java.util.HashMap;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;

public class OnlineDB {
    public static MongoClient client;
    public static MongoDatabase base;

    private static boolean _ready = false;
    
    public static void init(){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.WARN);

        client = MongoClients.create(Defaults.CONNECTION_STR);

        base = client.getDatabase(Defaults.BASE_NAME);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close(); //close the connection on shutdown
        }));

        IndexerWordsDB      = base.getCollection(Defaults.INDEXER_MONGO_WORDS);
        IndexerRecordsDB    = base.getCollection(Defaults.INDEXER_MONGO_RECORDS);
        IndexerLinksDB      = base.getCollection(Defaults.INDEXER_MONGO_LINKS);

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

    public static MongoCollection<Document> IndexerWordsDB;
    public static MongoCollection<Document> IndexerLinksDB;
    public static MongoCollection<Document> IndexerRecordsDB;

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

    public static String getParagraph(long id){
        FindIterable<Document> doc = ParagraphsDB.find(new Document("index" , id));
        Document para = doc.first();
        if (para != null){
            return para.getString("text");
        }
        return null;
    }

    public static String getTitle(long id){
        FindIterable<Document> doc = TitlesDB.find(new Document("index" , id));
        Document para = doc.first();
        if (para != null){
            return para.getString("text");
        }
        return null;
    }
    public static void SuggestionInsert(String in)
    {
        FindIterable<Document> TempCheck = RankerSuggestionsDB.find(eq("Query",in));
        if(TempCheck.first()==null)
        {
            Document Insertion = new Document("Query",in).append("Score",1);
            RankerSuggestionsDB.insertOne(Insertion);
        }
        else
        {
            Bson filter = eq("Query",in);
            Bson update = inc("Score",1);
            RankerSuggestionsDB.updateOne(filter,update);
        }
    }
    public static ArrayList<Document> FindSuggestions(String in)
    {
        FindIterable<Document> QueryResult;
        Bson sort = new Document("Score",-1);
        ArrayList<Document> Returned = new ArrayList<>();

        if(!in.equals(""))
            QueryResult  = RankerSuggestionsDB.find(new Document("Query", new Document("$regex", '^'+in).append("$options", "i"))).sort(sort);
        else {
            QueryResult=RankerSuggestionsDB.find().sort(sort);
        }

        if(QueryResult.first() != null)
            QueryResult.into(Returned);

        return Returned;
    }
}
