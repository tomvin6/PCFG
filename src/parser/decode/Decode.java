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
	private static CKYDecoder ckyDecoder;
	public static Set<Rule> m_setGrammarRules = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;

    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory 
     */
	public static Decode m_singDecoder = null;
	    
	public static Decode getInstance(Grammar g)
	{
		if (m_singDecoder == null)
		{
			ckyDecoder = new CKYDecoder(g);
			m_singDecoder = new Decode();
			m_setGrammarRules = g.getSyntacticRules();
			m_mapLexicalRules = g.getLexicalEntries();			
		}
		return m_singDecoder;
	}
    
	public Tree decode(List<String> input){
		Tree tree = new Tree(new Node("TOP"));
		tree.Node root = ckyDecoder.getTreeIfExist(input);
		if (root == null) { // RUN BASELINE
			// Done: Baseline Decoder
			//       Returns a flat tree with NN labels on all leaves
			Iterator<String> theInput = input.iterator();
			while (theInput.hasNext()) {
				String theWord = (String) theInput.next();
				Node preTerminal = new Node("NN");
				Terminal terminal = new Terminal(theWord);
				preTerminal.addDaughter(terminal);
				tree.getRoot().addDaughter(preTerminal);
			}
		} else {
			tree.getRoot().addDaughter(root);
		}
		return tree;
	}

	
	
	
}
