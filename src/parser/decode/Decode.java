package decode;

import grammar.Grammar;
import grammar.Rule;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.decode.CKYDecoder;
import tree.Node;
import tree.Terminal;
import tree.Tree;

public class Decode {

	public static Set<Rule> m_setGrammarRules = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;
	private static Grammar _g;
    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory 
     */
	public static Decode m_singDecoder = null;
	    
	public static Decode getInstance(Grammar g)
	{
		_g = g;
		if (m_singDecoder == null)
		{
			m_singDecoder = new Decode();
			m_setGrammarRules = g.getSyntacticRules();
			m_mapLexicalRules = g.getLexicalEntries();			
		}
		return m_singDecoder;
	}
    
	public Tree decode(List<String> input){
		
		// Done: Baseline Decoder
		//       Returns a flat tree with NN labels on all leaves 
		
		Tree t = new Tree(new Node("TOP"));
		Iterator<String> theInput = input.iterator();
		while (theInput.hasNext()) {
			String theWord = (String) theInput.next();
			Node preTerminal = new Node("NN");
			Terminal terminal = new Terminal(theWord);
			preTerminal.addDaughter(terminal);
			t.getRoot().addDaughter(preTerminal);
		}
		
		// TODO: CYK decoder
		String [] words = new String[]{ "EFRWT","ANFIM","MGIEIM","M"};
		//String[] words = input.toArray(new String[input.size()]);
		CKYDecoder decoder = new CKYDecoder(words,_g);
		System.out.println();
		
		return t;
		
	}

	
	
	
}
