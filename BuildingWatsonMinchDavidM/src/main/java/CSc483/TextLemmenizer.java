package CSc483;

/**
 * TextLemmenizer class
 * This class contains static methods that stems, lemmenizes, tokenizes and simplifies a 
 * given String based on the following set of rules:
 * 1) The default Stanford CoreNLP Tokenizing and Lemmenizing rules
 * 2) Punctuation is then removed
 * 3) Stop words are removed
 *   3a) If it's a query, then if the query contains more than 4 words, stop words are removed
 *       That is, short queries are treated like quotes
 *   3b) If it's a document, then any stop words that aren't part of a quote are removed
 * After the text is lemmenized, it is returned.
 * NOTE: stemming and lemmenization can each be turned off using the class static variables
 * If the text was originally an ArrayList of queries, it is returned as a hash map mapping the
 * original query to the lemmenized version.
 */

import java.util.HashMap;
import java.util.ArrayList;

import java.util.List;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.Arrays;

import java.lang.invoke.MethodHandles;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.patterns.surface.Token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class TextLemmenizer{

  Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

  public static boolean STEM = true;
  public static boolean LEMMENIZE = true;

  public static String[] STOPWORDS = {"a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"}; // From http://xpo6.com/list-of-english-stop-words/

  public static String lemmenizeText(String toLemmenize){

    ArrayList<String> tokenArray = new ArrayList<String>();

    // The next 12 lines come from the guide posted to Piazza by Jesse Bartels
    // set the list of annotators to run

    Properties props = new Properties();
    if (LEMMENIZE) {
      props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
    } else {
      props.setProperty("annotators", "tokenize, ssplit, pos");
    }
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    Annotation document = new Annotation(toLemmenize);
    pipeline.annotate(document);

    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
    for (CoreMap sentence : sentences) {
      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
        // Only use this token if it's not punctuation
        if (LEMMENIZE && !Pattern.matches("\\p{Punct}+", token.get(LemmaAnnotation.class)) || 
          (!LEMMENIZE && !Pattern.matches("\\p{Punct}+", token.word()))){

          if (LEMMENIZE){
            if (STEM){
              Stemmer stem = new Stemmer();
              tokenArray.add(stem.stem(token.get(LemmaAnnotation.class)));
            } else {
              tokenArray.add(token.get(LemmaAnnotation.class));
            } 
          } else {
            if (STEM){
              Stemmer stem = new Stemmer();
              tokenArray.add(stem.stem(token.word()));
            } else {
              tokenArray.add(token.word());
            }
          }
        }
      }
    }

    String tokenString = removeStopWords(tokenArray);

    return tokenString.trim();
  }

  public static HashMap<String, String> lemmenizeQueries(ArrayList<String> toLemmenize){
    HashMap<String, String> lemmenized = new HashMap<String, String>();

    for (String query : toLemmenize){
      lemmenized.put(query, lemmenizeText(query));
    }
    return lemmenized;
  }

  private static String removeStopWords(ArrayList<String> toRemoveFrom){
    String finalString = "";
    int stopWordCount = 0;
    for (String testString : toRemoveFrom){
      if (toRemoveFrom.size() - 4 > stopWordCount && Arrays.binarySearch(STOPWORDS,testString) >= 0) {
        //Skip over stop words unless we have less than 4 words in toRemoveFrom
        stopWordCount++;
        continue;
      }

      finalString += " " + testString;
    }
    return finalString;
  }
}
