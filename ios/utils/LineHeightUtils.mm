#import "LineHeightUtils.h"

@implementation LineHeightUtils

+ (void)applyLineHeight:(CGFloat)lineHeight
       toParagraphStyle:(NSMutableParagraphStyle *)paragraphStyle {
  if (paragraphStyle == nil) {
    return;
  }

  paragraphStyle.minimumLineHeight = lineHeight;
}

+ (NSNumber *)baselineOffsetForLineHeight:(CGFloat)lineHeight
                                     font:(UIFont *)font {
  if (font == nil || lineHeight <= font.lineHeight) {
    return nil;
  }

  return @((lineHeight - font.lineHeight) / 2.0);
}

+ (void)applyBaselineOffsetToAttributes:(NSMutableDictionary *)attributes {
  NSParagraphStyle *paragraphStyle = attributes[NSParagraphStyleAttributeName];
  UIFont *font = attributes[NSFontAttributeName];
  CGFloat lineHeight =
      paragraphStyle != nil ? paragraphStyle.minimumLineHeight : 0;
  NSNumber *baselineOffset = [self baselineOffsetForLineHeight:lineHeight
                                                          font:font];

  if (baselineOffset == nil) {
    [attributes removeObjectForKey:NSBaselineOffsetAttributeName];
  } else {
    attributes[NSBaselineOffsetAttributeName] = baselineOffset;
  }
}

+ (void)applyBaselineOffsetsInTextStorage:(NSTextStorage *)textStorage
                                    range:(NSRange)range {
  if (textStorage == nil || range.length == 0) {
    return;
  }

  NSMutableArray<NSValue *> *ranges = [NSMutableArray array];
  NSMutableArray *offsets = [NSMutableArray array];

  [textStorage
      enumerateAttributesInRange:range
                         options:0
                      usingBlock:^(NSDictionary<NSAttributedStringKey, id>
                                       *_Nonnull attrs,
                                   NSRange attrsRange, BOOL *_Nonnull stop) {
                        NSParagraphStyle *paragraphStyle =
                            attrs[NSParagraphStyleAttributeName];
                        UIFont *font = attrs[NSFontAttributeName];
                        CGFloat lineHeight =
                            paragraphStyle != nil
                                ? paragraphStyle.minimumLineHeight
                                : 0;
                        NSNumber *baselineOffset =
                            [self baselineOffsetForLineHeight:lineHeight
                                                         font:font];

                        [ranges addObject:[NSValue valueWithRange:attrsRange]];
                        [offsets addObject:baselineOffset ?: [NSNull null]];
                      }];

  for (NSUInteger i = 0; i < ranges.count; i++) {
    NSRange attrsRange = [ranges[i] rangeValue];
    id baselineOffset = offsets[i];
    if (baselineOffset == [NSNull null]) {
      [textStorage removeAttribute:NSBaselineOffsetAttributeName
                             range:attrsRange];
    } else {
      [textStorage addAttribute:NSBaselineOffsetAttributeName
                          value:baselineOffset
                          range:attrsRange];
    }
  }
}

@end
