package com.frotty27.rpgmobs.assets;

public enum AssetType {

    ABILITIES("abilities"), EFFECTS("effects");

    private final String id;

    AssetType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
