package parser.decode;

import grammar.Grammar;
import grammar.Rule;

import java.util.*;

public class CKYDecoder
{
    private Cell[][] chart;
    private Set<grammar.Rule> _sRules;
    private Cell topCell;

    public CKYDecoder(String [] words ,Grammar g) {
        this.decode(words, g);
    }

    private void decode(String [] words ,Grammar g) {
        Map<String, Set<grammar.Rule>> lexRules = g.getLexicalEntries();
        _sRules = g.getSyntacticRules();
        chart = new Cell[words.length][words.length];
        // init phase
        for(int i = 0 ; i < words.length ; i++) {
            chart[0][i] = new Cell(lexRules.get(words[i]), words[i]);
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
                    Set<Rule> rules = RulesBelongToo(leftChildCell, rightChildCell);
                    upsertRulesToCellUsingBestProbLogic(cell, rules, leftChildCell, rightChildCell);
                    z++;
                    t--;
                }
            }
        }
        this.topCell = chart[chart.length - 1][0];
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


    private Set<grammar.Rule> RulesBelongToo(Cell x, Cell y) {
        HashSet rulesToReturn = new HashSet<grammar.Rule>();

        for (grammar.Rule xRule : x.rulesMatches.keySet()) {
            for (grammar.Rule yRule : y.rulesMatches.keySet()) {
                List<String> e1 = xRule.getLHS().getSymbols();
                List<String> e2 = yRule.getLHS().getSymbols();
                List<String> bothSymbols = new ArrayList<String>();
                bothSymbols.addAll(e1);
                bothSymbols.addAll(e2);
                for (grammar.Rule resultRule : _sRules) {

                    List<String> e3 = resultRule.getRHS().getSymbols();
                    if(!rulesToReturn.contains(resultRule) && CompareTwoLists(e3,bothSymbols))
                    {
                        rulesToReturn.add(resultRule);
                    }
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

    public Cell getTopCell() {
        return topCell;
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
}

