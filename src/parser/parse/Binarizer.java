package parser.parse;

import tree.Tree;
import treebank.Treebank;
import tree.Node;
import java.util.*;

public class Binarizer {

	public int compareTreebanks(List<Tree> trees1, List<Tree> trees2) {
		int match = 0;
		int notMatch = 0;
		for (int i = 0; i < trees1.size(); i++) {
			Tree myTree1 = trees1.get(i);
			Tree myTree2 = trees2.get(i);
			boolean isEquals = compareTrees(myTree1.getRoot(), myTree2.getRoot());
			if (isEquals) {
				match++;
			} else {
				notMatch++;
			}
		}
		return match;
	}

	private boolean compareTrees(Node a, Node b) {
		if (a.getDaughters().isEmpty() && b.getDaughters().isEmpty()) {
			return true;
		}
		if (!a.getDaughters().isEmpty() && !b.getDaughters().isEmpty()) {
			boolean isEqual = a.getIdentifier().equals(b.getIdentifier());
			isEqual = isEqual && a.getDaughters().size() == b.getDaughters().size();
			for (int i = 0; i < a.getDaughters().size(); i++) {
				isEqual = isEqual && compareTrees(a.getDaughters().get(i), b.getDaughters().get(i));
			}
			return isEqual;
		}
		return false;
	}


	public Treebank binarizeTreebank(Treebank treebank, int markovOrder) {
		Treebank transformedTreebank = new Treebank();
		for (int i = 0; i < treebank.size(); i++) {
			Tree myTree = treebank.getAnalyses().get(i);
			binarizeTree(myTree.getRoot(), markovOrder);
			Tree binaryTree = new Tree(myTree.getRoot());
			transformedTreebank.add(binaryTree);
		}
		return transformedTreebank;
	}

	public Treebank undoBinarizeForTreebank(Treebank treebank, int markovOrder) {
		Treebank transformedTreebank = new Treebank();
		List<Tree> trees = treebank.getAnalyses();
		for (int i = 0; i < trees.size(); i++) {
			Tree myTree = trees.get(i);
			undoBinarizationForTree(myTree.getRoot());
			Tree binaryTree = new Tree(myTree.getRoot());
			transformedTreebank.add(binaryTree);
		}
		return transformedTreebank;
	}

	public List<Tree> undoBinarizeForListOfTrees(List<Tree> trees, int markovOrder) {
		List<Tree> notBinTrees = new LinkedList<Tree>();
		for (int i = 0; i < trees.size(); i++) {
			Tree myTree = trees.get(i);
			undoBinarizationForTree(myTree.getRoot());
			Tree binaryTree = new Tree(myTree.getRoot());
			notBinTrees.add(binaryTree);
		}
		return notBinTrees;
	}

	// this method undo binarization process for a single tree
	private void undoBinarizationForTree(Node node) {
		for (int i=node.getDaughters().size()-1;i>=0;i--){
			undoBinarizationForTree(node.getDaughters().get(i));
		}
		if (node.getIdentifier().contains("@")){
			for (Node daughter:node.getDaughters()){
				node.getParent().addDaughter(daughter);
				daughter.setParent(node);
			}
			node.getParent().removeDaughter(node);
		}
	}

	private void binarizeTree(Node node, int model) {
		if (node.getDaughters().size() > 2) {
			boolean hForModel = model > -1;
			Node tempParent = node;

			//Cloning is required since we are changing the original list during the loop
			List<Node> daughters=new ArrayList<>(node.getDaughters());
			LinkedList<String> labels = new LinkedList<>();
			if (model != 0)
				labels.add(daughters.get(0).getIdentifier());

			for (Node next:daughters.subList(1, daughters.size()-1)){
				String labelStr = (hForModel ? "/" : "") + joinner(labels)
						+ (hForModel ? "/" : "");
				Node subNode = new Node(labelStr + "@" + node.getIdentifier());
				subNode.addDaughter(next);
				node.removeDaughter(next);
				tempParent.addDaughter(subNode);
				tempParent = subNode;
				labels.add(next.getIdentifier());
				if (hForModel && labels.size() > model) {
					labels.removeFirst();
				}
			}
			Node lastDaughter=daughters.get(daughters.size()-1);
			tempParent.addDaughter(lastDaughter);
			node.removeDaughter(lastDaughter);
		}
		for (Node daughter : node.getDaughters()) {
			binarizeTree(daughter, model);
		}
	}

	private String joinner(List<String> str) {
		Iterator<String> it = str.iterator();
		if (!it.hasNext())
			return "";

		StringBuilder sb = new StringBuilder();
		for (;;) {
			String e = it.next();
			sb.append(e);
			if (!it.hasNext())
				return sb.toString();
			sb.append(',');
		}
	}


	private String getNewNodeName(Node root, int childNum, int markovOrder) {
		if (childNum == 0) {
			return root.getIdentifier(); // mark original node
		}
		if (markovOrder == 0) {
			return "@" + root.getIdentifier();
		}
		StringBuilder markovSiblingBuilder = new StringBuilder();
		List<Node> daughters = root.getDaughters();
		int startIndex = markovOrder < 0 ? 0 :  (childNum - markovOrder < 0 ? 0 : childNum - markovOrder);
		for (int i = startIndex; i < childNum; i++) {
			markovSiblingBuilder.append(daughters.get(i)).append("/");
		}
		return "@" + root.getIdentifier() + "/" + markovSiblingBuilder.toString();
	}

//	private tree.Node handleMultipleCase(tree.Node root, int childNumber, String originalParentName, int markovModel) {
//		// handle left child and attach as left subtree
//		List<tree.Node> binaryChildren = new LinkedList<tree.Node>();
//		tree.Node leftChild = root.getDaughters().get(childNumber);
//		binaryChildren.add(binarizeTree(leftChild, markovModel));
//		// handle right child and attach as right sub tree
//		int totalChildren = root.getDaughters().size();
//		int nextChildForHandling = childNumber + 1;
//		if (childNumber < totalChildren - 1) { // exist right child
//			tree.Node rightChild = handleMultipleCase(root, nextChildForHandling, originalParentName, markovModel);
//			binaryChildren.add(rightChild);
//		}
//		// if we have only one child, return it
//		if (binaryChildren.size() == 1) {
//			return binaryChildren.get(0);
//		} else { // create an internal node to hold left and right children
//			Node newNode;
//			newNode = new tree.Node(getNewNodeName(root, childNumber, markovModel), root.isRoot(), root.getParent(), binaryChildren);
//			binaryChildren.get(0).setParent(newNode); // update children parents
//			binaryChildren.get(1).setParent(newNode);
//			return newNode;
//		}
//	}

//	private String getHorizontalMarkovAnnotation(String parent, String sibling, int markovOrder) {
//		if (markovOrder == 0) {
//			return parent;
//		}
//		StringBuilder markovSiblingBuilder = new StringBuilder();
//		String[] siblingsToKeep = sibling.split("/");
//		int startIndex = markovOrder < 0 ? 0 :  siblingsToKeep.length - markovOrder;
//		for (int i = startIndex; i < siblingsToKeep.length; i++) {
//			markovSiblingBuilder.append(siblingsToKeep[i]).append("/");
//		}
//		return parent.split("/")[0] + "/" + markovSiblingBuilder.toString();
//	}


}
