package io.github.dfauth.dormant.actor.repository;

import io.github.dfauth.dormant.Decoder;
import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.dormant.Encoder;
import io.github.dfauth.dormant.actor.Foldable;
import io.github.dfauth.dormant.actor.Scalable;
import io.github.dfauth.dormant.model.Price;
import lombok.*;

import java.util.Optional;
import java.util.function.Function;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class PriceVolume implements Dormant, Foldable<Price, PriceVolume>, Scalable<PriceVolume> {

    public static Function<PriceVolume, Optional<PriceVolume>> smaFinisher(int period) {
        return r -> r.count >= period ? Optional.of(new PriceVolume(r.price / r.count, r.volume / r.count, 0)) : Optional.empty();
    }

    @Getter
    private double price;
    @Getter
    private double volume;
    private int count;

    public PriceVolume(double price, int volume) {
        this(price, volume, 0);
    }

    @Override
    public void write(Encoder encoder) {
        encoder.writeDouble(price)
                .writeDouble(volume)
                .writeInt(count);
    }

    @Override
    public void read(Decoder decoder) {
        decoder.readDouble(d -> this.price = d)
                .readDouble(d -> this.volume = d)
                .readInt(i -> this.count = i);
    }

    @Override
    public PriceVolume aggregate(Price price) {
        this.price += price.close();
        this.volume += price.volume();
        this.count++;
        return this;
    }

    @Override
    public PriceVolume combine(PriceVolume priceVolume) {
        this.price += priceVolume.price;
        this.volume += priceVolume.volume;
        this.count += priceVolume.count;
        return this;
    }

    @Override
    public PriceVolume fold(PriceVolume priceVolume) {
        return combine(priceVolume);
    }

    @Override
    public PriceVolume scale(double multiplier) {
        this.price *= multiplier;
        this.volume *= multiplier;
        return this;
    }
}
