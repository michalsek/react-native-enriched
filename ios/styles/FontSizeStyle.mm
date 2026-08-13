#import "EnrichedTextInputView.h"
#import "FontExtension.h"
#import "StyleHeaders.h"

@implementation FontSizeStyle

+ (StyleType)getType {
  return FontSize;
}

- (NSString *)getKey {
  return @"EnrichedFontSize";
}

- (void)applyStyling:(NSRange)range {
  NSString *value = [self getValueAt:range.location];
  if (value == nullptr) {
    return;
  }
  CGFloat fontSize = [self scaledValue:value];
  if (fontSize <= 0) {
    return;
  }

  [self.host.textView.textStorage
      enumerateAttribute:NSFontAttributeName
                 inRange:range
                 options:0
              usingBlock:^(id _Nullable value, NSRange fontRange,
                           BOOL *_Nonnull stop) {
                UIFont *font = (UIFont *)value;
                if (font != nullptr) {
                  [self.host.textView.textStorage
                      addAttribute:NSFontAttributeName
                             value:[font setSize:fontSize]
                             range:fontRange];
                }
              }];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSString *value = attributes[[self getKey]];
  UIFont *font = attributes[NSFontAttributeName];
  if (value == nullptr || font == nullptr) {
    return;
  }
  CGFloat fontSize = [self scaledValue:value];
  if (fontSize <= 0) {
    return;
  }
  attributes[NSFontAttributeName] = [font setSize:fontSize];
}

@end
