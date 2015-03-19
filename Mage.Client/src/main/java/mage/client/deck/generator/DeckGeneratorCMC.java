package mage.client.deck.generator;


public class DeckGeneratorCMC
{
        public final int min;
        public final int max;
        public final float percentage;
        private int amount = 0;

        DeckGeneratorCMC(int min, int max, float percentage)
        {
            this.min = min;
            this.max = max;
            this.percentage = percentage;
        }

        public void setAmount(int amount)
        {
            this.amount = amount;
        }

        public int getAmount()
        {
            return this.amount;
        }
}
