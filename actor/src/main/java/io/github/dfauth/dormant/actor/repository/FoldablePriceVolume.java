package io.github.dfauth.dormant.actor.repository;

import io.github.dfauth.dormant.Decoder;
import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.dormant.Encoder;
import io.github.dfauth.dormant.actor.Foldable;
import io.github.dfauth.dormant.model.Price;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class FoldablePriceVolume implements Dormant, Foldable<Price, FoldablePriceVolume> {

    private double price;
    private double volume;
    private int count;

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
    public FoldablePriceVolume aggregate(Price price) {
        this.price += price.close();
        this.volume += price.volume();
        this.count++;
        return this;
    }

    @Override
    public FoldablePriceVolume combine(FoldablePriceVolume priceVolume) {
        this.price += priceVolume.price;
        this.volume += priceVolume.volume;
        this.count += priceVolume.count;
        return this;
    }

    public static Function<FoldablePriceVolume, Optional<PriceVolume>> smaFinisher(int period) {
        return r -> r.count >= period ? Optional.of(new PriceVolume(r.price / r.count, r.volume / r.count)) : Optional.empty();
    }

    public static Function<FoldablePriceVolume, PriceVolume> emaFinisher(double multiplier) {
        return r -> new PriceVolume(r.price * multiplier, r.volume * multiplier);
    }
}
