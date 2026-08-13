# Line-height parity — CSS-centred line boxes

The editor used to grow every line box to the font's natural height whenever that
height exceeded the requested `line-height`, instead of letting the glyphs
overflow a line box of exactly `line-height`. CSS does the latter, so a native
render drifted from web on every font whose `hhea` ascent + descent exceeds its
authored line-height — a per-line error, not a one-off offset.

Measured against a DS4 parity corpus (Coast to Coast at 24pt, band 1.3):
web line pitch 31.18dp, native 43.4dp — the font's natural height. Eleven
lines of Modern Chunk at 48pt rendered 860dp tall against web's 633.6dp.

## The rule both platforms implement

A line box is `above + below` tall with the baseline `above` from its top,
where, over every attribute run on the line (`L` = that run's requested line
height, `F` = its font's natural height, `A`/`D` its ascent/descent):

```
halfLeading = (L - F) / 2          // signed: negative when the glyphs overflow
above       = max(A + halfLeading) // over the runs
below       = max(D + halfLeading)
```

Two consequences, both of which CSS has and neither of which TextKit or
`Layout` gives you for free: a glyph box taller than the line box overflows it
**symmetrically**, and a line holding spans of different sizes takes its height
from the **tallest** span, not the first.

**iOS** (`ios/utils/LineHeightUtils.mm`, `ios/utils/LineBoxLayoutDelegate.mm`,
wired up in `EnrichedTextView.mm` / `EnrichedTextInputView.mm`): `applyLineHeight`
also sets `paragraphStyle.maximumLineHeight`, so the line fragment is exactly
the requested height in both directions.

That alone puts the pitch right and the glyphs in the wrong place. TextKit sits
the glyphs on the **bottom** edge of a line fragment, so clamping the fragment
spills the entire overflow above the line instead of half of it. A negative
`NSBaselineOffsetAttributeName` cannot pull them back down — the typesetter
grows the run's descent by the same amount to keep the glyphs inside the line,
so the two cancel exactly. The parity corpus shows the cancellation directly:
before `maximumLineHeight`, native sat `(F-L)/2` **below** web; after it, and
with a negative baseline offset written on every run, native sat `(F-L)/2`
**above** web. The offset moved nothing; only the fragment height changed.

So the line box and the baseline are computed in an `NSLayoutManagerDelegate`
(`layoutManager:shouldSetLineFragmentRect:lineFragmentUsedRect:baselineOffset:…`),
the one hook that can place a baseline inside a fragment, and
`baselineOffsetForLineHeight` is gone — its callers now only strip the
attribute, since a per-run offset would stack on top of the delegate's baseline
and shift runs against each other rather than moving the line's shared one.

A line that is taller than its text (an inline attachment) keeps TextKit's
geometry: the delegate only ever grows a fragment, never shrinks one.

### The paragraph mark is not a span

The delegate measures the runs on the line, and the last line of every
paragraph carries one more run than it looks like: the `\n` that ends it. The
HTML parser lays the whole document down in `defaultTypingAttributes` and then
applies each `<span>`'s font over the span's own range, so the mark keeps the
view's default font — 14pt system unless the view sets style props — while the
line-height style, being a paragraph attribute, does cover it. Half-leading is
per run, so that mark asks for `(L - 16.7)/2` of leading on each side: at
`L = 57.6` its `below` is 23.6dp against a 48pt face's 6.7dp, and `max` hands
the line box the mark's descent instead of the text's.

That inflated every paragraph's last line by `((A - D) - (A₁₄ - D₁₄)) / 2` —
independent of `L`, and driven by the *text's* ascent-descent spread rather
than by any overflow, which is why it hit faces that fit their line box just as
hard: Wobbles at 48pt (`F < L`) gained 17.3dp per paragraph break, measured off
the ink rows of the parity PNGs. The glyphs on the line did not move; every
line after it did.

CSS gives a line break no inline box at all, so `rangeWithoutTrailingBreak`
drops the mark before the runs are measured. A line that is nothing but its
break keeps it — one run always measures exactly its own line height, so the
empty paragraph still gets a box of `L`.

**Android** (`common/spans/LineHeightMetrics.kt`): the same one-sided guard
lived in `expandToCenteredLineHeight`, which early-returned when the requested
height was not larger. Renamed to `centerInLineHeight` and made it shrink as
well as grow; when shrinking, `top`/`bottom` are clamped to the new
ascent/descent so `includeFontPadding` cannot grow the line back.

Android needs nothing beyond that. It adjusts the line's own `ascent`/`descent`
rather than a fragment rect, and `Layout` puts the baseline at `-ascent` from
the line top, so halving the shrink across both edges *is* the symmetric
overflow. `EnrichedInlineLineHeightSpan.chooseHeight` already rebuilds the
line's natural metrics and applies the tallest covering span's line height, so
the tallest span already wins a mixed row.

## Invariance

A line whose runs all fit their line boxes (`F <= L`) lands in exactly the same
place as before. Its fragment was `L` and TextKit put the baseline at `L - D`
from the top, with the old positive `NSBaselineOffsetAttributeName` of
`(L - F)/2` raising the glyphs; the delegate computes
`above = (F - D) + (L - F)/2`, and `L - D - (L - F)/2 == (F - D) + (L - F)/2`.
The box height is `above + below = L`, unchanged. Only the sign of
`halfLeading` distinguishes the corrected case.

Dropping the paragraph mark is what makes that hold on the last line of a
paragraph as well: with the mark measured, `below` came from a run the line
does not draw, so the box was `L` plus the spread above, and a `F <= L`
paragraph moved everything under it down. With it dropped, every line of such a
paragraph is `L` tall again and the whole block sits where it did before this
change.

## Known residual

The delegate splits `F = UIFont.lineHeight`, which is `ascent + descent +
lineGap`; CSS half-leading is computed over `ascent + descent` alone and leaves
the line gap out. TextKit puts that gap above the glyphs, so on a font with a
non-zero `hhea` lineGap native sits `lineGap / 2` below web. This is a constant
sub-dp residual on fonts that fit their line box, deliberately preserved so
those renders do not move. Switching the delegate to
`font.ascender - font.descender` would remove it — one line, but it moves every
render, so it wants its own parity run.

A paragraph with a bottom margin sets `paragraphSpacing`, which TextKit folds
into the line fragment rect of that paragraph's last line. The delegate reads
the taller proposal as an attachment-driven line and returns early, so that one
line keeps TextKit's placement — right while `F <= L`, `(F - L)/2` too high
otherwise. Subtracting `paragraphSpacing` and `paragraphSpacingBefore` from
`proposedHeight` before the attachment check fixes it; it moves margin-offset
renders, so it also wants its own parity run.
