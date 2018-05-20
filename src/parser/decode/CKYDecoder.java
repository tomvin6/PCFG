package parser.decode;

import grammar.Grammar;

import java.util.*;

public class CKYDecoder
{
    public class Cell
    {
        public String word;
        public int startIndex;
        public int endIndex;
        public Set<grammar.Rule> rulesMatches;
        public Cell left;
        public Cell right;
        public double prob;

        public Cell()
        {
            rulesMatches = new HashSet<grammar.Rule>();
        }

    }
    private Cell[][] chart;
    private Set<grammar.Rule> _sRules;
    public CKYDecoder(String [] words ,Grammar g)
    {
        Map<String, Set<grammar.Rule>> lexRules = g.getLexicalEntries();
        _sRules = g.getSyntacticRules();
        chart = new Cell[words.length][words.length];
        // first step init;
        for(int i = 0 ; i < words.length ; i++)
        {
            Cell cell = new Cell();
            cell.word = words[i];
            cell.rulesMatches = lexRules.get(words[i]);
            chart[0][i] = cell;
        }

       // get all rules for the table
        for(int i = 1; i < chart.length; i++)
        {
            for(int j = 0 ; j <chart.length - i; j++)
            {
                int z = 0;
                int t = i + j;
                Set<grammar.Rule> ruleMatches = new HashSet<grammar.Rule>();
                for(int k = i - 1; k >= 0 && z < i && t >= 0; k-- )// column down
                {
                    Cell cell;
                    if(chart[i][j] == null)
                    {
                        cell = new Cell();
                        chart[i][j] = cell;
                    }
                    cell = chart[i][j];
                    cell.rulesMatches.addAll(RulesBelongToo(chart[k][j],chart[z][t]));
                    chart[i][j] = cell;
                    z++;
                    t--;
                }

            }
        }
    }


    private Set<grammar.Rule> RulesBelongToo(Cell x, Cell y)
    {
        HashSet rulesToReturn = new HashSet<grammar.Rule>();

        for (grammar.Rule xRule : x.rulesMatches) {
            for (grammar.Rule yRule : y.rulesMatches) {
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
    private boolean CompareTwoLists(List<String> s1, List<String> s2)
    {
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
}

