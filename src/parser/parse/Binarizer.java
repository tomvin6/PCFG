package parser.parse;

import tree.Tree;
import treebank.Treebank;
import tree.Node;
import java.util.*;

public class Binarizer {

	public boolean compareTreebanks(Treebank treeband1, Treebank treeband2) {
		for (int i = 0; i < treeband1.size(); i++) {
			Tree myTree1 = treeband1.getAnalyses().get(i);
			Tree myTree2 = treeband2.getAnalyses().get(i);
			boolean isEquals = compareTrees(myTree1.getRoot(), myTree2.getRoot());
			if (isEquals == false) {
				return false;
			}
		}
		return true;
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
			tree.Node rootBinaryTreeNode = binarizeTree(myTree.getRoot(), markovOrder);
			Tree binaryTree = new Tree(rootBinaryTreeNode);
			transformedTreebank.add(binaryTree);
		}
		return transformedTreebank;
	}

	public Treebank undoBinarizeForTreebank(Treebank treebank, int markovOrder) {
		Treebank transformedTreebank = new Treebank();
		for (int i = 0; i < treebank.size(); i++) {
			Tree myTree = treebank.getAnalyses().get(i);
			tree.Node rootBinaryTreeNode = undoBinarizationForTree(myTree.getRoot());
			Tree binaryTree = new Tree(rootBinaryTreeNode);
			transformedTreebank.add(binaryTree);
		}
		return transformedTreebank;
	}

	// this method undo binarization process for a single tree
	private Node undoBinarizationForTree(Node root) {
		if (root.getDaughters().isEmpty()) {
			return new Node(root.getIdentifier(), root.isRoot(), root.getParent(), root.getDaughters());
		}
		Node newRoot = new Node(root.getIdentifier(), root.isRoot(), root.getParent());
		for (Node child : root.getDaughters()) {
			Node originalChildSubTree = undoBinarizationForTree(child);
			if (originalChildSubTree.getIdentifier().startsWith("@")) {
				for (Node subTreeChild : originalChildSubTree.getDaughters()) {
					newRoot.addDaughter(subTreeChild);
				}
			} else {
				newRoot.addDaughter(originalChildSubTree);
			}
		}
		return newRoot;
	}

	private Node binarizeTree(Node root, int markovModel) {
		// if root has no children or only one child, we are already
		// in binary mode, return the new nodes
		if (root.getDaughters().isEmpty() || root.getDaughters().size() == 1) {
			Node newNode = new Node(root.getIdentifier(), root.isRoot(), root.getParent());

			if (!root.getDaughters().isEmpty()) { // single child case
				Node childrenNode = binarizeTree(root.getDaughters().get(0), markovModel);
				newNode.addDaughter(childrenNode);
				childrenNode.setParent(newNode);

			}
			return newNode; // single child OR leaf case
		}
		// if we have exactly two children, handle each and return parent
		if (root.getDaughters().size() == 2) {
			List<tree.Node> binaryChildren = new LinkedList<tree.Node>();
			Node newNode = new tree.Node(root.getIdentifier(), root.isRoot(), root.getParent(), binaryChildren);
			for (Node child : root.getDaughters()) {
				Node node = binarizeTree(child, markovModel);
				node.setParent(newNode);
				binaryChildren.add(node);
			}
			return newNode;
		} else {
			return handleMultipleCase(root, 0, root.getIdentifier() + "", markovModel);
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

	private tree.Node handleMultipleCase(tree.Node root, int childNumber, String originalParentName, int markovModel) {
		// handle left child and attach as left subtree
		List<tree.Node> binaryChildren = new LinkedList<tree.Node>();
		tree.Node leftChild = root.getDaughters().get(childNumber);
		binaryChildren.add(binarizeTree(leftChild, markovModel));
		// handle right child and attach as right sub tree
		int totalChildren = root.getDaughters().size();
		int nextChildForHandling = childNumber + 1;
		if (childNumber < totalChildren - 1) { // exist right child
			tree.Node rightChild = handleMultipleCase(root, nextChildForHandling, originalParentName, markovModel);
			binaryChildren.add(rightChild);
		}
		// if we have only one child, return it
		if (binaryChildren.size() == 1) {
			return binaryChildren.get(0);
		} else { // create an internal node to hold left and right children
			Node newNode;
			newNode = new tree.Node(getNewNodeName(root, childNumber, markovModel), root.isRoot(), root.getParent(), binaryChildren);
			binaryChildren.get(0).setParent(newNode); // update children parents
			binaryChildren.get(1).setParent(newNode);
			return newNode;
		}
	}

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
