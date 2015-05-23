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
package mage.sets.timespiral;

import java.util.UUID;

import mage.constants.CardType;
import mage.constants.Rarity;
import mage.abilities.Ability;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.StormAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.Outcome;
import mage.game.Game;
import mage.players.Player;
import mage.target.TargetPlayer;

/**
 *
 * @author Plopman
 */
public class IgniteMemories extends CardImpl {

    public IgniteMemories(UUID ownerId) {
        super(ownerId, 164, "Ignite Memories", Rarity.UNCOMMON, new CardType[]{CardType.SORCERY}, "{4}{R}");
        this.expansionSetCode = "TSP";


        // Target player reveals a card at random from his or her hand. Ignite Memories deals damage to that player equal to that card's converted mana cost.
        this.getSpellAbility().addTarget(new TargetPlayer());
        this.getSpellAbility().addEffect(new IgniteMemoriesEffect());
        // Storm
        this.addAbility(new StormAbility());
    }

    public IgniteMemories(final IgniteMemories card) {
        super(card);
    }

    @Override
    public IgniteMemories copy() {
        return new IgniteMemories(this);
    }
}


class IgniteMemoriesEffect extends OneShotEffect {

    public IgniteMemoriesEffect() {
        super(Outcome.Damage);
        staticText = "Target player reveals a card at random from his or her hand. Ignite Memories deals damage to that player equal to that card's converted mana cost";
    }

    public IgniteMemoriesEffect(final IgniteMemoriesEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Player player = game.getPlayer(targetPointer.getFirst(game, source));
        if (player != null && player.getHand().size() > 0) {
            Cards revealed = new CardsImpl();
            Card card = player.getHand().getRandom(game);
            revealed.add(card);
            player.revealCards("Ignite Memories", revealed, game);
            player.damage(card.getManaCost().convertedManaCost(), id, game, false, true);
            return true;
        }
        return false;
    }

    @Override
    public IgniteMemoriesEffect copy() {
        return new IgniteMemoriesEffect(this);
    }

}