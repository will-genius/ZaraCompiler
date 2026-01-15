import java.util.HashMap;
import java.util.Map;

public class OOPSymbolTable {

    // ==========================================
    // 1. NESTED CLASSES (All inside this file)
    // ==========================================

    public static class Symbol {
        public String name;
        public String type;
        public String scope; // "global", "class", "local"

        public Symbol(String name, String type, String scope) {
            this.name = name;
            this.type = type;
            this.scope = scope;
        }
    }

    public static class ClassSymbol extends Symbol {
        public Map<String, Symbol> members = new HashMap<>();
        public String parentClass;

        public ClassSymbol(String name, String parentClass) {
            super(name, "class", "global");
            this.parentClass = parentClass;
        }
    }

    public static class MethodSymbol extends Symbol {
        public Map<String, Symbol> locals = new HashMap<>();
        public String returnType;

        public MethodSymbol(String name, String returnType) {
            super(name, "method", "class");
            this.returnType = returnType;
        }
    }

    // ==========================================
    // 2. MAIN TABLE LOGIC
    // ==========================================

    public Map<String, ClassSymbol> classes = new HashMap<>();
    public ClassSymbol currentClass;
    public MethodSymbol currentMethod;

    public void defineClass(String name, String parent) {
        if (classes.containsKey(name)) {
            System.out.println(" Error: Class '" + name + "' already defined.");
            return;
        }
        ClassSymbol newClass = new ClassSymbol(name, parent);
        classes.put(name, newClass);
        currentClass = newClass;
        currentMethod = null;
        System.out.println("Define Class: " + name + (parent != null ? " extends " + parent : ""));
    }

    public void defineField(String name, String type) {
        if (currentClass == null) {
            System.out.println("Error: Field '" + name + "' defined outside class.");
            return;
        }
        Symbol field = new Symbol(name, type, "field");
        currentClass.members.put(name, field);
        System.out.println("  Define Field: " + name + " (" + type + ")");
    }

    public void defineMethod(String name, String returnType) {
        if (currentClass == null) {
            System.out.println(" Error: Method '" + name + "' defined outside class.");
            return;
        }
        MethodSymbol method = new MethodSymbol(name, returnType);
        currentClass.members.put(name, method);
        currentMethod = method;
        System.out.println("  Define Method: " + name + " -> " + returnType);
    }

    public void defineLocal(String name, String type) {
        if (currentMethod == null) {
            System.out.println("Error: Local variable '" + name + "' defined outside method.");
            return;
        }
        Symbol local = new Symbol(name, type, "local");
        currentMethod.locals.put(name, local);
        System.out.println("    Define Local: " + name + " (" + type + ")");
    }

    public Symbol resolve(String name) {
        // 1. Local
        if (currentMethod != null && currentMethod.locals.containsKey(name)) {
            return currentMethod.locals.get(name);
        }
        // 2. Class Field
        if (currentClass != null) {
            return resolveMember(currentClass, name);
        }
        return null;
    }

    private Symbol resolveMember(ClassSymbol cls, String name) {
        if (cls.members.containsKey(name)) return cls.members.get(name);
        if (cls.parentClass != null && classes.containsKey(cls.parentClass)) {
            return resolveMember(classes.get(cls.parentClass), name);
        }
        return null;
    }
}