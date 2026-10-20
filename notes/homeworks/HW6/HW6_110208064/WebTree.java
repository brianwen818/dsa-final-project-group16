import java.io.IOException;
import java.util.ArrayList;


public class WebTree {
	public WebNode root;
	
	public WebTree(WebPage rootPage){
		this.root = new WebNode(rootPage);
	}
	
	public void setPostOrderScore(ArrayList<Keyword> keywords) throws IOException{
		setPostOrderScore(root, keywords);
	}
	
	private void setPostOrderScore(WebNode startNode, ArrayList<Keyword> keywords) throws IOException{
    for (WebNode child : startNode.children) {
        setPostOrderScore(child, keywords);
    }
    startNode.setNodeScore(keywords);
	}
	
	public void eularPrintTree(){
		eularPrintTree(root);
	}
	
	private void eularPrintTree(WebNode startNode){
    String indent = repeat("\t", startNode.getDepth() - 1);
    System.out.print(indent + "(" + startNode.webPage.name + "," + startNode.nodeScore);

    if (startNode.children.isEmpty()) {
        System.out.println(")");
    } else {
        System.out.println();
        for (WebNode child : startNode.children) {
            eularPrintTree(child);
        }
        System.out.println(indent + ")");
    }
}
	
	private String repeat(String str,int repeat){
		String retVal  = "";
		for(int i=0;i<repeat;i++){
			retVal+=str;
		}
		return retVal;
	}
}