package uni.apt.engine;

import java.util.ArrayList;
import java.util.List;

public class WordProps{ //struct
    public String stemmed;
    public ArrayList<String> links = new ArrayList<>();
    public ArrayList<List<WordRecord>> indices = new ArrayList<>();

    public ArrayList<Float> TFs = new ArrayList<>();

    public ArrayList<Long> titleIds = new ArrayList<>();

    //IDF == storage.getNumWebsites() / links.size();
}