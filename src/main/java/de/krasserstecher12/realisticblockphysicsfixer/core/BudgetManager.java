package de.krasserstecher12.realisticblockphysicsfixer.core;

public final class BudgetManager {
    private int remainingGlobalWork;

    public BudgetManager(int globalWorkBudget) {
        this.remainingGlobalWork = Math.max(0, globalWorkBudget);
    }

    public boolean tryConsume(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (remainingGlobalWork < amount) {
            return false;
        }
        remainingGlobalWork -= amount;
        return true;
    }

    public int remainingGlobalWork() {
        return remainingGlobalWork;
    }
}
