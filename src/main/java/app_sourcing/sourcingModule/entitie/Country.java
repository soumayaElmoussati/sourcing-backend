package app_sourcing.sourcingModule.entitie;

public enum Country {
    KSA, UAE, KW, QA, OM, BH, IQ;

    public static boolean isGccCountry(Country country) {
        return country != null && country != IQ;
    }
}

