#import "EnrichedTextInputView.h"
#import "LineHeightUtils.h"
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
                [LineHeightUtils applyLineHeight:lineHeight
                                toParagraphStyle:pStyle];
                [self.host.textView.textStorage
                    addAttribute:NSParagraphStyleAttributeName
                           value:pStyle
                           range:subRange];
              }];
  [LineHeightUtils
      applyBaselineOffsetsInTextStorage:self.host.textView.textStorage
                                  range:range];
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
  [LineHeightUtils applyLineHeight:lineHeight toParagraphStyle:pStyle];
  attributes[NSParagraphStyleAttributeName] = pStyle;
  [LineHeightUtils applyBaselineOffsetToAttributes:attributes];
}

@end
