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
        Map<String, Set<grammar.Rule>> rulesMap = myGrammer.rulesMap;
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



    private void upsertRulesToCellUsingBestProbLogic(Cell cell, Set<Rule> rules, Cell leftChildCell, Cell rightChildCell) {
        double leftChildCellMinusLogProb = leftChildCell != null ? leftChildCell.minusLogProb : 0;
        double rightChildCellMinusLogProb = rightChildCell != null ? rightChildCell.minusLogProb : 0;
        for (Rule rule : rules) {
            String leftKey = rule.getLHS().getSymbols().get(0);
            if (!cell.rulesMatches.containsKey(leftKey)) {
                cell.rulesMatches.put(leftKey, new HashSet<grammar.Rule>());
            }
            cell.rulesMatches.get(leftKey).add(rule);
            //summming logs
            double possibleLogProb = leftChildCellMinusLogProb + rightChildCellMinusLogProb + rule.getMinusLogProb();
            if(possibleLogProb > cell.minusLogProb)
            {
                cell.minusLogProb = possibleLogProb;
                cell.leftChild = leftChildCell;
                cell.rightChild = rightChildCell;
                cell.bestRule = rule;
            }
        }
    }

    private Set<grammar.Rule> minimizeRulesBelongToo(Cell x, Cell y, Map<String, Set<grammar.Rule>> rulesMap) {
        Set<grammar.Rule> rulesToReturn = new HashSet<grammar.Rule>();

        for (String left : x.rulesMatches.keySet()) {
            if(left.length() == 1) // UNARY
            {
                if (rulesMap.containsKey(left)) {
                    rulesToReturn.addAll(rulesMap.get(left));
                }
            }

            for (String right : y.rulesMatches.keySet()) {
                if (right.length() == 1)
                {
                    if (rulesMap.containsKey(right)) {
                        rulesToReturn.addAll(rulesMap.get(right));
                    }
                }

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
        public double minusLogProb;
        public Cell leftChild;
        public Cell rightChild;
        public Rule bestRule;
        public Map<String, Set<Rule>> rulesMatches = new HashMap<String, Set<Rule>>();
        public Cell() {
            leftChild = null;
            rightChild = null;
            minusLogProb = -1;
        }

        // for lexical initialization
        public Cell(Set<Rule> rules, String word) {
            this.word = word;
            leftChild = null;
            rightChild = null;
            minusLogProb = -2;

            for (Rule rule : rules) {
                String key = rule.getLHS().getSymbols().get(0);
                if (!rulesMatches.containsKey(key)) {
                    rulesMatches.put(key, new HashSet<Rule>());
                }
                rulesMatches.get(key).add(rule);
                if(rule.getMinusLogProb() > minusLogProb)
                {
                    minusLogProb = rule.getMinusLogProb();
                    bestRule = rule;
                }
            }

        }

    }



    public tree.Node getTreeIfExist(List<String> words) {
        boolean isLegal = false;
        Cell topCell = this.runCYK(words);
        // iterate tree
        if ( topCell.rulesMatches.containsKey("S")) {// contains S is not good should have all starting symbols in grammar
            topCell.bestRule = topCell.rulesMatches.get("S").iterator().next();
            tree.Node root = buildTree(topCell);
            return root;
        }
        return null; // no legal tree
    }

    private tree.Node buildTree(CKYDecoder.Cell topCell) {
        // it's a terminal
        if (topCell.rightChild == null && topCell.leftChild == null) {
            return MakeNodeContainTerminal(topCell);
        }

        List<tree.Node> children = new LinkedList<tree.Node>();
        tree.Node leftTreeChild = buildTree(topCell.leftChild);
        tree.Node rightTreeChild = buildTree(topCell.rightChild);
        children.add(leftTreeChild);
        children.add(rightTreeChild);

        tree.Node parent = new tree.Node(((grammar.Event)topCell.bestRule.getLHS()).toString(), false, null, children);
        leftTreeChild.setParent(parent);
        rightTreeChild.setParent(parent);
        return parent;
    }

    private tree.Node MakeNodeContainTerminal(Cell cell) {
        tree.Node parent = new tree.Node(((grammar.Event)cell.bestRule.getLHS()).toString(), false, null);
        tree.Node terminal = new tree.Node(((grammar.Event)cell.bestRule.getRHS()).toString(), false, parent);
        parent.addDaughter(terminal);
        return parent;
    }

}

