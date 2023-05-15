package uni.apt.server;


import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uni.apt.core.OnlineDB;

import java.util.ArrayList;
import java.util.Map;

@RestController
public class SuggestionsHandler  {
    @GetMapping(value ={"/Query/","/Query"})
    public ArrayList<Document> Suggest(@RequestParam Map<String,String> params) {
        String Query  = params.get("q");
        if(Query.equals(""))
        {
            return OnlineDB.FindSuggestions("");
        }
        return OnlineDB.FindSuggestions(Query);
    }
}
