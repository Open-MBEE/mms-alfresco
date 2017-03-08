package gov.nasa.jpl.view_repo.db;

public class NodeType {

    private Integer id;
    private String name;
    
    public NodeType(){
        
    }
    
    public NodeType(Integer id, String name) {
        super();
        this.id = id;
        this.name = name;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

}
