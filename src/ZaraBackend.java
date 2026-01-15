import java.util.*;

public class ZaraBackend {
    private List<TACInstruction> instructions;
    private Map<String, Integer> stackMap = new HashMap<>();
    private int stackOffset = -8; // Start below RBP

    public ZaraBackend(List<TACInstruction> instructions) {
        this.instructions = instructions;
    }

    public void generate() {
        System.out.println("\n--- 📟 Generated x86-64 Assembly 📟 ---");

        // 1. Header (Standard Linux/Mac Setup)
        System.out.println(".global main");
        System.out.println(".text");
        System.out.println("main:");

        // 2. Prologue (Setup Stack Frame)
        System.out.println("    pushq %rbp");        // Save old base pointer
        System.out.println("    movq %rsp, %rbp");   // Set new base pointer

        // 3. Map Variables to Memory
        // We scan the code to find all variables (x, y, t0...) and give them space.
        mapVariablesToStack();
        int totalStackSize = Math.abs(stackOffset);
        System.out.println("    subq $" + totalStackSize + ", %rsp"); // Allocate space

        // 4. Translate Instructions
        for (TACInstruction instr : instructions) {
            translate(instr);
        }

        // 5. Epilogue (Clean up and Exit)
        System.out.println("exit_label:");
        System.out.println("    movq %rbp, %rsp");   // Restore stack pointer
        System.out.println("    popq %rbp");         // Restore base pointer
        System.out.println("    ret");               // Return from main
    }

    private void mapVariablesToStack() {
        for (TACInstruction i : instructions) {
            // Check result variable (e.g., "t0" in t0 = a + b)
            if (i.result != null && !isKeyword(i.result) && !stackMap.containsKey(i.result)) {
                stackMap.put(i.result, stackOffset);
                stackOffset -= 8; // Move down 8 bytes (size of 64-bit integer)
            }
            // Check operand 1 (e.g., "x" in t0 = x + 1)
            if (i.operand1 != null && !isNumeric(i.operand1) && !isKeyword(i.operand1) && !stackMap.containsKey(i.operand1)) {
                stackMap.put(i.operand1, stackOffset);
                stackOffset -= 8;
            }
        }
    }

    private void translate(TACInstruction i) {
        // Comment showing the original TAC line
        System.out.println("\n    # " + i.toString());

        // --- CASE 1: LABEL (L0:) ---
        if (i.operator.equals(":")) {
            System.out.println(i.result + ":");
            return;
        }

        // --- CASE 2: UNCONDITIONAL JUMP (goto L0) ---
        if (i.result.equals("goto")) {
            System.out.println("    jmp " + i.operand1);
            return;
        }

        // --- CASE 3: CONDITIONAL JUMP (if_false t0 goto L1) ---
        // In x86, false is 0. So we compare t0 with 0.
        if (i.result.equals("if_false")) {
            String loc = getLoc(i.operand1);
            System.out.println("    cmpq $0, " + loc); // Compare var with 0
            System.out.println("    je " + i.operand2);  // Jump if Equal (Zero)
            return;
        }

        // --- CASE 4: PRINT (Custom implementation) ---
        if (i.result.equals("print")) {
            // For simplicity, we just move value to RDI (argument register)
            // Real compiler would call printf
            System.out.println("    # (Printing not fully implemented in bare asm)");
            System.out.println("    movq " + getLoc(i.operand1) + ", %rdi");
            return;
        }

        // --- CASE 5: ASSIGNMENT / ARITHMETIC (t0 = a + b) ---
        // 1. Load Operand 1 into RAX (Accumulator)
        if (i.operand1 != null) {
            String source = getLoc(i.operand1);
            System.out.println("    movq " + source + ", %rax");
        }

        // 2. Perform Operation with Operand 2
        if (i.operand2 != null) {
            String source2 = getLoc(i.operand2);
            switch (i.operator) {
                case "+": System.out.println("    addq " + source2 + ", %rax"); break;
                case "-": System.out.println("    subq " + source2 + ", %rax"); break;
                case "*": System.out.println("    imulq " + source2 + ", %rax"); break; // Signed multiply
                case "<":
                    // Compare logic: cmp b, a -> setl (set if less) -> move to rax
                    System.out.println("    cmpq " + source2 + ", %rax");
                    System.out.println("    setl %al");         // Set low byte to 1 if Less
                    System.out.println("    movzbq %al, %rax"); // Zero-extend byte to 64-bit
                    break;
            }
        }

        // 3. Store Result (RAX) into Destination
        if (i.result != null) {
            String dest = getLoc(i.result);
            System.out.println("    movq %rax, " + dest);
        }
    }

    // Helper: Get location string. Either "$5" (Literal) or "-8(%rbp)" (Variable)
    private String getLoc(String val) {
        if (isNumeric(val)) return "$" + val;
        if (stackMap.containsKey(val)) return stackMap.get(val) + "(%rbp)";
        return "$" + val; // Fallback for labels or unknown
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+");
    }

    private boolean isKeyword(String str) {
        return str.matches("goto|if_false|param|call|return");
    }
}