package uni.apt;

import org.apache.commons.text.similarity.CosineDistance;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.tartarus.snowball.ext.PorterStemmer;

import static uni.apt.core.InversionCounter.countInversions;

//TODO: fix issues related to stemming

public class test {


    public static void main(String[] args){
        CosineDistance cosineDistance = new CosineDistance();
        String word = "player";
        PorterStemmer stemmer = new PorterStemmer();
        stemmer.setCurrent(word);
        stemmer.stem();
        System.out.printf("%s \n" , stemmer.getCurrent());

        String s1 = "Abdo Mohammed";
        String s2 = "aBDO mOHAMMED";
        System.out.printf("%s , %s = %f\n" , s1 , s2 , (1.0 - (LevenshteinDistance.getDefaultInstance().apply(s1 , s2) / (float) Math.max(s1.length() , s2.length())))) ;
    }
}
