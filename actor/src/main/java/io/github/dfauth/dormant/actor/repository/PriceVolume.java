package io.github.dfauth.dormant.actor.repository;

import io.github.dfauth.dormant.Decoder;
import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.dormant.Encoder;
import io.github.dfauth.dormant.actor.CalculationState;
import io.github.dfauth.dormant.model.Price;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PriceVolume implements Dormant {

    private double price;
    private double volume;

    @Override
    public void write(Encoder encoder) {
        encoder.writeDouble(price).writeDouble(volume);
    }

    @Override
    public void read(Decoder decoder) {
        decoder.readDouble(d -> this.price = d)
                .readDouble(d -> this.volume = d);
    }

    public static class PriceVolumeCalculationState implements CalculationState<Price, PriceVolume, PriceVolumeCalculationState> {

        private final PriceVolume priceVolume = new PriceVolume();
        private int count = 0;

        @Override
        public Optional<PriceVolume> current() {
            return Optional.of(new PriceVolume(priceVolume.getPrice() / count, priceVolume.getVolume() / count));
        }

        @Override
        public void accept(Price price) {
            priceVolume.price += price.getClose().doubleValue();
            priceVolume.volume += price.getVolume();
            this.count++;
        }
    }

}
