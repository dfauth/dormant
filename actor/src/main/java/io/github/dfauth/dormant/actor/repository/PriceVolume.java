package io.github.dfauth.dormant.actor.repository;

import io.github.dfauth.dormant.Decoder;
import io.github.dfauth.dormant.Dormant;
import io.github.dfauth.dormant.Encoder;
import io.github.dfauth.dormant.actor.Scalable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PriceVolume implements Dormant, Scalable<PriceVolume> {

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

    @Override
    public PriceVolume aggregate(PriceVolume other) {
//        this.price += other.price;
//        this.volume += other.volume;
//        return this;
        return new PriceVolume(this.price + other.price, this.volume + other.volume);
    }

    @Override
    public PriceVolume scale(double multiplier) {
//        this.price *= multiplier;
//        this.volume *= multiplier;
//        return this;
        return new PriceVolume(this.price * multiplier, this.volume * multiplier);
    }

}
