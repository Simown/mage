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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Simown
 */
public class DeckGeneratorPool
{

    private static class PoolCMC
    {
        protected int min;
        protected int max;
        protected int amount;

        PoolCMC(int min, int max, int amount)
        {
            this.min = min;
            this.max = max;
            this.amount = amount;
        }
    }

    // 40 card deck
    private static final float CREATURE_PERCENTAGE_40 = 0.33f;
    private static final float LAND_PERCENTAGE_40 = 0.33f;
    private static final float NONCREATURE_PERCENTAGE_40 = 0.33f;
    // 60 card deck
    private static final float CREATURE_PERCENTAGE_60 = 0.33f;
    private static final float LAND_PERCENTAGE_60 = 0.33f;
    private static final float NONCREATURE_PERCENTAGE_60 = 0.33f;

    // Count how many copies of the card exists in the deck to check we don't go over 4 copies
    private Map<String, Integer> cardCounts = new HashMap<>();

    private final int creatureCount;
    private final int nonCreatureCount;
    private final int landCount;

    private boolean monoColored = false;

    private final int deckSize;
    private final List<ColoredManaSymbol> allowedColors;
    private final List<PoolCMC> poolCMCs;
    private List<Card> deckCards;

    public DeckGeneratorPool(final int deckSize, final List<ColoredManaSymbol> allowedColors)
    {
        this.deckSize = deckSize;
        this.allowedColors = allowedColors;

        if(this.deckSize > 40) {
            this.creatureCount = (int)Math.ceil(deckSize * CREATURE_PERCENTAGE_60);
            this.nonCreatureCount = (int)Math.ceil(deckSize * NONCREATURE_PERCENTAGE_60);
            this.landCount = (int)Math.ceil(deckSize * LAND_PERCENTAGE_60);
            // TODO: TESTING OUT SOME DIFFERENT NUMBERS
            final int nonLands = deckSize - landCount;
            poolCMCs = new ArrayList<PoolCMC>() {{
                add(new PoolCMC(0, 2, (int)Math.ceil(nonLands*0.20)));
                add(new PoolCMC(3, 4, (int)Math.ceil(nonLands*0.40)));
                add(new PoolCMC(5, 6, (int)Math.ceil(nonLands*0.30)));
                add(new PoolCMC(7, 100, (int)Math.ceil(nonLands*0.10)));
            }};

        }
        else {
            this.creatureCount = (int)Math.ceil(deckSize * CREATURE_PERCENTAGE_40);
            this.nonCreatureCount = (int)Math.ceil(deckSize * NONCREATURE_PERCENTAGE_40);
            this.landCount = (int)Math.ceil(deckSize * LAND_PERCENTAGE_40);

            // TODO: TESTING OUT SOME DIFFERENT NUMBERS
            final int nonLands = deckSize - landCount;
            poolCMCs = new ArrayList<PoolCMC>() {{
                add(new PoolCMC(0, 2, (int)Math.ceil(nonLands*0.20)));
                add(new PoolCMC(3, 5, (int)Math.ceil(nonLands*0.50)));
                add(new PoolCMC(6, 7, (int)Math.ceil(nonLands*0.20)));
                add(new PoolCMC(7, 100, (int)Math.ceil(nonLands*0.10)));
            }};
        }
        if(allowedColors.size() == 1) {
            monoColored = true;
        }
    }

    public boolean isValidSpellCard(Card card)
    {
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

    private boolean cardProducesChosenColors(Card card) {
        boolean valid = false;
        for (Mana mana : card.getMana()) {
            for (ColoredManaSymbol color : allowedColors) {
                valid |= (mana.getColor(color) > 0);
            }
        }
        return valid;
    }

    protected static boolean isColoredMana(String symbol) {
        return symbol.equals("W") || symbol.equals("G") || symbol.equals("U") || symbol.equals("B") || symbol.equals("R") || symbol.contains("/");
    }

    public void addBasicLands(int numberLands) {
        // Get basic lands and add em
    }

    public Deck getDeck() {
        // Reconstruct the pool and return the deck
        return null;
    }

    public boolean addCard(Card card)
    {
        // Get the CMC of this card to add
        int cardCMC = card.getManaCost().convertedManaCost();
        // Check
        for(PoolCMC poolCMC: poolCMCs)
        {
            if(cardCMC >= poolCMC.min && cardCMC <= poolCMC.max) {
                if(poolCMC.amount != 0) {
                    int existingCount = cardCounts.get((card.getName()));
                    cardCounts.put(card.getName(), existingCount+1);
                    poolCMC.amount -= 1;
                    return true;
                }
            }
        }
        // Failed to add it to any pool
        return false;
    }

    public boolean hasCompleteSpells()
    {
        for(PoolCMC poolCMC: poolCMCs)
        {
            if(poolCMC.amount > 0)
                return false;
        }
        return true;
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
}
