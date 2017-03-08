package gov.nasa.jpl.view_repo.webscripts;

public class TableCell{
    private int row;
    private int column;
    
    public TableCell(int row, int column){
        this.row = row;
        this.column = column;
    }
    
    public int getRow(){
        return row;
    }
    public void setRow(int row){
        this.row = row;
    }
    
    public int getColumn(){
        return this.column;
    }
    public void setColumn(int column){
        this.column = column;
    }
}
