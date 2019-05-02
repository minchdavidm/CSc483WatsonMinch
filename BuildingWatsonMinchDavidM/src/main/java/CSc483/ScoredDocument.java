package CSc483;

/**
 * ScoredDocument class
 * This class is an object that stores the title of a document and the score that document received
 * given a certain query.
 */

public class ScoredDocument implements Comparable<ScoredDocument> {

  String documentID;
  double score;

  public ScoredDocument(String documentID, double score){
    this.documentID = documentID.substring(2, documentID.length() - 2); //Remove [[ ]]
    this.score = score;
  }

  public String getDocumentID(){
    return this.documentID;
  }

  public double getScore(){
    return this.score;
  }

  @Override
  public int compareTo(ScoredDocument o) {
    
    return ((Double)o.score).compareTo(score);
  }

}
