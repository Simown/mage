/*
 *  Copyright 2010 BetaSteward_at_googlemail.com. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY BetaSteward_at_googlemail.com ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BetaSteward_at_googlemail.com OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and should not be interpreted as representing official policies, either expressed
 *  or implied, of BetaSteward_at_googlemail.com.
 */
package mage.client.deck.generator;

import mage.Mana;
import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.constants.ColoredManaSymbol;

import java.util.*;

/**
 *
 * @author Simown
 */
public class DeckGeneratorPool
{
    // 40 card deck
    private static final int CREATURE_COUNT_40 = 15;
    private static final int LAND_COUNT_40 = 17;
    private static final int NONCREATURE_COUNT_40 = 8;
    // 60 card deck
    private static final int CREATURE_COUNT_60 = 23;
    private static final int LAND_COUNT_60 = 24;
    private static final int NONCREATURE_COUNT_60 = 13;

    // Count how many copies of the card exists in the deck to check we don't go over 4 copies
    private Map<String, Integer> cardCounts = new HashMap<>();

    private final int creatureCount;
    private final int nonCreatureCount;
    private final int landCount;

    private boolean monoColored = false;

    private final int deckSize;
    private final List<ColoredManaSymbol> allowedColors;
    private final List<DeckGeneratorCMC> poolCMCs;

    private List<Card> deckCards = new ArrayList<>();
    private List<Card> reserveSpells = new ArrayList();
    private Deck deck;

    public DeckGeneratorPool(final int deckSize, final List<ColoredManaSymbol> allowedColors)
    {
        this.deckSize = deckSize;
        this.allowedColors = allowedColors;
        this.deck = new Deck();

        if(this.deckSize > 40) {
//            this.creatureCount = (int)Math.ceil(deckSize * CREATURE_PERCENTAGE_60);
//            this.nonCreatureCount = (int)Math.ceil(deckSize * NONCREATURE_PERCENTAGE_60);
//            this.landCount = (int)Math.ceil(deckSize * LAND_PERCENTAGE_60);
            this.creatureCount = CREATURE_COUNT_60;
            this.nonCreatureCount = NONCREATURE_COUNT_60;
            this.landCount = LAND_COUNT_60;
            // TODO: TESTING OUT SOME DIFFERENT NUMBERS
            poolCMCs = new ArrayList<DeckGeneratorCMC>() {{
                add(new DeckGeneratorCMC(0, 2, 0.20f));
                add(new DeckGeneratorCMC(3, 5, 0.45f));
                add(new DeckGeneratorCMC(6, 7, 0.30f));
                add(new DeckGeneratorCMC(8, 100, 0.5f));
            }};

        }
        else {
//            this.creatureCount = (int)Math.ceil(deckSize * CREATURE_PERCENTAGE_40);
//            this.nonCreatureCount = (int)Math.ceil(deckSize * NONCREATURE_PERCENTAGE_40);
//            this.landCount = (int)Math.ceil(deckSize * niceilLAND_PERCENTAGE_40);
            this.creatureCount = CREATURE_COUNT_40;
            this.nonCreatureCount = NONCREATURE_COUNT_40;
            this.landCount = LAND_COUNT_40;
            // TODO: TESTING OUT SOME DIFFERENT NUMBERS
            poolCMCs = new ArrayList<DeckGeneratorCMC>() {{
                add(new DeckGeneratorCMC(0, 2, 0.30f));
                add(new DeckGeneratorCMC(3, 4, 0.40f));
                add(new DeckGeneratorCMC(5, 6, 0.25f));
                add(new DeckGeneratorCMC(7, 100, 0.5f));
            }};
        }
        if(allowedColors.size() == 1) {
            monoColored = true;
        }
    }

