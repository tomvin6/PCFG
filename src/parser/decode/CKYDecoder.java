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
        rule.setMinusLogProb(-Math.log(0.001));
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

                    // IF BINARY   ---> HANDLE BINARY CASE-
                    TreepleRules rules = minimizeRulesBelongToo(leftChildCell, rightChildCell, rulesMap);
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

    private void upsertRulesToCellUsingBestProbLogic(Cell cell, TreepleRules rules, Cell leftChildCell, Cell rightChildCell) {
            for (Rule rule : rules.leftUnaryRules) {
                String leftKey = rule.getLHS().getSymbols().get(0);

                if (!cell.rulesMatches.containsKey(leftKey)) {
                    AllLHSRules allLHSRules = new AllLHSRules();
                    cell.rulesMatches.put(leftKey, allLHSRules);
                }
                // key exist
                cell.rulesMatches.get(leftKey).addSyntRuleWithSameLHS("l", rule, leftChildCell, rightChildCell); // best prob maintained inside
                cell.segment = leftKey;
            }

        for (Rule rule : rules.rightUnaryRules) {
            String leftKey = rule.getLHS().getSymbols().get(0);

            if (!cell.rulesMatches.containsKey(leftKey)) {
                AllLHSRules allLHSRules = new AllLHSRules();
                cell.rulesMatches.put(leftKey, allLHSRules);
            }
            // key exist
            cell.rulesMatches.get(leftKey).addSyntRuleWithSameLHS("r", rule, leftChildCell, rightChildCell); // best prob maintained inside
            cell.segment = leftKey;
        }

        for (Rule rule : rules.binaryRules) {
            String leftKey = rule.getLHS().getSymbols().get(0);

            if (!cell.rulesMatches.containsKey(leftKey)) {
                AllLHSRules allLHSRules = new AllLHSRules();
                cell.rulesMatches.put(leftKey, allLHSRules);
            }
            // key exist
            cell.rulesMatches.get(leftKey).addSyntRuleWithSameLHS("s", rule, leftChildCell, rightChildCell); // best prob maintained inside
            cell.segment = leftKey;
        }

    }

    public class TreepleRules {
        Set<grammar.Rule> leftUnaryRules;
        Set<grammar.Rule> rightUnaryRules;
        Set<grammar.Rule> binaryRules;
        public TreepleRules(Set<grammar.Rule> binaryRules,Set<grammar.Rule> leftUnaryRules, Set<grammar.Rule> rightUnaryRules) {
            this.leftUnaryRules = leftUnaryRules;
            this.rightUnaryRules = rightUnaryRules;
            this.binaryRules = binaryRules;
        }
    }

    private TreepleRules minimizeRulesBelongToo(Cell x, Cell y, Map<String, Set<grammar.Rule>> rulesMap) {
        Set<grammar.Rule> binaryRules = new HashSet<grammar.Rule>();
        Set<grammar.Rule> leftUnaryRules = new HashSet<grammar.Rule>();
        Set<grammar.Rule> rightUnaryRules = new HashSet<grammar.Rule>();


        for (String left : x.rulesMatches.keySet()) {
            if (rulesMap.containsKey(left)) {
                leftUnaryRules.addAll(rulesMap.get(left));
            }

            for (String right : y.rulesMatches.keySet()) {
                if (rulesMap.containsKey(right)) {
                    rightUnaryRules.addAll(rulesMap.get(right));
                }

                String key = left.concat(" ").concat(right);
                if (rulesMap.containsKey(key)) {
                    binaryRules.addAll(rulesMap.get(key));
                }
            }
        }
        return new TreepleRules(binaryRules, leftUnaryRules, rightUnaryRules);
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
        public String segment;
        public Map<String, AllLHSRules> rulesMatches = new HashMap<String, AllLHSRules>();
        public Cell() { }

        // for lexical initialization
        public Cell(Set<grammar.Rule> rules, String word) {
            this.segment = word;
            for (grammar.Rule rule : rules) {
                String key = rule.getLHS().getSymbols().get(0);
                if (!rulesMatches.containsKey(key)) {
                    rulesMatches.put(key, new AllLHSRules());
                }
                rulesMatches.get(key).addLexRules(rule);
            }
        }

    }
    public class AllLHSRules {
        private double bestMinusLogProb = Double.MAX_VALUE; // of all rules that start with the same LHS
        public Cell leftChildBackPointer = null, rightChildBackPointer = null;
        public Set<Rule> allRulesWithLHS = new HashSet<Rule>();
        public Rule bestRuleWithLHS;

        public AllLHSRules() {
        }

        public void addLexRules(Rule rule) {
            this.allRulesWithLHS.add(rule);
            if (rule.getMinusLogProb() < this.bestMinusLogProb) {
                this.bestMinusLogProb = rule.getMinusLogProb();
                this.bestRuleWithLHS = rule;
            }
        }
        // if a rule with the same LHS is better, update best rule values
        public void addSyntRuleWithSameLHS(String ruleType, Rule rule, Cell left, Cell right) {
            this.allRulesWithLHS.add(rule);
            Cell bestLeft = null;
            Cell bestRight = null;
            String r1Key = rule.getRHS().getSymbols().get(0);
            double bestProbSoFar;
            if (ruleType.equals("l")) { // left unary rule
                bestProbSoFar = rule.getMinusLogProb() + getKeyOrNNProb(left, r1Key);
                bestLeft = left;
            } else if (ruleType.equals("r")) { // right unary rule
                bestProbSoFar = rule.getMinusLogProb() + getKeyOrNNProb(right, r1Key);
                bestRight = right;
            } else { // binary rule
                String r2Key = rule.getRHS().getSymbols().get(1);
                bestLeft = left;
                bestRight = right;
                bestProbSoFar = getKeyOrNNProb(left, r1Key) + getKeyOrNNProb(right, r2Key);
            }
            if (bestProbSoFar < this.bestMinusLogProb) {
                this.bestMinusLogProb = bestProbSoFar;
                this.leftChildBackPointer = bestLeft;
                this.rightChildBackPointer = bestRight;
                this.bestRuleWithLHS = rule;
            }

        }

        private double getKeyOrNNProb(Cell child, String r1Key) {
            if (child.rulesMatches.containsKey(r1Key)) {
                child.rulesMatches.get(r1Key).getBestMinusLogProb();
            }
            return -Math.log(0.0001);
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
        if (bestRule.leftChildBackPointer != null) {
            children.add(buildTree(bestRule.leftChildBackPointer));
        }
        if (bestRule.rightChildBackPointer != null) {
            children.add(buildTree(bestRule.rightChildBackPointer));
        }

        tree.Node parent = new tree.Node(((grammar.Event)bestRule.bestRuleWithLHS.getLHS()).getSymbols().get(0), false, null, children);
        for (tree.Node child : children) {
            child.setParent(parent);
        }
        return parent;
    }

    private tree.Node MakeNodeContainTerminal(AllLHSRules bestRule) {
        tree.Node parent = new tree.Node(((grammar.Event)bestRule.bestRuleWithLHS.getLHS()).getSymbols().get(0), false, null);
        tree.Node terminal = new tree.Node(((grammar.Event)bestRule.bestRuleWithLHS.getRHS()).getSymbols().get(0), false, parent);
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
        return bestRuleData;
    }
}

