import java.util.HashMap;
import java.util.Map;


public class symbol{
    private String name;
    private String type;

    public symbol(String name, String type){
        this.name=name;
        this.type=type;
    }

    public String getName(){return name;}
    public String getType(){return type;}

    public void setType(String newType){
        this.type= newType;
    }

    @Override
    public String toString(){
        return "Symbol{name='" + name + "', type='" + type + "'}";
    }
}
class symboltable{
    private Map<String,symbol> symbols;

    public symboltable(){
        this.symbols= new HashMap<>();
        System.out.println("Symbol Table Initialized");
    }

    public void add(String name, String type){
        if( symbols.containsKey(name)){
            System.out.println("Warning : symbol"+name+"already exists");
        };
        symbol newSym = new symbol(name, type);
        symbols.put(name,newSym);

        System.out.println("Added Symbol: Name='" + name + "', Type='" + type + "'");

    }

    public void update(String name, String newtype){
        if( !symbols.containsKey(name)){
            System.out.println("Warning : symbol"+name+"not found, cannot update.");
            return;
        };

        symbol sym = symbols.get(name);
        sym.setType(newtype);

        System.out.println("Updated Symbol: Name='" + name + "', New Type='" + newtype + "'");
    }

    public symbol get(String name){
        if( !symbols.containsKey(name)){
            System.out.println("Warning : symbol"+name+"not found");
            return null;
        };
        return symbols.get(name);
    }


}

class Main {
    public static void main(String[] args) {
        System.out.println("--- Simulating Symbol  ---");

        // 1. Initialize the Symbol Table
        symboltable symbolTable = new symboltable();

        System.out.println("\nProcessing declarations:");

        // 2. Add variables (simulating the Zara program)
        // integer count
        symbolTable.add("count", "integer");

        // float price
        symbolTable.add("price", "float");

        // string message
        symbolTable.add("message", "string");

        // array numbers[10]
        symbolTable.add("numbers", "array");

        // stack operations
        symbolTable.add("operations", "stack");

        // subprogram calculate_total
        symbolTable.add("calculate_total", "subprogram");

        // 3. Test Retrieval
        System.out.println("\n--- Testing Retrieval ---");
        symbol retrieved = symbolTable.get("price");
        if (retrieved != null) {
            System.out.println("Retrieving symbol 'price': " + retrieved.toString());
        }

        // 4. Test Error Handling
        System.out.println("\n--- Testing Non-Existent Symbol ---");
        symbolTable.get("non_existent_var");
    }
}
