package uni.apt.server;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import org.bson.*;
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
public class TranslateHandler {

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

    public static final int PARA_SIZE = 160;
    public static final int TITLE_MAX_SIZE = 80;


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

        for (BsonValue bsonValue : arr) {
            result.add(handle((BsonDocument) bsonValue, selector));
        }

        return result;
    }

    private static TranslatedDocument handle(BsonDocument doc , List<QuerySelector> selector){
        System.out.println("Processing: " + doc);
        IndeterminateTranslatedDocument temp = new IndeterminateTranslatedDocument();
        temp.boldRanges = new ArrayList<>();
        temp.paragraph = OnlineDB.getParagraph(doc.getInt32("paragraphID").getValue());
        int lID = doc.getInt32("paragraphID").getValue() - 1;
        int hID = doc.getInt32("paragraphID").getValue() + 1;

//        while (temp.paragraph.length() < PARA_SIZE * 2){
//            String l = OnlineDB.getParagraph(lID);
//            String h = OnlineDB.getParagraph(hID);
//            if (l != null)
//                temp.paragraph = l + " " + temp.paragraph;
//            if (h != null)
//                temp.paragraph = temp.paragraph + " " + h;
//
//            lID--;
//            hID++;
//
//            if (l == null && h == null)
//                break;
//        }

        System.out.println("temp.paragraph=" + temp.paragraph);

        for (QuerySelector q : selector){
            handle(q , temp);
        }

        //System.out.println("Handle finished");

        TranslatedDocument ret = new TranslatedDocument();
        ret.title = OnlineDB.getTitle(doc.getInt32("titleID").getValue());

        if (ret.title.length() > TITLE_MAX_SIZE){
            ret.title = ret.title.substring(0 , TITLE_MAX_SIZE);
            ret.title += "..";
        }

        //System.out.println("Title: " + ret.title);

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
            int sub_start = Math.max(0 , start - PARA_SIZE);
            int sub_end   = Math.min(l , start + PARA_SIZE);

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
            int l = temp.paragraph.length();
            int sub_start = 0;
            int sub_end   = Math.min(l , PARA_SIZE);

            temp.paragraph = temp.paragraph.substring(sub_start , sub_end);

            if (sub_end != l)
                temp.paragraph += "...";

            ret.paragraphs.add(temp.paragraph);
            ret.bold.add(false);
        }

//        //System.out.println("Done");
//        for (String s : ret.paragraphs){
//            System.out.println(s);
//        }

        return ret;
    }

    private static void handle(QuerySelector s , IndeterminateTranslatedDocument doc){

        StringBuilder match = new StringBuilder(s.getWords().get(0));
        for (int i = 1; i < s.getWords().size(); i++) {
            match.append(" ").append(s.getWords().get(i));
        }

        //System.out.println("Handle:" + match + " , doc: " + doc.toString() );

        Pattern pattern = Pattern.compile("(" + match + ")" , Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(doc.paragraph);
        while (matcher.find()){
            doc.boldRanges.add(new Range(matcher.start(), matcher.end()));
        }
    }
}
