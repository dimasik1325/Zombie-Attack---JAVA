package game.weapons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Тесты амуниции и перезарядки (headless). */
class WeaponTest {

    @Test
    void pistolFiresAndSpendsAmmo() {
        Pistol p = new Pistol();
        assertEquals(12, p.getAmmoInMag());
        assertTrue(p.canFire());
        p.consume();
        assertEquals(11, p.getAmmoInMag());
        assertEquals(60, p.getReserve());
    }

    @Test
    void emptyMagazineCannotFire() {
        Pistol p = new Pistol();
        for (int i = 0; i < 12; i++) {
            p.consume();
        }
        assertEquals(0, p.getAmmoInMag());
        assertTrue(p.isEmpty());
        assertFalse(p.canFire());
    }

    @Test
    void reloadTakesTimeThenRefillsFromReserve() {
        Pistol p = new Pistol();
        p.consume(); // 11 в магазине
        assertTrue(p.startReload());
        assertFalse(p.startReload(), "повторная перезарядка не должна начаться");
        assertFalse(p.canFire(), "во время перезарядки стрелять нельзя");

        p.update(0.5f);
        assertTrue(p.isReloading(), "перезарядка 1.1с ещё идёт");
        p.update(0.7f);
        assertFalse(p.isReloading(), "перезарядка завершена");
        assertEquals(12, p.getAmmoInMag());
        assertEquals(59, p.getReserve(), "один патрон ушёл из резерва");
    }

    @Test
    void reloadImpossibleWithFullMagOrEmptyReserve() {
        Pistol p = new Pistol();
        assertFalse(p.startReload(), "магазин полон");
        while (p.getReserve() > 0) {
            p.addReserve(-1);
        }
        p.consume();
        assertFalse(p.startReload(), "резерв пуст");
    }

    @Test
    void shotgunHasPelletsAndSpread() {
        Shotgun s = new Shotgun();
        assertEquals(6, s.getPellets());
        assertEquals(17f, s.getDamage(), 1e-6);
        assertTrue(s.getSpread() > 0.03f, "у дробовика заметный разброс");
        assertEquals(6, s.getMagSize());
    }

    @Test
    void addReserveCapsAtMax() {
        Pistol p = new Pistol();
        int added = p.addReserve(1000);
        assertEquals(60, added, "добавлено до лимита 120-60=60");
        assertTrue(p.isReserveFull());
        assertEquals(0, p.addReserve(10));
    }

    @Test
    void switchOutCancelsReload() {
        Pistol p = new Pistol();
        p.consume();
        assertTrue(p.startReload());
        p.cancelReload();
        assertFalse(p.isReloading());
        p.update(5f);
        assertEquals(11, p.getAmmoInMag(), "перезарядка была отменена");
    }
}
