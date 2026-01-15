import java.util.ArrayList;
import java.util.List;

public class TACInstruction {
        String result;    // e.g., "t1"
        String operand1;  // e.g., "b"
        String operator;  // e.g., "*"
        String operand2;  // e.g., "c" (can be null for simple assignments)

        public TACInstruction(String result, String operand1, String operator, String operand2) {
            this.result = result;
            this.operand1 = operand1;
            this.operator = operator;
            this.operand2 = operand2;
        }

    @Override
    public String toString() {
        // Format: L1:
        if (operator.equals(":")) return result + ":";

        // Format: goto L1
        if (result.equals("goto")) return "goto " + operand1;

        // Format: if_false t0 goto L1
        if (result.equals("if_false")) return "if_false " + operand1 + " goto " + operand2;

        // Format: param x
        if (result.equals("param")) return "param " + operand1;

        // Format: call func, n
        if (result.equals("call")) return "call " + operand1 + ", " + operand2;

        // Standard: t0 = a + b
        if (operand2 != null) return result + " = " + operand1 + " " + operator + " " + operand2;
        return result + " = " + operand1;
    }

    static class CodeGenerator {
        List<TACInstruction> instructions = new ArrayList<>();
        int tempCounter = 0;
        int labelCounter = 0;

        public List<TACInstruction> getInstructions() {
            return instructions;
        }
        public String newTemp() { return "t" + (tempCounter++); }

        // NEW: Generate Labels (L1, L2...)
        public String newLabel() { return "L" + (labelCounter++); }

        public void emit(String res, String op1, String op, String op2) {
            instructions.add(new TACInstruction(res, op1, op, op2));
        }

        // NEW: Helper methods for Control Flow
        public void emitLabel(String label) { emit(label, "", ":", ""); }
        public void emitJump(String label) { emit("goto", label, "", ""); }
        public void emitIfFalse(String condition, String label) { emit("if_false", condition, "goto", label); }
        public void emitParam(String param) { emit("param", param, "", ""); }
        public void emitCall(String func, String count) { emit("call", func, "", count); }

        public void printCode() {
            System.out.println("\n--- Generated Three-Address Code ---");
            for (TACInstruction i : instructions) System.out.println(i);
        }
    }

}
