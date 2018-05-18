package parser.parse;

import tree.Tree;
import treebank.Treebank;
import tree.Node;
import java.util.*;

public class Binarizer {

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
				binaryChildren.add(binarizeTree(child, markovModel));
			}
			binaryChildren.get(0).setParent(newNode); // update children parents
			binaryChildren.get(1).setParent(newNode);
			return newNode;
		} else {
			tree.Node newRoot = handleMultipleCase(root, 0, root.getIdentifier() + "", markovModel);
			return newRoot;
		}
	}

//	private tree.Node iterativeHandleMultipleCase(tree.Node root, int markovModel) {
//		List<Node> daughters = root.getDaughters();
//		List<tree.Node> binaryChildren = new LinkedList<tree.Node>();
//		// handle left child
//		tree.Node leftChild = root.getDaughters().get(0);
//		binaryChildren.add(binarizeTree(leftChild, markovModel));
//
//		if (daughters.size() == 2) { // exactly two children
//			tree.Node rightChild = root.getDaughters().get(1);
//			binaryChildren.add(binarizeTree(rightChild, markovModel));
//		} else { // more than 2 children
//			for (int childNum = 1; childNum < daughters.size() - 2; childNum++) {
//				Node newParentNode = new tree.Node(getNewNodeName(root, markovModel), root.isRoot(), root.getParent());
//				binaryChildren.add(newParentNode); // right child in parent
//				Node tmpChild = binarizeTree(daughters.get(childNum), markovModel);
//				newParentNode.addDaughter(tmpChild);
//
//
//
//
//
//
//				binaryChildren.get(0).setParent(newParentNode); // update children parents
//				binaryChildren.get(1).setParent(newParentNode);
//
//
//			}
//		}
//	}
//
	private String getNewNodeName(Node root, int childNum, int markovOrder) {
		if (markovOrder == 0) {
			return root.getIdentifier() + "";
		}
		StringBuilder markovSiblingBuilder = new StringBuilder();
		List<Node> daughters = root.getDaughters();
		int startIndex = markovOrder < 0 ? 0 :  (childNum - markovOrder < 0 ? 0 : childNum - markovOrder);
		for (int i = startIndex; i < childNum; i++) {
			markovSiblingBuilder.append(daughters.get(i)).append("/");
		}
		return root.getIdentifier() + "" + "/" + markovSiblingBuilder.toString();
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

    private String getHorizontalMarkovAnnotation(String parent, String sibling, int markovOrder) {
		if (markovOrder == 0) {
			return parent;
		}
		StringBuilder markovSiblingBuilder = new StringBuilder();
		String[] siblingsToKeep = sibling.split("/");
		int startIndex = markovOrder < 0 ? 0 :  siblingsToKeep.length - markovOrder;
		for (int i = startIndex; i < siblingsToKeep.length; i++) {
			markovSiblingBuilder.append(siblingsToKeep[i]).append("/");
        }
        return parent.split("/")[0] + "/" + markovSiblingBuilder.toString();
    }


}
