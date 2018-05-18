package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;

import tree.Node;
import tree.Tree;
import treebank.Treebank;
import utils.CountMap;


/**
 *
 * @author Reut Tsarfaty
 *
 * CLASS: Train
 *
 * Definition: a learning component
 * Role: reads off a grammar from a treebank
 * Responsibility: keeps track of rule counts
 *
 */

public class Train {


    /**
     * Implementation of a singleton pattern
     * Avoids redundant instances in memory 
     */
    public static Train m_singTrainer = null;

    public static Train getInstance()
    {
        if (m_singTrainer == null)
        {
            m_singTrainer = new Train();
        }
        return m_singTrainer;
    }

    public static void main(String[] args) {

    }

    public Grammar train(Treebank myTreebank)
    {
        Grammar myGrammar = new Grammar();
        for (int i = 0; i < myTreebank.size(); i++) {
            Tree myTree = myTreebank.getAnalyses().get(i);
            List<Rule> theRules = getRules(myTree);
            myGrammar.addAll(theRules);
        }
        calcLexicalRuleProbabilities(myGrammar);
        calcSyntacticRuleProbabilities(myGrammar);
        return myGrammar;
    }

    private void calcSyntacticRuleProbabilities(Grammar myGrammar) {
		Set<Rule> syntacticRules = (Set<Rule>) myGrammar.getSyntacticRules();
		CountMap<Rule> ruleCounts = (CountMap<Rule>) myGrammar.getRuleCounts();
        CountMap<String> nonTerminalsCount = new CountMap<String>();
        // count non terminals for denominators
        for (Map.Entry<Rule, Integer> ruleCount: ruleCounts.entrySet()) {
            if (nonTerminalsCount.containsKey(((Event)ruleCount.getKey().getLHS()).toString())) {
                int denominator = nonTerminalsCount.get(((Event)ruleCount.getKey().getLHS()).toString()) + ruleCount.getValue();
                nonTerminalsCount.put(((Event)ruleCount.getKey().getLHS()).toString(), denominator);
            } else {
                nonTerminalsCount.put(((Event)ruleCount.getKey().getLHS()).toString(), ruleCount.getValue());
            }
        }
		for (Rule r : syntacticRules) {
			if (ruleCounts.containsKey(r)) {
                double logProb = Math.log(1.0 * ruleCounts.get(r) / nonTerminalsCount.get(((Event)r.getLHS()).toString()));
                r.setMinusLogProb(logProb != 0 ? -logProb : 0);
			}
		}
    }

    private void calcLexicalRuleProbabilities(Grammar grammar) {
        Map<String, Set<Rule>> lexicalEntries = grammar.getLexicalEntries();
        CountMap<Rule> ruleCounts = (CountMap<Rule>) grammar.getRuleCounts();
        int denominator = 0;
        for (Map.Entry<String, Set<Rule>> item : lexicalEntries.entrySet()) {
            denominator = 0;
            for (Rule rule : item.getValue()) {
                if (ruleCounts.containsKey(rule)) {
                    denominator += ruleCounts.get(rule);
                }
            }
            for (Rule rule : item.getValue()) {
                double logProb = Math.log(1.0 * ruleCounts.get(rule) / denominator);
                rule.setMinusLogProb(logProb != 0 ? -logProb : 0);
            }
        }
    }

//	private Integer getDenominator(CountMap<Rule> ruleCounts, String key, Set<String> nonTerminalSymbols) {
//		Integer sum = 0;
//		for (String nonTerminal : nonTerminalSymbols) {
//			Integer ruleCount = ruleCounts.get(new Rule(key, nonTerminal));
//			sum += ruleCount;
//		}
//		return sum;
//	}


    public List<Rule> getRules(Tree myTree)
    {
        List<Rule> theRules = new ArrayList<Rule>();

        List<Node> myNodes = myTree.getNodes();
        for (int j = 0; j < myNodes.size(); j++) {
            Node myNode = myNodes.get(j);
            if (myNode.isInternal())
            {
                Event eLHS = new Event(myNode.getIdentifier());
                Iterator<Node> theDaughters = myNode.getDaughters().iterator();
                StringBuffer sb = new StringBuffer();
                while (theDaughters.hasNext()) {
                    Node n = (Node) theDaughters.next();
                    sb.append(n.getIdentifier());
                    if (theDaughters.hasNext())
                        sb.append(" ");
                }
                Event eRHS = new Event (sb.toString());
                Rule theRule = new Rule(eLHS, eRHS);
                if (myNode.isPreTerminal())
                    theRule.setLexical(true);
                if (myNode.isRoot())
                    theRule.setTop(true);
                theRules.add(theRule);
            }
        }
        return theRules;
    }

}
