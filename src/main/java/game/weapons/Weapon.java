package game.weapons;

/**
 * Базовое оружие: магазин, резерв, скорострельность, разброс,
 * перезарядка. Конкретные параметры — в Pistol/Shotgun.
 */
public abstract class Weapon {

    private final String name;
    private final float damage;          // урон за пулю
    private final float headshotMult;    // множитель хедшота
    private final int pellets;           // пуль за выстрел (дробовик)
    private final float spread;          // разброс (радианы-подобный коэффициент)
    private final float fireInterval;    // мин. пауза между выстрелами, сек
    private final float reloadTime;      // время перезарядки, сек
    private final int magSize;
    private final int maxReserve;

    private int ammoInMag;
    private int reserve;
    private float cooldown;
    private boolean reloading;
    private float reloadLeft;

    protected Weapon(String name, float damage, float headshotMult, int pellets, float spread,
                     float fireInterval, float reloadTime, int magSize, int maxReserve,
                     int startReserve) {
        this.name = name;
        this.damage = damage;
        this.headshotMult = headshotMult;
        this.pellets = pellets;
        this.spread = spread;
        this.fireInterval = fireInterval;
        this.reloadTime = reloadTime;
        this.magSize = magSize;
        this.maxReserve = maxReserve;
        this.reserve = startReserve;
        this.ammoInMag = magSize;
    }

    /** Может ли стрелять прямо сейчас. */
    public boolean canFire() {
        return !reloading && cooldown <= 0f && ammoInMag > 0;
    }

    /** Пустой магазин и можно щёлкнуть затвором. */
    public boolean isEmpty() {
        return ammoInMag == 0 && !reloading && cooldown <= 0f;
    }

    /** Расходует патрон и взводит кулдаун. */
    public void consume() {
        if (ammoInMag > 0) {
            ammoInMag--;
        }
        cooldown = fireInterval;
    }

    /** Начать перезарядку. @return true, если перезарядка начата. */
    public boolean startReload() {
        if (reloading || ammoInMag == magSize || reserve <= 0) {
            return false;
        }
        reloading = true;
        reloadLeft = reloadTime;
        return true;
    }

    /** Тик: кулдаун и завершение перезарядки. */
    public void update(float tpf) {
        if (cooldown > 0f) {
            cooldown -= tpf;
        }
        if (reloading) {
            reloadLeft -= tpf;
            if (reloadLeft <= 0f) {
                int need = magSize - ammoInMag;
                int take = Math.min(need, reserve);
                ammoInMag += take;
                reserve -= take;
                reloading = false;
            }
        }
    }

    /** Смена оружия отменяет незавершённую перезарядку. */
    public void cancelReload() {
        reloading = false;
    }

    /** Добавить патроны в резерв. @return сколько реально добавлено. */
    public int addReserve(int amount) {
        int before = reserve;
        reserve = Math.min(maxReserve, Math.max(0, reserve + amount));
        return reserve - before;
    }

    // ------------------------------------------------------------
    //  Геттеры
    // ------------------------------------------------------------

    public String getName() {
        return name;
    }

    public float getDamage() {
        return damage;
    }

    public float getHeadshotMult() {
        return headshotMult;
    }

    public int getPellets() {
        return pellets;
    }

    public float getSpread() {
        return spread;
    }

    public int getAmmoInMag() {
        return ammoInMag;
    }

    public int getReserve() {
        return reserve;
    }

    public int getMagSize() {
        return magSize;
    }

    public boolean isReloading() {
        return reloading;
    }

    /** Прогресс перезарядки 0..1 (для анимации). */
    public float getReloadProgress() {
        return reloading ? 1f - reloadLeft / reloadTime : 0f;
    }

    /** Полный ли резерв (для HUD). */
    public boolean isReserveFull() {
        return reserve >= maxReserve;
    }
}