    public boolean isValidSpellCard(Card card)
    {
        Object cC = cardCounts.get((card.getName()));
        if(cC == null)
            cardCounts.put(card.getName(), 0);

        int cardCount = cardCounts.get((card.getName()));

        // Check it hasn't already got 4 copies in the deck
        if(cardCount < 4) {
            if(cardFitsChosenColors(card)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidLandCard(Card card)
    {
        Object cC = cardCounts.get((card.getName()));
        if(cC == null)
            cardCounts.put(card.getName(), 0);

        int cardCount = cardCounts.get((card.getName()));

        if(cardCount < 4) {
            if(cardProducesChosenColors(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean cardFitsChosenColors(Card card) {
        for (String symbol : card.getManaCost().getSymbols()) {
            boolean found = false;
            symbol = symbol.replace("{", "").replace("}", "");
            if (isColoredMana(symbol)) {
                for (ColoredManaSymbol allowed : allowedColors) {
                    if (symbol.contains(allowed.toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<DeckGeneratorCMC> getCMCsForSpellCount(int cardsCount) {
        List<DeckGeneratorCMC> adjustedCMCs = new ArrayList<>(this.poolCMCs);
        // For each CMC calculate how many spell cards are needed, given the total amount of cards
        for(DeckGeneratorCMC deckCMC : adjustedCMCs) {
            deckCMC.setAmount((int)Math.ceil(deckCMC.percentage * cardsCount));
        }
        return adjustedCMCs;
    }

    public void addReserve(Card card) {
        this.reserveSpells.add(card);
    }

    private List<Card> getFixedSpells()
    {
        Random random = new Random();
        int spellSize = deckCards.size();
        // Less spells than needed
        if(spellSize < (deckSize - landCount)) {

            int spellsNeeded = (deckSize-landCount)-spellSize;

            for(int i = 0; i < spellsNeeded; ++i) {
                // Need to ensure there are reserved spells or weirdness ensues
                deckCards.add(reserveSpells.get(random.nextInt(reserveSpells.size())));
            }
        }
        // More spells than needed
        else if(spellSize > (deckSize - landCount)) {

            int spellsRemoved = (spellSize)-(deckSize-landCount);
            for(int i = 0; i < spellsRemoved; ++i) {
                deckCards.remove(random.nextInt(deckCards.size()));
            }
        }
        // Return the fixed amount
        return deckCards;
    }


    public Map<String, Double> calculateSpellColourPercentages() {

        // Double for percentages
        final Map<String, Integer> colorCount = new HashMap<>();
        for (final ColoredManaSymbol color : ColoredManaSymbol.values()) {
            colorCount.put(color.toString(), 0);
        }

        // Counts how many colored mana symbols we've seen in total so we can get the percentage of each color
        int totalCount = 0;

        List<Card> fixedSpells = getFixedSpells();
        for(Card spell: fixedSpells) {
            for (String symbol : spell.getManaCost().getSymbols()) {
            symbol = symbol.replace("{", "").replace("}", "");
                if (isColoredMana(symbol)) {
                    for (ColoredManaSymbol allowed : allowedColors) {
                        if (symbol.contains(allowed.toString())) {
                            int cnt = colorCount.get(allowed.toString());
                            colorCount.put(allowed.toString(), cnt+1);
                            totalCount++;
                        }
                    }
                }
            }
        }
        final Map<String, Double> percentages = new HashMap<>();
        for(Map.Entry<String, Integer> singleCount: colorCount.entrySet()) {
            String color = singleCount.getKey();
            int count = singleCount.getValue();
            // Calculate the percentage this colour has out of the total colour counts
            double percentage = (count / (double) totalCount) * 100;
            percentages.put(color, percentage);
        }
        return percentages;
    }


    public Map<String,Integer> calculateManaCounts(List<Card> deckLands)
    {
        Map<String, Integer> manaCounts = new HashMap<>();
        for (final ColoredManaSymbol color : ColoredManaSymbol.values()) {
            manaCounts.put(color.toString(), 0);
        }
        for(Card land: deckLands)  {
            for(Mana landMana: land.getMana()) {
                for (ColoredManaSymbol color : allowedColors) {
                    int amount = landMana.getColor(color);
                    if (amount > 0) {
                        Integer count = manaCounts.get(color.toString());
                        manaCounts.put(color.toString(), count+amount);
                    }
                }
            }
        }
        return manaCounts;
    }


    private boolean cardProducesChosenColors(Card card) {
        int score = 0;
        for (Mana mana : card.getMana()) {
            for (ColoredManaSymbol color : allowedColors) {
                score = score + mana.getColor(color);
            }
        }
        if (score > 1) {
            return true;
        }
        return false;
    }

    protected static boolean isColoredMana(String symbol) {
        return symbol.equals("W") || symbol.equals("G") || symbol.equals("U") || symbol.equals("B") || symbol.equals("R") || symbol.contains("/");
    }

    public Deck getDeck() {

        Set<Card> actualDeck = deck.getCards();
        for(Card card : deckCards)
            actualDeck.add(card);
        return deck;
    }

    public void addCard(Card card)
    {
        Object cnt = cardCounts.get((card.getName()));
        if(cnt == null)
            cardCounts.put(card.getName(), 0);
        int existingCount = cardCounts.get((card.getName()));
        cardCounts.put(card.getName(), existingCount+1);
        deckCards.add(card);
    }

    public int getCreatureCount() {
        return creatureCount;
    }

    public int getNonCreatureCount() {
        return nonCreatureCount;
    }

    public int getLandCount() {
        return landCount;
    }

    public boolean isMonoColoredDeck() {
        return monoColored;
    }

    public int getDeckSize() {
        return deckSize;
    }
}
