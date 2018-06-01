package decode;

import grammar.Grammar;
import grammar.Rule;

import java.util.*;

import tree.Node;
import tree.Terminal;
import tree.Tree;

public class Decode {

	public static Set<Rule> grammerRulesSet = null;
	public static Map<String, Set<Rule>> lexicalRulesMap = null;

	/**
	 * Implementation of a singleton pattern Avoids redundant instances in
	 * memory
	 */
	public static Decode m_singDecoder = null;

	public static Decode getInstance(Grammar g) {
		if (m_singDecoder == null) {
			m_singDecoder = new Decode();
			grammerRulesSet = g.getSyntacticRules();
			lexicalRulesMap = g.getLexicalEntries();
		}
		return m_singDecoder;
	}

	private class Cell {
		final private double minusLogProb;
		final private grammar.Event event;
		final private Cell child1;
		final private Cell child2;

		final private boolean isLex;

		private Cell(grammar.Event event, double minusLogProb, Cell child1, Cell child2, boolean isLex) {
			this.minusLogProb = minusLogProb;
			this.child1 = child1;
			this.child2 = child2;
			this.event = event;
			this.isLex = isLex;
		}

		public Cell(grammar.Event event, double minusLogProb, Cell child1, Cell child2) {
			this(event, minusLogProb, child1, child2, false);
			assert child1 != null;
			assert child2 != null;
		}

		public Cell(grammar.Event event, double minusLogProb, Cell child) {
			this(event, minusLogProb, child, null, false);
			assert child != null;
		}

		public Cell(grammar.Event event) {
			this(event, -0.0, null, null, true);
		}

		public double getMinusLogProb() {
			return minusLogProb;
		}

		public grammar.Event getEvent() {
			return event;
		}

		public Cell getChild1() {
			return child1;
		}

		public Cell getChild2() {
			return child2;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (child1 == null)
				sb.append('"');
			sb.append(event.toString());
			if (child1 != null) {
				sb.append("->(");
				if (child1.isLex)
					sb.append('"');
				sb.append(child1.event);
				if (child1.isLex)
					sb.append('"');
				if (child2 != null)
					sb.append(' ').append(child2.event);
				sb.append(")[").append(String.format("%.2f", minusLogProb)).append(']');
			} else
				sb.append('"');
			return sb.toString();
		}

	}

	private Cell getCell(ChartCell cell, String symbol) {
		if (cell == null)
			return null;
		return cell.get(symbol);
	}

	private boolean addTagProb(ChartCell cell, Cell tp) {
		return cell.addTagProb(tp);
	}

	public Tree decode(List<String> input) {
		Tree tree = null;
		// by guidelines, no line should be greater than 40
		if (input.size() <= 40)
			tree = ckyParse(input);

		if (tree == null || input.size() > 40) {
			System.out.println("return baseline parser for input " + input);
			tree = dummyParse(input);
		}
		return tree;
	}

