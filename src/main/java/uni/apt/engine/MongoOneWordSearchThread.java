package uni.apt.engine;

import com.mongodb.client.MongoIterable;
import org.bson.Document;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class MongoOneWordSearchThread  extends Thread implements RankerSearchThread{
    String _word;

    LinkedList<WordProps> result;

    public MongoOneWordSearchThread(String _word) {
        this._word = _word;
    }

    @Override
    public void run() {
        super.run();
        result = new LinkedList<>();

        Document matcher = new Document("word", new Document("$regex", Pattern.compile("" + _word + "(?i)")));
    }

    @Override
    public List<Ranker.SearchResult> getResult() {
        return null;
    }

    @Override
    public void WaitForResult() {
        try {
            this.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
