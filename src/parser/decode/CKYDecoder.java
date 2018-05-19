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
            for(int j = 0 ; j < chart.length - i; j++)
            {
                for(int k = 0 ; k < j ;k++)
                {
                    Cell cell = new Cell();
                    cell.rulesMatches = RulesBelongToo(chart[j][k],chart[k][i]);
                    chart[j][i] = cell;
                }
            }
        }
    }

    public boolean processString(String [] w, Grammar g)
    {
        int length = w.length;
        chart = new Cell[w.length][w.length];

        for (int i = 0; i < length; ++i)
        {
            for (int j = 0; j < length; ++j)
                chart[i][j] = new Cell();
        }

        for (int i = 0; i < length; ++i)
        {
            HashSet<String> keys = (HashSet<String>)g.getTerminalSymbols();
            for (String key : keys)
            {
                if(keys.contains(w[i]))
                {
                    Cell cell = new Cell();
                    cell.word = w[i];
                    chart[i][i] = cell;
                }
            }
        }
        for (int l = 2; l <= length; ++l)
        {
            for (int i = 0; i <= length - l; ++i)
            {
                int j = i + l - 1;
                for (int k = i; k <= j - 1; ++k)
                {
                    Cell cell = new Cell();
                    cell.rulesMatches = RulesBelongToo(chart[i][k],chart[k+1][j]);
                }
            }
        }
        if (chart[0][length - 1].rulesMatches.contains(g.getStartSymbols())) // we started from 0
            return true;

        return false;
    }

    private Set<grammar.Rule> RulesBelongToo(Cell x, Cell y)
    {
        HashSet rulesToReturn = new HashSet<grammar.Rule>();
        if(x == null || y == null || x.rulesMatches == null || y.rulesMatches == null)
        {
            return  rulesToReturn;
        }
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

