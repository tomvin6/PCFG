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
		// we have more than one child
		String uniqueLabel = root.getIdentifier();
		tree.Node newRoot = handleMultipleCase(root, 0, uniqueLabel, markovModel);
		return newRoot;
	}

	private tree.Node handleMultipleCase(tree.Node root, int childNumber, String parentLabel, int markovModel) {
		// handle left child and attach as left subtree
		List<tree.Node> binaryChildren = new LinkedList<tree.Node>();
		tree.Node leftChild = root.getDaughters().get(childNumber);
		binaryChildren.add(binarizeTree(leftChild, markovModel));
		// handle right child and attach as right sub tree
		if (childNumber < root.getDaughters().size() - 1) { // exist right child
			String newLabel = getHorizontalMarkovAnnotation(parentLabel, leftChild.getIdentifier(), markovModel);
			tree.Node rightChild = handleMultipleCase(root, childNumber + 1, newLabel, markovModel);
			binaryChildren.add(rightChild);
		}
		// if we have only one child, return it
		if (binaryChildren.size() == 1) {
			return binaryChildren.get(0);
		} else { // create an internal node to hold left and right children
			Node newNode = new tree.Node(parentLabel, root.isRoot(), root.getParent(), binaryChildren);
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
		int startIndex = getStartIndex(siblingsToKeep.length, markovOrder);
		for (int i = startIndex; i < siblingsToKeep.length; i++) {
			markovSiblingBuilder.append(siblingsToKeep[i]).append("/");
        }
        return parent.split("/")[0] + "/" + markovSiblingBuilder.toString();
    }

    private int getStartIndex(int siblingSize, int markovOrder) {
		if (markovOrder == - 1) {
			return 0;
		} else {
			return siblingSize - (markovOrder < 0 ? 0 :  siblingSize - markovOrder);
		}
	}
}
