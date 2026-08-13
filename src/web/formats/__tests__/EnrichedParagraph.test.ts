import {
  paragraphStyleToCss,
  parseCssDimension,
  parseCssMarginShorthand,
} from '../EnrichedParagraph';

describe('EnrichedParagraph', () => {
  it('parses CSS dimensions used by paragraph margins', () => {
    expect(parseCssDimension('14px')).toBe(14);
    expect(parseCssDimension('6.5px')).toBe(6.5);
    expect(parseCssDimension('inherit')).toBeNull();
  });

  it('parses vertical values from margin shorthand', () => {
    expect(parseCssMarginShorthand('14px')).toEqual({
      marginBottom: 14,
      marginTop: 14,
    });
    expect(parseCssMarginShorthand('14px 2px')).toEqual({
      marginBottom: 14,
      marginTop: 14,
    });
    expect(parseCssMarginShorthand('14px 2px 6px')).toEqual({
      marginBottom: 6,
      marginTop: 14,
    });
  });

  it('serializes paragraph alignment and margins into one style attribute', () => {
    expect(
      paragraphStyleToCss({
        marginBottom: 6,
        marginTop: 14,
        textAlign: 'center',
      })
    ).toBe('text-align: center; margin-top: 14px; margin-bottom: 6px');
  });
});
