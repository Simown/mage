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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import mage.Mana;
import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.cards.repository.CardCriteria;
import mage.cards.repository.CardInfo;
import mage.cards.repository.CardRepository;
import mage.cards.repository.ExpansionInfo;
import mage.cards.repository.ExpansionRepository;
import mage.client.util.sets.ConstructedFormats;
import mage.constants.CardType;
import mage.constants.ColoredManaSymbol;
import mage.constants.Rarity;
import mage.interfaces.rate.RateCallback;
import mage.utils.DeckBuilder;


/**
 * Generates random card pool and builds a deck.
 *
 * @author nantuko
 * @author Simown
 */
public class DeckGenerator {

    private static final int SPELL_CARD_POOL_SIZE = 180;

    private static final int DECK_LANDS = 17;
    private static final int MAX_NON_BASIC_SOURCE = DECK_LANDS / 2;

    private static final int MAX_TRIES = 4096;
    private static final int ADDITIONAL_CARDS_FOR_3_COLOR_DECKS = 20;
    private static DeckGenerationDialog genDialog;

    public static String generateDeck() {
        genDialog = new DeckGenerationDialog();
        Deck deck = buildDeck();
        String deckPath = genDialog.saveDeck(deck);
        return deckPath;
    }

    /**
     * Generates card pool
     */
    protected static Deck buildDeck() {

        String selectedColors = genDialog.getSelectedColors();
        List<ColoredManaSymbol> allowedColors = new ArrayList<ColoredManaSymbol>();
        selectedColors = selectedColors != null ? selectedColors.toUpperCase() : getRandomColors("X");

        String format = genDialog.getSelectedFormat();
        List<String> setsToUse = ConstructedFormats.getSetsByFormat(format);
        if (setsToUse.isEmpty()) {
            // use all
            setsToUse = ExpansionRepository.instance.getSetCodes();
        }

        int deckSize = genDialog.getDeckSize();
        if (deckSize < 40) {
            deckSize = 40;
        }
        if (selectedColors.contains("X")) {
            selectedColors = getRandomColors(selectedColors);
        }

        for (int i = 0; i < selectedColors.length(); i++) {
            char c = selectedColors.charAt(i);
            allowedColors.add(ColoredManaSymbol.lookup(c));
        }

        int cardPoolSize = SPELL_CARD_POOL_SIZE;
        if (selectedColors.length() > 2) {
            cardPoolSize += ADDITIONAL_CARDS_FOR_3_COLOR_DECKS;
        }
        List<Card> spellCardPool = generateSpellCardPool(cardPoolSize, allowedColors, setsToUse);
        List<Card> landCardPool = generateNonBasicLandCardPool(MAX_NON_BASIC_SOURCE, allowedColors, setsToUse);

        // System.out.println("deck generator card pool: spells=" + spellCardPool.size() + ", lands=" + landCardPool.size());

        final List<String> setsToUseFinal = setsToUse;

        Deck deck = DeckBuilder.buildDeck(spellCardPool, allowedColors, setsToUseFinal, landCardPool, deckSize, new RateCallback() {
            @Override
            public int rateCard(Card card) {
                return 6;
            }

            @Override
            public Card getBestBasicLand(ColoredManaSymbol color, List<String> setsToUse) {
                return DeckGenerator.getBestBasicLand(color, setsToUseFinal);
            }
        });
        return deck;
    }

    private static String getRandomColors(String _selectedColors) {
        Random random = new Random();
        List<Character> availableColors = new ArrayList();
        availableColors.add('R');
        availableColors.add('G');
        availableColors.add('B');
        availableColors.add('U');
        availableColors.add('W');

        StringBuilder generatedColors = new StringBuilder();
        int randomColors = 0;
        for (int i = 0; i < _selectedColors.length(); i++) {
            char currentColor = _selectedColors.charAt(i);
            if (currentColor != 'X') {
                generatedColors.append(currentColor);
                availableColors.remove(new Character(currentColor));
            } else {
                randomColors++;
            }
        }

        for (int i = 0; i < randomColors && !availableColors.isEmpty(); i++) {
            int index = random.nextInt(availableColors.size());
            generatedColors.append(availableColors.remove(index));
        }

        return generatedColors.toString();
    }

    /**
     * Generates card pool of cardsCount cards that have manacost of allowed colors.
     *
     * @param cardsCount
     * @param allowedColors
     * @return
     */
    private static List<Card> generateSpellCardPool(int cardsCount, List<ColoredManaSymbol> allowedColors, List<String> setsToUse) {
        List<Card> spellCardPool = new ArrayList<Card>();

        CardCriteria spellCriteria = new CardCriteria();
        spellCriteria.setCodes(setsToUse.toArray(new String[0]));
        spellCriteria.notTypes(CardType.LAND);

        List<CardInfo> cardPool = CardRepository.instance.findCards(spellCriteria);
        int cardPoolCount = cardPool.size();
        Random random = new Random();
        if (cardPoolCount > 0) {
            int tries = 0;
            int count = 0;
            while (count < cardsCount) {
                Card card = cardPool.get(random.nextInt(cardPoolCount)).getMockCard();
                if (cardFitsChosenColors(card, allowedColors)) {
                    spellCardPool.add(card);
                    count++;
                }
                tries++;
                if (tries > MAX_TRIES) { // to avoid infinite loop
                    throw new IllegalStateException("Not enough cards for chosen colors to generate deck: " + allowedColors);
                }
            }
        } else {
            throw new IllegalStateException("Not enough cards to generate deck.");
        }

        return spellCardPool;
    }

