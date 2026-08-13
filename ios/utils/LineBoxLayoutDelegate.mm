#import "LineBoxLayoutDelegate.h"

// A line can legitimately be taller than its text when an attachment drives it;
// this much slack absorbs layout rounding without swallowing that case.
static const CGFloat kNonTextLineSlack = 1.0;

@implementation LineBoxLayoutDelegate

+ (instancetype)shared {
  static LineBoxLayoutDelegate *shared = nil;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    shared = [[self alloc] init];
  });

  return shared;
}

// The paragraph mark keeps the view's default font (14pt system) because the
// HTML's styles only span the text, and half-leading is per run, so a mark left
// in the line's metrics sizes the box from a font that draws nothing. CSS gives
// a line break no inline box at all.
+ (NSRange)rangeWithoutTrailingBreak:(NSRange)range
                            inString:(NSString *)string {
  NSCharacterSet *newlines = [NSCharacterSet newlineCharacterSet];
  NSRange trimmed = range;

  while (trimmed.length > 0) {
    unichar last = [string characterAtIndex:NSMaxRange(trimmed) - 1];

    if (![newlines characterIsMember:last]) {
      break;
    }

    trimmed.length -= 1;
  }

  // A line holding nothing but its break still needs a box, and one run always
  // measures exactly its line height.
  return trimmed.length > 0 ? trimmed : range;
}

+ (CGFloat)lineHeightForFont:(UIFont *)font
              paragraphStyle:(NSParagraphStyle *)paragraphStyle {
  CGFloat lineHeight = font.lineHeight;

  if (paragraphStyle == nil) {
    return lineHeight;
  }

  if (paragraphStyle.lineHeightMultiple > 0) {
    lineHeight *= paragraphStyle.lineHeightMultiple;
  }
  if (paragraphStyle.minimumLineHeight > 0) {
    lineHeight = MAX(paragraphStyle.minimumLineHeight, lineHeight);
  }
  if (paragraphStyle.maximumLineHeight > 0) {
    lineHeight = MIN(paragraphStyle.maximumLineHeight, lineHeight);
  }

  return lineHeight;
}

// TextKit sits the glyphs on the bottom edge of a line fragment and ignores a
// negative NSBaselineOffsetAttributeName, so a font taller than its line height
// spills the whole overflow above the line. CSS splits that overflow evenly, and
// takes the line box from the tallest span rather than the paragraph's first, so
// both the box and the baseline are computed here instead.
- (BOOL)layoutManager:(NSLayoutManager *)layoutManager
    shouldSetLineFragmentRect:(inout CGRect *)lineFragmentRect
         lineFragmentUsedRect:(inout CGRect *)lineFragmentUsedRect
               baselineOffset:(inout CGFloat *)baselineOffset
              inTextContainer:(NSTextContainer *)textContainer
                forGlyphRange:(NSRange)glyphRange {
  NSTextStorage *textStorage = layoutManager.textStorage;

  if (textStorage == nil || glyphRange.length == 0) {
    return YES;
  }

  NSRange charRange = [layoutManager characterRangeForGlyphRange:glyphRange
                                                actualGlyphRange:NULL];

  if (charRange.length == 0 || NSMaxRange(charRange) > textStorage.length) {
    return YES;
  }

  NSRange metricsRange =
      [LineBoxLayoutDelegate rangeWithoutTrailingBreak:charRange
                                             inString:textStorage.string];

  __block CGFloat above = 0;
  __block CGFloat below = 0;
  __block BOOL measured = NO;

  [textStorage
      enumerateAttributesInRange:metricsRange
                         options:0
                      usingBlock:^(
                          NSDictionary<NSAttributedStringKey, id> *_Nonnull attrs,
                          NSRange attrsRange, BOOL *_Nonnull stop) {
                        UIFont *font = attrs[NSFontAttributeName];

                        if (![font isKindOfClass:[UIFont class]]) {
                          return;
                        }

                        CGFloat lineHeight = [LineBoxLayoutDelegate
                            lineHeightForFont:font
                               paragraphStyle:
                                   attrs[NSParagraphStyleAttributeName]];
                        CGFloat descent = -font.descender;
                        CGFloat halfLeading =
                            (lineHeight - font.lineHeight) / 2.0;

                        above = MAX(above,
                                    font.lineHeight - descent + halfLeading);
                        below = MAX(below, descent + halfLeading);
                        measured = YES;
                      }];

  if (!measured) {
    return YES;
  }

  CGFloat height = above + below;
  CGFloat proposedHeight = lineFragmentRect->size.height;

  if (proposedHeight - height > kNonTextLineSlack) {
    return YES;
  }

  if (height > proposedHeight) {
    lineFragmentRect->size.height = height;
    lineFragmentUsedRect->size.height =
        MAX(lineFragmentUsedRect->size.height, height);
  }

  *baselineOffset = above;

  return YES;
}

@end
