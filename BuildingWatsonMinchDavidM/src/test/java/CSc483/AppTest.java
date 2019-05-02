package CSc483;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Watson
 */
public class AppTest{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void testParseArgs(){
        String[] args1 = {"-v", "-p", "-q", "query", "time"};
        String[] args2 = {};
        String[] args3 = {"-v"};
        assertTrue(WatsonMinch.parseArgs(args1, "-v"));
        assertTrue(WatsonMinch.parseArgs(args1, "-p"));
        assertTrue(WatsonMinch.parseArgs(args1, "-q"));
        assertEquals(WatsonMinch.parseArgs(args1), "query time");
        assertFalse(WatsonMinch.parseArgs(args2, "-v"));
        assertFalse(WatsonMinch.parseArgs(args2, "-p"));
        assertFalse(WatsonMinch.parseArgs(args2, "-q"));
        assertEquals(WatsonMinch.parseArgs(args2), "");
        assertTrue(WatsonMinch.parseArgs(args3, "-v"));
        assertFalse(WatsonMinch.parseArgs(args3, "-p"));
        assertFalse(WatsonMinch.parseArgs(args3, "-q"));
        assertEquals(WatsonMinch.parseArgs(args3), "");
    }

    @Test
    public void testTextLemmenizer(){
        
        System.out.println(TextLemmenizer.lemmenizeText("This string should be lemmenized."));
        System.out.println(TextLemmenizer.lemmenizeText("We ain't got nuthin' to do 'bout dis."));
        System.out.println(TextLemmenizer.lemmenizeText("What if something is in quotes, like the phrase \"To be, or not to be. That is the question.\" Whether 'tis noble to share mine eggs, cooked light and fluffy, or to scarf down the whole thing myself."));
    }

    @Test
    public void testJeopardyQuestionParser() {
        WatsonMinch.loadInQuestionKey();
    }

    @Test
    public void testTitleCatcher() {
        WikipediaParser parser = new WikipediaParser();
        assertTrue(parser.isTitle("[[This is a title]]"));
        assertFalse(parser.isTitle("This [[is not]] a title"));
        assertFalse(parser.isTitle("[[This is not a titl]]e"));
        assertFalse(parser.isTitle("T[[his is not a title]]"));
        assertFalse(parser.isTitle("[[This is | not a title]]"));
    }

    @Test
    public void testParser() {
//        WikipediaParser parser = new WikipediaParser();
//        parser.parse();
    }
}
