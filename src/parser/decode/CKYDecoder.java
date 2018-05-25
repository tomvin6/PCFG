package parser.decode;

import grammar.Grammar;
import grammar.Rule;
import javafx.util.Pair;

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
        rule.setMinusLogProb(Math.log(0.001));
        Set<grammar.Rule> rules = new HashSet<Rule>();
        rules.add(rule);
        return rules;
    }

//    public class MinimizeCell {
//        public Set<String> pos;
//        public MinimizeCell(Set<String> pos) {
//            this.pos = pos;
//        }
//        public MinimizeCell rightChild;
//        public MinimizeCell leftChild;
//    }

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
                    //
                    // IF BINARY   ---> HANDLE BINARY CASE-
                    Set<Rule> rules = minimizeRulesBelongToo(leftChildCell, rightChildCell, rulesMap);
                    upsertRulesToCellUsingBestProbLogic(cell, rules, leftChildCell, rightChildCell);
                    // TODO IF UNARY - HANDLE BINARY CASE

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
            String leftKey = rule.getLHS().getSymbols().get(0);

            if (!cell.rulesMatches.containsKey(leftKey)) {
                AllLHSRules allLHSRules = new AllLHSRules();
                cell.rulesMatches.put(leftKey, allLHSRules);
            }
            // key exist
            cell.rulesMatches.get(leftKey).addRuleWithSameLHS(rule, leftChildCell, rightChildCell); // best prob maintained inside
        }
    }


    private Set<grammar.Rule> minimizeRulesBelongToo(Cell x, Cell y, Map<String, Set<grammar.Rule>> rulesMap) {
        Set<grammar.Rule> rulesToReturn = new HashSet<grammar.Rule>();

        for (String left : x.rulesMatches.keySet()) {
            for (String right : y.rulesMatches.keySet()) {
                String key = left.concat(" ").concat(right);
                if (rulesMap.containsKey(key)) {
                    rulesToReturn.addAll(rulesMap.get(key));
                }
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
        public Map<String, AllLHSRules> rulesMatches = new HashMap<String, AllLHSRules>();
        public Cell() { }

        // for lexical initialization
        public Cell(Set<grammar.Rule> rules, String word) {
            this.word = word;
            for (grammar.Rule rule : rules) {
                String key = rule.getLHS().getSymbols().get(0);
                if (!rulesMatches.containsKey(key)) {
                    rulesMatches.put(key, new AllLHSRules());
                }
                rulesMatches.get(key).addRuleWithSameLHS(rule, null, null);
            }
        }

    }
    public class AllLHSRules {
        private double bestMinusLogProb = Double.MAX_VALUE; // of all rules that start with the same LHS
        public Cell leftChildBackPointer, rightChildBackPointer;
        public Set<Rule> allRulesWithLHS = new HashSet<Rule>();
        public Rule bestRuleWithLHS;

        public AllLHSRules() {
        }

        // if a rule with the same LHS is better, update best rule values
        public void addRuleWithSameLHS(Rule rule, Cell left, Cell right) {
            this.allRulesWithLHS.add(rule);
            // it's leafs (words segments), best prob is just the prob itself
            if (left == null || right == null) {
                if (rule.getMinusLogProb() < this.bestMinusLogProb) {
                    this.bestMinusLogProb = rule.getMinusLogProb();
                    this.bestRuleWithLHS = rule;
                }
            } else { // internal nodes
                String r1Key = rule.getRHS().getSymbols().get(0);
                String r2Key = rule.getRHS().getSymbols().get(1);
                double bestProbSoFar = rule.getMinusLogProb() + left.rulesMatches.get(r1Key).getBestMinusLogProb() + right.rulesMatches.get(r2Key).getBestMinusLogProb();
                if (bestProbSoFar < this.bestMinusLogProb) {
                    this.bestMinusLogProb = bestProbSoFar;
                    this.leftChildBackPointer = left;
                    this.rightChildBackPointer = right;
                    this.bestRuleWithLHS = rule;
                }
            }
        }

        public double getBestMinusLogProb() {
            return bestMinusLogProb;
        }
    }


    public tree.Node getTreeIfExist(List<String> words) {
        boolean isLegal = false;
        Map<String, AllLHSRules> allLegalTopCells = new HashMap<String, AllLHSRules>();
        Cell topCell = this.runCYK(words);
        // iterate tree
        if (topCell.rulesMatches.containsKey("S")) {
            allLegalTopCells.put("S", topCell.rulesMatches.get("S"));
            topCell.rulesMatches = allLegalTopCells; // override only with legal rules for top cell
            tree.Node root = buildTree(topCell);
            return root;
        }
        return null; // no legal tree
    }

    private tree.Node buildTree(CKYDecoder.Cell topCell) {
        AllLHSRules bestRule = getBestRule(topCell);
        // it's a terminal
        if (bestRule.rightChildBackPointer == null && bestRule.leftChildBackPointer == null) {
            return MakeNodeContainTerminal(bestRule);
        }

        List<tree.Node> children = new LinkedList<tree.Node>();
        children.add(buildTree(bestRule.leftChildBackPointer));
        children.add(buildTree(bestRule.rightChildBackPointer));

        tree.Node parent = new tree.Node(((grammar.Event)bestRule.bestRuleWithLHS.getLHS()).getSymbols().get(0), false, null, children);
        children.get(0).setParent(parent);
        children.get(0).setParent(parent);
        return parent;
    }

    private tree.Node MakeNodeContainTerminal(AllLHSRules bestRule) {
        tree.Node parent = new tree.Node(((grammar.Event)bestRule.bestRuleWithLHS.getLHS()).getSymbols().get(0), false, null);
        tree.Node terminal = new tree.Node(((grammar.Event)bestRule.bestRuleWithLHS.getRHS()).toString(), false, parent);
        parent.addDaughter(terminal);
        return parent;
    }

    private static AllLHSRules getBestRule(CKYDecoder.Cell cell) {
        double bestRuleMinusLogProb = Double.MAX_VALUE; // we need to minimize
        AllLHSRules bestRuleData = null;
        Collection<AllLHSRules> sharedLHSRules = cell.rulesMatches.values();
        Iterator<AllLHSRules> iterator = sharedLHSRules.iterator();
        while (iterator.hasNext()) {
            AllLHSRules allRulesThatShareLHS = iterator.next();
            if (allRulesThatShareLHS.getBestMinusLogProb() < bestRuleMinusLogProb) {
                bestRuleMinusLogProb = allRulesThatShareLHS.bestMinusLogProb;
                bestRuleData = allRulesThatShareLHS;
            }
        }
        if (bestRuleData == null || sharedLHSRules.isEmpty()) {
            throw new IllegalArgumentException("should not got here...");
        }
        return bestRuleData;
//        if (bestRuleData == null && !sharedLHSRules.isEmpty()) { // probably NN word
//            bestRuleData = setForAllLeftTag.iterator().next().entrySet().iterator().next();
//            bestRuleData.getValue().bestMinusLogProb = Math.log(0.0001);
//        }

    }
}

