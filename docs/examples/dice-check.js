// Run main with a player as the subject. The cube stays still until "Бросить".
function main(c)
{
    var ui = mappet.createUI(c, "handler").background();

    ui.dice().difficulty(15).base(5).id("dice");

    ui.button("Бросить").id("roll")
        .rx(0.5).ry(1, -28).wh(160, 20).anchorX(0.5);

    c.getSubject().openUI(ui);
}

function handler(c)
{
    var context = c.getSubject().getUIContext();

    if (context.getLast() === "roll")
    {
        // Spin/stop milliseconds, difficulty, raw D20 (1..20), base.
        // Replace 12 with the result supplied by your game logic.
        context.get("dice").roll(2400, 15, 12, 5);
    }
    else if (context.getLast() === "dice")
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
