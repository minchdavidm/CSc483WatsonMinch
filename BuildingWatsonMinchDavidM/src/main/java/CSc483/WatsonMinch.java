package CSc483;

/**
 * WatsonMinch class
 * This class handles the interface of the Watson project.
 * Dependent Classes:
 * WikipediaParser
 * TextLemmenizer
 *
 * The indexing happens in the WikipediaParser class.
 * WikipediaParser takes the dump of Wikipedia page files,
 * parses them into titles and articles, and for each
 * article, it lemmenizes the content using Stanford CoreNLP API.
 * To simplify the code here, all the lemmenizing and stemming takes place
 * in the TextLemminizer class, which has specific rules for lemmenizing
 * queries, and others for lemmenizing documents. Lemmenize queries is
 * called directly from this main method, but lemmenize documents is called
 * from the Wikipedia parser. Note that the WikipediaParser is object
 * oriented, whereas TextLemmenizer's methods can be accessed statically.
 * Once an article is lemmenized, the lone punctuation is removed by 
 * a simple regex. Once we have the lemmenized article content,
 * the document is indexed using Lucene where the Title is preserved (non-tokenized)
 * as a Document name, and the article text is tokenized and searchable.
 * 
 * The default behavior of WatsonMinch is to expect that the indexing has already
 * occurred and attempts to load the Lucene index from the src/resources/lucene-files/ directory. 
 * Wikipedia documents will only be parsed if the -p option is given, so if that 
 * option is given, the Wikipedia files must be present in the src/resources/wiki-subset/ directory.

 * Then, the answer (query) is given, the stop words are removed from the query if
 * the query is longer than 4 words, then the top documents are printed with their 
 * relative score, and Watson V0.2 makes a guess as to what the question was. 
 *
 * USAGE:
 * mvn compile
 * mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch"  
 * ^^ This runs the default settings: Loads the Lucene index, runs all the Jeopardy queries, and prints 
 *    the results to the terminal and evaluates the accuracy. 
 * mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch" -Dexec.args="-p"
 * ^^ The -p flag indicates that the Wikipedia pages need to be parsed. Instead of loading the Lucene index,
 *    the Wikipedia pages are sought out and parsed. This takes a long time, and creates a Lucene index that
 *    can be loaded for the next run.
 * mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch" -Dexec.args="-v"
 * ^^ The -v flag indicates that verbose output should be given. That means that the top 10 documents and their scores
 *    will be printed after every query. This flag can be given at the same time as -p.
 * mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch" -Dexec.args="-q QUERY TERMS"
 * ^^ The -q flag indicates that all the following text is a specific query. Instead of loading the Jeopardy queries,
 *    this is the only query done, and it's top 10 documents are printed in order with their scores. If you want to
 *    parse the Wikipedia pages as well, the -p flag must come before the -q flag, because every term after -q will
 *    be considered part of the query. With the -q flag, the 10 documents and their scores are already printed,
 *    so the verbose flag if present is redundant and should not be included.
 * mvn exec:java -Dexec.mainClass="CSc483.WatsonMinch" -Dexec.args="-?"
 * ^^ The -? flag displays this usage information. If -? is present, then no operations will be attempted.
 *
 * @see WikipediaParser
 * @see TextLemmenizer
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;

import java.io.File;
import java.io.IOException;

import java.nio.file.Paths;

import java.lang.ClassLoader;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class WatsonMinch {

  public static final String questionsFilePath = "questions.txt";
  public static final String wikipediaFilesPath = "src/main/resources/wiki-subset/";
  public static final String luceneOutputDir = "src/main/resources/lucene-files/";


  public static void main(String[] args) {

    if (parseArgs(args, "-?"))
      displayUsage(0);

    System.out.println( "Watson V0.2 is loading..." );

    boolean verbose    = parseArgs(args, "-v");
    boolean parse      = parseArgs(args, "-p");
    boolean queryGiven = parseArgs(args, "-q");
    String  query      = parseArgs(args);

    if (queryGiven && query.equals("")){
      System.err.println("Error: Query flag given, but no query was found.");
      displayUsage(1);
    }
    if (queryGiven) {
      //Always print the top 10 documents if there's only 1 query
      verbose = true;
    }

    IndexReader wikipediaIndex;

    WikipediaParser parser = new WikipediaParser();
    if (parse) {
      //Then we need to parse wikipedia files
      System.out.println("Initializing parsing of Wikipedia files. This may take a while.");
      parser.parse();
      wikipediaIndex = parser.getIndex();

    } else {
      //Then we need to load Lucene indices
      wikipediaIndex = loadLuceneIndex();
      parser.setLuceneIndex(wikipediaIndex);
    }

    if (wikipediaIndex == null || wikipediaIndex.numDocs() < 1){
      printIssueWithReadingInFile(parse);
      displayUsage(2);
    }
        
    System.out.println("Watson V0.2 has loaded.\n");

    // Load in query(/ies)

    ArrayList<String> queries = new ArrayList<String>();
    if (queryGiven){
      queries.add(query);
    } else {
      //Get queries from file
      queries.addAll(getQueriesFromFile());
    }

    //parse, lemmenize, and tokenize query(/ies)
    HashMap<String, String> lemmenizedQueries = TextLemmenizer.lemmenizeQueries(queries); 
    
    //run query, score documents
    HashMap<String, ArrayList<ScoredDocument>> scores = parser.score(lemmenizedQueries);

    // Give verbose output if requested

    if (verbose) {
      // print out top 10 scores for each query
      // Original query:
      // Tokenized query: 
      // Top 10 documents and scores:
      for (String answer : queries){
        System.out.println("Original query: " + answer);
        String lemmenized = lemmenizedQueries.get(answer);
        System.out.println("Tokenized query: " + lemmenized);
        for (ScoredDocument document : scores.get(lemmenized)){
          System.out.println("Potential Question: " + document.getDocumentID() + ", with score " + document.getScore());
        }
      }
    }

    HashMap<String, ArrayList<String>> questionKey = new HashMap<String, ArrayList<String>>();
    if (!queryGiven) {
      //load in question key
      questionKey.putAll(loadInQuestionKey());
    }

    int questionsCorrect = 0;
    int totalQuestions   = 0;
    int questionsInTop10 = 0;

    for (String answer : queries){
      // Print answer
      System.out.println("And the answer is:");
      System.out.println("> " + answer);
      String question = scores.get(lemmenizedQueries.get(answer)).get(0).getDocumentID();
      //question = question.substring(2, question.length() - 2);
      System.out.println("What is... " + question + "?");
    
      // Judge accuracy if we were doing jeopardy documents
      if (!queryGiven) {
        totalQuestions++;
        //For original query, compare top document to question key
        //if they match, increment score
        if (isCorrectQuestion(question, questionKey.get(answer))){
          System.out.println("That's right!");
          questionsCorrect++;
        } else {
          System.out.println("That's wrong. Possible responses:");
          for (String response : questionKey.get(answer)){
            System.out.println(response);
          }

          for (int i = 1; i < 10; i++){
            String lessQuestion = scores.get(lemmenizedQueries.get(answer)).get(i).getDocumentID();
            if (isCorrectQuestion(lessQuestion, questionKey.get(answer))){
              System.out.println("However, the document ranked #" + i + " did have the answer!");
              questionsInTop10++;
              break;
            }
          }
        }
      }
    }  

    if (!queryGiven) {
      //Display overall score
      if (totalQuestions != 0){
        //double score = ((double) questionsCorrect) / ((double) totalQuestions) * 100;
        System.out.println("Watson V0.2 got " + questionsCorrect + " out of " + totalQuestions + " right.");
        System.out.println("Of the wrong questions, " + questionsInTop10 + " were results in the top 10 documents.");
        //System.out.printf("That's a %.02f accuracy!\n", score);
      } else {
        System.err.println("Watson V0.2 did something unexpected. Map ID 002");
      }
    }

    System.out.println("Watson V0.2 is powering down.");

        
  }

  public static String parseArgs(String[] args){
    //If -q is present, then all following arguments are the query
    String query = "";
    boolean startQuery = false;
    if (parseArgs(args, "-q")){
      for (String arg : args){
        if (startQuery)
          query += " " + arg;
        if (arg.indexOf("-q") != -1)
          startQuery = true;
      }  
    }
    return query.trim();
  }

  public static boolean parseArgs(String[] args, String param){
    //-v, -p, -q are possible arguments
    for (String arg : args){
      if (arg.indexOf(param) == 0) //Only count if the param starts with -param, it can have stuff afterwards
        return true;
    }
    return false;
  }

  public static void displayUsage(int exitCode){

    System.out.println();
    System.out.println("USAGE:");
    System.out.println("mvn compile");
    System.out.println("mvn exec:java -Dexec.mainClass=\"CSc483.WatsonMinch\"");
    System.out.println("^^ This runs the default settings: Loads the Lucene index, runs all the Jeopardy queries, and prints");
    System.out.println("   the results to the terminal and evaluates the accuracy.");
    System.out.println("mvn exec:java -Dexec.mainClass=\"CSc483.WatsonMinch\" -Dexec.args=\"-p\"");
    System.out.println("^^ The -p flag indicates that the Wikipedia pages need to be parsed. Instead of loading the Lucene index,");
    System.out.println("   the Wikipedia pages are sought out and parsed. This takes a long time, and creates a Lucene index that");
    System.out.println("   can be loaded for the next run.");
    System.out.println("mvn exec:java -Dexec.mainClass=\"CSc483.WatsonMinch\" -Dexec.args=\"-v\"");
    System.out.println("^^ The -v flag indicates that verbose output should be given. That means that the top 10 documents and their scores");
    System.out.println("   will be printed after every query. This flag can be given at the same time as -p.");
    System.out.println("mvn exec:java -Dexec.mainClass=\"CSc483.WatsonMinch\" -Dexec.args=\"-q QUERY TERMS\"");
    System.out.println("^^ The -q flag indicates that all the following text is a specific query. Instead of loading the Jeopardy queries,");
    System.out.println("   this is the only query done, and it's top 10 documents are printed in order with their scores. If you want to");
    System.out.println("   parse the Wikipedia pages as well, the -p flag must come before the -q flag, because every term after -q will");
    System.out.println("   be considered part of the query. With the -q flag, the 10 documents and their scores are already printed,");
    System.out.println("   so the verbose flag if present is redundant and should not be included.");
    System.out.println("mvn exec:java -Dexec.mainClass=\"CSc483.WatsonMinch\" -Dexec.args=\"-?\"");
    System.out.println("^^ The -? flag displays this usage information. If -? is present, then no operations will be attempted.");

    System.exit(exitCode);
  }

//  public LuceneIndex loadLuceneIndex(){
//    //Locate Lucene index
//    //Load Lucene index.
//  }

  public static void printIssueWithReadingInFile(boolean parse){
    System.err.println("Error: Lucene Index could not be configured.");
    if (parse){
      System.err.println("The files in the " + wikipediaFilesPath + " directory could not be");
      System.err.println("parsed as Wikipedia article information.");
    } else {
      System.err.println("The file in the " + luceneOutputDir + " direcotry could not be");
      System.err.println("read in as a Lucene index.");
    }
    System.err.println("The directory contents may be missing or corrupted.");
    System.err.println("Please rectify before continuing.");
  }

  public static ArrayList<String> getQueriesFromFile(){
    ArrayList<String> queries = new ArrayList<String>();
    for (String query : loadInQuestionKey().keySet()) {
      queries.add(query); //the loadInQuestionKey has the queries for keys, so return the keyset it generates
    }
    return queries;
  }

  //Note, the ArrayList<String> is the list of all possible questions (responses) for the answer (Jeopardy Clue).
  //This should be in the src/main/resources/questions.txt directory
  public static HashMap<String, ArrayList<String>> loadInQuestionKey(){
    HashMap<String, ArrayList<String>> questionKey = new HashMap<String, ArrayList<String>>();

    boolean USING_CATEGORIES = true;

    try {
      ClassLoader classLoader = WatsonMinch.class.getClassLoader();
      File file = new File(classLoader.getResource(questionsFilePath).getFile());
      Scanner scanner = new Scanner(file);

      int lineCount = 0;
      String category = "";
      String query = "";
      while (scanner.hasNextLine()){
        lineCount++;
        if (lineCount == 1) {
          //Category
          if (USING_CATEGORIES) {
            category = " " + scanner.nextLine().trim();
          } else {
            scanner.nextLine(); //skip this line
          }
        } else if (lineCount == 2) {
          //Query
          query = scanner.nextLine().trim() + category;
        } else if (lineCount == 3) {
          //Potential correct Questions
          ArrayList<String> questions = new ArrayList<String>();
          for (String goodQuestion : scanner.nextLine().trim().split("\\|")){
            questions.add(goodQuestion.trim());
          }
          questionKey.put(query, questions);
        } else {
          //Verify that the line is blank.
          String blankLine = scanner.nextLine().trim();
          if (!blankLine.isEmpty()){
            System.err.println("Error: The question.txt file is in the wrong format!");
            System.err.println("The sequence must be:");
            System.err.println("CATEGORY");
            System.err.println("answer query");
            System.err.println("potential question 1 | potential question 2 | ... | potential question n");
            System.err.println("BLANK LINE");
            System.err.println();
            System.err.println("This line '" + blankLine + "' was supposed to be a blank line, but it is not.");
            System.exit(3);
          }
          lineCount = 0;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return questionKey;
  }

  public static IndexReader loadLuceneIndex() {
    //Note: IndexReader lets me implement tf-idf manually
    try{
      Directory index = FSDirectory.open(Paths.get(luceneOutputDir));
      IndexReader reader = DirectoryReader.open(index);
      return reader;
    } catch (IOException e) {
      e.printStackTrace(); //Main will take care of letting the user know
    }
    return null;
  }

  public static boolean isCorrectQuestion(String question, ArrayList<String> potentialQuestions){
    for (String potentialQuestion : potentialQuestions){
      if (potentialQuestion.equalsIgnoreCase(question)){
        return true;
      }
    }
    return false;
  }

}
