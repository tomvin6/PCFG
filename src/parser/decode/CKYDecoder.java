package parser.decode;

import grammar.Grammar;
import grammar.Rule;

import java.util.*;

public class CKYDecoder {
    private Grammar myGrammer;

    public CKYDecoder(Grammar g) {
        this.myGrammer = g;
    }
    private Set<grammar.Rule> getLexRuleOrNN(Map<String, Set<grammar.Rule>> lexRules, String word) {
        if (lexRules.containsKey(word)) {
            return lexRules.get(word);
        }
        grammar.Rule rule = new grammar.Rule("NN", word);
        Set<grammar.Rule> rules = new HashSet<Rule>();
        rules.add(rule);
        return rules;
    }

    private Cell runCYK(List<String> words) {
        Map<String, Set<grammar.Rule>> lexRules = this.myGrammer.getLexicalEntries();
        Set<grammar.Rule> _sRules = this.myGrammer.getSyntacticRules();
        Map<String, Set<grammar.Rule>> rulesMap =  buildRulesMap(_sRules);
        Cell[][] chart = new Cell[words.size()][words.size()];
        // init phase
        for(int i = 0 ; i < words.size() ; i++) {
            chart[0][i] = new Cell(getLexRuleOrNN(lexRules, words.get(i)), words.get(i));
        }

        // get all rules for the table
        for(int i = 1; i < chart.length; i++) {
            for(int j = 0 ; j <chart.length - i; j++) {
                int z = 0;
                int t = i + j;
                for(int k = i - 1; k >= 0 && z < i && t >= 0; k-- ) {
                    Cell cell;
                    if(chart[i][j] == null) {
                        cell = new Cell();
                        chart[i][j] = cell;
                    }
                    cell = chart[i][j];
                    Cell leftChildCell = chart[k][j];
                    Cell rightChildCell = chart[z][t];
                    Set<Rule> rules = RulesBelongToo(leftChildCell, rightChildCell, rulesMap);
                    upsertRulesToCellUsingBestProbLogic(cell, rules, leftChildCell, rightChildCell);
                    z++;
                    t--;
                }
            }
        }
        return chart[chart.length - 1][0];
    }

    private Map<String, Set<grammar.Rule>> buildRulesMap(Set<Rule> sRules) {
        Map<String, Set<Rule>> rulesMap = new HashMap<String, Set<Rule>>();
        for (grammar.Rule rule : sRules) {
            if (rulesMap.containsKey(((grammar.Event)rule.getRHS()).toString())) {
                rulesMap.get(((grammar.Event)rule.getRHS()).toString()).add(rule);
            } else {
                Set<Rule> rules = new HashSet<Rule>();
                rules.add(rule);
                rulesMap.put(((grammar.Event)rule.getRHS()).toString(), rules);
            }
        }
        return rulesMap;
    }

    private void upsertRulesToCellUsingBestProbLogic(Cell cell, Set<Rule> rules, Cell leftChildCell, Cell rightChildCell) {
        for (Rule rule : rules) {
            if (cell.rulesMatches.containsKey(rule)) {
                BestRuleData bestRuleData = cell.rulesMatches.get(rule);
                if (rule.getMinusLogProb() < bestRuleData.minusLogProb) {
                    bestRuleData.minusLogProb = rule.getMinusLogProb();
                    bestRuleData.leftChildBackPointer = leftChildCell;
                    bestRuleData.rightChildBackPointer = rightChildCell;
                }
            } else {
                cell.rulesMatches.put(rule, new BestRuleData(rule.getMinusLogProb(), leftChildCell, rightChildCell));
            }
        }
    }

