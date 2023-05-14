package uni.apt.server;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import org.bson.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uni.apt.core.OnlineDB;
import uni.apt.core.QuerySelector;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class Translate {

    public static class StringListSerializer extends JsonSerializer<List<String>>{
        @Override
        public void serialize(List<String> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            for (String t : value){
                gen.writeString(t);
            }
            gen.writeEndArray();
        }
    }

    public static class BooleanListSerializer extends JsonSerializer<List<Boolean>>{
        @Override
        public void serialize(List<Boolean> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            for (Boolean t : value){
                gen.writeBoolean(t);
            }
            gen.writeEndArray();
        }
    }
    public static class IndeterminateTranslatedDocument {
        String paragraph;
        List<Range> boldRanges;
    }


    public static class TranslatedDocument implements Serializable {
        @JsonSerialize(using = StringListSerializer.class)
        List<String> paragraphs = new LinkedList<>();
        @JsonSerialize(using = BooleanListSerializer.class)
        List<Boolean> bold = new LinkedList<>();

        @JsonSerialize(using = StringSerializer.class)
        String title;
    }

    public static class Range{
        int start;
        int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    @PostMapping("/translate")
    public List<TranslatedDocument> translate(@RequestBody String body){
        BsonDocument data = BsonDocument.parse(body);
        List<QuerySelector> selector = QuerySelector.parseString(data.getString("query").getValue());
        List<TranslatedDocument> result = new LinkedList<>();
        System.out.println(data.getString("items").getValue());

        BsonArray arr = BsonArray.parse(data.getString("items").getValue());

        for (int i = 0;i < arr.size();i++){
            result.add(handle((BsonDocument) arr.get(i) , selector));
        }

        return result;
    }

    private static TranslatedDocument handle(BsonDocument doc , List<QuerySelector> selector){
        System.out.println("Processing: " + doc);
        IndeterminateTranslatedDocument temp = new IndeterminateTranslatedDocument();
        temp.boldRanges = new ArrayList<>();
        temp.paragraph = OnlineDB.getParagraph(doc.getInt32("paragraphID").getValue());
        for (QuerySelector q : selector){
            handle(q , temp);
        }

        System.out.println("Handle finished");

        TranslatedDocument ret = new TranslatedDocument();
        ret.title = OnlineDB.getParagraph(doc.getInt32("titleID").getValue());

        if (temp.boldRanges.size() > 0) {
            int avg = 0;

            //first , sort the ranges based on the start index
            if (temp.boldRanges.size() > 1)
                temp.boldRanges.sort(Comparator.comparingInt(o -> o.start));

            LinkedList<Range> newRanges = new LinkedList<>();

            //second , we search for overlapping regions
            for (int i = 0; i < temp.boldRanges.size(); i++) {
                Range r = temp.boldRanges.get(i);
                if (newRanges.size() == 0){
                    newRanges.add(r);
                }else{
                    Range last = newRanges.getLast();
                    if (last.start <= r.start && last.end >= r.start){ //if it has the same range , then try to grow
                        last.end = Math.max(last.end , r.end);
                    }else{
                        newRanges.add(r);
                    }
                }
            }

            //search for the longest bold range
            int start = 0;
            int _max = -1;
            for (Range r : newRanges){
                if (_max < r.end - r.start){
                    _max = r.end - r.start;
                    start = r.start;
                }
            }

            //take only 120 chars from the paragraph (at most)
            int l = temp.paragraph.length();
            int sub_start = Math.max(0 , start - 60);
            int sub_end   = Math.min(l , start + 60);

            temp.paragraph = temp.paragraph.substring(sub_start , sub_end);

            if (sub_start != 0)
                temp.paragraph = "..." + temp.paragraph;

            if (sub_end != l)
                temp.paragraph = temp.paragraph + "...";

            //now we start the splitting process
            Range prev = new Range(0,0);
            for (Range r : newRanges){
                int s = r.start - sub_start + ((sub_start != 0) ? 3 : 0);
                int e = r.end - sub_start +   ((sub_start != 0) ? 3 : 0);

                if (s < 0)
                    s = 0;

                if (e < 0)
                    continue;

                if (s > temp.paragraph.length())
                    break; //we are done

                if (e > temp.paragraph.length())
                    e = temp.paragraph.length();

                ret.paragraphs.add(temp.paragraph.substring(prev.end , s));
                ret.bold.add(false);
                ret.paragraphs.add(temp.paragraph.substring(s , e));
                ret.bold.add(true);

                prev.start = s;
                prev.end = e;
            }

            if (prev.end != temp.paragraph.length()){
                ret.paragraphs.add(temp.paragraph.substring(prev.end));
                ret.bold.add(false);
            }

        }else{
            ret.paragraphs.add(temp.paragraph.substring(0 , Math.min(temp.paragraph.length() , 60)));
        }

        System.out.println("Done");

        return ret;
    }

    private static void handle(QuerySelector s , IndeterminateTranslatedDocument doc){

        StringBuilder match = new StringBuilder(s.getWords().get(0));
        for (int i = 1; i < s.getWords().size(); i++) {
            match.append(" ").append(s.getWords().get(i));
        }

        System.out.println("Handle:" + match + " , doc: " + doc.toString() );

        Pattern pattern = Pattern.compile("(" + match + ")" , Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(doc.paragraph);
        while (matcher.find()){
            doc.boldRanges.add(new Range(matcher.start(), matcher.end()));
        }

        System.out.println("done");

    }
}
