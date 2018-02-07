package gov.nasa.jpl.view_repo.db;

public class EdgeTypes {
 
    private Integer id;
    private String name;
 
    public EdgeTypes() {
    }
 
    public EdgeTypes(String name){
        this.name = name;
    }
    public EdgeTypes(Integer id, String name) {
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
 
    @Override
    public String toString() {
        return "EdgeType [id=" + id + ", name=" + name + "]";
    }
 
}