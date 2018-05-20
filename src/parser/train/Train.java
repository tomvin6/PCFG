package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.*;

import parser.decode.CKYDecoder;
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
//        SyntacticProbWithGoodTuringSmoothing(myGrammar);
//        calcLexicalGoodTuringSmoothingProbabilities(myGrammar);
        return myGrammar;
    }

    private void SyntacticProbWithGoodTuringSmoothing(Grammar myGrammar) {
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
        // we have non terminal counts + rules count
        // calc Nr map (the number of n-grams that occur exactly r times)
        CountMap<Integer> nrCounts = calcNrValues(ruleCounts.entrySet());

        // calc N values
        long nSum = ruleCounts.allCounts();

        // calc r* / N:
        for (Rule r : syntacticRules) {
            if (ruleCounts.containsKey(r)) {
                int rCount = ruleCounts.get(r);
                double rStar = (rCount + 1) * (nrCounts.get(rCount + 1)) / nrCounts.get(rCount);
                double logProb = Math.log(1.0 * rStar / nSum);
                r.setMinusLogProb(logProb != 0 ? -logProb : 0);
            }
        }
    }

    private utils.CountMap<Integer> calcNrValues(Set<Map.Entry<Rule, Integer>> entries) {
        CountMap<Integer> nrCounts = new CountMap<Integer>();
        for (Map.Entry<Rule, Integer> rule: entries) {
            if (nrCounts.containsKey(rule.getValue())) {
                nrCounts.put(rule.getValue(), rule.getValue() + nrCounts.get(rule.getValue()));
            } else {
                nrCounts.put(rule.getValue(), 1);
            }
        }
        return nrCounts;
    }

    private void calcLexicalGoodTuringSmoothingProbabilities(Grammar grammar) {
        Map<String, Set<Rule>> lexicalEntries = grammar.getLexicalEntries();
        CountMap<Rule> ruleCounts = (CountMap<Rule>) grammar.getRuleCounts();
        int nSum = ruleCounts.allCounts();
        // calc Nr values
        CountMap<Integer> nrCounts = calcNrValues(ruleCounts.entrySet());

        for (Map.Entry<String, Set<Rule>> item : lexicalEntries.entrySet()) {
            for (Rule rule : item.getValue()) {
                if (ruleCounts.containsKey(rule)) {
                    int rCount = ruleCounts.get(rule);
                    double rStar = (rCount + 1) * (nrCounts.get(rCount + 1)) / nrCounts.get(rCount);
                    double logProb = Math.log(1.0 * rStar / nSum);
                    rule.setMinusLogProb(logProb != 0 ? -logProb : 0);
                }
            }

        }
    }

    private static void calcSyntacticRuleProbabilities(Grammar myGrammar) {
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

    private static void calcLexicalRuleProbabilities(Grammar grammar) {
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

    public static void main(String[] args) {
        Grammar myGrammar = new Grammar();

        List<Rule> theRules = new ArrayList<grammar.Rule>();

        Event S = new Event ("S");
        Event A = new Event("A");
        Event B = new Event("B");
        Event C = new Event("C");

        Event e1 = new Event ("A B");
        Event e2 = new Event ("B B");
        Event e3 = new Event ("C C");
        Event e4 = new Event ("C A");
        Event e5 = new Event ("B A");
        Event e6 = new Event ("A A");

        Event t1 = new Event("a");
        Event t2 = new Event("b");

        Rule Sr = new Rule(S.toString(),e1.toString(),false,true);
        Rule Sr2 = new Rule(S.toString(),e2.toString(),false,true);

        Rule Ar =  new Rule(A.toString(),e3.toString(),false,false);
        Rule Ar2 =  new Rule(A.toString(),e1.toString(),false,false);
        Rule Ar3 =  new Rule(A.toString(),t1.toString(),true,false);

        Rule Br =  new Rule(B.toString(),e2.toString(),false,false);
        Rule Br2 =  new Rule(B.toString(),e4.toString(),false,false);
        Rule Br3 =  new Rule(B.toString(),t2.toString(),true,false);

        Rule Cr =  new Rule(C.toString(),e5.toString(),false,false);
        Rule Cr2 =  new Rule(C.toString(),e6.toString(),false,false);
        Rule Cr3 =  new Rule(C.toString(),t2.toString(),true,false);

        theRules.add(Sr);
        theRules.add(Sr2);
        theRules.add(Ar);
        theRules.add(Ar2);
        theRules.add(Ar3);
        theRules.add(Br);
        theRules.add(Br2);
        theRules.add(Br3);
        theRules.add(Cr);
        theRules.add(Cr2);
        theRules.add(Cr3);

        myGrammar.addAll(theRules);
        calcLexicalRuleProbabilities(myGrammar);
        calcSyntacticRuleProbabilities(myGrammar);

        String [] words = new String[]{ "a","a","b","b"};
        //String[] words = input.toArray(new String[input.size()]);
        CKYDecoder decoder = new CKYDecoder(words,myGrammar);

        // iterate tree
        for (Map.Entry<Rule, CKYDecoder.BestRuleData> rule : decoder.getTopCell().rulesMatches.entrySet()) {
            if (((Event)rule.getKey().getLHS()).toString().equals("S")) { // legal parse tree
                // todo........
            } else {
                // TODO: RETURN DUMMY
            }
        }

        System.out.println();
    }
}

