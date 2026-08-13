import Paragraph from '@tiptap/extension-paragraph';

export interface ParagraphStyleAttributes {
  marginBottom: number | null;
  marginTop: number | null;
  textAlign: string | null;
}

export function parseCssDimension(value: string): number | null {
  const parsed = parseFloat(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function parseCssMarginShorthand(
  value: string
): Pick<ParagraphStyleAttributes, 'marginBottom' | 'marginTop'> {
  const values = value.trim().split(/\s+/).filter(Boolean);
  const topValue = values[0];
  const bottomValue = values.length >= 3 ? values[2] : topValue;
  const top = topValue ? parseCssDimension(topValue) : null;
  const bottom = bottomValue ? parseCssDimension(bottomValue) : top;

  return {
    marginBottom: bottom,
    marginTop: top,
  };
}

export function paragraphStyleToCss({
  marginBottom,
  marginTop,
  textAlign,
}: ParagraphStyleAttributes): string | null {
  const declarations: string[] = [];

  if (textAlign) {
    declarations.push(`text-align: ${textAlign}`);
  }
  if (marginTop != null && marginTop > 0) {
    declarations.push(`margin-top: ${marginTop}px`);
  }
  if (marginBottom != null && marginBottom > 0) {
    declarations.push(`margin-bottom: ${marginBottom}px`);
  }

  return declarations.length > 0 ? declarations.join('; ') : null;
}

export const EnrichedParagraph = Paragraph.extend({
  addAttributes() {
    return {
      textAlign: {
        default: null,
        parseHTML: (element) => {
          const value = element.style.textAlign;
          return value || null;
        },
      },
      marginTop: {
        default: null,
        parseHTML: (element) => {
          const shorthand = element.style.margin
            ? parseCssMarginShorthand(element.style.margin)
            : null;
          return element.style.marginTop
            ? parseCssDimension(element.style.marginTop)
            : (shorthand?.marginTop ?? null);
        },
      },
      marginBottom: {
        default: null,
        parseHTML: (element) => {
          const shorthand = element.style.margin
            ? parseCssMarginShorthand(element.style.margin)
            : null;
          return element.style.marginBottom
            ? parseCssDimension(element.style.marginBottom)
            : (shorthand?.marginBottom ?? null);
        },
      },
    };
  },

  renderHTML({ HTMLAttributes }) {
    const style = paragraphStyleToCss({
      marginBottom: HTMLAttributes.marginBottom ?? null,
      marginTop: HTMLAttributes.marginTop ?? null,
      textAlign: HTMLAttributes.textAlign ?? null,
    });
    const attributes = { ...HTMLAttributes };
    delete attributes.marginBottom;
    delete attributes.marginTop;
    delete attributes.textAlign;

    return ['p', { ...attributes, ...(style ? { style } : {}) }, 0];
  },
});
