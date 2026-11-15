package io.github.dfauth.dormant.actor;

public interface Scalable<R extends Scalable<R>> {

    R combine(R r);

    R scale(double multiplier);

    static Foldable.FoldableScalableDouble scalable(double d) {
        return new Foldable.FoldableScalableDouble(d);
    }

}
