package gov.nasa.jpl.view_repo.util;

/**
 * Created by han on 7/12/17.
 */

public class PngBstNode{
    EmsScriptNode data;
    PngBstNode left;
    PngBstNode right;
    public PngBstNode(EmsScriptNode data){
        this.data = data;
        left = null;
        right = null;
    }
}