	private Tree ckyParse(List<String> input) {
		ChartCell[][] chart = new ChartCell[input.size()][];

		Set<Rule> biRules = new HashSet<>();
		Set<Rule> uniRules = new HashSet<>();
		for (Rule r : grammerRulesSet) {
			if (r.getRHS().getSymbols().size() == 2) {
				biRules.add(r);
			} else {
				uniRules.add(r);
			}
		}
		// init
		for (int i = 0; i < input.size(); i++) {
			String seg = input.get(i);
			chart[i] = new ChartCell[input.size() + 1];
			Set<Rule> lexRules = lexicalRulesMap.get(seg);
			if (lexRules == null || lexRules.isEmpty()) {
				lexRules = new HashSet<>();
				Rule nnRule = new Rule("NN", seg, true);
				nnRule.setMinusLogProb(0.0); // that breaks the model, but works
				lexRules.add(nnRule);
			} else {
				lexRules = new HashSet<>(lexRules);
			}

			// Add lex rules
			for (Rule rule : lexRules) {
				if (chart[i][i + 1] == null)
					chart[i][i + 1] = new ChartCell();
				if (chart[i][i] == null)
					chart[i][i] = new ChartCell();
				Cell lexCell = new Cell(rule.getRHS());
				chart[i][i].addTagProb(lexCell);
				Cell synCell = new Cell(rule.getLHS(), rule.getMinusLogProb(), lexCell);
				chart[i][i + 1].addTagProb(synCell);
			}

			// Add unary rules
			boolean added;
			do {
				added = false;
				for (Rule rule : uniRules) {
					grammar.Event lhs = rule.getLHS();
					grammar.Event rhs = rule.getRHS();
					List<String> symbols = rhs.getSymbols();
					Cell child = getCell(chart[i][i + 1], symbols.get(0));
					if (child != null) {
						double minusLogProb = child.getMinusLogProb() + rule.getMinusLogProb();
						Cell tp = new Cell(lhs, minusLogProb, child);
						added = added || addTagProb(chart[i][i + 1], tp);
					}
				}
			} while (added);
		}

		// printChart(chart);
		for (int i = 2; i <= input.size(); i++) { // row

			for (int j = i - 2; j >= 0; j--) { // col
				for (int k = j + 1; k <= i - 1; k++) {
					if (chart[j][i] == null) {
						chart[j][i] = new ChartCell();
					}

					for (Rule r : biRules) {
						grammar.Event lhs = r.getLHS();
						grammar.Event rhs = r.getRHS();
						List<String> symbols = rhs.getSymbols();

						Cell left = getCell(chart[j][k], symbols.get(0));
						Cell right = getCell(chart[k][i], symbols.get(1));

						if (left != null && right != null) {
							double minusLogProb = left.getMinusLogProb() + right.getMinusLogProb()
									+ r.getMinusLogProb();
							Cell tp = new Cell(lhs, minusLogProb, left, right);
							addTagProb(chart[j][i], tp);
						}
					}
					boolean added;
					do {
						added = false;
						for (Rule r : uniRules) {
							grammar.Event lhs = r.getLHS();
							grammar.Event rhs = r.getRHS();
							List<String> symbols = rhs.getSymbols();
							Cell child = getCell(chart[j][i], symbols.get(0));
							if (child != null) {
								double minusLogProb = child.getMinusLogProb() + r.getMinusLogProb();
								Cell tp = new Cell(lhs, minusLogProb, child);
								added = added || addTagProb(chart[j][i], tp);
							}
						}
					} while (added);
				}
			}
		}
		Cell start = null;
		for (Cell tp : chart[0][input.size()].values()) {
			if (tp.event.toString().equals("S")) {
				if (start == null || start.getMinusLogProb() > tp.getMinusLogProb())
					start = tp;
			}
		}
		if (start == null)
			return null;
		Node top = new Node("TOP");
		Node s = new Node("S");
		s.setParent(top);
		top.addDaughter(s);
		buildTree(s, start);
		Tree tree = new Tree(top);
		// printChart(chart);
		return tree;
	}

	private void buildTree(Node node, Cell tp) {
		node.setIdentifier(tp.getEvent().toString());
		if (tp.getChild1() != null) {
			Node d1 = new Node(tp.getChild1().getEvent().toString());
			node.addDaughter(d1);
			d1.setParent(node);
			buildTree(d1, tp.getChild1());
		}
		if (tp.getChild2() != null) {
			Node d2 = new Node(tp.getChild1().getEvent().toString());
			node.addDaughter(d2);
			d2.setParent(node);
			buildTree(d2, tp.getChild2());
		}

	}

	@SuppressWarnings("unused")
	private void printChart(ChartCell[][] chart) {
		int[] width = new int[chart[0].length];
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				int thisWidth = chart[i][j] != null ? chart[i][j].values().toString().length() : 0;
				if (thisWidth > width[j])
					width[j] = thisWidth;
			}
		}
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				String format = "%-" + width[j] + "s";
				if (chart[i][j] == null)
					System.out.print(String.format(format, ""));
				else
					System.out.print(String.format(format, chart[i][j].values()));
				System.out.print("|");
			}
			System.out.println("");
		}
	}

	private Tree dummyParse(List<String> input) {
		Tree t = new Tree(new Node("TOP"));
		Iterator<String> theInput = input.iterator();
		while (theInput.hasNext()) {
			String theWord = theInput.next();
			Node preTerminal = new Node("NN");
			Terminal terminal = new Terminal(theWord);
			preTerminal.addDaughter(terminal);
			t.getRoot().addDaughter(preTerminal);
		}
		return t;
	}

	private class ChartCell extends HashMap<String, Cell> {
		private static final long serialVersionUID = 7657267520681027890L;

		public boolean addTagProb(Cell tp) {
			String sym = tp.getEvent().toString();
			if (this.containsKey(sym)) {
				Cell existing = get(sym);
				if (existing.getMinusLogProb() <= tp.getMinusLogProb()) {
					return false;
				}
			}
			this.put(sym, tp);
			return true;
		}
	}
}
