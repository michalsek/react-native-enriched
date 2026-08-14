import { Mark } from '@tiptap/core';
import type { Editor } from '@tiptap/react';

export interface TextStyleAttributes {
  fontFamily: string | null;
  fontSize: number | null;
  letterSpacing: number | null;
  lineHeight: number | null;
  foregroundColor: string | null;
}

export const TEXT_STYLE_ATTRIBUTES = [
  'fontFamily',
  'fontSize',
  'letterSpacing',
  'lineHeight',
  'foregroundColor',
] as const;

function parseCssDimension(value: string): number | null {
  const parsed = parseFloat(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function parseCssFontFamily(value: string): string | null {
  const firstFamily = value.split(',')[0]?.trim().replace(/['"]+/g, '').trim();
  return firstFamily ? firstFamily : null;
}

/** Formats a number omitting the trailing ".0" so the HTML output stays clean. */
function formatCssNumber(value: number): string {
  return String(value);
}

/**
 * A single mark carrying the inline text styles (font family, font size,
 * letter spacing, line height), rendered as a `<span style="...">` tag -
 * the same representation the native platforms use.
 */
export const EnrichedTextStyle = Mark.create({
  name: 'textStyle',

  addAttributes() {
    return {
      fontFamily: {
        default: null,
        parseHTML: (element) =>
          element.style.fontFamily
            ? parseCssFontFamily(element.style.fontFamily)
            : null,
      },
      fontSize: {
        default: null,
        parseHTML: (element) =>
          element.style.fontSize
            ? parseCssDimension(element.style.fontSize)
            : null,
      },
      letterSpacing: {
        default: null,
        parseHTML: (element) =>
          element.style.letterSpacing
            ? parseCssDimension(element.style.letterSpacing)
            : null,
      },
      lineHeight: {
        default: null,
        parseHTML: (element) =>
          element.style.lineHeight
            ? parseCssDimension(element.style.lineHeight)
            : null,
      },
      foregroundColor: {
        default: null,
        parseHTML: (element) => element.style.color || null,
      },
    };
  },

  parseHTML() {
    return [
      {
        tag: 'span[style]',
        getAttrs: (element) => {
          const { style } = element as HTMLElement;
          const hasAnyTextStyle =
            style.fontFamily ||
            style.fontSize ||
            style.letterSpacing ||
            style.lineHeight ||
            style.color;
          return hasAnyTextStyle ? {} : false;
        },
      },
    ];
  },

  renderHTML({ mark }) {
    const { fontFamily, fontSize, letterSpacing, lineHeight, foregroundColor } =
      mark.attrs as TextStyleAttributes;

    const declarations: string[] = [];
    if (fontFamily != null) {
      declarations.push(`font-family: ${fontFamily}`);
    }
    if (fontSize != null) {
      declarations.push(`font-size: ${formatCssNumber(fontSize)}px`);
    }
    if (letterSpacing != null) {
      declarations.push(`letter-spacing: ${formatCssNumber(letterSpacing)}px`);
    }
    if (lineHeight != null) {
      declarations.push(`line-height: ${formatCssNumber(lineHeight)}px`);
    }
    if (foregroundColor != null) {
      declarations.push(`color: ${foregroundColor}`);
    }

    return ['span', { style: declarations.join('; ') }, 0];
  },
});

/**
 * Applies (or removes, when the value is null) a single text style attribute
 * to the current selection. When no attribute is left, the whole mark gets
 * removed.
 */
export function setTextStyleAttribute(
  editor: Editor,
  attribute: (typeof TEXT_STYLE_ATTRIBUTES)[number],
  value: string | number | null,
  collapsedSelectionMode: 'typing' | 'paragraph' = 'typing'
) {
  if (editor.isActive('codeBlock')) {
    return;
  }

  const currentAttributes = editor.getAttributes('textStyle');
  const mergedAttributes = {
    fontFamily: currentAttributes.fontFamily ?? null,
    fontSize: currentAttributes.fontSize ?? null,
    letterSpacing: currentAttributes.letterSpacing ?? null,
    lineHeight: currentAttributes.lineHeight ?? null,
    foregroundColor: currentAttributes.foregroundColor ?? null,
    [attribute]: value,
  };

  const isEmpty = TEXT_STYLE_ATTRIBUTES.every(
    (key) => mergedAttributes[key] == null
  );

  const { from, to, empty, $from } = editor.state.selection;
  const appliesToParagraph = empty && collapsedSelectionMode === 'paragraph';
  const chain = editor.chain().focus();
  if (appliesToParagraph && $from.start() !== $from.end()) {
    chain.setTextSelection({ from: $from.start(), to: $from.end() });
  }
  if (isEmpty) chain.unsetMark('textStyle');
  else chain.setMark('textStyle', mergedAttributes);
  if (appliesToParagraph) chain.setTextSelection({ from, to });
  chain.run();
}
