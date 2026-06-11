#import "EnrichedTextInputView.h"
#import "FontExtension.h"
#import "StyleHeaders.h"

@implementation FontFamilyStyle

+ (StyleType)getType {
  return FontFamily;
}

- (NSString *)getKey {
  return @"EnrichedFontFamily";
}

- (void)applyStyling:(NSRange)range {
  NSString *family = [self getValueAt:range.location];
  if (family == nullptr) {
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
                  UIFont *newFont = [font setFamily:family];
                  [self.host.textView.textStorage
                      addAttribute:NSFontAttributeName
                             value:newFont
                             range:fontRange];
                }
              }];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSString *family = attributes[[self getKey]];
  UIFont *font = attributes[NSFontAttributeName];
  if (family == nullptr || font == nullptr) {
    return;
  }
  attributes[NSFontAttributeName] = [font setFamily:family];
}

@end
