// Run main with a player as the subject. The cube stays still until roll(c).
function main(c)
{
    var ui = mappet.createUI(c, "handler");
    // A string from a player state works just like ui.dice("blue").
    // If the state is absent/empty, the component selects original.
    var diceColor = c.getSubject().getStates().getString("dice_color");
    // Only the dice panel: no button, button handler, or reserved footer.
    ui.dice(diceColor).difficulty(15).base(5).id("dice"); // Default: 240x300 GUI pixels.

    c.getSubject().openUI(ui);
}

// Invoke from your own game logic while this UI is open.
function roll(c)
{
    var context = c.getSubject().getUIContext();

    // Spin/stop milliseconds, difficulty, raw D20 (1..20), base.
    // Replace 12 with the result supplied by your game logic.
    context.get("dice").roll(2400, 15, 12, 5);
    context.sendToPlayer();
}

function handler(c)
{
    var context = c.getSubject().getUIContext();

    if (context.getLast() === "dice")
    {
        // Same GUI event mechanism as a button, on completion or left-click skip.
        var data = context.getData();
        var total = data.getLong("dice.total");
        var outcome = data.getString("dice.outcome");
        var skipped = data.getBoolean("dice.skipped");

        c.send("Результат: " + total + ", исход: " + outcome
            + (skipped ? " (анимация пропущена)" : ""));
    }
}
