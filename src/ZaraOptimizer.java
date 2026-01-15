import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZaraOptimizer {
    private List<TACInstruction> instructions;

    public ZaraOptimizer(List<TACInstruction> instructions) {
        this.instructions = instructions;
    }

    public void optimize() {
        System.out.println("\n--- ⚡ Running Optimizations ⚡ ---");
        boolean changed;
        // Loop passes 1 & 2 until code stabilizes
        do {
            changed = false;
            if (constantFolding()) changed = true;
            if (deadCodeElimination()) changed = true;
        } while (changed);

        loopInvariantCodeMotion();
    }

    // Pass 1: Constant Folding
    private boolean constantFolding() {
        boolean changed = false;
        for (TACInstruction instr : instructions) {
            if (instr.operand2 != null && isNumeric(instr.operand1) && isNumeric(instr.operand2)) {
                try {
                    double v1 = Double.parseDouble(instr.operand1);
                    double v2 = Double.parseDouble(instr.operand2);
                    double result = 0;

                    switch (instr.operator) {
                        case "+": result = v1 + v2; break;
                        case "-": result = v1 - v2; break;
                        case "*": result = v1 * v2; break;
                        case "/": if(v2!=0) result = v1/v2; else return false; break;
                        default: return false;
                    }

                    // Convert to integer string if whole number
                    String valStr = (result % 1 == 0) ? String.valueOf((int)result) : String.valueOf(result);
                    System.out.println("   [Constant Folding] " + instr + "  ->  " + instr.result + " = " + valStr);

                    instr.operator = "";
                    instr.operand1 = valStr;
                    instr.operand2 = null;
                    changed = true;
                } catch (Exception e) {}
            }
        }
        return changed;
    }

    // --- Pass 2: Dead Code Elimination (Aggressive) ---
    private boolean deadCodeElimination() {
        // 1. Collect all variables that are USED
        List<String> usedVars = new ArrayList<>();
        for (TACInstruction instr : instructions) {
            if (instr.operand1 != null) usedVars.add(instr.operand1);
            if (instr.operand2 != null) usedVars.add(instr.operand2);
            if (instr.result != null && (instr.result.equals("if_false") || instr.result.equals("param") || instr.result.equals("call"))) {
                usedVars.add(instr.operand1);
            }
        }

        boolean changed = false;
        Iterator<TACInstruction> iter = instructions.iterator();
        while (iter.hasNext()) {
            TACInstruction instr = iter.next();

            // CHECK: Is the result variable EVER used?
            // (We removed the 'startsWith("t")' check so it cleans EVERYTHING)
            if (instr.result != null && !usedVars.contains(instr.result)) {

                // SAFETY: Don't delete keywords/instructions that don't produce values
                boolean isKeyword = instr.result.equals("goto") ||
                        instr.result.equals("if_false") ||
                        instr.result.equals("call") ||
                        instr.result.equals("param") ||
                        instr.operator.equals(":"); // Don't delete Labels!

                if (!isKeyword) {
                    System.out.println("   [Dead Code] Removed unused: " + instr);
                    iter.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    // --- Pass 3: Loop Invariant Code Motion (Improved) ---
    private void loopInvariantCodeMotion() {
        for (int i = 0; i < instructions.size(); i++) {
            TACInstruction instr = instructions.get(i);

            // 1. Detect end of a loop
            if (instr.result != null && instr.result.equals("goto")) {
                String labelName = instr.operand1;
                int labelIndex = findLabelIndex(labelName);

                if (labelIndex != -1 && labelIndex < i) {
                    // 2. Scan the loop body
                    for (int k = labelIndex + 1; k < i; k++) {
                        TACInstruction candidate = instructions.get(k);

                        // CHECK 1: Is it a Math Operation? (t1 = 5 * 20)
                        boolean isMath = candidate.operand2 != null &&
                                isNumeric(candidate.operand1) &&
                                isNumeric(candidate.operand2);

                        // CHECK 2: Is it a Simple Constant Assignment? (t1 = 100)
                        // (This catches cases where Constant Folding already ran!)
                        boolean isConst = candidate.operand2 == null &&
                                candidate.operator.equals("") &&
                                isNumeric(candidate.operand1);

                        if (isMath || isConst) {
                            System.out.println("   [Code Motion] Moving out of loop: " + candidate);
                            instructions.remove(k);
                            instructions.add(labelIndex, candidate); // Move BEFORE label

                            // Adjust indices
                            labelIndex++;
                            i++;
                        }
                    }
                }
            }
        }
    }

    private int findLabelIndex(String labelName) {
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i).operator.equals(":") && instructions.get(i).result.equals(labelName)) return i;
        }
        return -1;
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }
}