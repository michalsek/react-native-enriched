#pragma once
#import <UIKit/UIKit.h>

// the order is aligned with the order of tags in parser
typedef NS_ENUM(NSInteger, StyleType) {
  BlockQuote,
  CodeBlock,
  UnorderedList,
  OrderedList,
  CheckboxList,
  Alignment,
  ParagraphMargin,
  H1,
  H2,
  H3,
  H4,
  H5,
  H6,
  Link,
  Mention,
  Image,
  // Inline text styles - they share a single <span> tag in the HTML output,
  // so they have to stay contiguous in this enum
  FontFamily,
  FontSize,
  LetterSpacing,
  LineHeight,
  ForegroundColor,
  InlineCode,
  Bold,
  Italic,
  Underline,
  Strikethrough,
  None,
};
