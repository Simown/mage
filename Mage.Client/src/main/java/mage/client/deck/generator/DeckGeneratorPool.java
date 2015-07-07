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

import mage.abilities.Ability;
import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.cards.repository.CardInfo;
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

    // Count how many copies of the card exists in the deck to check we don't go over 4 copies (or 1 for singleton)
    private Map<String, Integer> cardCounts = new HashMap<>();

    private final int creatureCount;
    private final int nonCreatureCount;
    private final int landCount;
    private final boolean isSingleton;

    // If there is only a single colour selected to generate a deck
    private boolean monoColored = false;

    private final int deckSize;
    private final List<ColoredManaSymbol> allowedColors;
    private final List<DeckGeneratorCMC> poolCMCs;

    private List<Card> deckCards = new ArrayList<>();
    private List<Card> reserveSpells = new ArrayList();
    private Deck deck;

    public DeckGeneratorPool(final int deckSize, final List<ColoredManaSymbol> allowedColors, boolean isSingleton)
    {
        this.deckSize = deckSize;
        this.allowedColors = allowedColors;
        this.isSingleton = isSingleton;

        this.deck = new Deck();

        if(this.deckSize > 40) {
            this.creatureCount = CREATURE_COUNT_60;
            this.nonCreatureCount = NONCREATURE_COUNT_60;
            this.landCount = LAND_COUNT_60;
            poolCMCs = new ArrayList<DeckGeneratorCMC>() {{
                add(new DeckGeneratorCMC(0, 2, 0.20f));
                add(new DeckGeneratorCMC(3, 5, 0.50f));
                add(new DeckGeneratorCMC(6, 7, 0.25f));
                add(new DeckGeneratorCMC(8, 100, 0.5f));
            }};

        }
        else {
            this.creatureCount = CREATURE_COUNT_40;
            this.nonCreatureCount = NONCREATURE_COUNT_40;
            this.landCount = LAND_COUNT_40;
            poolCMCs = new ArrayList<DeckGeneratorCMC>() {{
                add(new DeckGeneratorCMC(0, 2, 0.30f));
                add(new DeckGeneratorCMC(3, 4, 0.45f));
                add(new DeckGeneratorCMC(5, 6, 0.20f));
                add(new DeckGeneratorCMC(7, 100, 0.5f));
            }};
        }

        if(allowedColors.size() == 1) {
            monoColored = true;
        }

    }

    public boolean isValidSpellCard(Card card)
    {
        int cardCount = getCardCount((card.getName()));
        // Check it hasn't already got the maximum number of copies in a deck
        if(cardCount < (isSingleton ? 1 : 4)) {
            if(cardFitsChosenColors(card)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidLandCard(Card card)
    {
        int cardCount = getCardCount((card.getName()));
        // No need to check if the land is valid for the colors chosen
        // They are all filtered before searching for lands to include
        // in the deck.
        if(cardCount < 4) {
            return true;
        }
        return false;
    }

    private boolean cardFitsChosenColors(Card card) {
        for (String symbol : card.getManaCost().getSymbols()) {
            boolean found = false;
            symbol = symbol.replace("{", "").replace("}", "");
            if (isColoredManaSymbol(symbol)) {
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

    public void tryAddReserve(Card card, int cardCMC) {
        // Only cards with CMC < 7 and don't already exist in the deck
        // can be added to our reserve pool as not to overwhelm the curve
        // with high CMC cards and duplicates.
        if(cardCMC < 7 && getCardCount(card.getName()) == 0) {
            this.reserveSpells.add(card);
        }
    }

    private int getCardCount(String cardName) {
        Object cC = cardCounts.get((cardName));
        if(cC == null)
            cardCounts.put(cardName, 0);
        return  cardCounts.get((cardName));
    }

    private List<Card> getFixedSpells()
    {
        Random random = new Random();
        int spellSize = deckCards.size();
        int nonLandSize = (deckSize - landCount);

        // Less spells than needed
        if(spellSize < nonLandSize) {

            int spellsNeeded = nonLandSize-spellSize;
            List<Card> spellsToAdd = new ArrayList<>(spellsNeeded);

            // Initial reservoir
            for(int i = 0; i < spellsNeeded-1; i++)
                spellsToAdd.add(reserveSpells.get(i));

            for(int j = spellsNeeded+1; j < reserveSpells.size()-1; j++) {
                int index = random.nextInt(j);
                Card randomCard = reserveSpells.get(index);
                if (index < j && isValidSpellCard(randomCard)) {
                    spellsToAdd.set(j, randomCard);
                }
            }
            // Add randomly selected spells needed
            deckCards.addAll(spellsToAdd);
        }

        // More spells than needed
        else if(spellSize > (deckSize - landCount)) {

            int spellsRemoved = (spellSize)-(deckSize-landCount);
            for(int i = 0; i < spellsRemoved; ++i) {
                deckCards.remove(random.nextInt(deckCards.size()));
            }
        }
        if(deckCards.size() != nonLandSize)
            throw new IllegalStateException("Not enough cards found to generate deck. Please try again");

        // Return the fixed amount
        return deckCards;
    }


    public Map<String, Double> calculateSpellColourPercentages() {

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
                if (isColoredManaSymbol(symbol)) {
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
            for(Ability landAbility: land.getAbilities()) {
                for (ColoredManaSymbol color : allowedColors) {
                    String abilityString = landAbility.getRule();
                    // FIXME: Only count mana generation of taplands now, need a way of including other mana sources.
                    if(abilityString.matches(".*Add \\{" + color.toString() + "\\} to your mana pool.")) {
                        Integer count = manaCounts.get(color.toString());
                        manaCounts.put(color.toString(), count + 1);
                    }
                }
            }
        }
        return manaCounts;
    }


    public List<Card> filterLands(List<CardInfo> landCardsInfo) {
        List<Card> matchingLandList = new ArrayList<>();
        for(CardInfo landCardInfo: landCardsInfo) {
            Card landCard = landCardInfo.getMockCard();
            if(cardProducesChosenColors(landCard)) {
                matchingLandList.add(landCard);
            }
        }
        return matchingLandList;
    }


    // FIXME: For 3 color decks, prefer tri-lands that produce exactly the colors chosen.
    private boolean cardProducesChosenColors(Card card) {
        // All mock card abilities will be MockAbilities so we can't differentiate between ManaAbilities
        // and other Abilities so we have to do some basic string matching on land cards for now.
        List<Ability> landAbilities = card.getAbilities();
        int count = 0;
        for(Ability ability : landAbilities) {

            String abilityString = ability.getRule();

            // Lands that tap to produce mana of the chosen type
            for(ColoredManaSymbol symbol : allowedColors) {
                if(abilityString.matches(".*Add \\{" + symbol.toString() + "\\} to your mana pool."))
                    count++;
            }
            // If the land taps to produce 2 or more of the chosen colors
            if(count > 1) {
                return true;
            }

            // Fetchlands of the chosen colors
            // FIXME: Only add fetchlands when there is basic lands to fetch (usually fine)
            if(abilityString.matches(makeFetchLandAbilityRegex())) {
                return true;
            }

            // TODO: Possibly include generic fetchlands,
        }
        return false;

    }

    // ... Search your library for <land card (A|B|C)> or <land card (A|B|C)> ...
    private String makeFetchLandAbilityRegex() {
        StringBuilder sb = new StringBuilder();
        sb.append(".*Search your library for.*");

        // Generate color matching group
        StringBuilder sb2 = new StringBuilder();
        sb2.append("(");
        for(ColoredManaSymbol color : allowedColors) {
            sb2.append(getBasicLandName(color.toString()));
            if(!color.equals(allowedColors.get(allowedColors.size() - 1))) {
                sb2.append("|");
            }
        }
        sb2.append(")");

        sb.append(sb2);
        sb.append(".*or.*");
        sb.append(sb2);
        sb.append(".*");
        return sb.toString();
    }

    private static boolean isColoredManaSymbol(String symbol) {
        // Hybrid mana
        if(symbol.contains("/")) {
            return true;
        }
        for(ColoredManaSymbol c: ColoredManaSymbol.values()) {
            if (symbol.charAt(0) == (c.toString().charAt(0))) {
                return true;
            }
        }
        return false;
    }

    public static String getBasicLandName(String symbolString) {
        switch(symbolString) {
            case "B":
                return "Swamp";
            case "G":
                return "Forest";
            case "R":
                return "Mountain";
            case "U":
                return "Island";
            case "W":
                return "Plains";
            default:
                return "";
        }
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
