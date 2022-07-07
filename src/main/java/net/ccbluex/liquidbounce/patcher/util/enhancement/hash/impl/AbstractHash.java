/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.patcher.util.enhancement.hash.impl;

import java.util.Objects;

public abstract class AbstractHash {

    private final int hash;
    private final Object[] objects;

    public AbstractHash(Object... items) {
        hash = Objects.hash(items);
        objects = items;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractHash)) {
            return false;
        }

        Object[] a = objects;
        Object[] a2 = ((AbstractHash) obj).objects;

        if (a == a2) {
            return true;
        }

        if (a == null || a2 == null) {
            return false;
        }

        int length = a.length;
        if (a2.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (a[i] != a2[i]) {
                if (!a[i].equals(a2[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
