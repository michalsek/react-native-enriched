#import "LineBoxLayoutDelegate.h"

NSString *const LineBoxHeightAttributeName = @"EnrichedLineBoxHeight";

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

+ (CGFloat)usedHeightOfAttributedString:(NSAttributedString *)string
                                  width:(CGFloat)width
                   maximumNumberOfLines:(NSUInteger)maximumNumberOfLines
                          lineBreakMode:(NSLineBreakMode)lineBreakMode {
  NSTextStorage *storage =
      [[NSTextStorage alloc] initWithAttributedString:string];
  NSLayoutManager *layoutManager = [[NSLayoutManager alloc] init];
  NSTextContainer *container =
      [[NSTextContainer alloc] initWithSize:CGSizeMake(width, CGFLOAT_MAX)];
  container.lineFragmentPadding = 0;
  container.maximumNumberOfLines = maximumNumberOfLines;
  container.lineBreakMode = lineBreakMode;
  layoutManager.delegate = [LineBoxLayoutDelegate shared];
  [layoutManager addTextContainer:container];
  [storage addLayoutManager:layoutManager];
  [layoutManager ensureLayoutForTextContainer:container];

  return CGRectGetMaxY([layoutManager usedRectForTextContainer:container]);
}

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

+ (CGFloat)lineHeightForAttributes:
               (NSDictionary<NSAttributedStringKey, id> *)attrs
                              font:(UIFont *)font {
  NSNumber *lineBoxHeight = attrs[LineBoxHeightAttributeName];

  if ([lineBoxHeight isKindOfClass:[NSNumber class]] &&
      lineBoxHeight.doubleValue > 0) {
    return lineBoxHeight.doubleValue;
  }

  return [LineBoxLayoutDelegate
      lineHeightForFont:font
         paragraphStyle:attrs[NSParagraphStyleAttributeName]];
}

+ (BOOL)hasExplicitLineHeight:(NSDictionary<NSAttributedStringKey, id> *)attrs {
  NSParagraphStyle *paragraphStyle = attrs[NSParagraphStyleAttributeName];

  return [attrs[LineBoxHeightAttributeName] doubleValue] > 0 ||
         paragraphStyle.minimumLineHeight > 0 ||
         paragraphStyle.maximumLineHeight > 0;
}

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
  __block BOOL hasAttachment = NO;
  __block BOOL explicitLineHeight = NO;

  [textStorage enumerateAttributesInRange:metricsRange
                                  options:0
                               usingBlock:^(NSDictionary *attrs,
                                            NSRange attrsRange, BOOL *stop) {
                                 UIFont *font = attrs[NSFontAttributeName];

                                 if (attrs[NSAttachmentAttributeName] != nil) {
                                   hasAttachment = YES;
                                 }

                                 if (![font isKindOfClass:[UIFont class]]) {
                                   return;
                                 }

                                 if ([LineBoxLayoutDelegate
                                         hasExplicitLineHeight:attrs]) {
                                   explicitLineHeight = YES;
                                 }

                                 CGFloat lineHeight = [LineBoxLayoutDelegate
                                     lineHeightForAttributes:attrs
                                                        font:font];
                                 CGFloat descent = -font.descender;
                                 CGFloat halfLeading =
                                     (lineHeight - font.lineHeight) / 2.0;

                                 above = MAX(above, font.lineHeight - descent +
                                                        halfLeading);
                                 below = MAX(below, descent + halfLeading);
                                 measured = YES;
                               }];

  if (!measured) {
    return YES;
  }

  CGFloat spacingBefore = MAX(0, CGRectGetMinY(*lineFragmentUsedRect) -
                                     CGRectGetMinY(*lineFragmentRect));
  CGFloat spacingAfter = MAX(0, CGRectGetMaxY(*lineFragmentRect) -
                                    CGRectGetMaxY(*lineFragmentUsedRect));
  CGFloat height = above + below;
  CGFloat proposedHeight = lineFragmentUsedRect->size.height;

  if (hasAttachment || !explicitLineHeight) {
    if (proposedHeight - height > kNonTextLineSlack) {
      return YES;
    }
    height = MAX(height, proposedHeight);
  }

  lineFragmentRect->size.height = spacingBefore + height + spacingAfter;
  lineFragmentUsedRect->origin.y =
      CGRectGetMinY(*lineFragmentRect) + spacingBefore;
  lineFragmentUsedRect->size.height = height;
  baselineOffset[0] = spacingBefore + above;

  return YES;
}

@end
