"use strict";

const gregnauticsHideIcyHorseExtras = event => {
    [
        "upgraded_saddle", "horse_hooves_gear", "horse_medkit_gear", "canister",
        "horse_stabilizer_gear", "wheel", "horse_cart_gear"
    ].forEach(path => {
        const id = `icys-better-horses:${path}`;
        if (Item.exists(id)) event.remove(Ingredient.of(id));
    });
};

RecipeViewerEvents.removeEntries("item", gregnauticsHideIcyHorseExtras);
RecipeViewerEvents.removeEntriesCompletely("item", gregnauticsHideIcyHorseExtras);
