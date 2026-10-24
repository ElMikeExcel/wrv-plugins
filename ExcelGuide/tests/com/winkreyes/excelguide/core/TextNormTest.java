package com.winkreyes.excelguide.core;

import java.util.Set;

/** Normalization, token, question-shape, and entry-match rules. */
public final class TextNormTest {

    public static void main(String[] args) {
        TestSupport.T t = new TestSupport.T();

        t.eq("apostrophes drop", "cant find my wand", TextNorm.normalize("Can't find my WAND!!"));
        t.eq("punctuation to spaces", "what is my level", TextNorm.normalize("What... is my level?"));
        t.eq("bedrock caps", "i lost my wand", TextNorm.normalize("I LOST MY WAND"));
        t.eq("empty", "", TextNorm.normalize("  "));
        t.eq("null safe", "", TextNorm.normalize(null));
        t.eq("numbers kept", "level 50 mining", TextNorm.normalize("level 50 mining"));

        t.eq("token set", Set.of("how", "many", "seals"), TextNorm.tokenSet("how many seals"));
        t.check("query terms drop stopwords",
                TextNorm.queryTerms("how do i claim the land").equals(Set.of("claim", "land")));
        t.check("query terms drop 1-char", !TextNorm.queryTerms("a b claim").contains("b"));

        t.check("question mark shapes", TextNorm.questionShaped("really?", "really"));
        t.check("opener shapes", TextNorm.questionShaped("How do I claim", "how do i claim"));
        t.check("statement does not shape", !TextNorm.questionShaped("i lost my wand", "i lost my wand"));
        t.check("declarative no shape", !TextNorm.questionShaped("the dragon is dead", "the dragon is dead"));

        Set<String> toks = TextNorm.tokenSet("i lost my wand today");
        t.check("single-word entry", TextNorm.entryMatches("wand", "i lost my wand today", toks));
        t.check("phrase entry", TextNorm.entryMatches("lost my wand", "i lost my wand today", toks));
        t.check("phrase needs padding match", !TextNorm.entryMatches("my wand", "i lost my wander", TextNorm.tokenSet("i lost my wander")));
        t.check("word matches up to stemming", TextNorm.entryMatches("wand", "wands are cool", TextNorm.tokenSet("wands are cool")));
        t.check("stemming is one-directional", !TextNorm.entryMatches("jobs", "my job", TextNorm.tokenSet("my job")));
        t.check("stem is not a free substring", !TextNorm.entryMatches("wand", "wander is cool", TextNorm.tokenSet("wander is cool")));
        t.eq("stem parties", "party", TextNorm.stem("parties"));
        t.eq("stem shops", "shop", TextNorm.stem("shops"));
        t.eq("stem keeps ss", "class", TextNorm.stem("class"));

        t.finish("TextNormTest");
    }
}