    /**
     * Check that card can be played using chosen (allowed) colors.
     *
     * @param card
     * @param allowedColors
     * @return
     */
    private static boolean cardFitsChosenColors(Card card, List<ColoredManaSymbol> allowedColors) {
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

    /**
     * Generates card pool of land cards that can produce allowed colors.
     *
     * @param landsCount
     * @param allowedColors
     * @return
     */
    private static List<Card> generateNonBasicLandCardPool(int landsCount, List<ColoredManaSymbol> allowedColors, List<String> setsToUse) {
        List<Card> nonBasicLandCardPool = new ArrayList<Card>();

        CardCriteria landCriteria = new CardCriteria();
        landCriteria.setCodes(setsToUse.toArray(new String[0]));
        landCriteria.types(CardType.LAND);
        landCriteria.notSupertypes("Basic");
        List<CardInfo> landCards = CardRepository.instance.findCards(landCriteria);

        int allCount = landCards.size();
        Random random = new Random();
        if (allCount > 0) {
            int tries = 0;
            int count = 0;
            while (count < landsCount) {
                Card card = landCards.get(random.nextInt(allCount)).getMockCard();
                if (cardCardProduceChosenColors(card, allowedColors)) {
                    nonBasicLandCardPool.add(card);
                    count++;
                }
                tries++;
                if (tries > MAX_TRIES) { // to avoid infinite loop
                    // return what have been found
                    return nonBasicLandCardPool;
                }
            }
        }

        return nonBasicLandCardPool;
    }

    /**
     * Checks that chosen card can produce mana of specific color.
     *
     * @param card
     * @param allowedColors
     * @return
     */
    private static boolean cardCardProduceChosenColors(Card card, List<ColoredManaSymbol> allowedColors) {
        int score = 0;
        for (Mana mana : card.getMana()) {
            for (ColoredManaSymbol color : allowedColors) {
                score += mana.getColor(color);
            }
        }
        if (score > 1) {
            return true;
        }
        return false;
    }

    /**
     * Get random basic land that can produce specified color mana.
     * Random here means random set and collector id for the same mana producing land.
     *
     * @param color
     * @return
     */
    private static Card getBestBasicLand(ColoredManaSymbol color, List<String> setsToUse) {
        String cardName = "";
        switch(color) {
            case G:
                cardName = "Forest";
                break;
            case W:
                cardName = "Plains";
                break;
            case R:
                cardName = "Mountain";
                break;
            case B:
                cardName = "Swamp";
                break;
            case U:
                cardName = "Island";
                break;
        }

        List<String> landSets = new LinkedList<String>();

        // decide from which sets basic lands are taken from
        for (String setCode :setsToUse) {
            ExpansionInfo expansionInfo = ExpansionRepository.instance.getSetByCode(setCode);
            if (expansionInfo.hasBasicLands()) {
                landSets.add(expansionInfo.getCode());
            }
        }

        // if sets have no basic land, take land from block
        if (landSets.isEmpty()) {
            for (String setCode :setsToUse) {
                ExpansionInfo expansionInfo = ExpansionRepository.instance.getSetByCode(setCode);
                List<ExpansionInfo> blockSets = ExpansionRepository.instance.getSetsFromBlock(expansionInfo.getBlockName());
                for (ExpansionInfo blockSet: blockSets) {
                    if (blockSet.hasBasicLands()) {
                        landSets.add(blockSet.getCode());
                    }
                }
            }
        }
        // if still no set with lands found, take one by random
        if (landSets.isEmpty()) {
            // if sets have no basic lands and also it has no parent or parent has no lands get last set with lands
            // select a set with basic lands by random
            Random generator = new Random();
            List<ExpansionInfo> basicLandSets = ExpansionRepository.instance.getSetsWithBasicLandsByReleaseDate();
            if (basicLandSets.size() > 0) {
                landSets.add(basicLandSets.get(generator.nextInt(basicLandSets.size())).getCode());
            }
        }

        if (landSets.isEmpty()) {
            throw new IllegalArgumentException("No set with basic land was found");
        }

        CardCriteria criteria = new CardCriteria();
        if (!landSets.isEmpty()) {
            criteria.setCodes(landSets.toArray(new String[landSets.size()]));
        }
        criteria.rarities(Rarity.LAND).name(cardName);
        List<CardInfo> cards = CardRepository.instance.findCards(criteria);

        if (cards.isEmpty() && !setsToUse.isEmpty()) {
            cards = CardRepository.instance.findCards(cardName);
        }

        int randomInt = new Random().nextInt(cards.size());
        return cards.get(randomInt).getMockCard();

    }

    protected static boolean isColoredMana(String symbol) {
        return symbol.equals("W") || symbol.equals("G") || symbol.equals("U") || symbol.equals("B") || symbol.equals("R") || symbol.contains("/");
    }
}
