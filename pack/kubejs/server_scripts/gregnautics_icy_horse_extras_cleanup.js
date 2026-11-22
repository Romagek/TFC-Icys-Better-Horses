"use strict";

// These Icy extras are replaced by the pack's saddle, Core shoes and wagon mods.
ServerEvents.recipes(event => {
    [
        "upgraded_saddle", "horse_hooves_gear", "horse_medkit_gear", "canister",
        "horse_stabilizer_gear", "wheel", "horse_cart_gear"
    ].forEach(path => {
        const id = `icys-better-horses:${path}`;
        event.remove({ output: id });
        event.remove({ input: id });
    });
});
