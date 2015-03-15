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

    private static DeckGeneratorDialog genDialog;
    private static DeckGeneratorPool genPool;

    public static String generateDeck() {
        genDialog = new DeckGeneratorDialog();
        Deck deck = buildDeck();
        String deckPath = genDialog.saveDeck(deck);
        return deckPath;
    }

    protected static Deck buildDeck() {

        String selectedColors = genDialog.getSelectedColors();
        List<ColoredManaSymbol> allowedColors = new ArrayList<ColoredManaSymbol>();
        selectedColors = selectedColors != null ? selectedColors.toUpperCase() : getRandomColors("X");
        String format = genDialog.getSelectedFormat();

        List<String> setsToUse = ConstructedFormats.getSetsByFormat(format);
        if (setsToUse.isEmpty()) {
            // Default to using all sets
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

        genPool = new DeckGeneratorPool(deckSize, allowedColors);
        generateCards(allowedColors, setsToUse);

        return new Deck();
    }

    private static void generateCards(List<ColoredManaSymbol> allowedColors, List<String> setsToUse)
    {
        final String [] sets = setsToUse.toArray(new String[0]);

        // Creatures
        final  CardCriteria creatureCriteria = new CardCriteria();
        creatureCriteria.setCodes(sets);
        creatureCriteria.notTypes(CardType.LAND);
        creatureCriteria.types(CardType.CREATURE);

        // Non-creatures (sorcery, instant, enchantment, artifact etc.)
        final CardCriteria nonCreatureCriteria = new CardCriteria();
        nonCreatureCriteria.setCodes(sets);
        nonCreatureCriteria.notTypes(CardType.LAND);
        nonCreatureCriteria.notTypes(CardType.CREATURE);

        // Non-basic land
        final CardCriteria nonBasicLandCriteria = new CardCriteria();
        nonBasicLandCriteria.types(CardType.LAND);
        nonBasicLandCriteria.notSupertypes("Basic");

        generateSpells(creatureCriteria, genPool.getCreatureCount());
        generateSpells(nonCreatureCriteria, genPool.getNonCreatureCount());
        generateLands(nonBasicLandCriteria, genPool.getNonBasicLandCount());
    }

    private static void generateSpells(CardCriteria criteria, int cardsCount)
    {
        List<CardInfo> cardPool = CardRepository.instance.findCards(criteria);
        int retrievedCount = cardPool.size();

        Random random = new Random();
        if (retrievedCount > 0) {
            int tries = 0;
            int count = 0;
            while (count < cardsCount) {
                Card card = cardPool.get(random.nextInt(retrievedCount)).getMockCard();
                if (genPool.isValidSpellCard(card)) {
                    genPool.addCard(card);
                    count++;
                }
                tries++;
                if (tries > MAX_TRIES) { // to avoid infinite loop
                    throw new IllegalStateException("Not enough cards for chosen colors to generate deck for the chosen colors.");
                }
            }
        } else {
            throw new IllegalStateException("Not enough cards to generate deck.");
        }

    }

    private static void generateLands(CardCriteria criteria, int landsCount) {

        List<CardInfo> landCards = CardRepository.instance.findCards(criteria);

        int allCount = landCards.size();
        Random random = new Random();
        if (allCount > 0) {
            int tries = 0;
            int count = 0;
            while (count < landsCount) {
                Card card = landCards.get(random.nextInt(allCount)).getMockCard();
                if (genPool.isValidLandCard(card)) {
                    genPool.addCard(card);
                    count++;
                }
                tries++;
                // to avoid infinite loop
                if (tries > MAX_TRIES) {
                    // Not a problem, just use what we have
                    break;
                }
            }
        }
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

}
