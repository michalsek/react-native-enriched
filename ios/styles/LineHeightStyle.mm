#import "EnrichedTextInputView.h"
#import "StyleHeaders.h"

@implementation LineHeightStyle

+ (StyleType)getType {
  return LineHeight;
}

- (NSString *)getKey {
  return @"EnrichedLineHeight";
}

- (void)applyStyling:(NSRange)range {
  NSString *value = [self getValueAt:range.location];
  if (value == nullptr) {
    return;
  }
  CGFloat lineHeight = [self scaledValue:value];
  if (lineHeight <= 0) {
    return;
  }

  [self.host.textView.textStorage
      enumerateAttribute:NSParagraphStyleAttributeName
                 inRange:range
                 options:0
              usingBlock:^(id _Nullable value, NSRange subRange,
                           BOOL *_Nonnull stop) {
                NSMutableParagraphStyle *pStyle =
                    [(NSParagraphStyle *)value mutableCopy];
                if (pStyle == nullptr) {
                  pStyle = [[NSMutableParagraphStyle alloc] init];
                }
                pStyle.minimumLineHeight = lineHeight;
                [self.host.textView.textStorage
                    addAttribute:NSParagraphStyleAttributeName
                           value:pStyle
                           range:subRange];
              }];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSString *value = attributes[[self getKey]];
  if (value == nullptr) {
    return;
  }
  CGFloat lineHeight = [self scaledValue:value];
  if (lineHeight <= 0) {
    return;
  }

  NSMutableParagraphStyle *pStyle =
      [attributes[NSParagraphStyleAttributeName] mutableCopy];
  if (pStyle == nullptr) {
    pStyle = [[NSMutableParagraphStyle alloc] init];
  }
  pStyle.minimumLineHeight = lineHeight;
  attributes[NSParagraphStyleAttributeName] = pStyle;
}

@end
