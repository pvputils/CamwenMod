package com.example;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
public final class FocusChatTestTodoAi {
    public static void main(String[] args) {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        Map<String, UUID> online = Map.of("alice", alice, "bob_2", bob);
        check(FocusChatTodoAi.matchingPlayers("Members: ALICE, [Bob_2] Alice.", online).equals(Set.of(alice, bob)), "punctuation, case and duplicate names");
        check(FocusChatTodoAi.matchingPlayers("Malice AliceExtra offline", online).isEmpty(), "whole words and online membership");
        check(FocusChatTodoAi.matchingPlayers("", online).isEmpty(), "empty reply");
        check(FocusChatTodoAi.matchingPlayers("abcdefghijklmnopq", Map.of("abcdefghijklmnopq", alice)).isEmpty(), "invalid username length");
        check(com.example.Configs.Config.NameplateTeam.ALLY.isFriendly && com.example.Configs.Config.NameplateTeam.FRIENDLY.isFriendly && !com.example.Configs.Config.NameplateTeam.FOCUS.isFriendly, "FOCUS is not friendly");
        System.out.println("FOCUS response username parsing and team checks passed.");
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
