package gov.nasa.jpl.view_repo.util;

import org.apache.log4j.Logger;

/**
 * Binary search tree needed to support SVG to PNG conversion.
 * @author lho
 *
 */
public class PngBst {
	static Logger logger = Logger.getLogger(PngBst.class);

	public static PngBstNode root;

	public PngBst(){
		PngBst.root = null;
	}

	/**
	 * given a SVG node, is there exist a corresponding PNG?
	 * compare using name; therefore, needed to replace SVG to PNG extension before comparing.
	 * @param node
	 * @return
	 */
	public boolean find(PngBstNode node){
		PngBstNode current = root;
		while(current != null && current.data != null && node != null && node.data != null){
			int c = current.data.getName().compareTo(node.data.getName().replace(".svg", ".png"));
			if(c == 0) return true;
			else if(c > 0) current = current.left;
			else current = current.right;
		}
		return false;
	}

	public void insert(EmsScriptNode emsScriptNode){
		if(emsScriptNode == null) return;
		PngBstNode newNode = new PngBstNode(emsScriptNode);
		if(root == null){
			root = newNode;
			return;
		}
		PngBstNode current = root;
		PngBstNode parent = null;
		while(true){
			parent = current;
			int c = emsScriptNode.getName().compareTo(current.data.getName());
			if(c == 0) return;
			else if(c < 0){
				current = current.left;
				if(current == null){
					parent.left = newNode;
					return;
				}
			}
			else{
				current = current.right;
				if(current == null){
					parent.right = newNode;
					return;
				}
			}
		}
	}

	public void display(PngBstNode root){
		if(root != null){
			display(root.left);
			logger.info(" " + root.data.getName());
			display(root.right);
		}
	}

}