    private Set<grammar.Rule> RulesBelongToo(Cell x, Cell y, Map<String, Set<grammar.Rule>> rulesMap) {
        HashSet rulesToReturn = new HashSet<grammar.Rule>();

        for (grammar.Rule xRule : x.rulesMatches.keySet()) {
            for (grammar.Rule yRule : y.rulesMatches.keySet()) {
                List<String> e1 = xRule.getLHS().getSymbols();
                List<String> e2 = yRule.getLHS().getSymbols();
                List<String> bothSymbols = new ArrayList<String>();
                bothSymbols.addAll(e1);
                bothSymbols.addAll(e2);
                String key = e1.get(0) + " " + e2.get(0);

                if (rulesMap.containsKey(key)) {
                    rulesToReturn.addAll(rulesMap.get(key));
                }
//                for (grammar.Rule resultRule : _sRules) {
//
//                    List<String> e3 = resultRule.getRHS().getSymbols();
//                    if(!rulesToReturn.contains(resultRule) && CompareTwoLists(e3,bothSymbols))
//                    {
//                        rulesToReturn.add(resultRule);
//                    }
//                }
            }
        }

        return rulesToReturn;
    }
    private boolean CompareTwoLists(List<String> s1, List<String> s2) {
        if (s1.size() != s2.size())
            return false;
        for(int i = 0 ; i < s1.size(); i++)
        {
            if(!s1.get(i).equals(s2.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    public class Cell {
        public String word;
        public Map<grammar.Rule, BestRuleData> rulesMatches = new HashMap<Rule, BestRuleData>();
        public Cell() {}

        // for lexical initialization
        public Cell(Set<grammar.Rule> rules, String word) {
            this.word = word;
            for (grammar.Rule rule : rules) {
                rulesMatches.put(rule, new BestRuleData(rule.getMinusLogProb(), null, null));
            }
        }

    }
    public class BestRuleData {
        public double minusLogProb;
        public Cell leftChildBackPointer, rightChildBackPointer;

        public BestRuleData(double minusLogProb, Cell leftChildBackPointer, Cell rightChildBackPointer) {
            this.minusLogProb = minusLogProb;
            this.leftChildBackPointer = leftChildBackPointer;
            this.rightChildBackPointer = rightChildBackPointer;
        }
    }


    public tree.Node getTreeIfExist(List<String> words) {
        boolean isLegal = false;
        Map<Rule, CKYDecoder.BestRuleData> allLegalTopCells = new HashMap<Rule, CKYDecoder.BestRuleData>();
        Cell topCell = this.runCYK(words);
        // iterate tree
        for (Map.Entry<Rule, CKYDecoder.BestRuleData> rule : topCell.rulesMatches.entrySet()) {
            if (((grammar.Event)rule.getKey().getLHS()).getSymbols().get(0).equals("S")) { // legal parse tree
                allLegalTopCells.put(rule.getKey(), rule.getValue());
                isLegal = true;
            }
        }
        if (isLegal) {
            topCell.rulesMatches = allLegalTopCells;
            tree.Node root = buildTree(topCell);
            return root;
        }
        return null; // no legal tree
    }

    private static tree.Node buildTree(CKYDecoder.Cell topCell) {
        Map.Entry<Rule, CKYDecoder.BestRuleData> bestRule = getBestRule(topCell);
        // it's a terminal
        if (bestRule.getValue().rightChildBackPointer == null && bestRule.getValue().leftChildBackPointer == null) {
            return MakeNodeContainTerminal(bestRule);
        }

        List<tree.Node> children = new LinkedList<tree.Node>();
        children.add(buildTree(bestRule.getValue().leftChildBackPointer));
        children.add(buildTree(bestRule.getValue().rightChildBackPointer));

        tree.Node parent = new tree.Node(((grammar.Event)bestRule.getKey().getLHS()).toString(), false, null, children);
        children.get(0).setParent(parent);
        children.get(0).setParent(parent);
        return parent;
    }

    private static tree.Node MakeNodeContainTerminal(Map.Entry<Rule, CKYDecoder.BestRuleData> bestRule) {
        tree.Node parent = new tree.Node(((grammar.Event)bestRule.getKey().getLHS()).toString(), false, null);
        tree.Node terminal = new tree.Node(((grammar.Event)bestRule.getKey().getRHS()).toString(), false, parent);
        parent.addDaughter(terminal);
        return parent;
    }

    private static Map.Entry<grammar.Rule, CKYDecoder.BestRuleData> getBestRule(CKYDecoder.Cell cell) {
        double bestRuleMinusLogProb = Double.MAX_VALUE; // we need to minimize
        Map.Entry<grammar.Rule, CKYDecoder.BestRuleData> bestRuleData = null;
        for (Map.Entry<Rule, CKYDecoder.BestRuleData> rule : cell.rulesMatches.entrySet()) {
            if (rule.getValue().minusLogProb < bestRuleMinusLogProb) {
                bestRuleMinusLogProb = rule.getValue().minusLogProb;
                bestRuleData = rule;
            }
        }
        if (bestRuleData == null && !cell.rulesMatches.isEmpty()) { // probably NN word
            bestRuleData = cell.rulesMatches.entrySet().iterator().next();
            bestRuleData.getValue().minusLogProb = Math.log(1.0);
        }
        return bestRuleData;
    }
}

